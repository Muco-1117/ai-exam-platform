package com.aiexam.controller;

import com.aiexam.common.RequireRole;
import com.aiexam.common.Result;
import com.aiexam.common.UserContext;
import com.aiexam.dto.QuestionDTO;
import com.aiexam.entity.Question;
import com.aiexam.service.QuestionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(@RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) Integer difficulty,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer reviewStatus,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        boolean admin = UserContext.roles().contains("ADMIN");
        Integer visibleReviewStatus = admin ? reviewStatus : Integer.valueOf(1);
        Page<Question> result = questionService.page(categoryId, type, difficulty, keyword, visibleReviewStatus, page, size);
        Page<Map<String, Object>> mapped = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream()
                .map(q -> questionService.toMap(q, !admin)).toList());
        return Result.ok(mapped);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(questionService.detail(id));
    }

    @RequireRole("ADMIN")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody QuestionDTO dto) {
        questionService.add(dto, UserContext.userId());
        return Result.ok();
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionDTO dto) {
        questionService.update(id, dto);
        return Result.ok();
    }

    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }

    @RequireRole("ADMIN")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        questionService.review(id, body.get("status"));
        return Result.ok();
    }
}
