package com.jetems.springk8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
public class Springk8sApplication {

    public static void main(String[] args) {
        SpringApplication.run(Springk8sApplication.class, args);
    }

}
