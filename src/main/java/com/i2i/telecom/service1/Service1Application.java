package com.i2i.telecom.service1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.i2i.telecom")
public class Service1Application {
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "service1");
        SpringApplication.run(Service1Application.class, args);
    }
}
