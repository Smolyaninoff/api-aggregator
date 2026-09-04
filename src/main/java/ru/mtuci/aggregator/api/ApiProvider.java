package ru.mtuci.aggregator.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public interface ApiProvider {
    String getName();
    Map<String, String> getDefaultParams();
    
    default String fetchRaw(Map<String, String> params) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URI uri = buildUri(params);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    URI buildUri(Map<String, String> params);
}