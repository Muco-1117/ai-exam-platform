package com.aiexam.service;

import com.aiexam.common.BusinessException;
import com.aiexam.entity.Favor;
import com.aiexam.entity.Question;
import com.aiexam.entity.Thumb;
import com.aiexam.mapper.FavorMapper;
import com.aiexam.mapper.QuestionMapper;
import com.aiexam.mapper.ThumbMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InteractService {

    private final FavorMapper favorMapper;
    private final ThumbMapper thumbMapper;
    private final QuestionMapper questionMapper;
    private final QuestionService questionService;

    // ---------------- 收藏 ----------------

    public void addFavor(Long userId, Long questionId) {
        if (existsFavor(userId, questionId)) {
            return;
        }
        Favor favor = new Favor();
        favor.setUserId(userId);
        favor.setQuestionId(questionId);
        favorMapper.insert(favor);
    }

    public void removeFavor(Long userId, Long questionId) {
        favorMapper.delete(new LambdaQueryWrapper<Favor>()
                .eq(Favor::getUserId, userId)
                .eq(Favor::getQuestionId, questionId));
    }

    public Page<Map<String, Object>> favorPage(Long userId, long page, long size) {
        Page<Favor> favors = favorMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Favor>()
                        .eq(Favor::getUserId, userId)
                        .orderByDesc(Favor::getCreateTime));
        Page<Map<String, Object>> result = new Page<>(favors.getCurrent(), favors.getSize(), favors.getTotal());
        result.setRecords(favors.getRecords().stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("questionId", f.getQuestionId());
            m.put("createTime", f.getCreateTime());
            m.put("favorTime", f.getCreateTime());
            Question q = questionMapper.selectById(f.getQuestionId());
            if (q != null) {
                m.put("question", questionService.toMap(q, true));
            }
            return m;
        }).toList());
        return result;
    }

    // ---------------- 点赞 ----------------

    /** 点赞(幂等) */
    public void addThumb(Long userId, Long questionId) {
        Long count = thumbMapper.selectCount(new LambdaQueryWrapper<Thumb>()
                .eq(Thumb::getUserId, userId)
                .eq(Thumb::getQuestionId, questionId));
        if (count > 0) {
            return;
        }
        Thumb thumb = new Thumb();
        thumb.setUserId(userId);
        thumb.setQuestionId(questionId);
        thumbMapper.insert(thumb);
    }

    public Long thumbCount(Long questionId) {
        return thumbMapper.selectCount(new LambdaQueryWrapper<Thumb>()
                .eq(Thumb::getQuestionId, questionId));
    }

    public List<Long> myThumbs(Long userId) {
        return thumbMapper.selectList(new LambdaQueryWrapper<Thumb>()
                        .eq(Thumb::getUserId, userId)).stream()
                .map(Thumb::getQuestionId).toList();
    }

    public List<Long> myFavors(Long userId) {
        return favorMapper.selectList(new LambdaQueryWrapper<Favor>()
                        .eq(Favor::getUserId, userId)).stream()
                .map(Favor::getQuestionId).toList();
    }

    private boolean existsFavor(Long userId, Long questionId) {
        return favorMapper.selectCount(new LambdaQueryWrapper<Favor>()
                .eq(Favor::getUserId, userId)
                .eq(Favor::getQuestionId, questionId)) > 0;
    }
}
