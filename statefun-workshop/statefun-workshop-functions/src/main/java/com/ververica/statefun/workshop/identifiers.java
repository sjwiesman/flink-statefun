package com.ververica.statefun.workshop;

import org.apache.flink.statefun.sdk.FunctionType;

public class identifiers {

  public static final FunctionType MANAGER_FN =
      new FunctionType("ververica", "transaction-manager");

  public static final FunctionType FRAUD_FN = new FunctionType("ververica", "fraud-count");

  public static final FunctionType MERCHANT_FN = new FunctionType("ververica", "merchant");

  public static final FunctionType MODEL_FN = new FunctionType("ververica", "model");
}
