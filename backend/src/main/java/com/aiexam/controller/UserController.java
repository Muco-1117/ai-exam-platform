package com.aiexam.controller;

import com.aiexam.common.RequireRole;
import com.aiexam.common.Result;
import com.aiexam.common.UserContext;
import com.aiexam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(userService.me(UserContext.userId()));
    }

    @RequireRole("ADMIN")
    @GetMapping("/page")
    public Result<Object> page(@RequestParam(required = false) String username,
                               @RequestParam(defaultValue = "1") long page,
                               @RequestParam(defaultValue = "10") long size) {
        return Result.ok(userService.page(username, page, size));
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam Long roleId) {
        userService.updateRole(id, roleId);
        return Result.ok();
    }
}
