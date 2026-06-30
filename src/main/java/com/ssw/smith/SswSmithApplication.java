package com.ssw.smith;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.ssw.smith.mapper")
@SpringBootApplication
public class SswSmithApplication {

    public static void main(String[] args) {
        SpringApplication.run(SswSmithApplication.class, args);
    }
}
