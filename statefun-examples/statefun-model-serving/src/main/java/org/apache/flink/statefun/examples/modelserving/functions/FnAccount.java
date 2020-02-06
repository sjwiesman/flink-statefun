package org.apache.flink.statefun.examples.modelserving.functions;

import java.time.Duration;
import org.apache.flink.statefun.examples.modelserving.util.UnsupportedTypeException;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.apache.flink.statefun.training.modelserving.generated.ConfirmedFraud;
import org.apache.flink.statefun.training.modelserving.generated.FraudHistory;
import org.apache.flink.statefun.training.modelserving.generated.QueryHistory;
import org.apache.flink.statefun.training.modelserving.generated.Timeout;

/**
 * A function that keeps track of the number of reported fraudulent transactions for an account on a
 * rolling 30 day period.
 */
public class FnAccount implements StatefulFunction {

  @Persisted
  private final PersistedValue<Integer> confirmedFraud =
      PersistedValue.of("confirmed", Integer.class);

  @Override
  public void invoke(Context context, Object input) {
    if (input instanceof ConfirmedFraud) {
      confirmedFraud.updateAndGet(count -> count + 1);

      context.sendAfter(Duration.ofDays(30), context.self(), Timeout.getDefaultInstance());
      return;
    }

    if (input instanceof Timeout) {
      confirmedFraud.updateAndGet(count -> count - 1);
      return;
    }

    if (input instanceof QueryHistory) {
      context.reply(FraudHistory.newBuilder().setCount(confirmedFraud.getOrDefault(0)).build());
      return;
    }

    throw new UnsupportedTypeException(input);
  }
}
