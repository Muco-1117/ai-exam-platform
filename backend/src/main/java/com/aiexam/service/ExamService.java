package com.aiexam.service;

import com.aiexam.common.BusinessException;
import com.aiexam.dto.AnswerSaveDTO;
import com.aiexam.dto.ExamCreateDTO;
import com.aiexam.entity.*;
import com.aiexam.client.JudgeClient;
import com.aiexam.mapper.*;
import com.aiexam.websocket.PushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考试服务:组卷、发布、开考(Redis缓存倒计时)、草稿保存、
 * 交卷判分(幂等)、定时自动交卷、成绩与排行榜。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final JudgeClient judgeClient;
    private final QuestionService questionService;
    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------------- 管理端 ----------------

    @Transactional
    public Long create(ExamCreateDTO dto, Long userId) {
        Exam exam = new Exam();
        exam.setName(dto.getName());
        exam.setCategoryId(dto.getCategoryId());
        exam.setDuration(dto.getDuration());
        exam.setPassScore(dto.getPassScore());
        exam.setStatus(0);
        exam.setCreatorId(userId);
        int total = dto.getQuestions().stream()
                .mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum();
        exam.setTotalScore(total);
        examMapper.insert(exam);
        int sort = 1;
        for (ExamCreateDTO.QuestionItem item : dto.getQuestions()) {
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question == null || !Integer.valueOf(1).equals(question.getReviewStatus())) {
                throw new BusinessException("题目不存在:" + item.getQuestionId());
            }
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(exam.getId());
            eq.setQuestionId(item.getQuestionId());
            eq.setScore(item.getScore());
            eq.setSort(sort++);
            examQuestionMapper.insert(eq);
        }
        return exam.getId();
    }

    public Page<Exam> page(long page, long size) {
        return examMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getCreateTime));
    }

    public List<Map<String, Object>> publishedList(Long userId) {
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, 1).orderByDesc(Exam::getCreateTime));
        Map<Long, ExamRecord> ownRecords = new HashMap<>();
        examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .orderByDesc(ExamRecord::getStartTime))
                .forEach(r -> ownRecords.putIfAbsent(r.getExamId(), r));
        return exams.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("name", e.getName());
            m.put("duration", e.getDuration());
            m.put("totalScore", e.getTotalScore());
            m.put("passScore", e.getPassScore());
            Long count = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                    .eq(ExamQuestion::getExamId, e.getId()));
            m.put("questionCount", count);
            m.put("createTime", e.getCreateTime());
            ExamRecord ownRecord = ownRecords.get(e.getId());
            m.put("recordId", ownRecord == null ? null : ownRecord.getId());
            m.put("recordStatus", ownRecord == null ? null : ownRecord.getStatus());
            return m;
        }).toList();
    }

    public void publish(Long id) {
        Exam exam = getExam(id);
        Long count = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, id));
        if (count == 0) {
            throw new BusinessException("考试还没有题目,无法发布");
        }
        Exam update = new Exam();
        update.setId(id);
        update.setStatus(1);
        examMapper.updateById(update);
    }

    @Transactional
    public void delete(Long id) {
        getExam(id);
        if(examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId,id)) > 0) throw new BusinessException("已有考生记录，不能删除考试");
        examMapper.deleteById(id);
        List<ExamQuestion> eqs = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        eqs.forEach(eq -> examQuestionMapper.deleteById(eq.getId()));
    }

    // ---------------- 学生端 ----------------

    /** 开始考试:存在进行中记录则返回原进度(防刷新重置) */
    @Transactional
    public Map<String, Object> start(Long examId, Long userId) {
        userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId).last("FOR UPDATE"));
        Exam exam = getExam(examId);
        if (exam.getStatus() != 1) {
            throw new BusinessException("考试未发布");
        }
        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, 0)
                .orderByDesc(ExamRecord::getStartTime)
                .last("LIMIT 1"));
        if (record == null) {
            // 已交卷则不允许重复考试
            Long done = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, examId)
                    .eq(ExamRecord::getUserId, userId)
                    .eq(ExamRecord::getStatus, 1));
            if (done > 0) {
                throw new BusinessException("您已参加过该考试");
            }
            record = new ExamRecord();
            record.setExamId(examId);
            record.setUserId(userId);
            record.setStartTime(LocalDateTime.now());
            record.setDeadline(LocalDateTime.now().plusMinutes(exam.getDuration()));
            record.setStatus(0);
            examRecordMapper.insert(record);
            // Redis缓存倒计时状态,TTL=时长+60秒
            redisTemplate.opsForValue().set("exam:record:" + record.getId(),
                    record.getDeadline().format(FMT),
                    Duration.ofSeconds(exam.getDuration() * 60L + 60));
        }
        return buildTakingView(exam, record);
    }

    /** 保存单题答案草稿(幂等upsert):并发插入撞唯一索引时回退为更新 */
    @Transactional
    public void saveAnswer(Long userId, AnswerSaveDTO dto) {
        ExamRecord record = lockedRecord(dto.getRecordId());
        if (!record.getUserId().equals(userId)) throw new BusinessException(403, "无权限操作他人考试记录");
        if (!LocalDateTime.now().isBefore(record.getDeadline())) throw new BusinessException(409, "考试时间已到，不能再作答");
        Long belongs = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, record.getExamId()).eq(ExamQuestion::getQuestionId, dto.getQuestionId()));
        if (belongs == 0) throw new BusinessException(400, "题目不属于本次试卷");
        if (record.getStatus() != 0) {
            throw new BusinessException("考试已交卷,不能再作答");
        }
        try {
            upsertAnswer(dto);
        } catch (DuplicateKeyException e) {
            upsertAnswer(dto);
        }
    }

    private void upsertAnswer(AnswerSaveDTO dto) {
        ExamAnswer exist = getAnswer(dto.getRecordId(), dto.getQuestionId());
        if (exist == null) {
            ExamAnswer answer = new ExamAnswer();
            answer.setRecordId(dto.getRecordId());
            answer.setQuestionId(dto.getQuestionId());
            answer.setUserAnswer(dto.getUserAnswer());
            examAnswerMapper.insert(answer);
        } else {
            exist.setUserAnswer(dto.getUserAnswer());
            examAnswerMapper.updateById(exist);
        }
    }

    /** 按记录+题目取作答记录:用 selectList 兜底,即使历史数据有重复行也不会抛 TooManyResults */
    private ExamAnswer getAnswer(Long recordId, Long questionId) {
        List<ExamAnswer> list = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId)
                .orderByAsc(ExamAnswer::getId));
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /** 交卷判分(幂等):重复交卷返回上次结果 */
    @Transactional
    public Map<String, Object> submit(Long recordId, Long userId) {
        ExamRecord record = lockedRecord(recordId);
        if (!record.getUserId().equals(userId)) throw new BusinessException(403, "无权限操作他人考试记录");
        if (record.getStatus() == 1) {
            return resultView(record);
        }
        int totalScore = judgeAndSave(record);
        record.setStatus(1);
        record.setSubmitTime(LocalDateTime.now());
        record.setTotalScore(totalScore);
        examRecordMapper.updateById(record);
        // WebSocket推送出分通知
        Exam exam = examMapper.selectById(record.getExamId());
        PushService.push(userId, "EXAM_SCORED", Map.of(
                "recordId", recordId,
                "examName", exam.getName(),
                "totalScore", totalScore));
        return resultView(record);
    }

    /** 默认查询本人；控制器校验管理员权限后才允许查询全部考生。 */
    public Page<Map<String, Object>> recordPage(Long userId, boolean allRecords, long page, long size) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        if (!allRecords) {
            wrapper.eq(ExamRecord::getUserId, userId);
        }
        wrapper.orderByDesc(ExamRecord::getStartTime);
        Page<ExamRecord> records = examRecordMapper.selectPage(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> result = new Page<>(records.getCurrent(), records.getSize(), records.getTotal());
        result.setRecords(records.getRecords().stream().map(this::recordMap).toList());
        return result;
    }

    /** 考试详情:答卷+逐题判分明细 */
    public Map<String, Object> recordDetail(Long recordId, Long userId, boolean admin) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) throw new BusinessException("考试记录不存在");
        if (!admin && record.getStatus() == 0) throw new BusinessException(403, "交卷后方可查看答案解析");
        if (!admin && !record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限查看");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        Map<String, Object> m = recordMap(record);
        m.put("examDuration", exam == null ? 0 : exam.getDuration());
        m.put("record", recordMap(record));
        List<ExamQuestion> eqs = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, record.getExamId())
                        .orderByAsc(ExamQuestion::getSort));
        List<Map<String, Object>> detail = new ArrayList<>();
        for (ExamQuestion eq : eqs) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            ExamAnswer answer = getAnswer(recordId, eq.getQuestionId());
            Map<String, Object> d = new HashMap<>();
            d.put("question", q == null ? null : questionService.toMap(q, false));
            d.put("score", eq.getScore());
            d.put("userAnswer", answer == null ? null : answer.getUserAnswer());
            d.put("isCorrect", answer == null ? null : answer.getIsCorrect());
            d.put("gotScore", answer == null ? 0 : answer.getScore());
            detail.add(d);
        }
        m.put("detail", detail);
        return m;
    }

    /** 考试排行榜(按得分降序,前50) */
    public List<Map<String, Object>> rank(Long examId) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .eq(ExamRecord::getStatus, 1)
                        .isNotNull(ExamRecord::getTotalScore)
                        .orderByDesc(ExamRecord::getTotalScore)
                        .orderByAsc(ExamRecord::getSubmitTime)
                        .last("LIMIT 50"));
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (ExamRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("rank", rank++);
            User user = userMapper.selectById(r.getUserId());
            m.put("nickname", user == null ? "未知用户" : user.getNickname());
            m.put("totalScore", r.getTotalScore());
            m.put("submitTime", r.getSubmitTime());
            result.add(m);
        }
        return result;
    }

    // ---------------- 定时自动交卷 ----------------

    /** 每30秒扫描到期未交卷的考试记录并自动判分 */
    @Scheduled(fixedDelay = 30_000)
    public void autoSubmitExpired() {
        List<ExamRecord> expired = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getStatus, 0)
                        .lt(ExamRecord::getDeadline, LocalDateTime.now()));
        for (ExamRecord record : expired) {
            try { transactionTemplate.executeWithoutResult(status -> {
                ExamRecord locked=lockedRecord(record.getId());
                if (locked.getStatus() != 0) return;
                int score=judgeAndSave(locked); locked.setStatus(1);locked.setSubmitTime(LocalDateTime.now());locked.setTotalScore(score);examRecordMapper.updateById(locked);
                PushService.push(locked.getUserId(), "EXAM_FORCE_SUBMIT", Map.of("recordId",locked.getId(),"totalScore",score));
            }); } catch (Exception e) { log.error("自动交卷失败，将在下一轮重试: recordId={}",record.getId(),e); }
        }
    }

    // ---------------- 私有方法 ----------------

    private int judgeAndSave(ExamRecord record) {
        List<ExamQuestion> eqs = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, record.getExamId()));
        int total = 0;
        int correctCount = 0;
        for (ExamQuestion eq : eqs) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            ExamAnswer answer = getAnswer(record.getId(), eq.getQuestionId());
            int got = 0;
            boolean correct = false;
            if (q != null && answer != null && answer.getUserAnswer() != null
                    && !answer.getUserAnswer().isBlank()) {
                correct = judgeClient.judge(q, answer.getUserAnswer());
                if (correct) {
                    got = eq.getScore();
                }
            }
            if (answer != null) {
                answer.setIsCorrect(correct ? 1 : 0);
                answer.setScore(got);
                examAnswerMapper.updateById(answer);
            }
            total += got;
            if (correct) {
                correctCount++;
            }
        }
        record.setCorrectCount(correctCount);
        return total;
    }

    private Map<String, Object> buildTakingView(Exam exam, ExamRecord record) {
        Map<String, Object> m = new HashMap<>();
        m.put("recordId", record.getId());
        m.put("examId", exam.getId());
        m.put("examName", exam.getName());
        m.put("duration", exam.getDuration());
        m.put("totalScore", exam.getTotalScore());
        m.put("passScore", exam.getPassScore());
        m.put("deadline", record.getDeadline().format(FMT));
        List<ExamQuestion> eqs = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, exam.getId())
                        .orderByAsc(ExamQuestion::getSort));
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamQuestion eq : eqs) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            if (q == null) {
                continue;
            }
            Map<String, Object> qm = questionService.toMap(q, true);
            qm.put("paperScore", eq.getScore());
            qm.put("score", eq.getScore());
            ExamAnswer answer = getAnswer(record.getId(), q.getId());
            qm.put("savedAnswer", answer == null ? null : answer.getUserAnswer());
            questions.add(qm);
        }
        m.put("questions", questions);
        return m;
    }

    private Map<String, Object> resultView(ExamRecord record) {
        Map<String, Object> m = new HashMap<>();
        Exam exam = examMapper.selectById(record.getExamId());
        m.put("recordId", record.getId());
        m.put("totalScore", record.getTotalScore());
        m.put("correctCount", record.getCorrectCount());
        m.put("passScore", exam == null ? 60 : exam.getPassScore());
        m.put("totalPaperScore", exam == null ? 100 : exam.getTotalScore());
        return m;
    }

    private Map<String, Object> recordMap(ExamRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("examId", r.getExamId());
        Exam exam = examMapper.selectById(r.getExamId());
        m.put("examName", exam == null ? "" : exam.getName());
        m.put("userId", r.getUserId());
        User user = userMapper.selectById(r.getUserId());
        m.put("nickname", user == null ? "" : user.getNickname());
        m.put("startTime", r.getStartTime());
        m.put("submitTime", r.getSubmitTime());
        m.put("deadline", r.getDeadline());
        m.put("status", r.getStatus());
        m.put("totalScore", r.getTotalScore());
        m.put("correctCount", r.getCorrectCount());
        return m;
    }

    private ExamRecord lockedRecord(Long id) {
        ExamRecord record=examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getId,id).last("FOR UPDATE"));
        if(record==null) throw new BusinessException("考试记录不存在");
        return record;
    }

    private Exam getExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        return exam;
    }

    private ExamRecord getOwnRecord(Long recordId, Long userId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("考试记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作他人考试记录");
        }
        return record;
    }
}
