package org.apache.flink.statefun.examples.modelserving.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A dummy implementation that simulates a service that might take a while to compute it's result.
 */
final class DummyQueryService implements QueryService {

  /**
   * used to complete the futures after an artificial random delay, that simulates network latency,
   * service busyness etc'
   */
  private final ScheduledExecutorService executor = newScheduledExecutorService();

  public CompletableFuture<Double> score(String merchantId) {
    CompletableFuture<Double> result = new CompletableFuture<>();

    final long randomCompletionDelay = ThreadLocalRandom.current().nextLong(5_000);
    executor.schedule(
        completeRandomly(merchantId, result), randomCompletionDelay, TimeUnit.MILLISECONDS);

    return result;
  }

  /**
   * returns a {@link Runnable}, that when runs, it completes the supplied future {@code result}
   * with a that might be either done or still running. If done, then the task status would have a
   * completion time.
   */
  private static Runnable completeRandomly(String taskId, CompletableFuture<Double> result) {
    return () -> {
      double failed = ThreadLocalRandom.current().nextDouble();

      if (failed < 0.1) {
        result.completeExceptionally(new TimeoutException());
      } else {
        double score = ThreadLocalRandom.current().nextDouble();
        result.complete(score);
      }
    };
  }

  /** A scheduled executor service with daemon threads. */
  private static ScheduledExecutorService newScheduledExecutorService() {
    return Executors.newSingleThreadScheduledExecutor(
        r -> {
          Thread t = new Thread(r);
          t.setDaemon(true);
          return t;
        });
  }
}
