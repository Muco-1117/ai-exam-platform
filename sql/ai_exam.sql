-- ============================================================
-- 《AI智能刷题考试平台》数据库初始化脚本
-- MySQL 8.0+  /  utf8mb4  /  InnoDB
-- 默认管理员账号: admin / 123456
-- 测试学生账号:  student / 123456
-- ============================================================

DROP DATABASE IF EXISTS `ai_exam`;
CREATE DATABASE `ai_exam` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `ai_exam`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    VARCHAR(50)  NOT NULL COMMENT '登录名',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 角色表
-- ------------------------------------------------------------
CREATE TABLE `role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code`        VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB COMMENT='角色表';

-- ------------------------------------------------------------
-- 3. 权限表
-- ------------------------------------------------------------
CREATE TABLE `permission` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '权限名称',
  `code`        VARCHAR(100) NOT NULL COMMENT '权限编码,如 question:add',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB COMMENT='权限表';

-- ------------------------------------------------------------
-- 4. 角色-权限关联表
-- ------------------------------------------------------------
CREATE TABLE `role_permission` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `role_id`       BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`),
  KEY `idx_permission` (`permission_id`)
) ENGINE=InnoDB COMMENT='角色-权限关联表';

-- ------------------------------------------------------------
-- 5. 用户-角色关联表
-- ------------------------------------------------------------
CREATE TABLE `user_role` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB COMMENT='用户-角色关联表';

-- ------------------------------------------------------------
-- 6. 题目分类表
-- ------------------------------------------------------------
CREATE TABLE `question_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称,如 Java基础',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB COMMENT='题目分类表';

-- ------------------------------------------------------------
-- 7. 题目表(单选/多选/判断/填空 统一存储)
-- ------------------------------------------------------------
CREATE TABLE `question` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `category_id`    BIGINT       NOT NULL COMMENT '分类ID',
  `type`           VARCHAR(20)  NOT NULL COMMENT '题型:SINGLE单选 MULTI多选 JUDGE判断 FILL填空',
  `content`        TEXT         NOT NULL COMMENT '题干',
  `options`        JSON         DEFAULT NULL COMMENT '选项,如[{"key":"A","text":"..."}],判断/填空为NULL',
  `answer`         VARCHAR(500) NOT NULL COMMENT '标准答案:单选A 多选A,B 判断true/false 填空答案文本',
  `analysis`       TEXT         DEFAULT NULL COMMENT '答案解析',
  `difficulty`     TINYINT      NOT NULL DEFAULT 1 COMMENT '难度:1简单 2中等 3困难',
  `score`          INT          NOT NULL DEFAULT 5 COMMENT '默认分值',
  `source`         VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '来源:MANUAL人工 AI智能生成',
  `review_status`  TINYINT      NOT NULL DEFAULT 1 COMMENT '审核状态:0待审核 1已通过',
  `creator_id`     BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_type` (`type`),
  KEY `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB COMMENT='题目表';

-- ------------------------------------------------------------
-- 8. AI生成日志表
-- ------------------------------------------------------------
CREATE TABLE `ai_generation_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`      BIGINT       NOT NULL COMMENT '操作人ID',
  `category_id`  BIGINT       DEFAULT NULL COMMENT '目标分类ID',
  `type`         VARCHAR(20)  NOT NULL COMMENT '目标题型',
  `difficulty`   TINYINT      NOT NULL DEFAULT 1 COMMENT '目标难度',
  `count`        INT          NOT NULL DEFAULT 1 COMMENT '生成数量',
  `knowledge`    VARCHAR(200) DEFAULT NULL COMMENT '知识点描述',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0生成中 1成功 2失败',
  `error_msg`    VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='AI出题日志表';

-- ------------------------------------------------------------
-- 9. 考试表
-- ------------------------------------------------------------
CREATE TABLE `exam` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '考试ID',
  `name`           VARCHAR(100) NOT NULL COMMENT '考试名称',
  `category_id`    BIGINT       DEFAULT NULL COMMENT '关联分类',
  `duration`       INT          NOT NULL COMMENT '考试时长(分钟)',
  `total_score`    INT          NOT NULL COMMENT '总分',
  `pass_score`     INT          NOT NULL DEFAULT 60 COMMENT '及格分',
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0未发布 1已发布 2已结束',
  `creator_id`     BIGINT       NOT NULL COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='考试表';

