package com.aiexam;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
@RestController
public class AiController {
 private final ObjectMapper mapper=new ObjectMapper();
 private final AtomicLong requests=new AtomicLong();
 public record Request(@NotNull Long categoryId,@NotNull @Pattern(regexp="SINGLE|MULTI|JUDGE|FILL") String type,@NotNull @Min(1) @Max(3) Integer difficulty,@NotNull @Min(1) @Max(10) Integer count,String knowledge,String apiUrl,String apiKey,String model) {}
 @GetMapping("/health") public Map<String,Object> health(){ return Map.of("service","ai-service","status","UP","version","2.0.0","requests",requests.get(),"database","无状态推理，不直接访问业务库"); }
 @PostMapping("/internal/generate")
 public Map<String,Object> generate(@Valid @RequestBody Request r) throws Exception {
  requests.incrementAndGet(); boolean demo=r.apiKey()==null||r.apiKey().isBlank();
  List<Map<String,Object>> questions=demo?demo(r):real(r);
  return Map.of("mode",demo?"DEMO":"LIVE","questions",questions,"service","ai-service");
 }
 private List<Map<String,Object>> real(Request r) throws Exception {
  var factory=new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(5000);factory.setReadTimeout(45000);
  String prompt="请生成"+r.count()+"道计算机课程题，题型"+r.type()+"，难度"+r.difficulty()+"，知识点："+r.knowledge()+"。仅输出JSON数组。每项含content、options、answer、analysis。选择题的options必须是对象数组，例如[{\"key\":\"A\",\"text\":\"选项内容\"},{\"key\":\"B\",\"text\":\"另一个选项\"}]，禁止返回纯字符串数组。选项编号A至H不重复且内容非空。单选答案必须对应一个选项编号，多选答案对应至少两个选项编号并以英文逗号分隔。判断题和填空题options为null，判断答案true/false，填空答案为文本。";
  var body=Map.of("model",r.model()==null||r.model().isBlank()?"deepseek-chat":r.model(),"messages",List.of(Map.of("role","user","content",prompt)),"temperature",0.7);
  JsonNode resp=RestClient.builder().requestFactory(factory).build().post().uri(r.apiUrl()).header("Authorization","Bearer "+r.apiKey()).body(body).retrieve().body(JsonNode.class);
  String content=resp.path("choices").path(0).path("message").path("content").asText();
  int first=content.indexOf('['),last=content.lastIndexOf(']');
  if(first<0||last<first) throw new IllegalArgumentException("模型未返回JSON数组");
  JsonNode arr=mapper.readTree(content.substring(first,last+1));
  return AiQuestionNormalizer.normalize(r.type(),r.count(),arr);
 }
 private List<Map<String,Object>> demo(Request r){
  List<Map<String,Object>> list=new ArrayList<>();
  for(int i=0;i<r.count();i++){
   var q=new LinkedHashMap<String,Object>();
   switch(r.type()){
    case "SINGLE" -> {q.put("content","服务之间通过 HTTP 调用时，哪一项有助于避免下游故障长期占用线程？（演示题 "+(i+1)+"）");q.put("options",List.of(Map.of("key","A","text","设置连接及读取超时"),Map.of("key","B","text","无限等待响应"),Map.of("key","C","text","关闭错误处理"),Map.of("key","D","text","共享所有线程状态")));q.put("answer","A");}
    case "MULTI" -> {q.put("content","以下哪些属于微服务独立部署的特点？（演示题 "+(i+1)+"）");q.put("options",List.of(Map.of("key","A","text","服务具有独立进程"),Map.of("key","B","text","所有模块必须放在同一JVM"),Map.of("key","C","text","通过明确契约通信"),Map.of("key","D","text","不需要处理网络失败")));q.put("answer","A,C");}
    case "JUDGE" -> {q.put("content","微服务调用需要设置超时并处理下游不可用的情况。（演示题 "+(i+1)+"）");q.put("answer","true");}
    default -> {q.put("content","本项目负责统一入口和请求转发的组件名称为____。（演示题 "+(i+1)+"）");q.put("answer","网关|gateway|api-gateway");}
   }
   q.put("analysis","本题由离线演示题库提供，用于验证独立AI服务、异步任务和审核入库链路。配置API密钥后可使用实时大模型出题。知识点："+Objects.toString(r.knowledge(),"微服务基础"));list.add(q);
  }
  return list;
 }
}
