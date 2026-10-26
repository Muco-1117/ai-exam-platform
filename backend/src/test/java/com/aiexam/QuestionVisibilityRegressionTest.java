package com.aiexam;

import com.aiexam.common.UserContext;
import com.aiexam.controller.QuestionController;
import com.aiexam.entity.Question;
import com.aiexam.service.QuestionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionVisibilityRegressionTest {
    private final QuestionService questions = mock(QuestionService.class);
    private final QuestionController controller = new QuestionController(questions);

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void administratorMayOmitReviewFilter() {
        UserContext.set(1L, Set.of("ADMIN"));
        when(questions.page(null, null, null, null, null, 1, 10))
                .thenReturn(new Page<>(1, 10));
        assertDoesNotThrow(() -> controller.page(null, null, null, null, null, 1, 10));
        verify(questions).page(null, null, null, null, null, 1, 10);
    }

    @Test
    void administratorCanFilterPendingQuestions() {
        UserContext.set(1L, Set.of("ADMIN"));
        when(questions.page(null, null, null, null, 0, 1, 10))
                .thenReturn(new Page<>(1, 10));
        assertDoesNotThrow(() -> controller.page(null, null, null, null, 0, 1, 10));
        verify(questions).page(null, null, null, null, 0, 1, 10);
    }

    @Test
    void studentCannotRequestPendingQuestionsOrAnswers() {
        UserContext.set(2L, Set.of("STUDENT"));
        Question question = new Question();
        question.setId(1L);
        Page<Question> result = new Page<>(1, 10);
        result.setRecords(List.of(question));
        when(questions.page(null, null, null, null, 1, 1, 10)).thenReturn(result);
        when(questions.toMap(same(question), eq(true))).thenReturn(Map.of("id", 1L));
        controller.page(null, null, null, null, 0, 1, 10);
        verify(questions).page(null, null, null, null, 1, 1, 10);
        verify(questions).toMap(question, true);
    }
}
