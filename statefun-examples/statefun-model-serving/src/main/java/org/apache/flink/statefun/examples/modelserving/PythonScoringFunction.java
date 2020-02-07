package org.apache.flink.statefun.examples.modelserving;

import java.util.Random;
import org.apache.flink.statefun.examples.modelserving.util.UnsupportedTypeException;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.training.modelserving.generated.FeatureVector;
import org.apache.flink.statefun.training.modelserving.generated.TransactionScore;

import static org.apache.flink.statefun.examples.modelserving.identifiers.SCORE;

/** This function should be Python. */
public class PythonScoringFunction implements StatefulFunction {

  private final Random model = new Random();

  @Override
  public void invoke(Context context, Object input) {
    FeatureVector vector = unwrap(input);

    TransactionScore score =
        TransactionScore.newBuilder()
            .setTransactionId(vector.getTransaction().getTransactionId())
            .setAccountId(vector.getTransaction().getAccount())
            .setScore(model.nextDouble())
            .build();

    context.send(SCORE, score);
  }

  private FeatureVector unwrap(Object input) {
    if (input instanceof FeatureVector) {
      return (FeatureVector) input;
    }

    throw new UnsupportedTypeException(input);
  }
}
