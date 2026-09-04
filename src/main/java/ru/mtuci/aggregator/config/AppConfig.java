package ru.mtuci.aggregator.config;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.mtuci.aggregator.api.ApiProvider;
import ru.mtuci.aggregator.api.impl.IpApiProvider;
import ru.mtuci.aggregator.api.impl.JsonPlaceholderProvider;
import ru.mtuci.aggregator.api.impl.OpenMeteoProvider;

public class AppConfig {
    private final Map<String, ApiProvider> providers;

    public AppConfig() {
        this(Stream.of(
                new JsonPlaceholderProvider(),
                new IpApiProvider(),
                new OpenMeteoProvider()
        ).collect(Collectors.toList()));
    }

    public AppConfig(Collection<ApiProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(ApiProvider::getName, p -> p));
    }

    public ApiProvider getProvider(String name) {
        return providers.get(name.toLowerCase());
    }

    public Map<String, ApiProvider> getAllProviders() {
        return Map.copyOf(providers);
    }
}