package com.aiexam.service;

import com.aiexam.common.BusinessException;
import com.aiexam.dto.PracticeSubmitDTO;
import com.aiexam.entity.PracticeRecord;
import com.aiexam.entity.Question;
import com.aiexam.entity.WrongQuestion;
import com.aiexam.client.JudgeClient;
import com.aiexam.mapper.PracticeRecordMapper;
import com.aiexam.mapper.QuestionMapper;
import com.aiexam.mapper.WrongQuestionMapper;
import com.aiexam.websocket.PushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final com.aiexam.mapper.ExamRecordMapper examRecordMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final JudgeClient judgeClient;
    private final QuestionService questionService;

    @Transactional
    public Map<String, Object> submit(Long userId, PracticeSubmitDTO dto) {
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null || !Integer.valueOf(1).equals(question.getReviewStatus())) {
            throw new BusinessException("题目不存在");
        }
        Long active = examRecordMapper.selectCount(new LambdaQueryWrapper<com.aiexam.entity.ExamRecord>()
                .eq(com.aiexam.entity.ExamRecord::getUserId, userId).eq(com.aiexam.entity.ExamRecord::getStatus, 0));
        if (active > 0) throw new BusinessException(403, "考试进行中，请交卷后再练习或查看解析");
        boolean correct = judgeClient.judge(question, dto.getUserAnswer());

        // 练习记录
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setQuestionId(dto.getQuestionId());
        record.setUserAnswer(dto.getUserAnswer());
        record.setIsCorrect(correct ? 1 : 0);
        practiceRecordMapper.insert(record);

        // 错题本联动:答错upsert累计,答对标记已掌握
        WrongQuestion exist = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, dto.getQuestionId()));
        if (!correct) {
            if (exist == null) {
                WrongQuestion wq = new WrongQuestion();
                wq.setUserId(userId);
                wq.setQuestionId(dto.getQuestionId());
                wq.setWrongCount(1);
                wq.setMastered(0);
                wq.setLastWrongAt(LocalDateTime.now());
                wrongQuestionMapper.insert(wq);
            } else {
                exist.setWrongCount(exist.getWrongCount() + 1);
                exist.setMastered(0);
                exist.setLastWrongAt(LocalDateTime.now());
                wrongQuestionMapper.updateById(exist);
            }
        } else if (exist != null) {
            exist.setMastered(1);
            wrongQuestionMapper.updateById(exist);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", correct);
        result.put("correctAnswer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        result.put("score", correct ? question.getScore() : 0);
        // WebSocket实时推送判分结果
        PushService.push(userId, "PRACTICE_RESULT",
                Map.of("questionId", dto.getQuestionId(), "isCorrect", correct));
        return result;
    }

    public Page<Map<String, Object>> page(Long userId, long page, long size) {
        Page<PracticeRecord> records = practiceRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .orderByDesc(PracticeRecord::getCreateTime));
        Page<Map<String, Object>> result = new Page<>(records.getCurrent(), records.getSize(), records.getTotal());
        result.setRecords(records.getRecords().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("questionId", r.getQuestionId());
            m.put("userAnswer", r.getUserAnswer());
            m.put("isCorrect", r.getIsCorrect());
            m.put("createTime", r.getCreateTime());
            Question q = questionMapper.selectById(r.getQuestionId());
            m.put("score", q != null && Integer.valueOf(1).equals(r.getIsCorrect()) ? q.getScore() : 0);
            if (q != null) {
                m.put("question", questionService.toMap(q, true));
            }
            return m;
        }).toList());
        return result;
    }

    public Map<String, Object> stats(Long userId) {
        List<PracticeRecord> all = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>().eq(PracticeRecord::getUserId, userId));
        long total = all.size();
        long correct = all.stream().filter(r -> r.getIsCorrect() == 1).count();
        Map<String, Object> m = new HashMap<>();
        m.put("totalCount", total);
        m.put("correctCount", correct);
        m.put("correctRate", total == 0 ? 0 : Math.round(correct * 1000.0 / total) / 10.0);
        // 分题型统计
        Map<String, Map<String, Object>> byType = new HashMap<>();
        for (PracticeRecord r : all) {
            Question q = questionMapper.selectById(r.getQuestionId());
            if (q == null) {
                continue;
            }
            Map<String, Object> stat = byType.computeIfAbsent(q.getType(), k -> {
                Map<String, Object> s = new HashMap<>();
                s.put("type", k);
                s.put("total", 0);
                s.put("correct", 0);
                return s;
            });
            stat.put("total", (int) stat.get("total") + 1);
            if (r.getIsCorrect() == 1) {
                stat.put("correct", (int) stat.get("correct") + 1);
            }
        }
        m.put("byType", byType.values());
        return m;
    }

    /** 错题本分页 */
    public Page<Map<String, Object>> wrongPage(Long userId, long page, long size) {
        Page<WrongQuestion> wrongs = wrongQuestionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getMastered, 0)
                        .orderByDesc(WrongQuestion::getLastWrongAt));
        Page<Map<String, Object>> result = new Page<>(wrongs.getCurrent(), wrongs.getSize(), wrongs.getTotal());
        result.setRecords(wrongs.getRecords().stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", w.getId());
            m.put("questionId", w.getQuestionId());
            m.put("mastered", w.getMastered());
            m.put("wrongCount", w.getWrongCount());
            m.put("lastWrongAt", w.getLastWrongAt());
            Question q = questionMapper.selectById(w.getQuestionId());
            if (q != null) {
                m.put("question", questionService.toMap(q, true));
            }
            return m;
        }).toList());
        return result;
    }

    public void markMastered(Long userId, Long questionId) {
        WrongQuestion exist = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
        if (exist == null) {
            throw new BusinessException("该题目不在错题本中");
        }
        exist.setMastered(1);
        wrongQuestionMapper.updateById(exist);
    }

    /**
     * 返回到期的错题复习队列。间隔按错误次数递增，避免用户每天重复刷同一批题。
     * 复习队列只返回不含答案的题卡，提交仍统一走 /practice/submit。
     */
    public Map<String, Object> reviewSet(Long userId, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 50));
        LocalDateTime now = LocalDateTime.now();
        List<WrongQuestion> active = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getMastered, 0));
        List<WrongQuestion> due = active.stream()
                .filter(item -> isDue(item, now))
                .sorted(Comparator.comparingInt((WrongQuestion item) -> item.getWrongCount() == null ? 1 : item.getWrongCount())
                        .reversed()
                        .thenComparing(WrongQuestion::getLastWrongAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
        List<Map<String, Object>> items = new ArrayList<>();
        for (WrongQuestion item : due) {
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question == null || !Integer.valueOf(1).equals(question.getReviewStatus())) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("questionId", item.getQuestionId());
            row.put("wrongCount", item.getWrongCount());
            row.put("lastWrongAt", item.getLastWrongAt());
            row.put("nextReviewAt", nextReviewAt(item, now));
            row.put("question", questionService.toMap(question, true));
            items.add(row);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("dueCount", items.size());
        result.put("activeCount", active.size());
        result.put("limit", limit);
        return result;
    }

    private boolean isDue(WrongQuestion item, LocalDateTime now) {
        if (item.getLastWrongAt() == null) return true;
        return !nextReviewAt(item, now).isAfter(now);
    }

    private LocalDateTime nextReviewAt(WrongQuestion item, LocalDateTime now) {
        if (item.getLastWrongAt() == null) return now;
        int wrongCount = Math.max(1, item.getWrongCount() == null ? 1 : item.getWrongCount());
        int intervalDays = switch (Math.min(wrongCount, 5)) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 14;
            default -> 30;
        };
        return item.getLastWrongAt().plus(intervalDays, ChronoUnit.DAYS);
    }
}
