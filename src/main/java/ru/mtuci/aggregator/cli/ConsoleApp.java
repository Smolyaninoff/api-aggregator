package ru.mtuci.aggregator.cli;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import ru.mtuci.aggregator.config.AppConfig;
import ru.mtuci.aggregator.model.AggregatedRecord;
import ru.mtuci.aggregator.service.AggregationService;
import ru.mtuci.aggregator.storage.DataStorage;
import ru.mtuci.aggregator.storage.impl.CsvFileStorage;
import ru.mtuci.aggregator.storage.impl.JsonFileStorage;

public class ConsoleApp {
    private final AggregationService service = new AggregationService();
    private final AppConfig config = new AppConfig();

    public void start(String[] args) {
        if (args.length > 0) {
            runAutomatic(args);
        } else {
            runInteractive();
        }
    }

    private void runInteractive() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Доступные API: " + String.join(",", config.getAllProviders().keySet()));
        
        while (true) {
            System.out.println("\n=== МЕНЮ ===");
            System.out.println("1. Запустить опрос");
            System.out.println("2. Показать содержимое файла");
            System.out.println("3. Выход");
            System.out.print("Выберите действие: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("3")) {
                break;
            }

            if (choice.equals("2")) {
                showFileContents(scanner);
                continue;
            }

            if (!choice.equals("1")) {
                System.out.println("️ Неверный выбор. Попробуйте снова.");
                continue;
            }

            System.out.print("Введите имена API через запятую: ");
            String apisInput = scanner.nextLine().trim();
            List<String> apis = apisInput.isEmpty() 
                ? List.of("jsonplaceholder", "ip-api", "open-meteo")
                : Arrays.asList(apisInput.split(","));
            
            System.out.print("Формат вывода (json/csv): ");
            String format = scanner.nextLine().trim().toLowerCase();
            if (!format.equals("json") && !format.equals("csv")) {
                System.out.println("Неверный формат. Используется json.");
                format = "json";
            }
            
            System.out.print("Имя файла: ");
            String file = scanner.nextLine().trim();
            if (file.isEmpty()) file = "output." + format;

            System.out.print("Режим записи (1 - новый файл, 2 - дозаписать в существующий): ");
            boolean append = scanner.nextLine().trim().equals("2");

            System.out.print("Макс. потоков (n): ");
            int threads = 3;
            try {
                threads = Integer.parseInt(scanner.nextLine().trim());
                if (threads <= 0) threads = 3;
            } catch (NumberFormatException e) {
                System.out.println("Неверное значение. Используется 3.");
            }
            
            System.out.print("Интервал опроса в сек (t, 0 для однократного): ");
            int interval = 0;
            try {
                interval = Integer.parseInt(scanner.nextLine().trim());
                if (interval < 0) interval = 0;
            } catch (NumberFormatException e) {
                System.out.println("Неверное значение. Используется 0.");
            }
            
            if (interval == 0) {
                System.out.println("Запуск однократного опроса...");
                service.aggregate(apis, format, Paths.get(file), append, threads);
            } else {
                System.out.println(" Запуск непрерывного опроса (интервал: " + interval + "с)...");
                service.startContinuous(apis, format, Paths.get(file), append, threads, interval);
            }
            
            System.out.println("\nОпрос завершён. Возврат в меню");
        }
        
        scanner.close();
    }

    private void showFileContents(Scanner scanner) {
        System.out.print("Имя файла: ");
        String file = scanner.nextLine().trim();
        if (file.isEmpty()) {
            System.out.println("Имя файла не может быть пустым.");
            return;
        }

        System.out.print("Формат файла (json/csv): ");
        String format = scanner.nextLine().trim().toLowerCase();
        DataStorage storage = "csv".equals(format) ? new CsvFileStorage() : new JsonFileStorage();

        System.out.print("Имя API для фильтрации (Enter — показать все): ");
        String apiName = scanner.nextLine().trim();

        try {
            List<AggregatedRecord> records = apiName.isEmpty()
                    ? storage.read(Paths.get(file))
                    : storage.readBySource(Paths.get(file), apiName);

            if (records.isEmpty()) {
                System.out.println("Записей не найдено.");
                return;
            }

            for (AggregatedRecord record : records) {
                System.out.printf("[%s] %s (%s): %s%n",
                        record.getId(), record.getSource(), record.getTimestamp(), record.getData());
            }
        } catch (Exception e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        }
    }

    private void runAutomatic(String[] args) {
        List<String> apis = List.of();
        String format = "json";
        String file = "output.json";
        int threads = 3;
        int interval = 5;
        boolean continuous = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--apis" -> { if (i + 1 < args.length) apis = Arrays.asList(args[++i].split(",")); }
                case "--format" -> { if (i + 1 < args.length) format = args[++i]; }
                case "--file" -> { if (i + 1 < args.length) file = args[++i]; }
                case "--threads" -> { 
                    if (i + 1 < args.length) {
                        int parsed = Integer.parseInt(args[++i]);
                        threads = (parsed > 0) ? parsed : 3;
                    }
                }
                case "--interval" -> { 
                    if (i + 1 < args.length) {
                        int parsed = Integer.parseInt(args[++i]);
                        interval = (parsed > 0) ? parsed : 5;
                    }
                }
                case "--continuous" -> continuous = true;
            }
        }
        
        if (apis.isEmpty()) {
            apis = List.of("jsonplaceholder", "ip-api", "open-meteo");
        }

        if (continuous) {
            System.out.println("Непрерывный режим: n=" + threads + ", t=" + interval + "s");
            System.out.println(" Нажмите 'q' для остановки...\n");
            service.startContinuous(apis, format, Paths.get(file), false, threads, interval);
        } else {
            System.out.println("Однократный параллельный режим: n=" + threads);
            service.aggregate(apis, format, Paths.get(file), false, threads);
        }
    }
}