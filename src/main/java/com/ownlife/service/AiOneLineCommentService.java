package com.ownlife.service;

import com.ownlife.dto.AiOneLineCommentDto;
import com.ownlife.dto.LifestylePatternAnalysisDto;
import com.ownlife.entity.Member;

public interface AiOneLineCommentService {

    AiOneLineCommentDto generateComment(Member member,
                                        LifestylePatternAnalysisDto lifestylePatternAnalysis,
                                        String weightGoalMessage);

    default AiOneLineCommentDto generateComment(Member member,
                                                LifestylePatternAnalysisDto lifestylePatternAnalysis,
                                                String weightGoalMessage,
                                                boolean forceRefresh) {
        return generateComment(member, lifestylePatternAnalysis, weightGoalMessage);
    }
}

