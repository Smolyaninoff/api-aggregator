package ru.mtuci.aggregator.api.impl;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import ru.mtuci.aggregator.api.ApiProvider;

public class OpenMeteoProvider implements ApiProvider {
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";

    @Override
    public String getName() {
        return "open-meteo";
    }

    @Override
    public Map<String, String> getDefaultParams() {
        return Map.of(
                "latitude", "55.75",
                "longitude", "37.62",
                "current_weather", "true",
                "hourly", "temperature_2m"
        );
    }

    @Override
    public URI buildUri(Map<String, String> params) {
        Map<String, String> effective = params.isEmpty() ? getDefaultParams() : params;
        String query = effective.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return URI.create(BASE_URL + "?" + query);
    }
}
