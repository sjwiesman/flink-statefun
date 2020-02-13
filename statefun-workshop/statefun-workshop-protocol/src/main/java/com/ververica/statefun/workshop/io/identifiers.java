package com.ververica.statefun.workshop.io;

import com.ververica.statefun.workshop.generated.Transaction;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;

public class identifiers {
    public static final IngressIdentifier<Transaction> TRANSACTIONS = new IngressIdentifier<>(Transaction.class, "ververica", "transactions");

    public static final EgressIdentifier<Transaction> ALERT = new EgressIdentifier<>("ververica", "alert", Transaction.class);
}
