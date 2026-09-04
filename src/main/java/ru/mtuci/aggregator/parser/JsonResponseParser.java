package ru.mtuci.aggregator.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;

public class JsonResponseParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String json) throws IOException {
        return MAPPER.readValue(json, Map.class);
    }
}