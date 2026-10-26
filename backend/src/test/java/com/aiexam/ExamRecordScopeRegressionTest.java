package com.aiexam;

import com.aiexam.common.BusinessException;
import com.aiexam.common.UserContext;
import com.aiexam.controller.ExamController;
import com.aiexam.entity.Exam;
import com.aiexam.entity.ExamRecord;
import com.aiexam.mapper.ExamMapper;
import com.aiexam.mapper.ExamQuestionMapper;
import com.aiexam.mapper.ExamRecordMapper;
import com.aiexam.service.ExamService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExamRecordScopeRegressionTest {
    private final ExamService service = mock(ExamService.class);
    private final ExamController controller = new ExamController(service);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    @BeforeAll
    static void initializeTableMetadata() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ExamRecord.class);
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void administratorDefaultRequestReturnsOnlyOwnRecords() throws Exception {
        UserContext.set(1L, Set.of("ADMIN"));
        when(service.recordPage(1L, false, 1, 10)).thenReturn(new Page<>(1, 10));
        mvc.perform(get("/api/exam/record/page")).andExpect(status().isOk());
        verify(service).recordPage(1L, false, 1, 10);
    }

    @Test
    void administratorCanExplicitlyRequestAllCandidates() throws Exception {
        UserContext.set(1L, Set.of("ADMIN"));
        when(service.recordPage(1L, true, 1, 10)).thenReturn(new Page<>(1, 10));
        mvc.perform(get("/api/exam/record/page").param("scope", "all"))
                .andExpect(status().isOk());
        verify(service).recordPage(1L, true, 1, 10);
    }

    @Test
    void studentDefaultRequestReturnsOnlyOwnRecords() throws Exception {
        UserContext.set(2L, Set.of("STUDENT"));
        when(service.recordPage(2L, false, 1, 10)).thenReturn(new Page<>(1, 10));
        mvc.perform(get("/api/exam/record/page")).andExpect(status().isOk());
        verify(service).recordPage(2L, false, 1, 10);
    }

    @Test
    void studentCannotRequestAllCandidates() {
        UserContext.set(2L, Set.of("STUDENT"));
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.recordPage(1, 10, "all")).getCode());
        verifyNoInteractions(service);
    }

    @Test
    void invalidScopeCannotFallBackToAllRecords() {
        UserContext.set(1L, Set.of("ADMIN"));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> controller.recordPage(1, 10, "unknown")).getCode());
        verifyNoInteractions(service);
    }

    @Test
    @SuppressWarnings("unchecked")
    void personalRecordQueryAlwaysFiltersByCurrentUser() {
        ExamRecordMapper records = mock(ExamRecordMapper.class);
        ExamService actual = new ExamService(null, null, null, records, null, null,
                null, null, null, null);
        when(records.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(call -> {
            LambdaQueryWrapper<ExamRecord> query = call.getArgument(1);
            assertTrue(query.getSqlSegment().contains("user_id ="));
            assertTrue(query.getParamNameValuePairs().containsValue(1L));
            return new Page<ExamRecord>(1, 10);
        });
        assertEquals(0, actual.recordPage(1L, false, 1, 10).getTotal());
        verify(records).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void managementRecordQueryIncludesAllCandidates() {
        ExamRecordMapper records = mock(ExamRecordMapper.class);
        ExamService actual = new ExamService(null, null, null, records, null, null,
                null, null, null, null);
        when(records.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(call -> {
            LambdaQueryWrapper<ExamRecord> query = call.getArgument(1);
            assertFalse(query.getSqlSegment().contains("user_id"));
            return new Page<ExamRecord>(1, 10);
        });
        actual.recordPage(1L, true, 1, 10);
        verify(records).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void examCenterShowsOnlyCurrentUsersParticipationAndKeepsUnattemptedExams() {
        ExamMapper exams = mock(ExamMapper.class);
        ExamRecordMapper records = mock(ExamRecordMapper.class);
        ExamQuestionMapper questions = mock(ExamQuestionMapper.class);
        ExamService actual = new ExamService(null, exams, questions, records, null, null,
                null, null, null, null);
        Exam completed = new Exam(); completed.setId(1L);
        Exam ongoing = new Exam(); ongoing.setId(2L);
        Exam unattempted = new Exam(); unattempted.setId(3L);
        when(exams.selectList(any())).thenReturn(List.of(completed, ongoing, unattempted));
        ExamRecord done = new ExamRecord();
        done.setId(20L); done.setExamId(1L); done.setUserId(1L); done.setStatus(1);
        ExamRecord active = new ExamRecord();
        active.setId(21L); active.setExamId(2L); active.setUserId(1L); active.setStatus(0);
        when(records.selectList(any())).thenAnswer(call -> {
            LambdaQueryWrapper<ExamRecord> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().contains("user_id ="));
            assertTrue(query.getParamNameValuePairs().containsValue(1L));
            return List.of(done, active);
        });
        List<Map<String, Object>> list = actual.publishedList(1L);
        assertEquals(20L, list.get(0).get("recordId"));
        assertEquals(1, list.get(0).get("recordStatus"));
        assertEquals(21L, list.get(1).get("recordId"));
        assertEquals(0, list.get(1).get("recordStatus"));
        assertNull(list.get(2).get("recordId"));
        assertNull(list.get(2).get("recordStatus"));
    }
}
