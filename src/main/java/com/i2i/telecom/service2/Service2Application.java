package com.i2i.telecom.service2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.i2i.telecom")
public class Service2Application {
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "service2");
        SpringApplication.run(Service2Application.class, args);
    }
}
