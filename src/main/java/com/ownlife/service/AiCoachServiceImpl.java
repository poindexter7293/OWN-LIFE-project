package com.ownlife.service;

import com.ownlife.dto.AiCoachResponseDto;
import com.ownlife.dto.CoachSummaryDto;
import com.ownlife.entity.MealLog;
import com.ownlife.entity.ExerciseLog;
import com.ownlife.entity.WeightLog;
import com.ownlife.repository.MealLogRepository;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.WeightLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCoachServiceImpl implements AiCoachService {

    private final MealLogRepository mealLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final WeightLogRepository weightLogRepository;
    private final OpenAiService openAiService;


    @Override
    public AiCoachResponseDto getRecommendation(Long memberId, String type) {
        CoachSummaryDto summary = buildCoachSummary(memberId);

        try {
            String prompt = buildPrompt(summary, type);
            String aiAnswer = openAiService.ask(prompt);

            List<String> messages = Arrays.stream(aiAnswer.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();

            return AiCoachResponseDto.builder()
                    .type(type)
                    .title("OWN 트레이너")
                    .summary("AI 기반 분석 결과입니다.")
                    .messages(messages)
                    .hasData(true)
                    .build();

        } catch (Exception e) {
            // AI 호출 실패 시 기존 규칙 기반 로직으로 fallback
            switch (type) {
                case "diet":
                    return dietResponse(summary);

                case "exercise":
                    return exerciseResponse(summary);

                case "analysis":
                    return analysisResponse(summary);

                case "feedback":
                    return feedbackResponse(summary);

                default:
                    return defaultResponse();
            }
        }
    }

    // --------------------------
    // 🍽 식단 추천
    // --------------------------
    private AiCoachResponseDto dietResponse(CoachSummaryDto summary) {
        List<String> messages = new ArrayList<>();

        if (!summary.isHasMealData()) {
            messages.add("최근 식단 기록이 없습니다.");
            messages.add("하루 한 끼라도 먼저 기록하는 습관부터 시작하세요.");
            messages.add("기록이 쌓이면 더 정확한 식단 추천이 가능합니다.");
        } else {
            if (summary.getAvgProtein() < 80) {
                messages.add("최근 단백질 섭취가 부족한 편입니다.");
                messages.add("닭가슴살, 계란, 두부, 그릭요거트 같은 단백질 식품을 추가해보세요.");
            } else {
                messages.add("단백질 섭취 흐름은 괜찮습니다.");
            }

            if (summary.getAvgCalories() > 2000) {
                messages.add("평균 섭취 칼로리가 높은 편입니다.");
                messages.add("저녁 탄수화물이나 간식 빈도를 먼저 줄이는 게 좋습니다.");
            } else if (summary.getAvgCalories() < 1200) {
                messages.add("섭취 칼로리가 너무 낮은 편입니다.");
                messages.add("너무 적게 먹기보다 꾸준히 유지 가능한 식단이 더 중요합니다.");
            } else {
                messages.add("총 섭취 칼로리 흐름은 무난합니다.");
            }

            if (summary.getAvgFat() > 60) {
                messages.add("지방 섭취 비중이 높아 보입니다. 튀김류나 야식 섭취를 점검해보세요.");
            }

            messages.add("오늘은 단백질을 우선 챙기고, 저녁은 가볍게 마무리하는 방향을 추천합니다.");
        }

        return AiCoachResponseDto.builder()
                .type("diet")
                .title("오늘 식단 추천")
                .summary("최근 식단 기록을 바탕으로 식사 방향을 정리했어요.")
                .messages(messages)
                .hasData(summary.isHasMealData())
                .build();
    }

    // --------------------------
    // 🏃 운동 추천
    // --------------------------
    private AiCoachResponseDto exerciseResponse(CoachSummaryDto summary) {
        List<String> messages = new ArrayList<>();

        if (!summary.isHasExerciseData()) {
            messages.add("최근 7일 운동 기록이 없습니다.");
            messages.add("이번 주는 가벼운 걷기나 러닝 20~30분부터 시작해보세요.");
            messages.add("운동은 강도보다 기록을 끊기지 않게 이어가는 게 더 중요합니다.");
        } else if (summary.getExerciseDays() <= 2) {
            messages.add("운동 빈도가 조금 낮은 편입니다.");
            messages.add("주 3회 정도까지 늘리면 흐름이 더 안정적입니다.");
            messages.add("오늘은 짧은 유산소와 가벼운 근력운동을 함께 해보세요.");
        } else if (summary.getExerciseDays() <= 4) {
            messages.add("운동 루틴이 어느 정도 자리 잡고 있습니다.");
            messages.add("지금 흐름을 유지하면서 유산소와 근력의 균형을 맞추면 좋습니다.");
            messages.add("오늘은 부족한 부위를 보완하는 식으로 진행해보세요.");
        } else {
            messages.add("운동을 꽤 꾸준히 하고 있습니다.");
            messages.add("지금 페이스를 유지하되, 피로 누적이 있다면 회복도 함께 챙기세요.");
            messages.add("오늘은 강도 조절이나 스트레칭 중심 루틴도 괜찮습니다.");
        }

        return AiCoachResponseDto.builder()
                .type("exercise")
                .title("오늘 운동 추천")
                .summary("최근 운동 기록을 바탕으로 운동 방향을 정리했어요.")
                .messages(messages)
                .hasData(summary.isHasExerciseData())
                .build();
    }

    // --------------------------
    // 📊 분석
    // --------------------------
    private AiCoachResponseDto analysisResponse(CoachSummaryDto summary) {
        List<String> messages = new ArrayList<>();

        messages.add("👉 현재 상태: " + summary.getOverallStatus());

        messages.add(String.format("최근 7일 평균 섭취 칼로리는 약 %.0fkcal 입니다.", summary.getAvgCalories()));
        messages.add(String.format("최근 7일 평균 단백질 섭취는 약 %.0fg 입니다.", summary.getAvgProtein()));
        messages.add(String.format("최근 7일 동안 운동한 날은 %d일입니다.", summary.getExerciseDays()));

        if (summary.getWeightChange() != null) {
            messages.add(String.format("최근 체중 변화는 %.1fkg 입니다.", summary.getWeightChange()));
        } else {
            messages.add("체중 변화는 아직 비교할 기록이 부족합니다.");
        }

        return AiCoachResponseDto.builder()
                .type("analysis")
                .title("최근 기록 분석")
                .summary("최근 7일 기록을 간단하게 요약했어요.")
                .messages(messages)
                .hasData(true)
                .build();
    }

    // --------------------------
    // 💬 피드백
    // --------------------------
    private AiCoachResponseDto feedbackResponse(CoachSummaryDto summary) {
        List<String> messages = new ArrayList<>();

        if (summary.getWeightChange() == null) {
            messages.add("현재 데이터로는 감량 흐름을 명확히 판단하기 어려운 상태입니다.");
        } else if (summary.getWeightChange() > 0.3) {
            messages.add("최근 체중이 증가 흐름입니다.");
            messages.add("저녁 섭취량과 간식 빈도를 먼저 점검하는 게 좋습니다.");
        } else if (summary.getWeightChange() < -0.3) {
            messages.add("최근 체중이 감소 흐름입니다.");
            messages.add("현재 패턴이 잘 맞고 있을 가능성이 높습니다.");
        } else {
            messages.add("체중 변화 폭은 크지 않습니다.");
            messages.add("지금 단계에서는 식단과 운동의 꾸준함이 더 중요합니다.");
        }

        if (summary.getAvgProtein() < 80) {
            messages.add("감량 중이라면 단백질 섭취를 조금 더 늘리는 게 좋습니다.");
        }

        if (summary.getExerciseDays() < 3) {
            messages.add("운동 빈도를 주 3회 수준까지 올리면 감량 흐름이 더 안정적일 수 있습니다.");
        }

        return AiCoachResponseDto.builder()
                .type("feedback")
                .title("감량 피드백")
                .summary("현재 기록 기준으로 감량 흐름을 정리했어요.")
                .messages(messages)
                .hasData(true)
                .build();
    }

    private AiCoachResponseDto defaultResponse() {
        return AiCoachResponseDto.builder()
                .type("default")
                .title("AI 코치")
                .summary("잘못된 요청입니다.")
                .messages(List.of("요청 타입 확인 필요"))
                .hasData(false)
                .build();
    }

    private double nullSafe(Double value) {
        return value == null ? 0 : value;
    }

    private CoachSummaryDto buildCoachSummary(Long memberId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        List<MealLog> meals =
                mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);

        List<ExerciseLog> exercises =
                exerciseLogRepository.findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(
                        memberId, startDate, endDate);

        List<WeightLog> weights =
                weightLogRepository.findByMember_MemberIdAndLogDateBetweenOrderByLogDateAsc(
                        memberId, startDate, endDate);

        double avgCalories = 0;
        double avgProtein = 0;
        double avgCarbs = 0;
        double avgFat = 0;

        if (!meals.isEmpty()) {
            Map<LocalDate, List<MealLog>> grouped =
                    meals.stream().collect(Collectors.groupingBy(MealLog::getMealDate));

            avgCalories = grouped.values().stream()
                    .mapToDouble(list -> list.stream().mapToDouble(m -> nullSafe(m.getCaloriesKcal())).sum())
                    .average()
                    .orElse(0);

            avgProtein = grouped.values().stream()
                    .mapToDouble(list -> list.stream().mapToDouble(m -> nullSafe(m.getProteinG())).sum())
                    .average()
                    .orElse(0);

            avgCarbs = grouped.values().stream()
                    .mapToDouble(list -> list.stream().mapToDouble(m -> nullSafe(m.getCarbG())).sum())
                    .average()
                    .orElse(0);

            avgFat = grouped.values().stream()
                    .mapToDouble(list -> list.stream().mapToDouble(m -> nullSafe(m.getFatG())).sum())
                    .average()
                    .orElse(0);
        }

        int exerciseDays = (int) exercises.stream()
                .map(ExerciseLog::getExerciseDate)
                .distinct()
                .count();

        Double currentWeight = null;
        Double weightChange = null;

        if (!weights.isEmpty()) {
            currentWeight = weights.get(weights.size() - 1).getWeightKg().doubleValue();

            if (weights.size() >= 2) {
                double first = weights.get(0).getWeightKg().doubleValue();
                double last = weights.get(weights.size() - 1).getWeightKg().doubleValue();
                weightChange = last - first;
            }
        }

        String overallStatus = buildOverallStatus(avgCalories, avgProtein, exerciseDays, weightChange, meals.isEmpty(), exercises.isEmpty());

        return CoachSummaryDto.builder()
                .hasMealData(!meals.isEmpty())
                .hasExerciseData(!exercises.isEmpty())
                .hasWeightData(!weights.isEmpty())
                .avgCalories(avgCalories)
                .avgProtein(avgProtein)
                .avgCarbs(avgCarbs)
                .avgFat(avgFat)
                .exerciseDays(exerciseDays)
                .currentWeight(currentWeight)
                .weightChange(weightChange)
                .overallStatus(overallStatus)
                .build();
    }

    private String buildOverallStatus(double avgCalories,
                                      double avgProtein,
                                      int exerciseDays,
                                      Double weightChange,
                                      boolean noMealData,
                                      boolean noExerciseData) {

        if (noMealData && noExerciseData) {
            return "기록이 거의 없어 분석이 어려운 상태";
        }

        if (!noMealData && noExerciseData) {
            return "식단 기록은 있으나 운동 기록이 부족한 상태";
        }

        if (avgProtein < 80 && exerciseDays >= 1) {
            return "운동 대비 단백질 섭취가 부족한 상태";
        }

        if (weightChange != null) {
            if (weightChange > 0.3) {
                return "체중 증가 흐름이 보여 조절이 필요한 상태";
            }
            if (weightChange < -0.3) {
                return "체중 감량 흐름이 비교적 잘 이어지는 상태";
            }
        }

        return "전반적으로 크게 나쁘지 않은 상태";
    }

    private String buildPrompt(CoachSummaryDto summary, String type) {
        return """
        아래는 사용자 최근 7일 기록입니다.

        평균 칼로리: %.0f kcal
        평균 단백질: %.0f g
        평균 탄수화물: %.0f g
        평균 지방: %.0f g
        운동 횟수: %d일
        체중 변화: %s
        현재 상태 요약: %s
        요청 유형: %s

        규칙:
        1. 답변은 한국어로 해주세요.
        2. 3~5줄 정도로 짧고 명확하게 작성하세요.
        3. 사용자가 바로 실천할 수 있는 조언 위주로 말하세요.
        4. 과장하거나 두루뭉술하게 말하지 마세요.
        5. OWN 트레이너처럼 친절하지만 현실적으로 말하세요.
        """.formatted(
                summary.getAvgCalories(),
                summary.getAvgProtein(),
                summary.getAvgCarbs(),
                summary.getAvgFat(),
                summary.getExerciseDays(),
                summary.getWeightChange() == null ? "없음" : String.format("%.1fkg", summary.getWeightChange()),
                summary.getOverallStatus(),
                type
        );
    }
}