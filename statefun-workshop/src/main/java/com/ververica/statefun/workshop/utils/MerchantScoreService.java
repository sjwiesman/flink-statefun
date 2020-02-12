package com.ververica.statefun.workshop.utils;

import java.util.concurrent.CompletableFuture;

/**
 * This interface exposes an external service, such as a 3rd party rest api, that returns a
 * trustworthiness score for a given merchant.
 */
public interface MerchantScoreService {

  CompletableFuture<Integer> query(String merchantId);
}
