package org.apache.flink.statefun.examples.modelserving.util;

import java.util.concurrent.CompletableFuture;

/** A remote service that can be queried asynchronously */
public interface QueryService {

  /** @return An instance of the query service */
  static QueryService getInstance() {
    return new DummyQueryService();
  }

  /**
   * @param merchantId The id for a merchant
   * @return A fraud score based on merchant history
   */
  CompletableFuture<Double> score(String merchantId);
}
