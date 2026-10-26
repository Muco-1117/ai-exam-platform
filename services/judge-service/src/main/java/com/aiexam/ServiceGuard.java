package com.aiexam;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Component
public class ServiceGuard extends OncePerRequestFilter {
 @Value("${service.token}") private String token;
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  if(req.getRequestURI().startsWith("/internal/") && !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),String.valueOf(req.getHeader("X-Service-Token")).getBytes(StandardCharsets.UTF_8))) {
   res.setStatus(403); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"Internal service authentication required\"}"); return;
  }
  chain.doFilter(req,res);
 }
}
