package org.apache.flink.statefun.examples.modelserving;

import java.util.Map;
import org.apache.flink.statefun.examples.modelserving.functions.FnAccount;
import org.apache.flink.statefun.examples.modelserving.functions.FnMerchant;
import org.apache.flink.statefun.examples.modelserving.functions.FnTransaction;
import org.apache.flink.statefun.examples.modelserving.util.QueryService;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

public class ModelServingModule implements StatefulFunctionModule {

  @Override
  public void configure(Map<String, String> globalConfiguration, Binder binder) {

    binder.bindFunctionProvider(IDs.TRANSACTION, ignore -> new FnTransaction());
    binder.bindFunctionProvider(IDs.ACCOUNT, ignore -> new FnAccount());
    binder.bindFunctionProvider(IDs.MERCHANT, ignore -> new FnMerchant(QueryService.getInstance()));

    binder.bindFunctionProvider(IDs.SCORER, ignore -> new PythonScoringFunction());
  }
}
