package com.aiexam.client;
import com.aiexam.common.BusinessException;
import com.aiexam.entity.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
@Component
public class JudgeClient {
 private final RestClient client;
 public JudgeClient(@Value("${services.judge-url}") String url,@Value("${services.token}") String token){
  var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(2000);f.setReadTimeout(5000);
  client=RestClient.builder().baseUrl(url).defaultHeader("X-Service-Token",token).requestFactory(f).build();
 }
 public boolean judge(Question q,String answer){
  try {
   var item=new HashMap<String,Object>();item.put("type",q.getType());item.put("answer",q.getAnswer());item.put("userAnswer",answer);
   JsonNode response=client.post().uri("/internal/judge").body(Map.of("items",List.of(item))).retrieve().body(JsonNode.class);
   if(response==null||!response.path("results").isArray()||response.path("results").isEmpty())throw new IllegalStateException();
   return response.path("results").path(0).path("correct").asBoolean();
  }catch(Exception ex){ throw new BusinessException(503,"判分服务暂不可用，请稍后重试；作答未计分"); }
 }
}
