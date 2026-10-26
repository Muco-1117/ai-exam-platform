package com.aiexam.common;

import java.util.Set;

/** 当前登录用户上下文(ThreadLocal) */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> ROLES = new ThreadLocal<>();

    public static void set(Long userId, Set<String> roles) {
        USER_ID.set(userId);
        ROLES.set(roles);
    }

    public static Long userId() {
        return USER_ID.get();
    }

    public static Set<String> roles() {
        return ROLES.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLES.remove();
    }
}
