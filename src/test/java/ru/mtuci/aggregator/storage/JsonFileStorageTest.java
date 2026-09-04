package ru.mtuci.aggregator.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.mtuci.aggregator.model.AggregatedRecord;
import ru.mtuci.aggregator.storage.impl.JsonFileStorage;

class JsonFileStorageTest {
    @TempDir Path tempDir;

    @Test
    void testSaveAndRead() throws Exception {
        JsonFileStorage storage = new JsonFileStorage();
        Path file = tempDir.resolve("test.json");
        AggregatedRecord rec = new AggregatedRecord("api1", Map.of("val", 1));

        storage.init(file, false);
        storage.appendRecord(rec);
        storage.close();

        List<AggregatedRecord> read = storage.read(file);
        assertEquals(1, read.size());
        assertEquals("api1", read.get(0).getSource());
    }

    @Test
    void testAppendMode() throws Exception {
        JsonFileStorage storage = new JsonFileStorage();
        Path file = tempDir.resolve("append.json");

        storage.init(file, false);
        storage.appendRecord(new AggregatedRecord("api1", Map.of()));
        storage.close();

        storage.init(file, true);
        storage.appendRecord(new AggregatedRecord("api2", Map.of()));
        storage.close();

        assertEquals(2, storage.read(file).size());
    }

    @Test
    void testReadNonExistentFile() throws Exception {
        JsonFileStorage storage = new JsonFileStorage();
        Path file = tempDir.resolve("nonexistent.json");

        List<AggregatedRecord> records = storage.read(file);
        assertTrue(records.isEmpty());
    }

    @Test
    void testSaveEmptyRecord() throws Exception {
        JsonFileStorage storage = new JsonFileStorage();
        Path file = tempDir.resolve("empty.json");

        storage.init(file, false);
        storage.appendRecord(new AggregatedRecord("api", Map.of()));
        storage.close();

        List<AggregatedRecord> records = storage.read(file);
        assertEquals(1, records.size());
        assertTrue(records.get(0).getData().isEmpty());
    }

    @Test
    void testMultipleRecords() throws Exception {
        JsonFileStorage storage = new JsonFileStorage();
        Path file = tempDir.resolve("multi.json");

        storage.init(file, false);
        for (int i = 0; i < 5; i++) {
            storage.appendRecord(new AggregatedRecord("api" + i, Map.of("index", i)));
        }
        storage.close();

        List<AggregatedRecord> records = storage.read(file);
        assertEquals(5, records.size());
    }

    @Test
    void testReadBySource() throws Exception {
        JsonFileStorage storage = new JsonFileStorage();
        Path file = tempDir.resolve("filter.json");

        storage.init(file, false);
        storage.appendRecord(new AggregatedRecord("api1", Map.of()));
        storage.appendRecord(new AggregatedRecord("api2", Map.of()));
        storage.appendRecord(new AggregatedRecord("api1", Map.of()));
        storage.close();

        List<AggregatedRecord> filtered = storage.readBySource(file, "api1");
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().allMatch(r -> r.getSource().equals("api1")));
    }

    @Test
    void testGetFormatName() {
        JsonFileStorage storage = new JsonFileStorage();
        assertEquals("json", storage.getFormatName());
    }
}