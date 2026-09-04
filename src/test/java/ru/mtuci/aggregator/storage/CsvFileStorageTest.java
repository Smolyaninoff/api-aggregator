package ru.mtuci.aggregator.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.mtuci.aggregator.model.AggregatedRecord;
import ru.mtuci.aggregator.storage.impl.CsvFileStorage;

class CsvFileStorageTest {
    @TempDir Path tempDir;

    @Test
    void testSaveAndReadCsv() throws Exception {
        CsvFileStorage storage = new CsvFileStorage();
        Path file = tempDir.resolve("test.csv");
        AggregatedRecord rec = new AggregatedRecord("api", Map.of("field", "value"));

        storage.init(file, false);
        storage.appendRecord(rec);
        storage.close();

        List<AggregatedRecord> read = storage.read(file);
        assertEquals(1, read.size());
        assertEquals("api", read.get(0).getSource());
    }

    @Test
    void testHeaderNotDuplicatedOnAppend() throws Exception {
        CsvFileStorage storage = new CsvFileStorage();
        Path file = tempDir.resolve("headers.csv");

        storage.init(file, false);
        storage.appendRecord(new AggregatedRecord("a", Map.of("x", 1)));
        storage.close();

        storage.init(file, true);
        storage.appendRecord(new AggregatedRecord("b", Map.of("y", 2)));
        storage.close();

        List<String> lines = java.nio.file.Files.readAllLines(file);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("id,source,timestamp"));
    }

    @Test
    void testReadNonExistentCsv() throws Exception {
        CsvFileStorage storage = new CsvFileStorage();
        Path file = tempDir.resolve("nonexistent.csv");

        List<AggregatedRecord> records = storage.read(file);
        assertTrue(records.isEmpty());
    }

    @Test
    void testEmptyCsvFile() throws Exception {
        CsvFileStorage storage = new CsvFileStorage();
        Path file = tempDir.resolve("empty.csv");

        storage.init(file, false);
        storage.close();

        List<AggregatedRecord> records = storage.read(file);
        assertTrue(records.isEmpty());
    }

    @Test
    void testGetFormatName() {
        CsvFileStorage storage = new CsvFileStorage();
        assertEquals("csv", storage.getFormatName());
    }

    @Test
    void testReadBySource() throws Exception {
        CsvFileStorage storage = new CsvFileStorage();
        Path file = tempDir.resolve("filter.csv");

        storage.init(file, false);
        storage.appendRecord(new AggregatedRecord("api1", Map.of()));
        storage.appendRecord(new AggregatedRecord("api2", Map.of()));
        storage.appendRecord(new AggregatedRecord("api1", Map.of()));
        storage.close();

        List<AggregatedRecord> filtered = storage.readBySource(file, "api1");
        assertEquals(2, filtered.size());
    }

    @Test
    void testMultipleFields() throws Exception {
        CsvFileStorage storage = new CsvFileStorage();
        Path file = tempDir.resolve("fields.csv");

        Map<String, Object> data = Map.of(
            "field1", "value1",
            "field2", 123,
            "nested.field", "nested_value"
        );

        storage.init(file, false);
        storage.appendRecord(new AggregatedRecord("api", data));
        storage.close();

        List<String> lines = java.nio.file.Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("field1"));
        assertTrue(lines.get(0).contains("field2"));
        assertTrue(lines.get(0).contains("nested.field"));
    }
}