package org.apache.flink.statefun.examples.modelserving;

import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.statefun.training.modelserving.generated.Transaction;
import org.apache.flink.statefun.training.modelserving.generated.TransactionScore;

public class identifiers {

  public static final IngressIdentifier<Transaction> TRANSACTIONS =
      new IngressIdentifier<>(Transaction.class, "apache/flink", "transactions");

  public static final EgressIdentifier<TransactionScore> SCORE =
      new EgressIdentifier<>("apache/flink", "scores", TransactionScore.class);

  public static final FunctionType TRANSACTION = new FunctionType("apache/flink", "transaction");

  public static final FunctionType MERCHANT = new FunctionType("apache/flink", "merchant");

  public static final FunctionType ACCOUNT = new FunctionType("apache/flink", "account");

  public static final FunctionType SCORER = new FunctionType("apache/flink", "scorer");
}
