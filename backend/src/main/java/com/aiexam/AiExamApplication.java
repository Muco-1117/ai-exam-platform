package com.aiexam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@MapperScan("com.aiexam.mapper")
@SpringBootApplication
public class AiExamApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiExamApplication.class, args);
    }
}
