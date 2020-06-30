package org.apache.flink.statefun.sdk.springboot;

public interface GreetingGenerator {
  String text(String name, int seenCount);
}
