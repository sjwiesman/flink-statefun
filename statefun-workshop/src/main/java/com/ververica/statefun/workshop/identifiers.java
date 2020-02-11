package com.ververica.statefun.workshop;

import com.ververica.statefun.workshop.generated.Transaction;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;

public class identifiers {

  public static final FunctionType MANAGER_FN =
      new FunctionType("ververica", "transaction-manager");

  public static final FunctionType FRAUD_FN = new FunctionType("ververica", "fraud-count");

  public static final FunctionType MERCHANT_FN = new FunctionType("ververica", "merchant");

  public static final FunctionType MODEL_FN = new FunctionType("ververica", "model");

  public static final EgressIdentifier<Transaction> ALERT =
      new EgressIdentifier<>("ververica", "alert", Transaction.class);

  public static final EgressIdentifier<Transaction> PROCESS =
      new EgressIdentifier<>("ververica", "process", Transaction.class);
}
