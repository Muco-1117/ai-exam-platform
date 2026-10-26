package com.aiexam.service;

import com.aiexam.common.BusinessException;
import com.aiexam.common.UserContext;
import com.aiexam.dto.QuestionDTO;
import com.aiexam.entity.Question;
import com.aiexam.entity.QuestionCategory;
import com.aiexam.mapper.QuestionCategoryMapper;
import com.aiexam.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    public Page<Question> page(Long categoryId, String type, Integer difficulty,
                               String keyword, Integer reviewStatus, long page, long size) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Question::getCategoryId, categoryId);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(Question::getType, type);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (reviewStatus != null) {
            wrapper.eq(Question::getReviewStatus, reviewStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Question::getContent, keyword);
        }
        wrapper.orderByAsc(Question::getId);
        return questionMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /** 题目详情,options解析为JSON数组返回 */
    public Map<String, Object> detail(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BusinessException("题目不存在");
        }
        boolean admin = UserContext.roles().contains("ADMIN");
        if (!admin && !Integer.valueOf(1).equals(q.getReviewStatus())) throw new BusinessException(403, "题目尚未审核通过");
        Map<String, Object> m = toMap(q, !admin);
        return m;
    }

    public void add(QuestionDTO dto, Long userId) {
        Question q = buildQuestion(dto);
        q.setSource("MANUAL");
        q.setReviewStatus(1);
        q.setCreatorId(userId);
        questionMapper.insert(q);
    }

    public void update(Long id, QuestionDTO dto) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("题目不存在");
        }
        Question q = buildQuestion(dto);
        q.setId(id);
        questionMapper.updateById(q);
    }

    public void delete(Long id) {
        questionMapper.deleteById(id);
    }

    public void review(Long id, Integer status) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("题目不存在");
        }
        if (status == null || (status != 0 && status != 1)) throw new BusinessException("审核状态无效");
        if (status == 1 && ("SINGLE".equals(exist.getType()) || "MULTI".equals(exist.getType()))) {
            List<QuestionDTO.OptionItem> options = objectMapper.convertValue(parseOptions(exist.getOptions()),
                    new com.fasterxml.jackson.core.type.TypeReference<List<QuestionDTO.OptionItem>>() {});
            validateChoice(exist.getType(), options, exist.getAnswer());
        }
        Question update = new Question();
        update.setId(id);
        update.setReviewStatus(status);
        questionMapper.updateById(update);
    }

    /** 将题目转为答题卡/详情Map,含分类名;stripAnswer=true时隐藏答案与解析 */
    public Map<String, Object> toMap(Question q, boolean stripAnswer) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", q.getId());
        m.put("categoryId", q.getCategoryId());
        QuestionCategory cat = categoryMapper.selectById(q.getCategoryId());
        m.put("categoryName", cat == null ? "" : cat.getName());
        m.put("type", q.getType());
        m.put("content", q.getContent());
        m.put("options", parseOptions(q.getOptions()));
        m.put("difficulty", q.getDifficulty());
        m.put("score", q.getScore());
        m.put("source", q.getSource());
        m.put("reviewStatus", q.getReviewStatus());
        m.put("createTime", q.getCreateTime());
        if (!stripAnswer) {
            m.put("answer", q.getAnswer());
            m.put("analysis", q.getAnalysis());
        }
        return m;
    }

    public List<Object> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            var raw = objectMapper.readTree(json);
            if (!raw.isArray()) return List.of();
            List<Object> result = new java.util.ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                var option = raw.get(i);
                if (option.isTextual()) {
                    String key = String.valueOf((char) ('A' + i));
                    String text = option.asText().trim();
                    var prefix = java.util.regex.Pattern.compile("^([A-Ha-h])[.、:：)）\\s]+(.+)$", java.util.regex.Pattern.DOTALL).matcher(text);
                    if (prefix.matches()) { key = prefix.group(1).toUpperCase(java.util.Locale.ROOT); text = prefix.group(2).trim(); }
                    result.add(Map.of("key", key, "text", text));
                } else {
                    result.add(objectMapper.convertValue(option, Map.class));
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void validateChoice(String type, List<QuestionDTO.OptionItem> options, String answer) {
        if (options == null || options.size() < 2 || options.size() > 8) throw new BusinessException("选择题需要2至8个选项");
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (var option : options) {
            if (option == null || option.getKey() == null || !option.getKey().matches("[A-H]")
                    || option.getText() == null || option.getText().isBlank()) throw new BusinessException("请补全选项编号A至H及内容");
            if (!keys.add(option.getKey())) throw new BusinessException("选项编号不能重复");
        }
        if (answer == null || answer.isBlank()) throw new BusinessException("请选择正确答案");
        String[] selected = answer.split(",", -1);
        java.util.Set<String> correct = new java.util.HashSet<>();
        for (String key : selected) {
            if (!keys.contains(key)) throw new BusinessException("正确答案必须对应已有的选项编号");
            correct.add(key);
        }
        if ("SINGLE".equals(type) && selected.length != 1) throw new BusinessException("单选题只能有一个正确答案");
        if ("MULTI".equals(type) && correct.size() < 2) throw new BusinessException("多选题至少需要两个正确答案");
    }

    public Question buildQuestion(QuestionDTO dto) {
        if ("SINGLE".equals(dto.getType()) || "MULTI".equals(dto.getType())) {
            validateChoice(dto.getType(), dto.getOptions(), dto.getAnswer());
        }
        Question q = new Question();
        q.setCategoryId(dto.getCategoryId());
        q.setType(dto.getType());
        q.setContent(dto.getContent());
        if (dto.getOptions() != null) {
            try {
                q.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
            } catch (Exception e) {
                throw new BusinessException("选项序列化失败");
            }
        }
        q.setAnswer(dto.getAnswer());
        q.setAnalysis(dto.getAnalysis());
        q.setDifficulty(dto.getDifficulty());
        q.setScore(dto.getScore() == null ? 5 : dto.getScore());
        return q;
    }
}
