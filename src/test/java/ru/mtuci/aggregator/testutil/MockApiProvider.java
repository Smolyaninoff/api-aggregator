package ru.mtuci.aggregator.testutil;

import java.net.URI;
import java.util.Map;

import ru.mtuci.aggregator.api.ApiProvider;

public class MockApiProvider implements ApiProvider {
    private final String name;
    private final String mockResponse;
    private final URI uri;
    private boolean shouldFail = false;

    public MockApiProvider(String name, String mockResponse) {
        this.name = name;
        this.mockResponse = mockResponse;
        this.uri = URI.create("http://mock/" + name);
    }

    public MockApiProvider(String name, String mockResponse, boolean shouldFail) {
        this(name, mockResponse);
        this.shouldFail = shouldFail;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getDefaultParams() {
        return Map.of();
    }

    @Override
    public URI buildUri(Map<String, String> params) {
        return uri;
    }

    @Override
    public String fetchRaw(Map<String, String> params) {
        if (shouldFail) {
            throw new RuntimeException("Simulated network error");
        }
        return mockResponse;
    }
}