package com.qhy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MatchEngineFull2Application {

    public static void main(String[] args) {
        SpringApplication.run(MatchEngineFull2Application.class, args);
    }

}
