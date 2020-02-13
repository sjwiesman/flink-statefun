package com.ververica.statefun.workshop.functions;

import com.ververica.statefun.workshop.generated.FraudScore;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;

import java.util.Random;

/**
 * This will be python
 */
public class PythonModelScorer implements StatefulFunction {

    private final Random gen = new Random();

    @Override
    public void invoke(Context context, Object input) {
        context.reply(FraudScore.newBuilder().setScore(gen.nextInt(100)).build());
    }
}
