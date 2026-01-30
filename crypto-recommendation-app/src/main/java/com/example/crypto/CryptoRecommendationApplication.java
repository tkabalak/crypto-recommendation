package com.example.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Spring Boot entrypoint for the Crypto Recommendation Service.
 */
@EnableCaching
@SpringBootApplication
public class CryptoRecommendationApplication {

  /**
   * Application bootstrap.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(CryptoRecommendationApplication.class, args);
  }
}
