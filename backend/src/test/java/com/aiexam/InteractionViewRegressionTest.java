package com.aiexam;

import com.aiexam.client.JudgeClient;
import com.aiexam.common.BusinessException;
import com.aiexam.dto.PracticeSubmitDTO;
import com.aiexam.entity.*;
import com.aiexam.mapper.*;
import com.aiexam.service.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InteractionViewRegressionTest {
    private final ExamRecordMapper exams = mock(ExamRecordMapper.class);
    private final PracticeRecordMapper practices = mock(PracticeRecordMapper.class);
    private final QuestionMapper questions = mock(QuestionMapper.class);
    private final WrongQuestionMapper wrongs = mock(WrongQuestionMapper.class);
    private final JudgeClient judge = mock(JudgeClient.class);
    private final QuestionService questionService = mock(QuestionService.class);
    private final PracticeService practice = new PracticeService(exams, practices, questions, wrongs, judge, questionService);

    @Test void incorrectPracticeEarnsZeroPoints() {
        Question q = new Question(); q.setId(8L); q.setReviewStatus(1); q.setScore(5); q.setAnswer("A");
        when(questions.selectById(8L)).thenReturn(q);
        when(exams.selectCount(any())).thenReturn(0L);
        when(judge.judge(q, "B")).thenReturn(false);
        PracticeSubmitDTO dto = new PracticeSubmitDTO(); dto.setQuestionId(8L); dto.setUserAnswer("B");
        assertEquals(0, practice.submit(2L, dto).get("score"));
    }

    @Test void practiceHistoryExposesItsQuestionAndAwardedScore() {
        PracticeRecord r = new PracticeRecord(); r.setId(3L); r.setQuestionId(8L); r.setIsCorrect(0);
        Page<PracticeRecord> page = new Page<>(1, 10, 1); page.setRecords(List.of(r));
        when(practices.selectPage(any(Page.class), any())).thenReturn(page);
        Question q = new Question(); q.setScore(5); when(questions.selectById(8L)).thenReturn(q);
        Map<String, Object> row = practice.page(2L, 1, 10).getRecords().get(0);
        assertEquals(8L, row.get("questionId")); assertEquals(0, row.get("score"));
    }

    @Test void wrongListExposesTheIdUsedByMarkMastered() {
        WrongQuestion w = new WrongQuestion(); w.setId(3L); w.setQuestionId(8L); w.setMastered(0);
        Page<WrongQuestion> page = new Page<>(1, 10, 1); page.setRecords(List.of(w));
        when(wrongs.selectPage(any(Page.class), any())).thenReturn(page);
        Map<String, Object> row = practice.wrongPage(2L, 1, 10).getRecords().get(0);
        assertEquals(8L, row.get("questionId")); assertEquals(0, row.get("mastered"));
    }

    @Test void favoriteListExposesTheIdUsedByRemovalAndItsDate() {
        FavorMapper favors = mock(FavorMapper.class);
        Favor f = new Favor(); f.setId(3L); f.setQuestionId(8L); f.setCreateTime(LocalDateTime.now());
        Page<Favor> page = new Page<>(1, 10, 1); page.setRecords(List.of(f));
        when(favors.selectPage(any(Page.class), any())).thenReturn(page);
        InteractService service = new InteractService(favors, mock(ThumbMapper.class), questions, questionService);
        Map<String, Object> row = service.favorPage(2L, 1, 10).getRecords().get(0);
        assertEquals(8L, row.get("questionId")); assertEquals(f.getCreateTime(), row.get("createTime"));
    }

    @Test void renamedCategoryCannotDuplicateAnotherCategory() {
        QuestionCategoryMapper categories = mock(QuestionCategoryMapper.class);
        QuestionCategory existing = new QuestionCategory(); existing.setId(1L); existing.setName("Java");
        QuestionCategory update = new QuestionCategory(); update.setName("数据库");
        when(categories.selectById(1L)).thenReturn(existing);
        when(categories.selectCount(any())).thenReturn(1L);
        CategoryService service = new CategoryService(categories, questions);
        assertThrows(BusinessException.class, () -> service.update(1L, update));
        verify(categories, never()).updateById(any(QuestionCategory.class));
    }

    @Test void userListCarriesCurrentRoleWithoutPassword() {
        UserMapper users = mock(UserMapper.class); UserRoleMapper links = mock(UserRoleMapper.class);
        RoleMapper roles = mock(RoleMapper.class);
        User u = new User(); u.setId(2L); u.setUsername("test"); u.setPassword("must-not-appear");
        UserRole link = new UserRole(); link.setUserId(2L); link.setRoleId(1L);
        Role role = new Role(); role.setId(1L); role.setCode("ADMIN"); role.setName("管理员");
        Page<User> page = new Page<>(1, 10, 1); page.setRecords(List.of(u));
        when(users.selectPage(any(Page.class), any())).thenReturn(page);
        when(links.selectList(any())).thenReturn(List.of(link));
        when(roles.selectBatchIds(any())).thenReturn(List.of(role)); when(roles.selectById(1L)).thenReturn(role);
        UserService service = new UserService(users, links, roles, mock(RolePermissionMapper.class), mock(PermissionMapper.class), mock(com.aiexam.common.JwtUtil.class));
        Map<String, Object> row = service.page(null, 1, 10).getRecords().get(0);
        assertEquals(1L, row.get("roleId")); assertEquals("管理员", row.get("roleName"));
        assertFalse(row.containsKey("password"));
    }
}
