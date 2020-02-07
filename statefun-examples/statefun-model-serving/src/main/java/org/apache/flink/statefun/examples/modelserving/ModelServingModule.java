package org.apache.flink.statefun.examples.modelserving;

import java.util.Map;
import org.apache.flink.statefun.examples.modelserving.functions.FnAccount;
import org.apache.flink.statefun.examples.modelserving.functions.FnMerchant;
import org.apache.flink.statefun.examples.modelserving.functions.FnTransaction;
import org.apache.flink.statefun.examples.modelserving.util.QueryService;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

import static org.apache.flink.statefun.examples.modelserving.identifiers.ACCOUNT;
import static org.apache.flink.statefun.examples.modelserving.identifiers.MERCHANT;
import static org.apache.flink.statefun.examples.modelserving.identifiers.SCORER;
import static org.apache.flink.statefun.examples.modelserving.identifiers.TRANSACTION;

public class ModelServingModule implements StatefulFunctionModule {

  @Override
  public void configure(Map<String, String> globalConfiguration, Binder binder) {

    binder.bindFunctionProvider(TRANSACTION, ignore -> new FnTransaction());
    binder.bindFunctionProvider(ACCOUNT, ignore -> new FnAccount());
    binder.bindFunctionProvider(MERCHANT, ignore -> new FnMerchant(QueryService.getInstance()));
    binder.bindFunctionProvider(SCORER, ignore -> new PythonScoringFunction());
  }
}
