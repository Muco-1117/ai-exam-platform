package com.aiexam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionDTO {
    private Long id;
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    @NotBlank(message = "题型不能为空")
    @jakarta.validation.constraints.Pattern(regexp="SINGLE|MULTI|JUDGE|FILL")
    private String type;
    @NotBlank(message = "题干不能为空")
    private String content;
    private List<OptionItem> options;
    @NotBlank(message = "答案不能为空")
    private String answer;
    private String analysis;
    @NotNull(message = "难度不能为空")
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(3)
    private Integer difficulty;
    private Integer score;

    @Data
    public static class OptionItem {
        private String key;
        private String text;
    }
}
