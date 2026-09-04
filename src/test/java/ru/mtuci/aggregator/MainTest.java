package ru.mtuci.aggregator;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @TempDir Path tempDir;

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMainWithInvalidApis() {
        String[] args = {
            "--apis", "invalid_api",
            "--format", "json",
            "--file", tempDir.resolve("main_test.json").toString(),
            "--threads", "1"
        };
        
        assertDoesNotThrow(() -> Main.main(args));
    }
}