package com.memorywedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MemoryWeddingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoryWeddingApplication.class, args);
    }
}
