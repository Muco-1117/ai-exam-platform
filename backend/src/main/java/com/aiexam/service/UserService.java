package com.aiexam.service;

import com.aiexam.common.BusinessException;
import com.aiexam.common.JwtUtil;
import com.aiexam.dto.LoginDTO;
import com.aiexam.dto.RegisterDTO;
import com.aiexam.entity.Permission;
import com.aiexam.entity.Role;
import com.aiexam.entity.RolePermission;
import com.aiexam.entity.User;
import com.aiexam.entity.UserRole;
import com.aiexam.mapper.PermissionMapper;
import com.aiexam.mapper.RoleMapper;
import com.aiexam.mapper.RolePermissionMapper;
import com.aiexam.mapper.UserMapper;
import com.aiexam.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public Map<String, Object> register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(1);
        userMapper.insert(user);
        // 新用户默认学生角色
        UserRole ur = new UserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(2L);
        userRoleMapper.insert(ur);
        String token = jwtUtil.generate(user.getId());
        return Map.of("token", token, "user", safeUser(user, Set.of("STUDENT")));
    }

    public Map<String, Object> login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        Set<String> roleCodes = roleCodes(user.getId());
        String token = jwtUtil.generate(user.getId());
        return Map.of("token", token, "user", safeUser(user, roleCodes));
    }

    public Map<String, Object> me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        Set<String> roleCodes = roleCodes(userId);
        Map<String, Object> result = safeUser(user, roleCodes);
        // 角色对应的权限编码列表
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)).stream()
                .map(UserRole::getRoleId).toList();
        List<String> permissions = roleIds.isEmpty() ? List.of() :
                rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                                .in(RolePermission::getRoleId, roleIds)).stream()
                        .map(RolePermission::getPermissionId)
                        .distinct()
                        .map(pid -> permissionMapper.selectById(pid))
                        .filter(p -> p != null)
                        .map(Permission::getCode)
                        .toList();
        result.put("permissions", permissions);
        return result;
    }

    public Page<Map<String, Object>> page(String username, long page, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(User::getUsername, username);
        }
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(user -> {
            Map<String, Object> item = safeUser(user, roleCodes(user.getId()));
            List<Role> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, user.getId())).stream()
                    .map(ur -> roleMapper.selectById(ur.getRoleId()))
                    .filter(java.util.Objects::nonNull).toList();
            item.put("roles", roles);
            if (!roles.isEmpty()) {
                item.put("roleId", roles.get(0).getId());
                item.put("roleName", roles.get(0).getName());
            }
            return item;
        }).toList());
        return response;
    }

    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Transactional
    public void updateRole(Long id, Long roleId) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        UserRole ur = new UserRole();
        ur.setUserId(id);
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);
    }

    private Set<String> roleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)).stream()
                .map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getCode).collect(Collectors.toSet());
    }

    private Map<String, Object> safeUser(User user, Set<String> roleCodes) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("nickname", user.getNickname());
        m.put("avatar", user.getAvatar());
        m.put("email", user.getEmail());
        m.put("phone", user.getPhone());
        m.put("status", user.getStatus());
        m.put("createTime", user.getCreateTime());
        m.put("roleCodes", roleCodes);
        return m;
    }
}
