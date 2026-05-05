package net.droth.pufoquote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Main entry point for the Pufoquote Spring Boot application. */
@SpringBootApplication
@EnableScheduling
public class PufoquoteApplication {
  public static void main(String[] args) {
    SpringApplication.run(PufoquoteApplication.class, args);
  }
}
