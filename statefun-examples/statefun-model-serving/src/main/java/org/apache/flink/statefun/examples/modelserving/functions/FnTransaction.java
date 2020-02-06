package org.apache.flink.statefun.examples.modelserving.functions;

import org.apache.flink.statefun.examples.modelserving.IDs;
import org.apache.flink.statefun.examples.modelserving.util.UnsupportedTypeException;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.apache.flink.statefun.training.modelserving.generated.FeatureVector;
import org.apache.flink.statefun.training.modelserving.generated.FraudHistory;
import org.apache.flink.statefun.training.modelserving.generated.QueryHistory;
import org.apache.flink.statefun.training.modelserving.generated.QueryMerchant;
import org.apache.flink.statefun.training.modelserving.generated.Transaction;

public class FnTransaction implements StatefulFunction {

  @Persisted
  private final PersistedValue<Integer> countDown = PersistedValue.of("count-down", Integer.class);

  @Persisted
  private final PersistedValue<Transaction> transactionState =
      PersistedValue.of("transaction", Transaction.class);

  @Persisted
  private final PersistedValue<Double> vendor = PersistedValue.of("vendor", Double.class);

  @Persisted
  private final PersistedValue<Integer> accountHistory =
      PersistedValue.of("history", Integer.class);

  @Override
  public void invoke(Context context, Object input) {
    if (input instanceof Transaction) {
      Transaction transaction = (Transaction) input;
      countDown.set(2);
      transactionState.set(transaction);

      context.send(IDs.ACCOUNT, transaction.getAccount(), QueryHistory.getDefaultInstance());
      context.send(IDs.MERCHANT, transaction.getMerchantId(), QueryMerchant.getDefaultInstance());

      return;
    }

    if (input instanceof FraudHistory) {
      FraudHistory history = (FraudHistory) input;
      accountHistory.set(history.getCount());

      checkAndScore(context);

      return;
    }

    throw new UnsupportedTypeException(input);
  }

  private void checkAndScore(Context context) {
    int count = countDown.updateAndGet(cc -> cc - 1);
    if (count != 0) {
      return;
    }

    FeatureVector vector =
        FeatureVector.newBuilder()
            .setMerchantFraud(vendor.get())
            .setTransaction(transactionState.get())
            .setFraudHistory(accountHistory.get())
            .build();

    context.send(IDs.SCORER, vector.getTransaction().getAccount(), vector);

    countDown.clear();
    accountHistory.clear();
    vendor.clear();
  }
}
