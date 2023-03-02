package com.kaimono.order.service.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "kaimono.catalog-service")
public record ClientProperties(@NotNull(message = "The URI must be defined.") URI uri) { }
