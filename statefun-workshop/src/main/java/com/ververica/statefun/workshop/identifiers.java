package com.ververica.statefun.workshop;

import org.apache.flink.statefun.sdk.FunctionType;

public class identifiers {
  public static final FunctionType FRAUD_TYPE = new FunctionType("ververica", "fraud-count");

  public static final FunctionType MERCHANT_TYPE = new FunctionType("ververica", "merchant");
}
