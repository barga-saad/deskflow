package com.deskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DeskflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeskflowApplication.class, args);
    }
}
