package com.ververica.statefun.workshop.io;

import static com.ververica.statefun.workshop.io.identifiers.ALERT;
import static com.ververica.statefun.workshop.io.identifiers.TRANSACTIONS;

import com.ververica.statefun.workshop.generated.Transaction;
import java.util.Map;
import org.apache.flink.statefun.flink.io.datastream.SinkFunctionSpec;
import org.apache.flink.statefun.flink.io.datastream.SourceFunctionSpec;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

public class WorkshopIOModule implements StatefulFunctionModule {
  @Override
  public void configure(Map<String, String> globalConfiguration, Binder binder) {
    IngressSpec<Transaction> transactions =
        new SourceFunctionSpec<>(TRANSACTIONS, new TransactionSource(100));
    binder.bindIngress(transactions);

    EgressSpec<Transaction> alert = new SinkFunctionSpec<>(ALERT, new TransactionLoggerSink());
    binder.bindEgress(alert);
  }
}
