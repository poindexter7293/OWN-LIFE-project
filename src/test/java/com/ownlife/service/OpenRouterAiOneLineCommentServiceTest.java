package com.ownlife.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ownlife.dto.AiOneLineCommentDto;
import com.ownlife.dto.DashboardSummaryDto;
import com.ownlife.dto.LifestylePatternAnalysisDto;
import com.ownlife.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenRouterAiOneLineCommentServiceTest {

    @Test
    @DisplayName("API 키가 없으면 기본 코멘트로 대체한다")
    void fallsBackWhenApiKeyIsMissing() {
        OpenRouterAiOneLineCommentService service = new OpenRouterAiOneLineCommentService(
                new StubDashboardService(buildDashboardSummary(360, 2100, 95, 55, 6)),
                new ObjectMapper(),
                true,
                "",
                "https://openrouter.ai/api/v1/chat/completions",
                "z-ai/glm-4.5-air:free",
                "",
                "http://localhost:8081",
                "OWN LIFE"
        );

        AiOneLineCommentDto result = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg");

        assertTrue(result.isFallback());
        assertFalse(result.getMessage().isBlank());
        assertEquals(5, result.getRemainingRefreshCount());
        assertTrue(result.isRefreshAllowed());
    }

    @Test
    @DisplayName("AI 응답이 오면 AI 코멘트를 우선 사용한다")
    void usesAiCommentWhenApiCallSucceeds() {
        OpenRouterAiOneLineCommentService service = new OpenRouterAiOneLineCommentService(
                new StubDashboardService(buildDashboardSummary(280, 1800, 88, 72, 4)),
                new ObjectMapper(),
                true,
                "test-key",
                "https://openrouter.ai/api/v1/chat/completions",
                "z-ai/glm-4.5-air:free",
                "",
                "http://localhost:8081",
                "OWN LIFE"
        ) {
            @Override
            protected String requestAiMessage(com.ownlife.dto.AiOneLineCommentPromptDto promptDto) {
                return "최근 3일간 섭취 패턴이 안정적이니 지금 흐름을 유지해 보세요.";
            }
        };

        AiOneLineCommentDto result = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg");

        assertFalse(result.isFallback());
        assertTrue(result.getMessage().contains("최근 3일간 섭취 패턴이 안정적"));
        assertEquals(5, result.getRemainingRefreshCount());
    }

    @Test
    @DisplayName("같은 날 기본 코멘트가 먼저 나와도 이후 AI 호출이 가능해지면 다시 시도한다")
    void retriesWhenCachedCommentIsFallback() {
        AtomicInteger requestCount = new AtomicInteger();

        OpenRouterAiOneLineCommentService service = new OpenRouterAiOneLineCommentService(
                new StubDashboardService(buildDashboardSummary(280, 1800, 88, 72, 4)),
                new ObjectMapper(),
                true,
                "test-key",
                "https://openrouter.ai/api/v1/chat/completions",
                "z-ai/glm-4.5-air:free",
                "",
                "http://localhost:8081",
                "OWN LIFE"
        ) {
            @Override
            protected String requestAiMessage(com.ownlife.dto.AiOneLineCommentPromptDto promptDto) throws IOException {
                if (requestCount.getAndIncrement() == 0) {
                    throw new IOException("temporary failure");
                }
                return "오늘은 운동 리듬이 좋아요. 지금처럼 꾸준히 이어가 보세요.";
            }

            @Override
            protected LocalDate currentDate() {
                return LocalDate.of(2026, 4, 9);
            }
        };

        AiOneLineCommentDto first = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg");
        AiOneLineCommentDto second = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg");

        assertTrue(first.isFallback());
        assertFalse(second.isFallback());
        assertEquals(2, requestCount.get());
        assertTrue(second.getMessage().contains("운동 리듬이 좋아요"));
    }

    @Test
    @DisplayName("수동 새로고침은 하루 5번까지만 가능하다")
    void limitsManualRefreshToFiveTimesPerDay() {
        AtomicInteger requestCount = new AtomicInteger();

        OpenRouterAiOneLineCommentService service = new OpenRouterAiOneLineCommentService(
                new StubDashboardService(buildDashboardSummary(280, 1800, 88, 72, 4)),
                new ObjectMapper(),
                true,
                "test-key",
                "https://openrouter.ai/api/v1/chat/completions",
                "primary-model",
                "",
                "http://localhost:8081",
                "OWN LIFE"
        ) {
            @Override
            protected String requestAiMessage(com.ownlife.dto.AiOneLineCommentPromptDto promptDto) {
                requestCount.incrementAndGet();
                return "새로고침으로 받은 AI 코멘트예요.";
            }

            @Override
            protected LocalDate currentDate() {
                return LocalDate.of(2026, 4, 9);
            }
        };

        AiOneLineCommentDto fifth = null;
        for (int i = 0; i < 5; i++) {
            fifth = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg", true);
        }
        AiOneLineCommentDto blocked = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg", true);

        assertEquals(5, requestCount.get());
        assertEquals(0, fifth.getRemainingRefreshCount());
        assertFalse(fifth.isRefreshAllowed());
        assertEquals("오늘 새로고침 소진", blocked.getBadgeLabel());
        assertEquals(0, blocked.getRemainingRefreshCount());
        assertFalse(blocked.isRefreshAllowed());
    }

    @Test
    @DisplayName("같은 회원의 AI 코멘트는 하루 동안 한 번만 호출하고 캐시를 재사용한다")
    void reusesDailyCachedComment() {
        AtomicInteger requestCount = new AtomicInteger();

        OpenRouterAiOneLineCommentService service = new OpenRouterAiOneLineCommentService(
                new StubDashboardService(buildDashboardSummary(280, 1800, 88, 72, 4)),
                new ObjectMapper(),
                true,
                "test-key",
                "https://openrouter.ai/api/v1/chat/completions",
                "z-ai/glm-4.5-air:free",
                "",
                "http://localhost:8081",
                "OWN LIFE"
        ) {
            @Override
            protected String requestAiMessage(com.ownlife.dto.AiOneLineCommentPromptDto promptDto) {
                requestCount.incrementAndGet();
                return "오늘은 단백질 섭취를 조금만 더 챙기면 더 좋아요.";
            }

            @Override
            protected LocalDate currentDate() {
                return LocalDate.of(2026, 4, 9);
            }
        };

        AiOneLineCommentDto first = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg");
        AiOneLineCommentDto second = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg");

        assertEquals(1, requestCount.get());
        assertEquals(first.getMessage(), second.getMessage());
    }

    @Test
    @DisplayName("기본 모델이 실패하면 fallback 모델로 다시 시도한다")
    void retriesWithFallbackModelWhenPrimaryFails() {
        AtomicInteger requestCount = new AtomicInteger();

        OpenRouterAiOneLineCommentService service = new OpenRouterAiOneLineCommentService(
                new StubDashboardService(buildDashboardSummary(280, 1800, 88, 72, 4)),
                new ObjectMapper(),
                true,
                "test-key",
                "https://openrouter.ai/api/v1/chat/completions",
                "primary-model",
                "secondary-model",
                "http://localhost:8081",
                "OWN LIFE"
        ) {
            @Override
            protected String requestAiMessageWithModel(com.ownlife.dto.AiOneLineCommentPromptDto promptDto, String targetModel) throws IOException {
                requestCount.incrementAndGet();
                if ("primary-model".equals(targetModel)) {
                    throw new IOException("primary failed");
                }
                return "대체 모델 응답이에요. 오늘도 꾸준히 이어가 보세요.";
            }
        };

        AiOneLineCommentDto result = service.generateComment(buildMember(), buildLifestylePattern(), "목표 체중까지 -4.0kg");

        assertFalse(result.isFallback());
        assertEquals(2, requestCount.get());
        assertTrue(result.getMessage().contains("대체 모델 응답"));
    }

    private Member buildMember() {
        Member member = new Member();
        member.setMemberId(1L);
        member.setNickname("테스터");
        member.setWeightKg(new BigDecimal("72.0"));
        member.setGoalWeight(new BigDecimal("68.0"));
        member.setGoalEatKcal(2200);
        member.setGoalBurnedKcal(500);
        return member;
    }

    private LifestylePatternAnalysisDto buildLifestylePattern() {
        return LifestylePatternAnalysisDto.builder()
                .periodLabel("최근 28일 기준")
                .title("주말 몰아형 루틴이 눈에 띄어요")
                .description("운동과 식단 기록을 바탕으로 생활 패턴을 분석했어요.")
                .insights(java.util.List.of())
                .build();
    }

    private DashboardSummaryDto buildDashboardSummary(int burnedCalories,
                                                      int intakeCalories,
                                                      int intakePercent,
                                                      int proteinGram,
                                                      int streakDays) {
        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setBurnedCalories(burnedCalories);
        dto.setIntakeCalories(intakeCalories);
        dto.setIntakePercent(intakePercent);
        dto.setProteinGram(proteinGram);
        dto.setStreakDays(streakDays);
        return dto;
    }

    private static class StubDashboardService extends DashboardService {

        private final DashboardSummaryDto summaryDto;

        StubDashboardService(DashboardSummaryDto summaryDto) {
            super(null, null, null, null);
            this.summaryDto = summaryDto;
        }

        @Override
        public DashboardSummaryDto getDashboardSummary(Long memberId) {
            return summaryDto;
        }
    }
}

