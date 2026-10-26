package com.aiexam.controller;

import com.aiexam.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.util.*;

@RestController
public class SystemController {
    private final RestClient client;
    private final String judgeUrl, aiUrl, gatewayUrl;
    public SystemController(@Value("${services.judge-url}") String judgeUrl, @Value("${services.ai-url}") String aiUrl, @Value("${services.gateway-url}") String gatewayUrl) {
        this.judgeUrl=judgeUrl;this.aiUrl=aiUrl;this.gatewayUrl=gatewayUrl;
        var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(1000);f.setReadTimeout(1500);
        client=RestClient.builder().requestFactory(f).build();
    }
    @GetMapping("/health") public Map<String,Object> health(){return Map.of("service","exam-service","status","UP","version","2.0.0");}
    @GetMapping("/api/system/topology") @RequireRole("ADMIN")
    public Result<Map<String,Object>> topology() {
        List<Map<String,Object>> services=new ArrayList<>();
        services.add(probe("api-gateway","统一网关",gatewayUrl,"HTTP转发、WebSocket代理、静态资源与路由回退"));
        services.add(Map.of("service","exam-service","name","考试业务服务","url","http://localhost:8081","status","UP","version","2.0.0","role","认证权限、题库、练习、考试与数据事务","storage","MySQL + Redis"));
        services.add(probe("judge-service","独立判分服务",judgeUrl,"单选、多选、判断、填空策略；无状态HTTP计算"));
        services.add(probe("ai-service","AI出题服务",aiUrl,"独立模型推理、离线演示；异步任务由业务服务调度"));
        return Result.ok(Map.of("services",services,"discovery","配置地址发现","communication","HTTP / JSON + X-Service-Token","pendingStorage","Redis，候选题有效期24小时","version","2.0.0"));
    }
    private Map<String,Object> probe(String id,String name,String url,String role){
        Map<String,Object> data=new LinkedHashMap<>();data.put("service",id);data.put("name",name);data.put("url",url);data.put("role",role);data.put("storage","独立进程，不共享业务数据库");
        long start=System.nanoTime();
        try { Map<?,?> health=client.get().uri(url+"/health").retrieve().body(Map.class);data.put("status",health==null?"DOWN":health.get("status"));data.put("version",health==null?"-":health.get("version"));data.put("requests",health==null?0:health.get("requests")); }
        catch(Exception e){data.put("status","DOWN");data.put("version","-");data.put("requests",0);}
        data.put("latencyMs",(System.nanoTime()-start)/1_000_000);return data;
    }
}
