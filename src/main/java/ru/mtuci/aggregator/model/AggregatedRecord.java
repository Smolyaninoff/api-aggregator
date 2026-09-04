package ru.mtuci.aggregator.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AggregatedRecord {
    private final String id;
    private final String source;
    private final Instant timestamp;
    private final Map<String, Object> data;

    @JsonCreator
    public AggregatedRecord(
            @JsonProperty("id") String id,
            @JsonProperty("source") String source,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("data") Map<String, Object> data) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.source = source;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.data = data;
    }

    public AggregatedRecord(String source, Map<String, Object> data) {
        this.id = UUID.randomUUID().toString();
        this.source = source;
        this.timestamp = Instant.now();
        this.data = data;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }
}