-- ------------------------------------------------------------
-- 10. 考试-题目关联表
-- ------------------------------------------------------------
CREATE TABLE `exam_question` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `exam_id`     BIGINT NOT NULL COMMENT '考试ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `score`       INT    NOT NULL DEFAULT 5 COMMENT '本题分值',
  `sort`        INT    NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_question` (`exam_id`,`question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB COMMENT='考试-题目关联表';

-- ------------------------------------------------------------
-- 11. 考试记录表(答题卡)
-- ------------------------------------------------------------
CREATE TABLE `exam_record` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `exam_id`         BIGINT       NOT NULL COMMENT '考试ID',
  `user_id`         BIGINT       NOT NULL COMMENT '考生ID',
  `start_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `deadline`        DATETIME     NOT NULL COMMENT '自动交卷截止时间',
  `submit_time`     DATETIME     DEFAULT NULL COMMENT '交卷时间',
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0进行中 1已交卷',
  `total_score`     INT          DEFAULT NULL COMMENT '得分',
  `correct_count`   INT          DEFAULT NULL COMMENT '答对题数',
  PRIMARY KEY (`id`),
  KEY `idx_exam` (`exam_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='考试记录表';

-- ------------------------------------------------------------
-- 12. 考试答题明细表
-- ------------------------------------------------------------
CREATE TABLE `exam_answer` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `record_id`   BIGINT       NOT NULL COMMENT '考试记录ID',
  `question_id` BIGINT       NOT NULL COMMENT '题目ID',
  `user_answer` VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  `is_correct`  TINYINT      DEFAULT NULL COMMENT '是否正确:1是 0否',
  `score`       INT          NOT NULL DEFAULT 0 COMMENT '得分',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_question` (`record_id`, `question_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB COMMENT='考试答题明细表';

-- ------------------------------------------------------------
-- 13. 刷题练习记录表
-- ------------------------------------------------------------
CREATE TABLE `practice_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
  `question_id` BIGINT       NOT NULL COMMENT '题目ID',
  `user_answer` VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  `is_correct`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否正确:1是 0否',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_question` (`user_id`,`question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB COMMENT='刷题练习记录表';

-- ------------------------------------------------------------
-- 14. 错题本表
-- ------------------------------------------------------------
CREATE TABLE `wrong_question` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`       BIGINT   NOT NULL COMMENT '用户ID',
  `question_id`   BIGINT   NOT NULL COMMENT '题目ID',
  `wrong_count`   INT      NOT NULL DEFAULT 1 COMMENT '累计错误次数',
  `mastered`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已掌握:0否 1是',
  `last_wrong_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近错误时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='错题本表';

-- ------------------------------------------------------------
-- 15. 收藏表
-- ------------------------------------------------------------
CREATE TABLE `favor` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `question_id` BIGINT   NOT NULL COMMENT '题目ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`)
) ENGINE=InnoDB COMMENT='题目收藏表';

-- ------------------------------------------------------------
-- 16. 点赞表
-- ------------------------------------------------------------
CREATE TABLE `thumb` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `question_id` BIGINT   NOT NULL COMMENT '题目ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`)
) ENGINE=InnoDB COMMENT='题目点赞表';

