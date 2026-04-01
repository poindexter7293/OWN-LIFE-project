package com.ownlife.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ownlife.dto.GoogleUserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class GoogleAuthService {

    private static final String GOOGLE_TOKENINFO_ENDPOINT = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final String googleClientId;
    private final String googleRedirectUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleAuthService(@Value("${google_client_id:}") String googleClientId,
                             @Value("${google_redirect_url:}") String googleRedirectUrl) {
        this.googleClientId = trimToNull(googleClientId);
        this.googleRedirectUrl = trimToNull(googleRedirectUrl);
    }

    public boolean isEnabled() {
        return StringUtils.hasText(googleClientId) && StringUtils.hasText(googleRedirectUrl);
    }

    public String getGoogleClientId() {
        return googleClientId;
    }

    public String getGoogleRedirectUrl() {
        return googleRedirectUrl;
    }

    public Optional<GoogleUserProfile> verifyCredential(String credential) {
        if (!isEnabled() || !StringUtils.hasText(credential)) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKENINFO_ENDPOINT + URLEncoder.encode(credential, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            if (!isValidGoogleToken(payload)) {
                return Optional.empty();
            }

            String subject = trimToNull(asString(payload.get("sub")));
            String email = trimToNull(asString(payload.get("email")));
            String name = trimToNull(asString(payload.get("name")));
            String pictureUrl = trimToNull(asString(payload.get("picture")));
            boolean emailVerified = extractEmailVerified(payload.get("email_verified"));

            if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)) {
                return Optional.empty();
            }

            return Optional.of(new GoogleUserProfile(
                    subject,
                    email.toLowerCase(Locale.ROOT),
                    name,
                    pictureUrl,
                    emailVerified
            ));
        } catch (IOException exception) {
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private boolean isValidGoogleToken(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return false;
        }

        String audience = trimToNull(asString(payload.get("aud")));
        if (!googleClientId.equals(audience)) {
            return false;
        }

        String issuer = trimToNull(asString(payload.get("iss")));
        if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
            return false;
        }

        Long expiresAt = parseEpochSeconds(payload.get("exp"));
        return expiresAt == null || Instant.ofEpochSecond(expiresAt).isAfter(Instant.now());
    }

    private boolean extractEmailVerified(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return false;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long parseEpochSeconds(Object value) {
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}


