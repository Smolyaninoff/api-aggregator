package ru.mtuci.aggregator.storage.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ru.mtuci.aggregator.model.AggregatedRecord;
import ru.mtuci.aggregator.storage.DataStorage;

public class JsonFileStorage implements DataStorage {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private BufferedWriter writer;
    private boolean isFirstRecord = true;
    private final ReentrantLock lock = new ReentrantLock();

    @Override 
    public String getFormatName() { 
        return "json"; 
    }

    @Override
    public void init(Path path, boolean append) throws IOException {
        lock.lock();
        try {
            boolean isNew = !append || !Files.exists(path) || Files.size(path) == 0;
            
            if (isNew) {
                writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                writer.write("[\n");
                isFirstRecord = true;
            } else {
                String content = Files.readString(path, StandardCharsets.UTF_8).trim();

                if (content.endsWith("]")) {
                    content = content.substring(0, content.length() - 1).trim();
                }
                
                Files.writeString(path, content, StandardCharsets.UTF_8, 
                    StandardOpenOption.TRUNCATE_EXISTING);
                
                writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, 
                    StandardOpenOption.APPEND);
                
                isFirstRecord = false;
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void appendRecord(AggregatedRecord record) throws IOException {
        lock.lock();
        try {
            if (writer == null) {
                throw new IllegalStateException("Storage not initialized. Call init() first.");
            }
            
            if (!isFirstRecord) {
                writer.write(",\n");
            }
            writer.write(MAPPER.writeValueAsString(record));
            writer.flush();
            isFirstRecord = false;
        } finally {
            lock.unlock();
        }
    }
    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            if (writer != null) {
                writer.write("\n]");
                writer.flush();
                writer.close();
                writer = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<AggregatedRecord> read(Path path) throws IOException {
        if (!Files.exists(path)) return new ArrayList<>();
        String json = Files.readString(path, StandardCharsets.UTF_8);
        if (json.isBlank()) return new ArrayList<>();
        AggregatedRecord[] records = MAPPER.readValue(json, AggregatedRecord[].class);
        return new ArrayList<>(List.of(records));
    }

    @Override
    public List<AggregatedRecord> readBySource(Path path, String source) throws IOException {
        return read(path).stream()
                .filter(r -> r.getSource().equalsIgnoreCase(source))
                .collect(Collectors.toList());
    }
}