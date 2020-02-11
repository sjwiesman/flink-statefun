package com.ververica.statefun.workshop.functions;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;

public class FeatureVectorBuilder implements StatefulFunction {

  @Override
  public void invoke(Context context, Object input) {}
}
