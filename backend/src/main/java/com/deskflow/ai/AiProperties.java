package com.deskflow.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("deskflow.ai")
public record AiProperties(boolean enabled, String baseUrl, String model, String apiKey, Duration timeout) {
}
