package com.ververica.statefun.workshop.utils;

import java.util.concurrent.CompletableFuture;

public interface QueryService {
  CompletableFuture<Integer> query(String merchantId);
}
