package com.aiexam.judge;

import com.aiexam.entity.Question;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 填空题判分策略:标准答案支持多个候选(以 | 分隔),
 * 任一候选匹配即正确;比较时忽略首尾空格与大小写。
 */
@Component
public class FillJudgeStrategy implements JudgeStrategy {

    @Override
    public boolean judge(Question question, String userAnswer) {
        if (userAnswer == null) {
            return false;
        }
        String actual = userAnswer.trim();
        if (actual.isEmpty()) {
            return false;
        }
        return Arrays.stream(question.getAnswer().split("\\|"))
                .map(String::trim)
                .anyMatch(expected -> expected.equalsIgnoreCase(actual));
    }
}
