package ru.mtuci.aggregator.api.impl;

import ru.mtuci.aggregator.api.ApiProvider;

import java.net.URI;
import java.util.Map;

public class JsonPlaceholderProvider implements ApiProvider {
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/users";

    @Override public String getName() { return "jsonplaceholder"; }
    @Override public Map<String, String> getDefaultParams() { return Map.of("id", "1"); }

    @Override public URI buildUri(Map<String, String> params) {
        String id = params.getOrDefault("id", "1");
        return URI.create(BASE_URL + "/" + id);
    }
}