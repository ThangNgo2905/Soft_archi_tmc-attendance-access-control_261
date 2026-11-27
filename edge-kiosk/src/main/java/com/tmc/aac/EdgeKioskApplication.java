package com.tmc.aac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EdgeKioskApplication {
    public static void main(String[] args) {
        SpringApplication.run(EdgeKioskApplication.class, args);
    }
}