-- ------------------------------------------------------------
-- 17. 系统配置表(AI接口密钥等)
-- ------------------------------------------------------------
CREATE TABLE `config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
  `remark`       VARCHAR(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB COMMENT='系统配置表';

-- ============================================================
-- 种子数据
-- ============================================================

-- 角色
INSERT INTO `role` (`id`,`name`,`code`,`description`) VALUES
(1,'管理员','ADMIN','系统管理员,拥有全部权限'),
(2,'学生','STUDENT','普通学生用户,刷题与考试');

-- 权限
INSERT INTO `permission` (`id`,`name`,`code`,`description`) VALUES
(1,'题目管理','question:manage','题目的增删改查与审核'),
(2,'AI出题','ai:generate','使用AI生成题目'),
(3,'考试管理','exam:manage','考试的创建发布与管理'),
(4,'用户管理','user:manage','用户信息与角色管理'),
(5,'分类管理','category:manage','题目分类管理'),
(6,'刷题练习','practice:do','在线刷题'),
(7,'参加考试','exam:take','参加在线考试'),
(8,'错题本','wrong:manage','查看与维护错题本'),
(9,'收藏点赞','favor:manage','收藏与点赞题目');

-- 管理员拥有全部权限
INSERT INTO `role_permission` (`role_id`,`permission_id`) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9);
-- 学生拥有练习类权限
INSERT INTO `role_permission` (`role_id`,`permission_id`) VALUES
(2,6),(2,7),(2,8),(2,9);

-- 用户(密码均为 123456 的 BCrypt)
INSERT INTO `user` (`id`,`username`,`password`,`nickname`,`email`,`status`) VALUES
(1,'admin','$2b$10$hyJU7Q3WiNl1tsuQvaI/8eN78uqkJxNOtq2lZJEq7n6ZW/B35jd3u','系统管理员','admin@aiexam.com',1),
(2,'student','$2b$10$hyJU7Q3WiNl1tsuQvaI/8eN78uqkJxNOtq2lZJEq7n6ZW/B35jd3u','测试学生','student@aiexam.com',1);

INSERT INTO `user_role` (`user_id`,`role_id`) VALUES (1,1),(2,2);

-- 题目分类
INSERT INTO `question_category` (`id`,`name`,`description`,`sort`) VALUES
(1,'Java基础','Java语言基础语法与面向对象',1),
(2,'数据结构','常见数据结构与算法',2),
(3,'数据库','MySQL与SQL基础',3),
(4,'计算机网络','网络协议与原理',4),
(5,'操作系统','进程线程与内存管理',5);

-- 种子题目
INSERT INTO `question` (`id`,`category_id`,`type`,`content`,`options`,`answer`,`analysis`,`difficulty`,`score`,`source`,`review_status`,`creator_id`) VALUES
(1,1,'SINGLE','下列关于Java语言特点的描述,错误的是?',
 '[{\"key\":\"A\",\"text\":\"Java支持多线程\"},{\"key\":\"B\",\"text\":\"Java是面向对象的编程语言\"},{\"key\":\"C\",\"text\":\"Java支持指针运算\"},{\"key\":\"D\",\"text\":\"Java具有跨平台特性\"}]',
 'C','Java为了安全性去除了指针概念,不支持指针运算,而是通过引用访问对象。',1,5,'MANUAL',1,1),
(2,1,'SINGLE','以下哪个不是Java的基本数据类型?',
 '[{\"key\":\"A\",\"text\":\"int\"},{\"key\":\"B\",\"text\":\"String\"},{\"key\":\"C\",\"text\":\"boolean\"},{\"key\":\"D\",\"text\":\"double\"}]',
 'B','String是引用类型,不是基本数据类型。Java的8种基本类型为byte/short/int/long/float/double/char/boolean。',1,5,'MANUAL',1,1),
(3,1,'JUDGE','Java中的接口(interface)可以包含构造方法。',
 NULL,
 'false','接口不能被实例化,因此不能包含构造方法。',2,5,'MANUAL',1,1),
(4,2,'SINGLE','栈的特点是?',
 '[{\"key\":\"A\",\"text\":\"先进先出\"},{\"key\":\"B\",\"text\":\"先进后出\"},{\"key\":\"C\",\"text\":\"随机存取\"},{\"key\":\"D\",\"text\":\"顺序无关\"}]',
 'B','栈是后进先出(先进后出)的数据结构,队列才是先进先出。',1,5,'MANUAL',1,1),
(5,3,'FILL','SQL中用于查询数据的语句关键字是____(填写英文大写)。',
 NULL,
 'SELECT','SELECT语句用于从数据库表中查询数据。',1,5,'MANUAL',1,1),
(6,4,'MULTI','以下属于TCP协议特点的有?',
 '[{\"key\":\"A\",\"text\":\"面向连接\"},{\"key\":\"B\",\"text\":\"可靠传输\"},{\"key\":\"C\",\"text\":\"无连接\"},{\"key\":\"D\",\"text\":\"流量控制\"}]',
 'A,B,D','TCP面向连接、提供可靠传输与流量控制;无连接是UDP的特点。',2,5,'MANUAL',1,1),
(7,5,'JUDGE','线程是CPU调度的最小单位。',
 NULL,
 'true','进程是资源分配的最小单位,线程是CPU调度的最小单位。',1,5,'MANUAL',1,1);

-- 种子考试
INSERT INTO `exam` (`id`,`name`,`category_id`,`duration`,`total_score`,`pass_score`,`status`,`creator_id`) VALUES
(1,'Java基础摸底测试',1,30,20,12,1,1);
INSERT INTO `exam_question` (`exam_id`,`question_id`,`score`,`sort`) VALUES
(1,1,5,1),(1,2,5,2),(1,3,5,3),(1,4,5,4);

-- 系统配置
INSERT INTO `config` (`config_key`,`config_value`,`remark`) VALUES
('ai.api.key','','大模型API密钥,填入后AI出题生效'),
('ai.api.url','https://api.deepseek.com/chat/completions','OpenAI兼容接口地址'),
('ai.api.model','deepseek-chat','模型名称');
