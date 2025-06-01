package com.fleximove.rating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class RatingManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(RatingManagementApplication.class, args);
    }
}
