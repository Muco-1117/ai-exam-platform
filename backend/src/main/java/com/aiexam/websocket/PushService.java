package com.aiexam.websocket;

import com.aiexam.common.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket推送服务:端点 /ws/push?token=JWT
 * 握手时校验token,连接与userId绑定,支持按用户定向推送。
 */
@Slf4j
@Component
@ServerEndpoint("/ws/push")
public class PushService {

    private static JwtUtil jwtUtil;
    private static final Map<Long, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        PushService.jwtUtil = jwtUtil;
    }

    @OnOpen
    public void onOpen(Session session) {
        // 前端以 /ws/push?token=JWT 方式连接,@PathParam 读不到查询参数,须从参数表获取
        String token = null;
        var values = session.getRequestParameterMap().get("token");
        if (values != null && !values.isEmpty()) {
            token = values.get(0);
        }
        Long userId = token == null ? null : jwtUtil.parse(token);
        if (userId == null) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
            return;
        }
        SESSIONS.put(userId, session);
        log.debug("WebSocket connected: userId={}", userId);
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.values().removeIf(s -> s.equals(session));
    }

    /** 按用户推送消息,消息格式 {type, data} */
    public static void push(Long userId, String type, Object data) {
        Session session = SESSIONS.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = MAPPER.writeValueAsString(Map.of("type", type, "data", data));
            synchronized (session) {
                session.getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            log.warn("WebSocket push failed: userId={}", userId, e);
        }
    }
}
