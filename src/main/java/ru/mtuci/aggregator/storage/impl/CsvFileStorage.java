package ru.mtuci.aggregator.storage.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import ru.mtuci.aggregator.model.AggregatedRecord;
import ru.mtuci.aggregator.storage.DataStorage;

public class CsvFileStorage implements DataStorage {
    private BufferedWriter writer;
    private Set<String> headers;
    private Path currentPath;
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public String getFormatName() {
        return "csv";
    }

    @Override
    public void init(Path path, boolean append) throws IOException {
        lock.lock();
        try {
            headers = new LinkedHashSet<>();
            headers.add("id");
            headers.add("source");
            headers.add("timestamp");
            currentPath = path;

            if (append && Files.exists(path) && Files.size(path) > 0) {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    headers.clear();
                    headers.addAll(Arrays.asList(lines.get(0).split(",", -1)));
                }
                writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } else {
                writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void appendRecord(AggregatedRecord record) throws IOException {
        lock.lock();
        try {
            Set<String> recordHeaders = new LinkedHashSet<>();
            collectHeadersToSet(record.getData(), "", recordHeaders);

            boolean hasNewHeaders = false;
            for (String h : recordHeaders) {
                if (!headers.contains(h)) {
                    hasNewHeaders = true;
                    break;
                }
            }

            if (hasNewHeaders && Files.exists(currentPath) && Files.size(currentPath) > 0) {
                rewriteWithNewHeadersStreaming(recordHeaders);
            } else if (Files.size(currentPath) == 0) {
                collectHeaders(record.getData(), "");
                writer.write(String.join(",", headers));
                writer.newLine();
            }
            writeRow(record);
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds newly discovered columns to the header set and rewrites the existing rows
     * with the widened column set, streaming line-by-line so the whole file is never
     * held in memory at once (unlike re-parsing it into a List<AggregatedRecord>).
     */
    private void rewriteWithNewHeadersStreaming(Set<String> recordHeaders) throws IOException {
        List<String> oldHeaderList = new ArrayList<>(headers);
        for (String h : recordHeaders) {
            headers.add(h);
        }

        if (writer != null) {
            writer.close();
        }

        Path tempPath = currentPath.resolveSibling(currentPath.getFileName().toString() + ".tmp");
        try (BufferedReader reader = Files.newBufferedReader(currentPath, StandardCharsets.UTF_8);
             BufferedWriter tempWriter = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            reader.readLine(); // skip old header line, a widened one is written below
            tempWriter.write(String.join(",", headers));
            tempWriter.newLine();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] oldVals = line.split(",", -1);
                String[] newVals = new String[headers.size()];
                Arrays.fill(newVals, "");
                for (int i = 0; i < oldHeaderList.size() && i < oldVals.length; i++) {
                    Integer idx = getIndex(oldHeaderList.get(i));
                    if (idx != null) newVals[idx] = oldVals[i];
                }
                tempWriter.write(String.join(",", newVals));
                tempWriter.newLine();
            }
        }

        Files.move(tempPath, currentPath, StandardCopyOption.REPLACE_EXISTING);
        writer = Files.newBufferedWriter(currentPath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private void writeRow(AggregatedRecord record) throws IOException {
        String[] values = new String[headers.size()];
        Arrays.fill(values, "");

        Integer idIdx = getIndex("id");
        Integer sourceIdx = getIndex("source");
        Integer timestampIdx = getIndex("timestamp");

        if (idIdx != null) values[idIdx] = record.getId();
        if (sourceIdx != null) values[sourceIdx] = record.getSource();
        if (timestampIdx != null) values[timestampIdx] = record.getTimestamp().toString();

        Map<String, String> flat = new HashMap<>();
        flattenToMap(record.getData(), "", flat);

        for (Map.Entry<String, String> e : flat.entrySet()) {
            Integer idx = getIndex(e.getKey());
            if (idx != null) {
                values[idx] = e.getValue().replace(",", ";");
            }
        }

        writer.write(String.join(",", values));
        writer.newLine();
    }

    private Integer getIndex(String key) {
        int idx = 0;
        for (String h : headers) {
            if (h.equals(key)) return idx;
            idx++;
        }
        return null;
    }

    private void collectHeadersToSet(Map<String, Object> map, String prefix, Set<String> set) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            if (e.getValue() instanceof Map) {
                collectHeadersToSet((Map<String, Object>) e.getValue(), key, set);
            } else {
                set.add(key);
            }
        }
    }

    private void collectHeaders(Map<String, Object> map, String prefix) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            if (e.getValue() instanceof Map) {
                collectHeaders((Map<String, Object>) e.getValue(), key);
            } else {
                headers.add(key);
            }
        }
    }

    private void flattenToMap(Map<String, Object> map, String prefix, Map<String, String> result) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            if (e.getValue() instanceof Map) {
                flattenToMap((Map<String, Object>) e.getValue(), key, result);
            } else if (e.getValue() instanceof List) {
                result.put(key, e.getValue().toString().replace(",", ";"));
            } else {
                result.put(key, e.getValue() == null ? "" : e.getValue().toString().replace(",", ";"));
            }
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            if (writer != null) writer.close();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<AggregatedRecord> read(Path path) throws IOException {
        if (!Files.exists(path)) return new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() <= 1) return new ArrayList<>();

        String[] hdrs = lines.get(0).split(",", -1);
        List<AggregatedRecord> records = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] vals = lines.get(i).split(",", -1);
            Map<String, Object> data = new HashMap<>();
            String id = vals.length > 0 ? vals[0] : "";
            String source = vals.length > 1 ? vals[1] : "";
            String ts = vals.length > 2 ? vals[2] : "";

            for (int j = 3; j < hdrs.length && j < vals.length; j++) {
                if (!vals[j].isEmpty()) setNestedValue(data, hdrs[j], vals[j]);
            }
            records.add(new AggregatedRecord(id, source, ts.isEmpty() ? Instant.now() : Instant.parse(ts), data));
        }
        return records;
    }

    @Override
    public List<AggregatedRecord> readBySource(Path path, String source) throws IOException {
        return read(path).stream()
                .filter(r -> r.getSource().equalsIgnoreCase(source))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String key, String value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
        }
        current.put(parts[parts.length - 1], parseValue(value));
    }

    private Object parseValue(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            if (value.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException ignored) {}
        return value;
    }
}