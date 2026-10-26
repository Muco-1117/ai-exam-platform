package com.aiexam.judge;

import com.aiexam.entity.Question;
import org.springframework.stereotype.Component;

/**
 * 判断题判分策略:接受 true/false(忽略大小写)。
 */
@Component
public class JudgeJudgeStrategy implements JudgeStrategy {

    @Override
    public boolean judge(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }
        return question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
    }
}
