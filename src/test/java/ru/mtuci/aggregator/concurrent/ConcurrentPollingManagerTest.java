package ru.mtuci.aggregator.concurrent;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import ru.mtuci.aggregator.config.AppConfig;
import ru.mtuci.aggregator.storage.impl.JsonFileStorage;

class ConcurrentPollingManagerTest {
    @TempDir Path tempDir;
    private JsonFileStorage storage;
    private AppConfig config;

    @BeforeEach
    void setUp() {
        storage = new JsonFileStorage();
        config = new AppConfig();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testManagerCreation() {
        ConcurrentPollingManager manager = new ConcurrentPollingManager(storage, config);
        
        assertNotNull(manager);
        assertFalse(manager.isRunning());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testStartAndStopWithInvalidApi() throws Exception {
        ConcurrentPollingManager manager = new ConcurrentPollingManager(storage, config);
        Path file = tempDir.resolve("test.json");
        
        manager.start(2, 1, List.of("non_existent_api"), file, false);
        assertTrue(manager.isRunning());

        manager.stop();
        assertFalse(manager.isRunning());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testStopWhenNotRunning() {
        ConcurrentPollingManager manager = new ConcurrentPollingManager(storage, config);
        
        assertDoesNotThrow(() -> manager.stop());
        assertFalse(manager.isRunning());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testStartWhenAlreadyRunning() throws Exception {
        ConcurrentPollingManager manager = new ConcurrentPollingManager(storage, config);
        Path file = tempDir.resolve("running.json");
        
        manager.start(1, 5, List.of("invalid_api"), file, false);
        assertTrue(manager.isRunning());
        
        manager.start(1, 5, List.of("invalid_api"), file, false);
        assertTrue(manager.isRunning());
        
        manager.stop();
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMultipleInvalidApis() throws Exception {
        ConcurrentPollingManager manager = new ConcurrentPollingManager(storage, config);
        Path file = tempDir.resolve("multi.json");
        
        manager.start(2, 1, List.of(
            "invalid_api_1",
            "invalid_api_2",
            "invalid_api_3"
        ), file, false);
        
        assertTrue(manager.isRunning());
        
        Thread.sleep(3000);
        
        manager.stop();
        assertFalse(manager.isRunning());
    }

}