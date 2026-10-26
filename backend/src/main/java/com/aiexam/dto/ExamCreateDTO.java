package com.aiexam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamCreateDTO {
    @NotBlank(message = "考试名称不能为空")
    private String name;
    private Long categoryId;
    @NotNull(message = "时长不能为空")
    @Min(1)
    private Integer duration;
    private Integer passScore = 60;
    @NotEmpty(message = "至少选择一道题目")
    private List<@jakarta.validation.Valid QuestionItem> questions;

    @Data
    public static class QuestionItem {
        @NotNull
        private Long questionId;
        @NotNull
        @Min(1)
        private Integer score;
    }
}
