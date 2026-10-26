package com.aiexam.service;

import com.aiexam.common.BusinessException;
import com.aiexam.entity.Question;
import com.aiexam.entity.QuestionCategory;
import com.aiexam.mapper.QuestionCategoryMapper;
import com.aiexam.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final QuestionCategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;

    public List<QuestionCategory> list() {
        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(QuestionCategory::getSort);
        return categoryMapper.selectList(wrapper);
    }

    public void add(QuestionCategory category) {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getName, category.getName()));
        if (count > 0) {
            throw new BusinessException("分类名称已存在");
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        categoryMapper.insert(category);
    }

    public void update(Long id, QuestionCategory dto) {
        QuestionCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("分类不存在");
        }
        if (dto.getName() != null && !dto.getName().equals(exist.getName())) {
            Long duplicates = categoryMapper.selectCount(new LambdaQueryWrapper<QuestionCategory>()
                    .eq(QuestionCategory::getName, dto.getName()).ne(QuestionCategory::getId, id));
            if (duplicates > 0) throw new BusinessException("分类名称已存在");
        }
        exist.setName(dto.getName() != null ? dto.getName() : exist.getName());
        exist.setDescription(dto.getDescription() != null ? dto.getDescription() : exist.getDescription());
        if (dto.getSort() != null) {
            exist.setSort(dto.getSort());
        }
        categoryMapper.updateById(exist);
    }

    public void delete(Long id) {
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException("该分类下存在题目,无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
