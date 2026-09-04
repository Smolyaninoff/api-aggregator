package ru.mtuci.aggregator.parser;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class JsonResponseParserTest {
    @Test
    void testParseValidJson() throws IOException {
        String json = "{\"name\":\"Alice\",\"age\":30}";
        Map<String, Object> result = JsonResponseParser.parse(json);
        
        assertEquals("Alice", result.get("name"));
        assertEquals(30, result.get("age"));
    }

    @Test
    void testParseNestedJson() throws IOException {
        String json = "{\"user\":{\"name\":\"Bob\",\"address\":{\"city\":\"Moscow\"}}}";
        Map<String, Object> result = JsonResponseParser.parse(json);
        
        assertTrue(result.get("user") instanceof Map);
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("Bob", user.get("name"));
        
        Map<String, Object> address = (Map<String, Object>) user.get("address");
        assertEquals("Moscow", address.get("city"));
    }

    @Test
    void testParseJsonWithArray() throws IOException {
        String json = "{\"items\":[1,2,3]}";
        Map<String, Object> result = JsonResponseParser.parse(json);
        
        assertTrue(result.get("items") instanceof java.util.List);
        java.util.List<?> items = (java.util.List<?>) result.get("items");
        assertEquals(3, items.size());
    }

    @Test
    void testParseEmptyJson() throws IOException {
        String json = "{}";
        Map<String, Object> result = JsonResponseParser.parse(json);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseInvalidJsonThrowsException() {
        assertThrows(IOException.class, () -> JsonResponseParser.parse("{invalid}"));
    }

    @Test
    void testParseJsonWithDifferentTypes() throws IOException {
        String json = "{\"string\":\"text\",\"number\":42,\"boolean\":true,\"null\":null}";
        Map<String, Object> result = JsonResponseParser.parse(json);
        
        assertEquals("text", result.get("string"));
        assertEquals(42, result.get("number"));
        assertEquals(true, result.get("boolean"));
        assertNull(result.get("null"));
    }
}