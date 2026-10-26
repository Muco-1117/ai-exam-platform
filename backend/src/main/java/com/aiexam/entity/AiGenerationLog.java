package com.aiexam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_generation_log")
public class AiGenerationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long categoryId;
    private String type;
    private Integer difficulty;
    private Integer count;
    private String knowledge;
    /** 0生成中 1成功 2失败 */
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}
