package ru.mtuci.aggregator.concurrent;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import ru.mtuci.aggregator.storage.impl.JsonFileStorage;
import ru.mtuci.aggregator.testutil.MockApiProvider;

class PollingTaskTest {
    @TempDir Path tempDir;
    private JsonFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new JsonFileStorage();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPollingTaskCreation() throws Exception {
        MockApiProvider provider = new MockApiProvider("test", "{\"key\":\"value\"}");
        AtomicBoolean isRunning = new AtomicBoolean(true);
        
        Path file = tempDir.resolve("create.json");
        storage.init(file, false);
        
        PollingTask task = new PollingTask(provider, storage, 1, isRunning);
        
        assertNotNull(task);
        
        storage.close();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPollingTaskStopsWhenFlagIsFalse() throws Exception {
        MockApiProvider provider = new MockApiProvider("test", "{\"key\":\"value\"}");
        AtomicBoolean isRunning = new AtomicBoolean(false);
        
        Path file = tempDir.resolve("stop.json");
        storage.init(file, false);
        
        PollingTask task = new PollingTask(provider, storage, 1, isRunning);
        
        CountDownLatch latch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            task.run();
            latch.countDown();
        });
        
        thread.start();
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(thread.isAlive());
        
        storage.close();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPollingTaskHandlesException() throws Exception {
        MockApiProvider failingProvider = new MockApiProvider("failing", "{\"error\":\"fail\"}", true);
        
        AtomicBoolean isRunning = new AtomicBoolean(true);
        
        Path file = tempDir.resolve("error.json");
        storage.init(file, false);
        
        PollingTask task = new PollingTask(failingProvider, storage, 1, isRunning);
        
        Thread thread = new Thread(task::run);
        thread.start();
        
        Thread.sleep(2000);
        
        isRunning.set(false);
        thread.join(1000);
        
        storage.close();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPollingTaskWithValidMockResponse() throws Exception {
        String mockJson = """
            {
                "latitude": 55.75,
                "longitude": 37.62,
                "current": {
                    "temperature": 25
                }
            }
            """;
        
        MockApiProvider provider = new MockApiProvider("mock", mockJson);
        AtomicBoolean isRunning = new AtomicBoolean(true);
        
        Path file = tempDir.resolve("valid.json");
        storage.init(file, false);
        
        PollingTask task = new PollingTask(provider, storage, 10, isRunning);
        
        Thread thread = new Thread(task::run);
        thread.start();
        
        Thread.sleep(1500);
        
        isRunning.set(false);
        thread.join(1000);
        
        storage.close();
    }
}