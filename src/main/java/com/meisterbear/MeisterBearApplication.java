package com.meisterbear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MeisterBearApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeisterBearApplication.class, args);
    }
}
