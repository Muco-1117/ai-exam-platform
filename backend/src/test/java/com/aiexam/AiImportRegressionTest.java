package com.aiexam;
import com.aiexam.ai.AiQuestionService;
import com.aiexam.common.BusinessException;
import com.aiexam.entity.*;
import com.aiexam.mapper.*;
import com.aiexam.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.transaction.support.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AiImportRegressionTest {
 private final AiGenerationLogMapper logs=mock(AiGenerationLogMapper.class);
 private final QuestionMapper questions=mock(QuestionMapper.class);
 private final StringRedisTemplate redis=mock(StringRedisTemplate.class);
 private final ValueOperations<String,String> values=mock(ValueOperations.class);
 private final TransactionTemplate transactions=mock(TransactionTemplate.class);
 private AiQuestionService service;
 @BeforeEach void setup(){
  var mapper=new ObjectMapper();var questionService=new QuestionService(questions,mock(QuestionCategoryMapper.class),mapper);
  service=new AiQuestionService(logs,mock(ConfigMapper.class),questions,redis,mapper,transactions,questionService,"http://127.0.0.1:1","local-test");
  when(redis.opsForValue()).thenReturn(values);when(values.setIfAbsent(anyString(),anyString(),any(java.time.Duration.class))).thenReturn(true);
  var log=new AiGenerationLog();log.setId(1L);log.setCount(1);log.setType("SINGLE");log.setCategoryId(1L);log.setDifficulty(1);log.setUserId(1L);when(logs.selectById(1L)).thenReturn(log);
 }
 @AfterEach void close(){service.close();}
 @Test void legacyCandidateOptionsAreNormalizedBeforeInsert(){
  when(values.get("ai:pending:1")).thenReturn("{\"questions\":[{\"content\":\"输入设备是？\",\"options\":[\"A. 键盘\",\"B. 显示器\"],\"answer\":\"A\"}]}");
  when(transactions.execute(any())).thenAnswer(call->((TransactionCallback<Integer>)call.getArgument(0)).doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
  assertEquals(1,service.confirm(1L,true));
  var saved=org.mockito.ArgumentCaptor.forClass(Question.class);verify(questions).insert(saved.capture());
  assertEquals(0,saved.getValue().getReviewStatus());assertTrue(saved.getValue().getOptions().contains("键盘"));assertTrue(saved.getValue().getOptions().contains("\"key\":\"A\""));verify(redis).delete("ai:pending:1");
 }
 @Test void invalidOptionsNeverReachInsertAndKeepCache(){
  when(values.get("ai:pending:1")).thenReturn("{\"questions\":[{\"content\":\"输入设备是？\",\"options\":[{\"key\":\"A\",\"text\":\"\"},{\"key\":\"B\",\"text\":\"显示器\"}],\"answer\":\"A\"}]}");
  assertThrows(BusinessException.class,()->service.confirm(1L,true));verifyNoInteractions(questions,transactions);verify(redis,never()).delete("ai:pending:1");
 }
 @Test void invalidLaterQuestionCannotPartiallyInsertBatch(){
  var log=new AiGenerationLog();log.setId(1L);log.setCount(2);log.setType("SINGLE");log.setCategoryId(1L);log.setDifficulty(1);log.setUserId(1L);when(logs.selectById(1L)).thenReturn(log);
  when(values.get("ai:pending:1")).thenReturn("{\"questions\":[{\"content\":\"有效题\",\"options\":[\"A.键盘\",\"B.显示器\"],\"answer\":\"A\"},{\"content\":\"无效题\",\"options\":[],\"answer\":\"A\"}]}");
  assertThrows(BusinessException.class,()->service.confirm(1L,true));verifyNoInteractions(questions,transactions);verify(redis,never()).delete("ai:pending:1");
 }
}
