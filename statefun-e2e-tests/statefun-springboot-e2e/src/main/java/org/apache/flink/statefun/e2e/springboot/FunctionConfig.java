package org.apache.flink.statefun.e2e.springboot;

import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FunctionConfig {

  @Bean
  public RandomKey key() {
    return new RandomKey();
  }

  public static class RandomKey {
    public String key() {
      return Integer.toHexString(new Random().nextInt(0xFFFF));
    }
  }
}
