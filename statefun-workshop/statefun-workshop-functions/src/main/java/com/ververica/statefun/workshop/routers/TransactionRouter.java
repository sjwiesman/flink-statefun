package com.ververica.statefun.workshop.routers;

import com.ververica.statefun.workshop.generated.Transaction;
import org.apache.flink.statefun.sdk.io.Router;

import static com.ververica.statefun.workshop.identifiers.MANAGER_FN;

public class TransactionRouter implements Router<Transaction> {

    @Override
    public void route(Transaction message, Downstream<Transaction> downstream) {
        String uid = message.getAccount() + message.getTimestamp().toString();
        downstream.forward(MANAGER_FN, uid, message);
    }
}
