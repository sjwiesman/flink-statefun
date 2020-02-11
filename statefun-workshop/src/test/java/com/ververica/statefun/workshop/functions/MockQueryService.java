package com.ververica.statefun.workshop.functions;

import com.ververica.statefun.workshop.utils.QueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockQueryService implements QueryService {

  private final List<CompletableFuture<Integer>> responses;

  public static Builder builder() {
    return new Builder();
  }

  private MockQueryService(List<CompletableFuture<Integer>> responses) {
    this.responses = responses;
  }

  @Override
  public CompletableFuture<Integer> query(String merchantId) {
    return responses.remove(0);
  }

  public static class Builder {
    private final List<CompletableFuture<Integer>> responses = new ArrayList<>();

    private Builder() {}

    public Builder withResponse(int score) {
      responses.add(CompletableFuture.completedFuture(score));
      return this;
    }

    public Builder withResponse(Throwable error) {
      CompletableFuture<Integer> future = new CompletableFuture<>();
      future.completeExceptionally(error);

      responses.add(future);
      return this;
    }

    public MockQueryService build() {
      return new MockQueryService(responses);
    }
  }
}
