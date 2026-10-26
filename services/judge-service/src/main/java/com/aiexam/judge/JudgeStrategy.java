package com.aiexam.judge;

import com.aiexam.entity.Question;

/**
 * 判分策略接口:所有题型的判分实现统一遵循该契约。
 * 新增题型只需实现本接口并在 JudgeFactory 注册。
 */
public interface JudgeStrategy {

    /**
     * @param question   题目(含标准答案)
     * @param userAnswer 用户答案
     * @return 是否判为正确
     */
    boolean judge(Question question, String userAnswer);
}
