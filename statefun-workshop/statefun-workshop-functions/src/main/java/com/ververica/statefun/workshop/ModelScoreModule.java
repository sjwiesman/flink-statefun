package com.ververica.statefun.workshop;

import com.ververica.statefun.workshop.functions.FraudCount;
import com.ververica.statefun.workshop.functions.TransactionManager;
import com.ververica.statefun.workshop.provider.MerchantProvider;
import com.ververica.statefun.workshop.routers.TransactionRouter;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

import java.util.Map;

import static com.ververica.statefun.workshop.identifiers.FRAUD_FN;
import static com.ververica.statefun.workshop.identifiers.MANAGER_FN;
import static com.ververica.statefun.workshop.identifiers.MERCHANT_FN;
import static com.ververica.statefun.workshop.io.identifiers.TRANSACTIONS;

public class ModelScoreModule implements StatefulFunctionModule {

    @Override
    public void configure(Map<String, String> globalConfiguration, Binder binder) {
        binder.bindIngressRouter(TRANSACTIONS, new TransactionRouter());

        binder.bindFunctionProvider(MANAGER_FN, ignore -> new TransactionManager());
        binder.bindFunctionProvider(FRAUD_FN, ignore -> new FraudCount());
        binder.bindFunctionProvider(MERCHANT_FN, new MerchantProvider());
    }
}
