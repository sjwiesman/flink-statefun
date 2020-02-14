package com.ververica.statefun.workshop;

import static com.ververica.statefun.workshop.identifiers.*;
import static com.ververica.statefun.workshop.io.identifiers.TRANSACTIONS;

import com.ververica.statefun.workshop.functions.PythonModelScorer;
import com.ververica.statefun.workshop.functions.solutions.FraudCount;
import com.ververica.statefun.workshop.functions.solutions.TransactionManager;
import com.ververica.statefun.workshop.provider.MerchantProvider;
import com.ververica.statefun.workshop.routers.TransactionRouter;
import java.util.Map;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

public class ModelScoreModule implements StatefulFunctionModule {

  @Override
  public void configure(Map<String, String> globalConfiguration, Binder binder) {
    binder.bindIngressRouter(TRANSACTIONS, new TransactionRouter());

    binder.bindFunctionProvider(MANAGER_FN, ignore -> new TransactionManager());
    binder.bindFunctionProvider(FRAUD_FN, ignore -> new FraudCount());
    binder.bindFunctionProvider(MERCHANT_FN, new MerchantProvider());
    binder.bindFunctionProvider(MODEL_FN, ignore -> new PythonModelScorer());
  }
}
