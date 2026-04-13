package com.ownlife.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.api-url}")
    private String apiUrl;

    @Value("${openrouter.model}")
    private String primaryModel;

    @Value("${openrouter.fallback-models:}")
    private String fallbackModels;

    @Value("${openrouter.site-url:http://localhost:8081}")
    private String siteUrl;

    @Value("${openrouter.app-name:OWN LIFE}")
    private String appName;

    public String ask(String prompt) {
        List<String> modelsToTry = buildModelList();

        for (String model : modelsToTry) {
            try {
                String result = askWithModel(prompt, model);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            } catch (Exception e) {
                System.out.println("OpenRouter model failed: " + model);
                e.printStackTrace();
            }
        }

        return "AI 응답 생성 중 오류가 발생했습니다.";
    }


    private String askWithModel(String prompt, String model) throws Exception {

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("HTTP-Referer", siteUrl);
        conn.setRequestProperty("X-Title", appName);
        conn.setDoOutput(true);

        String requestBody = """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "system",
                  "content": "당신은 OWN LIFE 서비스의 'OWN 트레이너'입니다. 사용자의 최근 식단, 운동, 체중 기록을 바탕으로 현실적이고 짧고 이해하기 쉬운 피드백을 제공합니다. 과장하지 말고, 친절하지만 단호하게 말하세요."
                },
                {
                  "role": "user",
                  "content": "%s"
                }
              ]
            }
            """.formatted(escapeJson(model), escapeJson(prompt));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        Scanner sc;
        if (responseCode >= 400) {
            sc = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
        } else {
            sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
        }

        String response = sc.useDelimiter("\\A").hasNext() ? sc.next() : "";

        if (responseCode >= 400) {
            System.out.println("OpenRouter error response (" + model + "): " + response);
            throw new RuntimeException("OpenRouter 호출 실패: " + responseCode);
        }

        return extractMessage(response);
    }

    private String extractMessage(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
            if (!contentNode.isMissingNode() && !contentNode.asText().isBlank()) {
                return contentNode.asText();
            }

            return "응답 파싱 실패";
        } catch (Exception e) {
            e.printStackTrace();
            return "응답 파싱 실패";
        }
    }

    private List<String> buildModelList() {
        List<String> models = new ArrayList<>();
        models.add(primaryModel);

        if (fallbackModels != null && !fallbackModels.isBlank()) {
            String[] split = fallbackModels.split(",");
            for (String model : split) {
                String trimmed = model.trim();
                if (!trimmed.isBlank()) {
                    models.add(trimmed);
                }
            }
        }

        return models;
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}