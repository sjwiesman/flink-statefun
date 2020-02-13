package com.ververica.statefun.workshop.provider;

import com.ververica.statefun.workshop.functions.MerchantFunction;
import com.ververica.statefun.workshop.utils.MerchantScoreService;
import com.ververica.statefun.workshop.utils.ProductionMerchantScoreService;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.StatefulFunctionProvider;

public class MerchantProvider implements StatefulFunctionProvider {

    @Override
    public StatefulFunction functionOfType(FunctionType type) {
        MerchantScoreService client = new ProductionMerchantScoreService();
        return new MerchantFunction(client);
    }
}
