package ru.mtuci.aggregator.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class ConsoleAppTest {
    @TempDir Path tempDir;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        outContent.reset();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAutomaticModeWithInvalidApis() {
        ConsoleApp app = new ConsoleApp();
        String[] args = {
            "--apis", "invalid_api_1,invalid_api_2",
            "--format", "json",
            "--file", tempDir.resolve("test.json").toString(),
            "--threads", "1"
        };
        
        assertDoesNotThrow(() -> app.start(args));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAutomaticModeWithEmptyApis() {
        ConsoleApp app = new ConsoleApp();
        String[] args = {
            "--apis", "invalid_api_1,invalid_api_2",
            "--format", "json",
            "--file", tempDir.resolve("empty.json").toString(),
            "--threads", "1"
        };
        
        assertDoesNotThrow(() -> app.start(args));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInvalidThreadCount() {
        ConsoleApp app = new ConsoleApp();
        String[] args = {
            "--apis", "invalid_api",
            "--format", "json",
            "--file", tempDir.resolve("test.json").toString(),
            "--threads", "-1"
        };
        
        assertDoesNotThrow(() -> app.start(args));
    }
}