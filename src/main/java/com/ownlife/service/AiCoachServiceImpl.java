package com.ownlife.service;

import com.ownlife.dto.AiCoachResponseDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiCoachServiceImpl implements AiCoachService {

    @Override
    public AiCoachResponseDto getRecommendation(Long memberId, String type) {
        // TODO: 다음 단계에서 실제 DB 조회 연결
        // 지금은 1차 동작 확인용 임시 데이터

        List<String> messages = new ArrayList<>();
        String title = "";
        String summary = "";

        switch (type) {
            case "diet":
                title = "오늘 식단 추천";
                summary = "최근 기록 기준으로 식단 밸런스를 조정해보세요.";
                messages.add("아침은 가볍게 먹더라도 단백질은 꼭 포함하세요.");
                messages.add("점심은 일반식 가능하지만 탄수화물 양은 과하지 않게 조절하세요.");
                messages.add("저녁은 늦게 먹는다면 탄수화물을 줄이고 단백질 위주로 구성하세요.");
                break;

            case "exercise":
                title = "오늘 운동 추천";
                summary = "최근 운동 빈도를 고려해 무리 없는 루틴을 추천합니다.";
                messages.add("오늘은 20~30분 유산소 + 가벼운 근력운동 조합이 좋습니다.");
                messages.add("상체 위주 운동을 했다면 하체나 코어 중심으로 균형을 맞추세요.");
                messages.add("운동 강도보다 꾸준함이 더 중요하니 무리하지 마세요.");
                break;

            case "analysis":
                title = "최근 기록 분석";
                summary = "최근 기록을 기반으로 현재 패턴을 간단히 분석했어요.";
                messages.add("기록이 누적되면 식단과 운동 패턴을 더 정확히 분석할 수 있습니다.");
                messages.add("지금은 식사 시간과 운동 종류를 꾸준히 남기는 것이 가장 중요합니다.");
                messages.add("최소 3일 이상 기록이 쌓이면 맞춤 추천 정확도가 올라갑니다.");
                break;

            case "feedback":
                title = "감량 피드백";
                summary = "감량 목표 기준으로 기본 피드백을 드릴게요.";
                messages.add("감량 중이라면 총칼로리보다 저녁 과식을 먼저 줄이는 게 효과적입니다.");
                messages.add("단백질 섭취와 운동 빈도가 함께 유지되어야 체중 감량이 안정적입니다.");
                messages.add("기록이 쌓이면 체중 변화와 함께 더 정확한 피드백이 가능합니다.");
                break;

            default:
                title = "AI 코치";
                summary = "요청한 추천 타입을 확인해주세요.";
                messages.add("올바른 추천 요청이 아닙니다.");
        }

        return AiCoachResponseDto.builder()
                .type(type)
                .title(title)
                .summary(summary)
                .messages(messages)
                .hasData(true)
                .build();
    }
}