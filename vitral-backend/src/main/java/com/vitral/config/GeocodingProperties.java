package com.vitral.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geocoding")
public record GeocodingProperties(boolean enabled, String searchUrl, String userAgent) {
}
