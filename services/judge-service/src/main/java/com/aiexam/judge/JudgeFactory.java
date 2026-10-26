package com.aiexam.judge;

import com.aiexam.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 判分工厂:按题型路由到对应策略。
 * 新增题型时新增策略实现类并在静态块注册即可,符合开闭原则。
 */
@Component
public class JudgeFactory {

    private static final Map<String, JudgeStrategy> STRATEGIES = new ConcurrentHashMap<>();

    public JudgeFactory(SingleJudgeStrategy single,
                        MultiJudgeStrategy multi,
                        JudgeJudgeStrategy judge,
                        FillJudgeStrategy fill) {
        STRATEGIES.put("SINGLE", single);
        STRATEGIES.put("MULTI", multi);
        STRATEGIES.put("JUDGE", judge);
        STRATEGIES.put("FILL", fill);
    }

    public JudgeStrategy get(String type) {
        JudgeStrategy strategy = STRATEGIES.get(type);
        if (strategy == null) {
            throw new BusinessException("不支持的题型:" + type);
        }
        return strategy;
    }
}
