package ru.mtuci.aggregator.storage;

import java.nio.file.Path;
import java.util.List;

import ru.mtuci.aggregator.model.AggregatedRecord;

public interface DataStorage {
    void init(Path path, boolean append) throws Exception;
    void appendRecord(AggregatedRecord record) throws Exception;
    void close() throws Exception;
    List<AggregatedRecord> read(Path path) throws Exception;
    List<AggregatedRecord> readBySource(Path path, String source) throws Exception;
    String getFormatName();
}