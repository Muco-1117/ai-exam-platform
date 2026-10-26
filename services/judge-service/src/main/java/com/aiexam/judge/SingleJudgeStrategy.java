package com.aiexam.judge;

import com.aiexam.entity.Question;
import org.springframework.stereotype.Component;

/**
 * 单选题判分策略:忽略首尾空格与大小写后精确匹配。
 */
@Component
public class SingleJudgeStrategy implements JudgeStrategy {

    @Override
    public boolean judge(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }
        return question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
    }
}
