package com.ownlife.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ownlife.dto.NaverUserProfile;
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
public class NaverAuthService {

    private static final String AUTHORIZE_ENDPOINT = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_ENDPOINT = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_ENDPOINT = "https://openapi.naver.com/v1/nid/me";
    private static final String NAVER_OAUTH_STATE = "naverOauthState";

    private final String naverClientId;
    private final String naverClientSecret;
    private final String naverRedirectUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    public NaverAuthService(@Value("${naver_client_id:}") String naverClientId,
                            @Value("${naver_client_secret:}") String naverClientSecret,
                            @Value("${naver_redirect_url:}") String naverRedirectUrl) {
        this.naverClientId = trimToNull(naverClientId);
        this.naverClientSecret = trimToNull(naverClientSecret);
        this.naverRedirectUrl = trimToNull(naverRedirectUrl);
    }

    public boolean isEnabled() {
        return StringUtils.hasText(naverClientId)
                && StringUtils.hasText(naverClientSecret)
                && StringUtils.hasText(naverRedirectUrl);
    }

    public String prepareAuthorizationUrl(HttpSession session) {
        if (!isEnabled() || session == null) {
            return null;
        }

        String state = generateState();
        session.setAttribute(NAVER_OAUTH_STATE, state);

        return AUTHORIZE_ENDPOINT
                + "?response_type=code"
                + "&client_id=" + urlEncode(naverClientId)
                + "&redirect_uri=" + urlEncode(naverRedirectUrl)
                + "&state=" + urlEncode(state);
    }

    public boolean isValidState(HttpSession session, String state) {
        if (session == null || !StringUtils.hasText(state)) {
            return false;
        }

        Object savedState = session.getAttribute(NAVER_OAUTH_STATE);
        session.removeAttribute(NAVER_OAUTH_STATE);
        return savedState instanceof String stringState && state.equals(stringState);
    }

    public Optional<NaverUserProfile> requestUserProfile(String code) {
        if (!isEnabled() || !StringUtils.hasText(code)) {
            return Optional.empty();
        }

        try {
            return Optional.of(requestUserProfileOrThrow(code, null));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Optional.empty();
        }
    }

    public Optional<NaverUserProfile> requestUserProfile(String code, String state) {
        if (!isEnabled() || !StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            return Optional.empty();
        }

        try {
            return Optional.of(requestUserProfileOrThrow(code, state));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Optional.empty();
        }
    }

    public NaverUserProfile requestUserProfileOrThrow(String code, String state) {
        if (!isEnabled()) {
            throw new IllegalStateException("네이버 로그인 설정이 아직 완료되지 않았습니다.");
        }
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("네이버 로그인 요청 정보가 올바르지 않습니다. 다시 시도해 주세요.");
        }

        String accessToken = requestAccessToken(code, state)
                .orElseThrow(() -> new IllegalStateException("네이버 액세스 토큰 발급에 실패했습니다. 네이버 앱 설정과 콜백 URL을 확인해 주세요."));

        return requestUserProfileWithAccessToken(accessToken)
                .orElseThrow(() -> new IllegalStateException("네이버 사용자 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }

    private Optional<String> requestAccessToken(String code, String state) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("grant_type", "authorization_code");
        queryParameters.put("client_id", naverClientId);
        queryParameters.put("client_secret", naverClientSecret);
        queryParameters.put("code", code);
        if (StringUtils.hasText(state)) {
            queryParameters.put("state", state);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT + "?" + buildQueryString(queryParameters)))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException(resolveTokenErrorMessage(response.body()));
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            String accessToken = trimToNull(asString(payload.get("access_token")));
            if (!StringUtils.hasText(accessToken)) {
                throw new IllegalStateException(resolveTokenErrorMessage(response.body()));
            }
            return Optional.ofNullable(accessToken);
        } catch (IOException exception) {
            throw new IllegalStateException("네이버 액세스 토큰 요청 중 네트워크 또는 응답 처리 오류가 발생했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("네이버 로그인 요청이 중단되었습니다. 다시 시도해 주세요.", exception);
        }
    }

    private Optional<NaverUserProfile> requestUserProfileWithAccessToken(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_ENDPOINT))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("네이버 사용자 정보 조회에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            if (!"00".equals(trimToNull(asString(payload.get("resultcode"))))) {
                throw new IllegalStateException(resolveProfileErrorMessage(payload));
            }

            Map<String, Object> profile = asMap(payload.get("response"));
            String id = trimToNull(asString(profile.get("id")));
            String email = trimToNull(asString(profile.get("email")));
            String name = trimToNull(asString(profile.get("name")));
            String nickname = trimToNull(asString(profile.get("nickname")));
            String profileImageUrl = trimToNull(asString(profile.get("profile_image")));

            if (!StringUtils.hasText(id)) {
                throw new IllegalStateException("네이버 사용자 식별 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            }
            if (!StringUtils.hasText(email)) {
                throw new IllegalStateException("네이버 계정 이메일 제공 동의가 필요합니다. 네이버에서 이메일 제공에 동의한 뒤 다시 시도해 주세요.");
            }

            return Optional.of(new NaverUserProfile(
                    id,
                    email.toLowerCase(Locale.ROOT),
                    name,
                    nickname,
                    profileImageUrl,
                    true
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("네이버 사용자 정보 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("네이버 사용자 정보 요청이 중단되었습니다. 다시 시도해 주세요.", exception);
        }
    }

    private String resolveTokenErrorMessage(String body) {
        Map<String, Object> payload = readPayload(body);
        String error = trimToNull(asString(payload.get("error")));
        String errorDescription = trimToNull(asString(payload.get("error_description")));
        if (StringUtils.hasText(errorDescription)) {
            return "네이버 액세스 토큰 발급에 실패했습니다: " + errorDescription;
        }
        if (StringUtils.hasText(error)) {
            return "네이버 액세스 토큰 발급에 실패했습니다: " + error;
        }
        return "네이버 액세스 토큰 발급에 실패했습니다. 네이버 앱 설정과 콜백 URL을 확인해 주세요.";
    }

    private String resolveProfileErrorMessage(Map<String, Object> payload) {
        String message = trimToNull(asString(payload.get("message")));
        if (StringUtils.hasText(message)) {
            return "네이버 사용자 정보 조회에 실패했습니다: " + message;
        }
        return "네이버 사용자 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
    }

    private Map<String, Object> readPayload(String body) {
        if (!StringUtils.hasText(body)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<>() {
            });
        } catch (IOException exception) {
            return Map.of();
        }
    }

    private String buildQueryString(Map<String, String> queryParameters) {
        return queryParameters.entrySet().stream()
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

