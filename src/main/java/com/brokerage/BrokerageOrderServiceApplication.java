package com.brokerage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BrokerageOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerageOrderServiceApplication.class, args);
    }
}
