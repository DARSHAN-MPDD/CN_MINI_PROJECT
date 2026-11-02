package com.network.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Smart Packet Delivery and Self-Healing Network Simulator.
 *
 * <p>Component scanning is rooted at {@code com.network.simulator} so all
 * controllers, services, and future packages are discovered automatically.</p>
 */
@SpringBootApplication
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }

}