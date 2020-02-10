package com.ververica.statefun.workshop.functions;

import com.ververica.statefun.workshop.utils.QueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MockQueryService implements QueryService {

  private final Map<String, List<CompletableFuture<Integer>>> responses;

  public static Builder builder() {
    return new Builder();
  }

  private MockQueryService(Map<String, List<CompletableFuture<Integer>>> responses) {
    this.responses = responses;
  }

  @Override
  public CompletableFuture<Integer> query(String merchantId) {
    List<CompletableFuture<Integer>> response = responses.get(merchantId);
    return response.remove(0);
  }

  public static class Builder {
    private final Map<String, List<CompletableFuture<Integer>>> responses = new HashMap<>();

    private Builder() {}

    public Builder withResponse(String id, int score) {
      responses
          .computeIfAbsent(id, ignore -> new ArrayList<>())
          .add(CompletableFuture.completedFuture(score));
      return this;
    }

    public Builder withResponse(String id, Throwable error) {
      CompletableFuture<Integer> future = new CompletableFuture<>();
      future.completeExceptionally(error);

      responses.computeIfAbsent(id, ignore -> new ArrayList<>()).add(future);
      return this;
    }

    public MockQueryService build() {
      return new MockQueryService(responses);
    }
  }
}
