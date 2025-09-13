package com.example.slotmachine.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SlotMachineServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SlotMachineServerApplication.class, args);
    }
}
