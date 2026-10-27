package com.aiexam;

import com.aiexam.entity.Question;
import com.aiexam.entity.WrongQuestion;
import com.aiexam.mapper.ExamRecordMapper;
import com.aiexam.mapper.PracticeRecordMapper;
import com.aiexam.mapper.QuestionMapper;
import com.aiexam.mapper.WrongQuestionMapper;
import com.aiexam.client.JudgeClient;
import com.aiexam.service.PracticeService;
import com.aiexam.service.QuestionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PracticeReviewTest {
    @Test
    void returnsOnlyDueUnmasteredQuestionsWithoutAnswers() {
        var exams = mock(ExamRecordMapper.class);
        var records = mock(PracticeRecordMapper.class);
        var questions = mock(QuestionMapper.class);
        var wrongs = mock(WrongQuestionMapper.class);
        var judge = mock(JudgeClient.class);
        var questionService = mock(QuestionService.class);
        var service = new PracticeService(exams, records, questions, wrongs, judge, questionService);

        WrongQuestion due = wrong(1L, 1, LocalDateTime.now().minusDays(2));
        WrongQuestion future = wrong(2L, 1, LocalDateTime.now().minusHours(2));
        when(wrongs.selectList(any())).thenReturn(List.of(due, future));
        Question q = new Question(); q.setId(1L); q.setReviewStatus(1); q.setContent("题干");
        when(questions.selectById(1L)).thenReturn(q);
        when(questionService.toMap(q, true)).thenReturn(Map.of("id", 1L, "content", "题干"));

        Map<String, Object> result = service.reviewSet(9L, 10);

        assertEquals(1, result.get("dueCount"));
        var items = (List<?>) result.get("items");
        assertEquals(1, items.size());
        assertEquals(1L, ((Map<?, ?>) items.get(0)).get("questionId"));
        verify(questionService).toMap(q, true);
    }

    @Test
    void clampsReviewLimitAndTreatsLegacyNullTimestampAsDue() {
        var wrongs = mock(WrongQuestionMapper.class);
        var questions = mock(QuestionMapper.class);
        var questionService = mock(QuestionService.class);
        var service = new PracticeService(mock(ExamRecordMapper.class), mock(PracticeRecordMapper.class),
                questions, wrongs, mock(JudgeClient.class), questionService);
        when(wrongs.selectList(any())).thenReturn(List.of(wrong(3L, 1, null)));
        Question q = new Question(); q.setId(3L); q.setReviewStatus(1);
        when(questions.selectById(3L)).thenReturn(q);
        when(questionService.toMap(q, true)).thenReturn(Map.of("id", 3L));
        Map<String, Object> result = service.reviewSet(9L, 999);
        assertEquals(50, result.get("limit"));
        assertEquals(1, result.get("dueCount"));
    }

    private WrongQuestion wrong(long questionId, int count, LocalDateTime lastWrongAt) {
        WrongQuestion item = new WrongQuestion();
        item.setQuestionId(questionId); item.setWrongCount(count); item.setMastered(0); item.setLastWrongAt(lastWrongAt);
        return item;
    }
}
