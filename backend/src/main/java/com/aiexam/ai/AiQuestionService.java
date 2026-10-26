package com.aiexam.ai;
import com.aiexam.common.BusinessException;
import com.aiexam.dto.QuestionDTO;
import com.aiexam.service.QuestionService;
import com.aiexam.entity.*;
import com.aiexam.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Duration;
import java.util.*;
@Service
public class AiQuestionService {
 private final QuestionService questionService;
 private final AiGenerationLogMapper logs; private final ConfigMapper configs; private final QuestionMapper questions;
 private final StringRedisTemplate redis; private final ObjectMapper mapper; private final ThreadPoolTaskExecutor executor; private final RestClient client; private final TransactionTemplate transactions;
 public AiQuestionService(AiGenerationLogMapper logs,ConfigMapper configs,QuestionMapper questions,StringRedisTemplate redis,ObjectMapper mapper,TransactionTemplate transactions,QuestionService questionService,@Value("${services.ai-url}")String url,@Value("${services.token}")String token){
  this.questionService=questionService;this.logs=logs;this.configs=configs;this.questions=questions;this.redis=redis;this.mapper=mapper;this.transactions=transactions;
  executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(2);executor.setMaxPoolSize(4);executor.setQueueCapacity(30);executor.setThreadNamePrefix("ai-generation-");executor.initialize();
  var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(2000);f.setReadTimeout(55000); client=RestClient.builder().baseUrl(url).defaultHeader("X-Service-Token",token).requestFactory(f).build();
 }
 @jakarta.annotation.PreDestroy public void close(){executor.shutdown();}
 public Long generate(Long userId,Long categoryId,String type,Integer difficulty,Integer count,String knowledge){
  AiGenerationLog task=new AiGenerationLog();task.setUserId(userId);task.setCategoryId(categoryId);task.setType(type);task.setDifficulty(difficulty);task.setCount(count);task.setKnowledge(knowledge);task.setStatus(0);logs.insert(task);
  try{executor.execute(()->run(task));}catch(Exception e){fail(task.getId(),"生成队列已满，请稍后重试");}
  return task.getId();
 }
 private void run(AiGenerationLog task){
  try{
   Map<String,Object> request=new HashMap<>();request.put("categoryId",task.getCategoryId());request.put("type",task.getType());request.put("difficulty",task.getDifficulty());request.put("count",task.getCount());request.put("knowledge",task.getKnowledge());request.put("apiUrl",config("ai.api.url"));request.put("apiKey",config("ai.api.key"));request.put("model",config("ai.api.model"));
   JsonNode response=client.post().uri("/internal/generate").body(request).retrieve().body(JsonNode.class);
   if(response==null||!response.path("questions").isArray()||response.path("questions").size()!=task.getCount())throw new IllegalStateException("出题服务返回格式无效");
   redis.opsForValue().set("ai:pending:"+task.getId(),mapper.writeValueAsString(response),Duration.ofHours(24));
   AiGenerationLog update=new AiGenerationLog();update.setId(task.getId());update.setStatus(1);logs.updateById(update);
  }catch(Exception e){fail(task.getId(),"出题失败，请检查AI服务状态、模型配置或输出格式");}
 }
 public Map<String,Object> logStatus(Long id){
  var task=logs.selectById(id);if(task==null)throw new BusinessException("日志不存在");
  Map<String,Object> result=new HashMap<>();result.put("id",id);result.put("status",task.getStatus());result.put("errorMsg",task.getErrorMsg());result.put("type",task.getType());result.put("count",task.getCount());result.put("createTime",task.getCreateTime());
  String pending=redis.opsForValue().get("ai:pending:"+id);
  if(pending!=null)try{var json=mapper.readTree(pending);result.put("previewCount",json.path("questions").size());result.put("preview",json.path("questions"));result.put("mode",json.path("mode").asText());}catch(Exception e){throw new BusinessException("候选题缓存解析失败");}
  return result;
 }
 public Page<AiGenerationLog> logPage(long page,long size){return logs.selectPage(new Page<>(page,size),new LambdaQueryWrapper<AiGenerationLog>().orderByDesc(AiGenerationLog::getCreateTime));}
 public int confirm(Long id,boolean confirm){
  String lock="ai:confirm-lock:"+id;String lockToken=UUID.randomUUID().toString();
  if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lock,lockToken,Duration.ofSeconds(30))))throw new BusinessException(409,"正在处理，请勿重复确认");
  try{
   String value=redis.opsForValue().get("ai:pending:"+id);if(value==null)throw new BusinessException("生成结果已处理或已过期");
   if(!confirm){redis.delete("ai:pending:"+id);return 0;}
   var log=logs.selectById(id);JsonNode nodes;
   try{nodes=mapper.readTree(value).path("questions");}catch(Exception e){throw new BusinessException("候选题缓存解析失败");}
   if(log==null||!nodes.isArray()||nodes.size()!=log.getCount())throw new BusinessException("候选题数量或日志无效");
   // Validate the complete batch before opening the insert transaction; retain the cache on failure.
   List<Question> candidates=new ArrayList<>();
   for(JsonNode n:nodes){
    QuestionDTO dto=new QuestionDTO();dto.setCategoryId(log.getCategoryId());dto.setType(log.getType());dto.setContent(n.path("content").asText());
    if(n.path("content").asText().isBlank()||n.path("answer").asText().isBlank())throw new BusinessException("候选题缺少题干或答案");
    if(n.hasNonNull("options"))dto.setOptions(mapper.convertValue(questionService.parseOptions(n.path("options").toString()),new com.fasterxml.jackson.core.type.TypeReference<List<QuestionDTO.OptionItem>>(){}));
    dto.setAnswer(n.path("answer").asText());dto.setAnalysis(n.path("analysis").asText());dto.setDifficulty(log.getDifficulty());dto.setScore(5);
    Question q=questionService.buildQuestion(dto);q.setSource("AI");q.setReviewStatus(0);q.setCreatorId(log.getUserId());candidates.add(q);
   }
   Integer count=transactions.execute(status->{for(Question q:candidates)questions.insert(q);return candidates.size();});
   redis.delete("ai:pending:"+id);return count==null?0:count;
  }finally{redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",Long.class),List.of(lock),lockToken);}
 }
 private String config(String key){var c=configs.selectOne(new LambdaQueryWrapper<Config>().eq(Config::getConfigKey,key));return c==null?"":Objects.toString(c.getConfigValue(),"");}
 private void fail(Long id,String message){var update=new AiGenerationLog();update.setId(id);update.setStatus(2);update.setErrorMsg(message);logs.updateById(update);}
}
