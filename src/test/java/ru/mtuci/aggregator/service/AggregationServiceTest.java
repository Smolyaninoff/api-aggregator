package ru.mtuci.aggregator.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import ru.mtuci.aggregator.config.AppConfig;
import ru.mtuci.aggregator.testutil.MockApiProvider;

class AggregationServiceTest {
    @TempDir Path tempDir;
    private AggregationService service;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig(List.of(
                new MockApiProvider("jsonplaceholder", "{\"id\":1,\"name\":\"Test User\"}"),
                new MockApiProvider("mock_api", "{\"value\":42}"),
                new MockApiProvider("failing_api", "", true)
        ));
        service = new AggregationService(config);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateJsonWithValidApi() throws Exception {
        Path file = tempDir.resolve("test.json");

        service.aggregate(
            List.of("jsonplaceholder"),
            "json",
            file,
            false,
            1
        );

        assertTrue(Files.exists(file));
        String content = Files.readString(file);
        assertTrue(content.contains("jsonplaceholder"));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateJsonWithInvalidApi() throws Exception {
        Path file = tempDir.resolve("invalid.json");

        assertDoesNotThrow(() ->
            service.aggregate(
                List.of("non_existent_api"),
                "json",
                file,
                false,
                1
            )
        );

    }


    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateJsonWithMultipleThreads() throws Exception {
        Path file = tempDir.resolve("threads.json");

        service.aggregate(
            List.of("jsonplaceholder", "mock_api", "failing_api"),
            "json",
            file,
            false,
            3
        );

        assertTrue(Files.exists(file));
    }


    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateJsonWithEmptyApiList() throws Exception {
        Path file = tempDir.resolve("empty.json");

        service.aggregate(
            List.of(),
            "json",
            file,
            false,
            1
        );

    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateCsvWithValidApi() throws Exception {
        Path file = tempDir.resolve("test.csv");

        service.aggregate(
            List.of("jsonplaceholder"),
            "csv",
            file,
            false,
            1
        );

        assertTrue(Files.exists(file));
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.size() >= 2); // заголовок + данные
        assertTrue(lines.get(0).contains("id,source,timestamp"));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateCsvWithInvalidApi() throws Exception {
        Path file = tempDir.resolve("invalid.csv");

        assertDoesNotThrow(() ->
            service.aggregate(
                List.of("non_existent_api"),
                "csv",
                file,
                false,
                1
            )
        );
    }


    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateWithMockProvider() throws Exception {
        Path file = tempDir.resolve("mock.json");

        service.aggregate(
            List.of("mock_api"),
            "json",
            file,
            false,
            1
        );

        assertTrue(Files.exists(file));
        String content = Files.readString(file);
        assertTrue(content.contains("mock_api"));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateCaseInsensitiveApiNames() throws Exception {
        Path file = tempDir.resolve("case.json");

        service.aggregate(
            List.of("JSONPLACEHOLDER", "Mock_Api"),
            "json",
            file,
            false,
            2
        );

        assertTrue(Files.exists(file));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateWithLargeThreadCount() throws Exception {
        Path file = tempDir.resolve("large_threads.json");

        service.aggregate(
            List.of("jsonplaceholder"),
            "json",
            file,
            false,
            10
        );

        assertTrue(Files.exists(file));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAggregateWithSingleThread() throws Exception {
        Path file = tempDir.resolve("single.json");

        service.aggregate(
            List.of("jsonplaceholder", "mock_api"),
            "json",
            file,
            false,
            1
        );

        assertTrue(Files.exists(file));
    }
}
