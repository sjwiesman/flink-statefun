package com.ververica.statefun.workshop.messages;

import com.ververica.statefun.workshop.functions.solutions.MerchantFunction;

/**
 * This message is sent to an instance of a {@link MerchantFunction}
 * to query the external service for the trustworthiness score of a merchant.
 */
public final class QueryMerchantScore {

    @Override
    public boolean equals(Object obj) {
        return obj instanceof QueryMerchantScore;
    }
}
