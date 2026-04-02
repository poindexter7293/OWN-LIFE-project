package com.ownlife.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ownlife.dto.KakaoUserProfile;
import jakarta.servlet.http.HttpSession;
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
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class KakaoAuthService {

    private static final String AUTHORIZE_ENDPOINT = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_ENDPOINT = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_ENDPOINT = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_OAUTH_STATE = "kakaoOauthState";
    private static final String KAKAO_SCOPE = "account_email profile_nickname profile_image";

    private final String kakaoRestApiKey;
    private final String kakaoRedirectUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    public KakaoAuthService(@Value("${kakao_rest_api_key:}") String kakaoRestApiKey,
                            @Value("${kakao_redirect_url:}") String kakaoRedirectUrl) {
        this.kakaoRestApiKey = trimToNull(kakaoRestApiKey);
        this.kakaoRedirectUrl = trimToNull(kakaoRedirectUrl);
    }

    public boolean isEnabled() {
        return StringUtils.hasText(kakaoRestApiKey) && StringUtils.hasText(kakaoRedirectUrl);
    }

    public String prepareAuthorizationUrl(HttpSession session) {
        if (!isEnabled() || session == null) {
            return null;
        }

        String state = generateState();
        session.setAttribute(KAKAO_OAUTH_STATE, state);

        return AUTHORIZE_ENDPOINT
                + "?response_type=code"
                + "&client_id=" + urlEncode(kakaoRestApiKey)
                + "&redirect_uri=" + urlEncode(kakaoRedirectUrl)
                + "&scope=" + urlEncode(KAKAO_SCOPE)
                + "&state=" + urlEncode(state);
    }

    public boolean isValidState(HttpSession session, String state) {
        if (session == null || !StringUtils.hasText(state)) {
            return false;
        }

        Object savedState = session.getAttribute(KAKAO_OAUTH_STATE);
        session.removeAttribute(KAKAO_OAUTH_STATE);
        return savedState instanceof String stringState && state.equals(stringState);
    }

    public Optional<KakaoUserProfile> requestUserProfile(String code) {
        if (!isEnabled() || !StringUtils.hasText(code)) {
            return Optional.empty();
        }

        return requestAccessToken(code)
                .flatMap(this::requestUserProfileWithAccessToken);
    }

    private Optional<String> requestAccessToken(String code) {
        Map<String, String> formParameters = new LinkedHashMap<>();
        formParameters.put("grant_type", "authorization_code");
        formParameters.put("client_id", kakaoRestApiKey);
        formParameters.put("redirect_uri", kakaoRedirectUrl);
        formParameters.put("code", code);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormBody(formParameters), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            String accessToken = trimToNull(asString(payload.get("access_token")));
            return Optional.ofNullable(accessToken);
        } catch (IOException exception) {
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<KakaoUserProfile> requestUserProfileWithAccessToken(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_ENDPOINT))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            String id = trimToNull(asString(payload.get("id")));
            Map<String, Object> kakaoAccount = asMap(payload.get("kakao_account"));
            Map<String, Object> profile = asMap(kakaoAccount.get("profile"));
            String email = trimToNull(asString(kakaoAccount.get("email")));
            String nickname = trimToNull(asString(profile.get("nickname")));
            String profileImageUrl = trimToNull(asString(profile.get("profile_image_url")));
            boolean emailVerified = extractBoolean(kakaoAccount.get("has_email"))
                    && extractBoolean(kakaoAccount.get("is_email_valid"))
                    && extractBoolean(kakaoAccount.get("is_email_verified"));

            if (!StringUtils.hasText(id) || !StringUtils.hasText(email)) {
                return Optional.empty();
            }

            return Optional.of(new KakaoUserProfile(
                    id,
                    email.toLowerCase(Locale.ROOT),
                    nickname,
                    profileImageUrl,
                    emailVerified
            ));
        } catch (IOException exception) {
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private String buildFormBody(Map<String, String> formParameters) {
        return formParameters.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String generateState() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        return Map.of();
    }

    private boolean extractBoolean(Object value) {
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

