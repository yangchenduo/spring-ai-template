package com.template.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.template"})
@MapperScan("com.template.app.mapper")
public class SpringAiTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiTemplateApplication.class, args);
    }
}
