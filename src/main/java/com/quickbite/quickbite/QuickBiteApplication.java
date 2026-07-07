package com.quickbite.quickbite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
public class QuickBiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickBiteApplication.class, args);
    }

}
