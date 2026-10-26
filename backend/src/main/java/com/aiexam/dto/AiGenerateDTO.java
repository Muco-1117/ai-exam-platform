package com.aiexam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiGenerateDTO {
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    @NotBlank(message = "题型不能为空")
    private String type;
    @Min(1) @Max(3)
    private Integer difficulty = 1;
    @Min(1) @Max(10)
    private Integer count = 3;
    private String knowledge;
}
