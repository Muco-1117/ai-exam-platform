package com.aiexam.common;

import com.aiexam.entity.Role;
import com.aiexam.entity.UserRole;
import com.aiexam.mapper.RoleMapper;
import com.aiexam.mapper.UserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** JWT认证 + @RequireRole角色校验拦截器 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final com.aiexam.mapper.UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Long userId = jwtUtil.parse(auth.substring(7));
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        var user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(401, "账号已禁用或不存在，请重新登录");
        Set<String> roles = loadRoles(userId);
        UserContext.set(userId, roles);

        RequireRole requireRole = hm.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            for (String role : requireRole.value()) {
                if (roles.contains(role)) {
                    return true;
                }
            }
            throw new BusinessException(403, "无权限访问");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private Set<String> loadRoles(Long userId) {
        List<UserRole> urs = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId));
        Set<String> codes = new HashSet<>();
        for (UserRole ur : urs) {
            Role role = roleMapper.selectById(ur.getRoleId());
            if (role != null) {
                codes.add(role.getCode());
            }
        }
        return codes;
    }
}
