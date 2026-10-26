package com.aiexam.judge;

import com.aiexam.entity.Question;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 多选题判分策略:两侧答案按逗号拆分、去重后做集合相等比较。
 * 多选、漏选、错选任一情况均判错。
 */
@Component
public class MultiJudgeStrategy implements JudgeStrategy {

    @Override
    public boolean judge(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }
        Set<String> expected = split(question.getAnswer());
        Set<String> actual = split(userAnswer);
        return expected.equals(actual);
    }

    private Set<String> split(String s) {
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
}
