package com.aiexam;
import com.aiexam.client.JudgeClient;
import com.aiexam.common.BusinessException;
import com.aiexam.dto.AnswerSaveDTO;
import com.aiexam.entity.*;
import com.aiexam.mapper.*;
import com.aiexam.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ExamRegressionTest {
 @Mock ExamMapper exams; @Mock ExamQuestionMapper paper; @Mock ExamRecordMapper records; @Mock ExamAnswerMapper answers; @Mock QuestionMapper questions; @Mock UserMapper users; @Mock JudgeClient judge; @Mock QuestionService questionService; @Mock StringRedisTemplate redis; @Mock TransactionTemplate transactions;
 @InjectMocks ExamService service;
 private ExamRecord record(){var r=new ExamRecord();r.setId(1L);r.setUserId(2L);r.setExamId(1L);r.setStatus(0);r.setDeadline(LocalDateTime.now().plusMinutes(10));return r;}
 private AnswerSaveDTO dto(){var d=new AnswerSaveDTO();d.setRecordId(1L);d.setQuestionId(1L);d.setUserAnswer("A");return d;}
 @Test void expiredRecordRejectsNewAnswers(){var r=record();r.setDeadline(LocalDateTime.now().minusSeconds(1));when(records.selectOne(any())).thenReturn(r);var e=assertThrows(BusinessException.class,()->service.saveAnswer(2L,dto()));assertEquals(409,e.getCode());verifyNoInteractions(answers);}
 @Test void foreignQuestionRejected(){when(records.selectOne(any())).thenReturn(record());when(paper.selectCount(any())).thenReturn(0L);assertThrows(BusinessException.class,()->service.saveAnswer(2L,dto()));verifyNoInteractions(answers);}
 @Test void activeRecordNeverRevealsAnswers(){when(records.selectById(1L)).thenReturn(record());assertEquals(403,assertThrows(BusinessException.class,()->service.recordDetail(1L,2L,false)).getCode());verifyNoInteractions(questionService);}
 @Test void administratorCanReadAnotherStudentsSubmittedRecord(){var r=record();r.setStatus(1);when(records.selectById(1L)).thenReturn(r);when(paper.selectList(any())).thenReturn(List.of());assertNotNull(service.recordDetail(1L,99L,true));}
 @Test void anotherStudentCannotReadRecord(){var r=record();r.setStatus(1);when(records.selectById(1L)).thenReturn(r);assertEquals(403,assertThrows(BusinessException.class,()->service.recordDetail(1L,99L,false)).getCode());}
 @Test void repeatedSubmitDoesNotCallJudge(){var r=record();r.setStatus(1);r.setTotalScore(15);r.setCorrectCount(3);when(records.selectOne(any())).thenReturn(r);assertEquals(15,service.submit(1L,2L).get("totalScore"));verifyNoInteractions(judge);}
}
