package com.aiexam.controller;

import com.aiexam.common.RequireRole;
import com.aiexam.common.BusinessException;
import com.aiexam.common.Result;
import com.aiexam.common.UserContext;
import com.aiexam.dto.AnswerSaveDTO;
import com.aiexam.dto.ExamCreateDTO;
import com.aiexam.entity.Exam;
import com.aiexam.service.ExamService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> publishedList() {
        return Result.ok(examService.publishedList(UserContext.userId()));
    }

    @RequireRole("ADMIN")
    @GetMapping("/page")
    public Result<Page<Exam>> page(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "10") long size) {
        return Result.ok(examService.page(page, size));
    }

    @RequireRole("ADMIN")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ExamCreateDTO dto) {
        return Result.ok(examService.create(dto, UserContext.userId()));
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        examService.publish(id);
        return Result.ok();
    }

    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{examId}/start")
    public Result<Map<String, Object>> start(@PathVariable Long examId) {
        return Result.ok(examService.start(examId, UserContext.userId()));
    }

    @PostMapping("/answer")
    public Result<Void> saveAnswer(@Valid @RequestBody AnswerSaveDTO dto) {
        examService.saveAnswer(UserContext.userId(), dto);
        return Result.ok();
    }

    @PostMapping("/record/{recordId}/submit")
    public Result<Map<String, Object>> submit(@PathVariable Long recordId) {
        return Result.ok(examService.submit(recordId, UserContext.userId()));
    }

    @GetMapping("/record/page")
    public Result<Page<Map<String, Object>>> recordPage(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(defaultValue = "mine") String scope) {
        if (!"mine".equals(scope) && !"all".equals(scope)) {
            throw new BusinessException(400, "不支持的考试记录范围");
        }
        boolean admin = UserContext.roles().contains("ADMIN");
        boolean allRecords = "all".equals(scope);
        if (allRecords && !admin) {
            throw new BusinessException(403, "仅管理员可以查看全部考生记录");
        }
        return Result.ok(examService.recordPage(UserContext.userId(), allRecords, page, size));
    }

    @GetMapping("/record/{recordId}/detail")
    public Result<Map<String, Object>> recordDetail(@PathVariable Long recordId) {
        boolean admin = UserContext.roles().contains("ADMIN");
        return Result.ok(examService.recordDetail(recordId, UserContext.userId(), admin));
    }

    @GetMapping("/rank/{examId}")
    public Result<List<Map<String, Object>>> rank(@PathVariable Long examId) {
        return Result.ok(examService.rank(examId));
    }
}
