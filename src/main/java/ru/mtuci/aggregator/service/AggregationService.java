package ru.mtuci.aggregator.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ru.mtuci.aggregator.api.ApiProvider;
import ru.mtuci.aggregator.concurrent.ConcurrentPollingManager;
import ru.mtuci.aggregator.config.AppConfig;
import ru.mtuci.aggregator.model.AggregatedRecord;
import ru.mtuci.aggregator.parser.JsonResponseParser;
import ru.mtuci.aggregator.storage.DataStorage;
import ru.mtuci.aggregator.storage.impl.CsvFileStorage;
import ru.mtuci.aggregator.storage.impl.JsonFileStorage;

public class AggregationService {
    private final AppConfig config;

    public AggregationService() {
        this(new AppConfig());
    }

    public AggregationService(AppConfig config) {
        this.config = config;
    }

    public void aggregate(List<String> apiNames, String format, Path filePath, boolean append, int maxThreads) {
        DataStorage storage = "csv".equalsIgnoreCase(format) ? new CsvFileStorage() : new JsonFileStorage();
        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

        try {
            storage.init(filePath, append);
            for (String name : apiNames) {
                ApiProvider provider = config.getProvider(name);
                if (provider == null) {
                    System.err.println("API '" + name + "' не найден.");
                    continue;
                }
                executor.submit(() -> {
                    try {
                        System.out.println("[" + Thread.currentThread().getName() + "] Запрос к " + provider.getName() + "...");
                        String rawJson = provider.fetchRaw(provider.getDefaultParams());
                        Map<String, Object> data = JsonResponseParser.parse(rawJson);
                        storage.appendRecord(new AggregatedRecord(provider.getName(), data));
                        System.out.println("[" + Thread.currentThread().getName() + "] Сохранено: " + provider.getName());
                    } catch (Exception e) {
                        System.err.println("[" + Thread.currentThread().getName() + "] Ошибка: " + e.getMessage());
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
            storage.close();
            System.out.println("Агрегация завершена.");
        } catch (Exception e) {
            System.err.println("Ошибка работы с файлом: " + e.getMessage());
        }
    }

    public void startContinuous(List<String> apiNames, String format, Path filePath, boolean append, int maxThreads, int interval) {
        DataStorage storage = "csv".equalsIgnoreCase(format) ? new CsvFileStorage() : new JsonFileStorage();
        ConcurrentPollingManager manager = new ConcurrentPollingManager(storage, config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nСигнал остановки. Завершаем потоки...");
            manager.stop();
        }));

        manager.start(maxThreads, interval, apiNames, filePath, append);
        manager.waitForStopSignal();
    }
}