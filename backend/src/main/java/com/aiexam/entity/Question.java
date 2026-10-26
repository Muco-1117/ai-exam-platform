package com.aiexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    /** SINGLE/MULTI/JUDGE/FILL */
    private String type;
    private String content;
    /** JSON数组字符串:[{"key":"A","text":"..."}] */
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private Integer score;
    /** MANUAL / AI */
    private String source;
    /** 0待审核 1通过 */
    private Integer reviewStatus;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
