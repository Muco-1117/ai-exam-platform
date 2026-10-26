package com.aiexam.controller;

import com.aiexam.common.Result;
import com.aiexam.common.UserContext;
import com.aiexam.dto.PracticeSubmitDTO;
import com.aiexam.service.PracticeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/practice/submit")
    public Result<Map<String, Object>> submit(@Valid @RequestBody PracticeSubmitDTO dto) {
        return Result.ok(practiceService.submit(UserContext.userId(), dto));
    }

    @GetMapping("/practice/page")
    public Result<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(practiceService.page(UserContext.userId(), page, size));
    }

    @GetMapping("/practice/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(practiceService.stats(UserContext.userId()));
    }

    @GetMapping("/wrong/page")
    public Result<Page<Map<String, Object>>> wrongPage(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return Result.ok(practiceService.wrongPage(UserContext.userId(), page, size));
    }

    @PutMapping("/wrong/{questionId}/master")
    public Result<Void> master(@PathVariable Long questionId) {
        practiceService.markMastered(UserContext.userId(), questionId);
        return Result.ok();
    }
}
