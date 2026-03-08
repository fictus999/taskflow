package com.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TaskFlow — Full Stack Task Management Platform
 * Entry point for the Spring Boot application.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class TaskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
    }
}
