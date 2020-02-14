package com.ververica.statefun.workshop.io;

import com.ververica.statefun.workshop.generated.Transaction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionLoggerSink extends RichSinkFunction<Transaction> {

  private static final Logger LOG = LoggerFactory.getLogger(TransactionLoggerSink.class);

  private transient Counter counter;

  @Override
  public void open(Configuration parameters) {
    counter = getRuntimeContext().getMetricGroup().counter("alerts");
  }

  @Override
  public void invoke(Transaction value, Context context) {
    counter.inc();
    LOG.info(
        String.format(
            "Suspected Fraud for account id %s at %s", value.getAccount(), value.getMerchant()));
  }
}
