package com.aiexam.controller;

import com.aiexam.common.Result;
import com.aiexam.common.UserContext;
import com.aiexam.service.InteractService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InteractController {

    private final InteractService interactService;

    @PostMapping("/favor/{questionId}")
    public Result<Void> addFavor(@PathVariable Long questionId) {
        interactService.addFavor(UserContext.userId(), questionId);
        return Result.ok();
    }

    @DeleteMapping("/favor/{questionId}")
    public Result<Void> removeFavor(@PathVariable Long questionId) {
        interactService.removeFavor(UserContext.userId(), questionId);
        return Result.ok();
    }

    @GetMapping("/favor/page")
    public Result<Page<Map<String, Object>>> favorPage(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactService.favorPage(UserContext.userId(), page, size));
    }

    @GetMapping("/favor/ids")
    public Result<List<Long>> favorIds() {
        return Result.ok(interactService.myFavors(UserContext.userId()));
    }

    @PostMapping("/thumb/{questionId}")
    public Result<Void> addThumb(@PathVariable Long questionId) {
        interactService.addThumb(UserContext.userId(), questionId);
        return Result.ok();
    }

    @GetMapping("/thumb/count")
    public Result<Long> thumbCount(@RequestParam Long questionId) {
        return Result.ok(interactService.thumbCount(questionId));
    }

    @GetMapping("/thumb/mine")
    public Result<List<Long>> myThumbs() {
        return Result.ok(interactService.myThumbs(UserContext.userId()));
    }
}
