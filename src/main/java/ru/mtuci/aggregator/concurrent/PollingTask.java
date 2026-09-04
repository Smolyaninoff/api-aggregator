package ru.mtuci.aggregator.concurrent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.mtuci.aggregator.api.ApiProvider;
import ru.mtuci.aggregator.model.AggregatedRecord;
import ru.mtuci.aggregator.parser.JsonResponseParser;
import ru.mtuci.aggregator.storage.DataStorage;

public class PollingTask implements Runnable {
    private final ApiProvider provider;
    private final DataStorage storage;
    private final int intervalSeconds;
    private final AtomicBoolean isRunning;

    public PollingTask(ApiProvider provider, DataStorage storage, int intervalSeconds, AtomicBoolean isRunning) {
        this.provider = provider;
        this.storage = storage;
        this.intervalSeconds = intervalSeconds;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            long startTime = System.currentTimeMillis();
            try {
                System.out.println("[" + Thread.currentThread().getName() + "] Запрос к " + provider.getName() + "...");
                String rawJson = provider.fetchRaw(provider.getDefaultParams());

                Map<String, Object> data = JsonResponseParser.parse(rawJson);
                
                AggregatedRecord record = new AggregatedRecord(provider.getName(), data);
                
                storage.appendRecord(record);
                System.out.println("[" + Thread.currentThread().getName() + "] Сохранено: " + provider.getName());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(" [" + Thread.currentThread().getName() + "] Поток прерван.");
                break;
            } catch (Exception e) {
                System.err.println("[" + Thread.currentThread().getName() + "] Ошибка в задаче " + provider.getName() + ": " + e.getMessage());
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                long sleepTime = Math.max(0, (intervalSeconds * 1000L) - duration);
                
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}