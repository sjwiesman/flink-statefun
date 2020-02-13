package com.ververica.statefun.workshop.io;

import com.ververica.statefun.workshop.generated.Transaction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionLoggerSink implements SinkFunction<Transaction> {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionLoggerSink.class);

    @Override
    public void invoke(Transaction value, Context context) {
        LOG.info(String.format("Suspected Fraud for account id %s at %s", value.getAccount(), value.getMerchant()));
    }
}
