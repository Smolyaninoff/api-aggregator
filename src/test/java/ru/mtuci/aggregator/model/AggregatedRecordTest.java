package ru.mtuci.aggregator.model;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AggregatedRecordTest {
    @Test
    void testRecordCreation() {
        Map<String, Object> data = Map.of("temp", 25.0);
        AggregatedRecord record = new AggregatedRecord("test_api", data);

        assertNotNull(record.getId());
        assertEquals("test_api", record.getSource());
        assertNotNull(record.getTimestamp());
        assertEquals(data, record.getData());
    }

    @Test
    void testImmutability() {
        Map<String, Object> data = Map.of("key", "value");
        AggregatedRecord record = new AggregatedRecord("api", data);
        
        assertSame(data, record.getData());
    }

    @Test
    void testIdIsUnique() {
        AggregatedRecord record1 = new AggregatedRecord("api", Map.of());
        AggregatedRecord record2 = new AggregatedRecord("api", Map.of());
        
        assertNotEquals(record1.getId(), record2.getId());
    }

    @Test
    void testTimestampIsCurrent() {
        Instant before = Instant.now();
        AggregatedRecord record = new AggregatedRecord("api", Map.of());
        Instant after = Instant.now();
        
        assertTrue(record.getTimestamp().isAfter(before) || record.getTimestamp().equals(before));
        assertTrue(record.getTimestamp().isBefore(after) || record.getTimestamp().equals(after));
    }

    @Test
    void testDifferentSources() {
        AggregatedRecord record1 = new AggregatedRecord("api1", Map.of());
        AggregatedRecord record2 = new AggregatedRecord("api2", Map.of());
        
        assertEquals("api1", record1.getSource());
        assertEquals("api2", record2.getSource());
    }

    @Test
    void testComplexData() {
        Map<String, Object> complexData = Map.of(
            "string", "value",
            "number", 42,
            "nested", Map.of("key", "value")
        );
        
        AggregatedRecord record = new AggregatedRecord("api", complexData);
        
        assertEquals(complexData, record.getData());
        assertTrue(record.getData().containsKey("string"));
        assertTrue(record.getData().containsKey("nested"));
    }
}