package com.graduation.fitmate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.graduation.fitmate.mapper")
public class FitmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitmateApplication.class, args);
    }
}

