package com.aiexam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private LocalDateTime submitTime;
    /** 0进行中 1已交卷 */
    private Integer status;
    private Integer totalScore;
    private Integer correctCount;
}
