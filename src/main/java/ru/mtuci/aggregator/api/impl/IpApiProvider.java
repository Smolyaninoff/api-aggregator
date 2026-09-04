package ru.mtuci.aggregator.api.impl;

import java.net.URI;
import java.util.Map;

import ru.mtuci.aggregator.api.ApiProvider;

public class IpApiProvider implements ApiProvider {
    private static final String BASE_URL = "http://ip-api.com/json/";

    @Override
    public String getName() {
        return "ip-api";
    }

    @Override
    public Map<String, String> getDefaultParams() {
        return Map.of();
    }

    @Override
    public URI buildUri(Map<String, String> params) {
        return URI.create(BASE_URL);
    }
}