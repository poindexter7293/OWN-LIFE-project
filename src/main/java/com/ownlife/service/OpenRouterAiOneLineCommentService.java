package com.ownlife.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ownlife.dto.AiOneLineCommentDto;
import com.ownlife.dto.AiOneLineCommentPromptDto;
import com.ownlife.dto.DashboardSummaryDto;
import com.ownlife.dto.LifestylePatternAnalysisDto;
import com.ownlife.entity.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpenRouterAiOneLineCommentService implements AiOneLineCommentService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterAiOneLineCommentService.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(7);

    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final List<String> fallbackModels;
    private final String siteUrl;
    private final String appName;
    private final Map<Long, CachedAiComment> dailyCommentCache = new ConcurrentHashMap<>();

    public OpenRouterAiOneLineCommentService(DashboardService dashboardService,
                                             ObjectMapper objectMapper,
                                             @Value("${openrouter.enabled:true}") boolean enabled,
                                             @Value("${openrouter.api-key:}") String apiKey,
                                             @Value("${openrouter.api-url:https://openrouter.ai/api/v1/chat/completions}") String apiUrl,
                                             @Value("${openrouter.model:z-ai/glm-4.5-air:free}") String model,
                                             @Value("${openrouter.fallback-models:}") String fallbackModels,
                                             @Value("${openrouter.site-url:http://localhost:8081}") String siteUrl,
                                             @Value("${openrouter.app-name:OWN LIFE}") String appName) {
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.fallbackModels = parseFallbackModels(fallbackModels);
        this.siteUrl = siteUrl;
        this.appName = appName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public AiOneLineCommentDto generateComment(Member member,
                                               LifestylePatternAnalysisDto lifestylePatternAnalysis,
                                               String weightGoalMessage) {
        if (member == null || member.getMemberId() == null) {
            return fallbackComment(null, lifestylePatternAnalysis, weightGoalMessage);
        }

        LocalDate today = currentDate();
        boolean aiAvailable = isAiAvailable();
        AiOneLineCommentDto cachedComment = getCachedComment(member.getMemberId(), today);
        if (cachedComment != null && (!cachedComment.isFallback() || !aiAvailable)) {
            return cachedComment;
        }

        DashboardSummaryDto dashboardSummary;
        try {
            dashboardSummary = dashboardService.getDashboardSummary(member.getMemberId());
        } catch (RuntimeException exception) {
            log.warn("AI 코멘트용 대시보드 요약을 가져오지 못해 기본 코멘트로 대체합니다. memberId={}", member.getMemberId(), exception);
            dashboardSummary = null;
        }

        AiOneLineCommentPromptDto promptDto = toPromptDto(member, dashboardSummary, lifestylePatternAnalysis, weightGoalMessage);

        if (!aiAvailable) {
            return cacheComment(member.getMemberId(), today, fallbackComment(promptDto, lifestylePatternAnalysis, weightGoalMessage));
        }

        try {
            String aiComment = requestAiMessage(promptDto);
            String sanitized = sanitizeOneLineComment(aiComment);
            if (!StringUtils.hasText(sanitized)) {
                return cacheComment(member.getMemberId(), today, fallbackComment(promptDto, lifestylePatternAnalysis, weightGoalMessage));
            }
            return cacheComment(member.getMemberId(), today, AiOneLineCommentDto.builder()
                    .message(sanitized)
                    .detail("최근 기록과 목표 데이터를 바탕으로 AI가 한 줄로 요약했어요.")
                    .tone(resolveTone(promptDto, sanitized))
                    .badgeLabel("AI 코멘트")
                    .fallback(false)
                    .build());
        } catch (Exception exception) {
            log.warn("OpenRouter AI 코멘트 생성에 실패해 기본 코멘트로 대체합니다. memberId={}", member.getMemberId(), exception);
            return cacheComment(member.getMemberId(), today, fallbackComment(promptDto, lifestylePatternAnalysis, weightGoalMessage));
        }
    }

    protected LocalDate currentDate() {
        return LocalDate.now();
    }

    protected String requestAiMessage(AiOneLineCommentPromptDto promptDto) throws IOException, InterruptedException {
        List<String> candidateModels = new ArrayList<>();
        if (StringUtils.hasText(model)) {
            candidateModels.add(model);
        }
        for (String fallbackModel : fallbackModels) {
            if (StringUtils.hasText(fallbackModel) && !candidateModels.contains(fallbackModel)) {
                candidateModels.add(fallbackModel);
            }
        }

        IOException lastIOException = null;
        InterruptedException lastInterruptedException = null;
        for (String candidateModel : candidateModels) {
            try {
                return requestAiMessageWithModel(promptDto, candidateModel);
            } catch (IOException exception) {
                lastIOException = exception;
                log.warn("OpenRouter 모델 호출에 실패했습니다. model={}", candidateModel, exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                lastInterruptedException = exception;
                break;
            }
        }

        if (lastInterruptedException != null) {
            throw lastInterruptedException;
        }
        if (lastIOException != null) {
            throw lastIOException;
        }
        return null;
    }

    protected String requestAiMessageWithModel(AiOneLineCommentPromptDto promptDto, String targetModel) throws IOException, InterruptedException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", targetModel);
        requestBody.put("temperature", 0.4);
        requestBody.put("max_tokens", 120);

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "너는 헬스케어 앱 OWN LIFE의 건강 코치다. 사용자의 요약 데이터를 바탕으로 한국어 존댓말 한 문장만 작성한다. 의학적 진단은 하지 말고, 친절하고 구체적으로 말해라. 1문장, 90자 이내, 불필요한 따옴표/번호/줄바꿈 금지.");
        messages.addObject()
                .put("role", "user")
                .put("content", buildPrompt(promptDto));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));

        if (StringUtils.hasText(siteUrl)) {
            requestBuilder.header("HTTP-Referer", siteUrl);
        }
        if (StringUtils.hasText(appName)) {
            requestBuilder.header("X-Title", appName);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenRouter API 호출 실패: status=" + response.statusCode() + ", body=" + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }

        JsonNode contentNode = choices.get(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode node : contentNode) {
                JsonNode textNode = node.path("text");
                if (textNode.isTextual()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(textNode.asText());
                }
            }
            return builder.toString();
        }
        return null;
    }

    private List<String> parseFallbackModels(String fallbackModelsValue) {
        if (!StringUtils.hasText(fallbackModelsValue)) {
            return List.of();
        }

        return java.util.Arrays.stream(fallbackModelsValue.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private boolean isAiAvailable() {
        return enabled && StringUtils.hasText(apiKey) && StringUtils.hasText(apiUrl) && StringUtils.hasText(model);
    }

    private AiOneLineCommentDto getCachedComment(Long memberId, LocalDate date) {
        CachedAiComment cachedAiComment = dailyCommentCache.get(memberId);
        if (cachedAiComment == null || !cachedAiComment.getCachedDate().equals(date)) {
            return null;
        }
        return copyComment(cachedAiComment.getComment());
    }

    private AiOneLineCommentDto cacheComment(Long memberId, LocalDate date, AiOneLineCommentDto comment) {
        if (memberId == null || date == null || comment == null) {
            return comment;
        }

        clearExpiredCache(date);
        dailyCommentCache.put(memberId, new CachedAiComment(date, copyComment(comment)));
        return copyComment(comment);
    }

    private void clearExpiredCache(LocalDate today) {
        dailyCommentCache.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().getCachedDate().isBefore(today));
    }

    private AiOneLineCommentDto copyComment(AiOneLineCommentDto comment) {
        if (comment == null) {
            return null;
        }

        return AiOneLineCommentDto.builder()
                .message(comment.getMessage())
                .detail(comment.getDetail())
                .tone(comment.getTone())
                .badgeLabel(comment.getBadgeLabel())
                .fallback(comment.isFallback())
                .build();
    }

    private AiOneLineCommentPromptDto toPromptDto(Member member,
                                                  DashboardSummaryDto dashboardSummary,
                                                  LifestylePatternAnalysisDto lifestylePatternAnalysis,
                                                  String weightGoalMessage) {
        return AiOneLineCommentPromptDto.builder()
                .memberId(member.getMemberId())
                .goalEatKcal(member.getGoalEatKcal())
                .goalBurnedKcal(member.getGoalBurnedKcal())
                .todayIntakeCalories(dashboardSummary != null ? dashboardSummary.getIntakeCalories() : 0)
                .todayBurnedCalories(dashboardSummary != null ? dashboardSummary.getBurnedCalories() : 0)
                .intakePercent(dashboardSummary != null ? dashboardSummary.getIntakePercent() : 0)
                .burnedPercent(dashboardSummary != null ? dashboardSummary.getBurnedPercent() : 0)
                .proteinGram(dashboardSummary != null ? dashboardSummary.getProteinGram() : 0)
                .proteinPercent(dashboardSummary != null ? dashboardSummary.getProteinPercent() : 0)
                .carbGram(dashboardSummary != null ? dashboardSummary.getCarbGram() : 0)
                .fatGram(dashboardSummary != null ? dashboardSummary.getFatGram() : 0)
                .streakDays(dashboardSummary != null ? dashboardSummary.getStreakDays() : 0)
                .weightGoalMessage(weightGoalMessage)
                .lifestyleTitle(lifestylePatternAnalysis != null ? lifestylePatternAnalysis.getTitle() : null)
                .lifestyleDescription(lifestylePatternAnalysis != null ? lifestylePatternAnalysis.getDescription() : null)
                .build();
    }

    private String buildPrompt(AiOneLineCommentPromptDto promptDto) {
        return String.format(Locale.KOREA,
                "다음은 사용자의 건강 기록 요약이다.\n" +
                        "- 목표 섭취 칼로리: %s kcal\n" +
                        "- 목표 소모 칼로리: %s kcal\n" +
                        "- 오늘 섭취 칼로리: %s kcal\n" +
                        "- 오늘 소모 칼로리: %s kcal\n" +
                        "- 오늘 섭취 달성률: %s%%\n" +
                        "- 오늘 소모 달성률: %s%%\n" +
                        "- 오늘 단백질 섭취: %sg (%s%%)\n" +
                        "- 오늘 탄수화물 섭취: %sg\n" +
                        "- 오늘 지방 섭취: %sg\n" +
                        "- 연속 기록일: %s일\n" +
                        "- 체중/목표 메시지: %s\n" +
                        "- 최근 생활 패턴 제목: %s\n" +
                        "- 최근 생활 패턴 설명: %s\n\n" +
                        "위 데이터를 바탕으로 사용자가 오늘 또는 최근 흐름에서 가장 주의하거나 유지하면 좋은 포인트를 한국어 존댓말 한 문장으로만 알려줘.",
                valueOrDash(promptDto.getGoalEatKcal()),
                valueOrDash(promptDto.getGoalBurnedKcal()),
                valueOrDash(promptDto.getTodayIntakeCalories()),
                valueOrDash(promptDto.getTodayBurnedCalories()),
                valueOrDash(promptDto.getIntakePercent()),
                valueOrDash(promptDto.getBurnedPercent()),
                valueOrDash(promptDto.getProteinGram()),
                valueOrDash(promptDto.getProteinPercent()),
                valueOrDash(promptDto.getCarbGram()),
                valueOrDash(promptDto.getFatGram()),
                valueOrDash(promptDto.getStreakDays()),
                textOrDash(promptDto.getWeightGoalMessage()),
                textOrDash(promptDto.getLifestyleTitle()),
                textOrDash(promptDto.getLifestyleDescription()));
    }

    private AiOneLineCommentDto fallbackComment(AiOneLineCommentPromptDto promptDto,
                                                LifestylePatternAnalysisDto lifestylePatternAnalysis,
                                                String weightGoalMessage) {
        String message;
        String tone;

        if (promptDto == null) {
            message = "최근 기록을 불러오는 중이라 기본 코멘트를 준비하고 있어요.";
            tone = "muted";
        } else if ((promptDto.getGoalEatKcal() == null || promptDto.getGoalEatKcal() <= 0)
                && (promptDto.getGoalBurnedKcal() == null || promptDto.getGoalBurnedKcal() <= 0)) {
            message = "목표 섭취·소모 칼로리를 설정하면 더 정확한 AI 코멘트를 받을 수 있어요.";
            tone = "accent";
        } else if (promptDto.getTodayBurnedCalories() >= 300 && promptDto.getProteinGram() < 60) {
            message = "오늘은 운동량 대비 단백질 섭취가 부족해 보여요. 회복용 식단을 조금 더 챙겨보세요.";
            tone = "caution";
        } else if (promptDto.getGoalEatKcal() != null && promptDto.getGoalEatKcal() > 0 && promptDto.getIntakePercent() >= 120) {
            message = "오늘 섭취 칼로리가 목표를 꽤 넘었어요. 다음 식사는 가볍게 조절해 보세요.";
            tone = "caution";
        } else if (promptDto.getStreakDays() >= 5) {
            message = String.format(Locale.KOREA, "%d일 연속 기록 중이에요. 지금의 루틴을 유지하면 흐름이 더 안정될 거예요.", promptDto.getStreakDays());
            tone = "balance";
        } else if (StringUtils.hasText(promptDto.getLifestyleTitle()) && promptDto.getLifestyleTitle().contains("주말 몰아")) {
            message = "주말에 활동량이 몰리는 패턴이 보여요. 평일에도 가벼운 루틴을 나눠보면 더 안정적입니다.";
            tone = "weekend";
        } else if (StringUtils.hasText(promptDto.getLifestyleTitle()) && promptDto.getLifestyleTitle().contains("식단 기록은 약한 편")) {
            message = "운동 루틴은 괜찮지만 식단 기록이 비는 날이 보여요. 식사 기록을 조금만 더 챙겨보세요.";
            tone = "accent";
        } else if (StringUtils.hasText(weightGoalMessage) && !weightGoalMessage.contains("미설정")) {
            message = "체중 목표 흐름은 잘 잡혀 있어요. 오늘 기록도 이어서 남기면 변화 추적이 더 정확해집니다.";
            tone = "accent";
        } else if (lifestylePatternAnalysis != null && StringUtils.hasText(lifestylePatternAnalysis.getTitle())) {
            message = sanitizeOneLineComment(lifestylePatternAnalysis.getTitle() + " 최근 패턴을 유지하면서 기록을 조금 더 쌓아보세요.");
            tone = "muted";
        } else {
            message = "오늘 기록을 조금 더 쌓으면 더 정확한 건강 코멘트를 드릴 수 있어요.";
            tone = "muted";
        }

        return AiOneLineCommentDto.builder()
                .message(message)
                .detail("최근 기록을 바탕으로 먼저 준비한 코멘트예요. 잠시 후 다시 보면 AI 코멘트가 반영될 수 있어요.")
                .tone(tone)
                .badgeLabel("기본 코멘트")
                .fallback(true)
                .build();
    }

    private String resolveTone(AiOneLineCommentPromptDto promptDto, String message) {
        if (promptDto != null && promptDto.getTodayBurnedCalories() >= 300 && promptDto.getProteinGram() < 60) {
            return "caution";
        }
        if (promptDto != null && promptDto.getGoalEatKcal() != null && promptDto.getGoalEatKcal() > 0 && promptDto.getIntakePercent() >= 120) {
            return "caution";
        }
        if (promptDto != null && promptDto.getStreakDays() >= 5) {
            return "balance";
        }
        if (message.contains("부족") || message.contains("초과") || message.contains("조절")) {
            return "caution";
        }
        if (message.contains("유지") || message.contains("안정") || message.contains("꾸준")) {
            return "balance";
        }
        return "accent";
    }

    private String sanitizeOneLineComment(String rawComment) {
        if (!StringUtils.hasText(rawComment)) {
            return null;
        }

        String sanitized = rawComment
                .replaceAll("[\r\n]+", " ")
                .replace("\"", "")
                .replace("“", "")
                .replace("”", "")
                .trim();

        if (!StringUtils.hasText(sanitized)) {
            return null;
        }

        int periodIndex = sanitized.indexOf('.');
        if (periodIndex > 0) {
            sanitized = sanitized.substring(0, periodIndex + 1).trim();
        }

        if (sanitized.length() > 110) {
            sanitized = sanitized.substring(0, 109).trim() + "…";
        }

        return sanitized;
    }

    private String valueOrDash(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String textOrDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private static final class CachedAiComment {

        private final LocalDate cachedDate;
        private final AiOneLineCommentDto comment;

        private CachedAiComment(LocalDate cachedDate, AiOneLineCommentDto comment) {
            this.cachedDate = cachedDate;
            this.comment = comment;
        }

        private LocalDate getCachedDate() {
            return cachedDate;
        }

        private AiOneLineCommentDto getComment() {
            return comment;
        }
    }
}

