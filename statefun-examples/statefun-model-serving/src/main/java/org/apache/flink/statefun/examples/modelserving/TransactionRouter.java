package org.apache.flink.statefun.examples.modelserving;

import org.apache.flink.statefun.sdk.io.Router;
import org.apache.flink.statefun.training.modelserving.generated.Transaction;

public class TransactionRouter implements Router<Transaction> {

  @Override
  public void route(Transaction message, Downstream<Transaction> downstream) {
    downstream.forward(identifiers.ACCOUNT, message.getAccount(), message);
  }
}
