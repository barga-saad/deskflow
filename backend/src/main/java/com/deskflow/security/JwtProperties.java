package com.deskflow.security;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("deskflow.jwt")
public record JwtProperties(
        @NotNull @Size(min = 32, message = "must be at least 32 characters") String secret,
        @NotNull Duration ttl) {
}
