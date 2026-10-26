package com.aiexam.controller;

import com.aiexam.ai.AiQuestionService;
import com.aiexam.common.RequireRole;
import com.aiexam.common.Result;
import com.aiexam.common.UserContext;
import com.aiexam.dto.AiGenerateDTO;
import com.aiexam.entity.AiGenerationLog;
import com.aiexam.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AiController {

    private final AiQuestionService aiQuestionService;
    private final UserMapper userMapper;

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@Valid @RequestBody AiGenerateDTO dto) {
        Long logId = aiQuestionService.generate(UserContext.userId(), dto.getCategoryId(),
                dto.getType(), dto.getDifficulty(), dto.getCount(), dto.getKnowledge());
        return Result.ok(Map.of("logId", logId));
    }

    @GetMapping("/log/{logId}")
    public Result<Map<String, Object>> logStatus(@PathVariable Long logId) {
        return Result.ok(aiQuestionService.logStatus(logId));
    }

    @GetMapping("/log/page")
    public Result<Page<AiGenerationLog>> logPage(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(aiQuestionService.logPage(page, size));
    }

    @PostMapping("/generate/{logId}/confirm")
    public Result<Map<String, Object>> confirm(@PathVariable Long logId,
                                               @RequestBody Map<String, Boolean> body) {
        int count = aiQuestionService.confirm(logId, Boolean.TRUE.equals(body.get("confirm")));
        return Result.ok(Map.of("count", count));
    }
}
