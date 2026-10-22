package com.buy2go.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class Buy2GoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Buy2GoApplication.class, args);
    }
}
