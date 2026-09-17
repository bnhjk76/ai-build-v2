package com.ticketwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class TicketWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketWalletApplication.class, args);
    }
}
