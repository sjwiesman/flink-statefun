package org.apache.flink.statefun.examples.modelserving.functions;

import org.apache.flink.statefun.examples.modelserving.util.QueryService;
import org.apache.flink.statefun.examples.modelserving.util.UnsupportedTypeException;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.AsyncOperationResult;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.training.modelserving.generated.MerchantScore;
import org.apache.flink.statefun.training.modelserving.generated.QueryMerchant;

@SuppressWarnings("unchecked")
public class FnMerchant implements StatefulFunction {

  private final QueryService client;

  public FnMerchant(QueryService client) {
    this.client = client;
  }

  @Override
  public void invoke(Context context, Object input) {
    if (input instanceof QueryMerchant) {
      String merchantId = context.self().id();
      context.registerAsyncOperation(context.caller(), client.score(merchantId));
      return;
    }

    if (input instanceof AsyncOperationResult) {
      AsyncOperationResult<Address, Double> result = (AsyncOperationResult<Address, Double>) input;

      Address address = result.metadata();

      if (result.unknown()) {
        context.registerAsyncOperation(address, client.score(context.self().id()));
        return;
      }

      double score = result.successful() ? result.value() : 0.0;
      context.send(address, MerchantScore.newBuilder().setScore(score).build());

      return;
    }

    throw new UnsupportedTypeException(input);
  }
}
