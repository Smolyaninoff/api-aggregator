package ru.mtuci.aggregator.concurrent;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.mtuci.aggregator.api.ApiProvider;
import ru.mtuci.aggregator.config.AppConfig;
import ru.mtuci.aggregator.storage.DataStorage;

public class ConcurrentPollingManager {
    private ExecutorService executor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final DataStorage storage;
    private final AppConfig config;

    public ConcurrentPollingManager(DataStorage storage, AppConfig config) {
        this.storage = storage;
        this.config = config;
    }

    public void start(int maxThreads, int interval, List<String> apiNames, Path filePath, boolean append) {
        if (isRunning.get()) {
            System.out.println("️ Опрос уже запущен.");
            return;
        }

        System.out.println(" Запуск опроса: n=" + maxThreads + ", t=" + interval + "s");
        System.out.println("Нажмите 'q' для остановки опроса\n");
        
        executor = Executors.newFixedThreadPool(maxThreads);
        isRunning.set(true);

        try {
            storage.init(filePath, append);
        } catch (Exception e) {
            System.err.println("Ошибка инициализации хранилища: " + e.getMessage());
            isRunning.set(false);
            executor.shutdown();
            return;
        }

        for (String name : apiNames) {
            ApiProvider provider = config.getProvider(name);
            if (provider != null) {
                executor.submit(new PollingTask(provider, storage, interval, isRunning));
            } else {
                System.err.println("API '" + name + "' не найден в конфиге.");
            }
        }
    }

    public void waitForStopSignal() {
        Scanner scanner = new Scanner(System.in);
        while (isRunning.get()) {
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("q")) {
                    System.out.println("\nПолучен сигнал остановки. Завершаем опрос...");
                    stop();
                    break;
                }
            }
        }
    }

    public void stop() {
        if (!isRunning.get()) return;

        System.out.println(" Остановка опроса...");
        isRunning.set(false);

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Принудительное завершение потоков.");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Не удалось завершить все потоки");
                }
            }
            
            storage.close();
            System.out.println("Опрос остановлен, ресурсы освобождены.");
            
        } catch (InterruptedException e) {
            executor.shutdownNow();
            System.err.println("Процесс остановки был прерван");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Ошибка при закрытии ресурсов: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}