package com.ververica.statefun.workshop.functions.exercises;

import com.ververica.statefun.workshop.messages.MerchantScore;
import com.ververica.statefun.workshop.messages.QueryMerchantScore;
import com.ververica.statefun.workshop.utils.MerchantScoreService;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;

/**
 * Our application relies on a 3rd party service that returns a trustworthiness score for each
 * merchant.
 *
 * <p>This function, when it receives a {@link QueryMerchantScore} message, will make up to <b>3
 * attempts</b> to query the service and return a score. If the service does not successfully return
 * a result within 3 tries it will return back an error.
 *
 * <p>All cases will result in a {@link MerchantScore} message be sent back to the caller function.
 */
public class MerchantFunction implements StatefulFunction {

    private final MerchantScoreService client;

    public MerchantFunction(MerchantScoreService client) {
        this.client = client;
    }

    @Override
    public void invoke(Context context, Object input) {

    }
}
