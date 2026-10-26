package com.aiexam.controller;

import com.aiexam.common.RequireRole;
import com.aiexam.common.Result;
import com.aiexam.entity.QuestionCategory;
import com.aiexam.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<QuestionCategory>> list() {
        return Result.ok(categoryService.list());
    }

    @RequireRole("ADMIN")
    @PostMapping
    public Result<Void> add(@RequestBody QuestionCategory category) {
        categoryService.add(category);
        return Result.ok();
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody QuestionCategory category) {
        categoryService.update(id, category);
        return Result.ok();
    }

    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
