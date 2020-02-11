package com.ververica.statefun.workshop.functions;

import static com.ververica.statefun.workshop.identifiers.*;

import com.ververica.statefun.workshop.generated.CheckMerchantScore;
import com.ververica.statefun.workshop.generated.FeatureVector;
import com.ververica.statefun.workshop.generated.FraudScore;
import com.ververica.statefun.workshop.generated.QueryFraud;
import com.ververica.statefun.workshop.generated.ReportedFraud;
import com.ververica.statefun.workshop.generated.ReportedMerchantScore;
import com.ververica.statefun.workshop.generated.Transaction;
import com.ververica.statefun.workshop.generated.UserResponse;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

public class TransactionManager implements StatefulFunction {

  private static final int THRESHOLD = 80;

  @Persisted
  private final PersistedValue<Transaction> transactionState =
      PersistedValue.of("transaction", Transaction.class);

  @Persisted
  private final PersistedValue<Integer> recentFraud =
      PersistedValue.of("recent-fraud", Integer.class);

  @Persisted
  private final PersistedValue<Integer> merchantScore =
      PersistedValue.of("merchant-score", Integer.class);

  @Override
  public void invoke(Context context, Object input) {
    if (input instanceof Transaction) {
      Transaction transaction = (Transaction) input;
      transactionState.set(transaction);

      String account = transaction.getAccount();
      context.send(FRAUD_FN, account, QueryFraud.getDefaultInstance());

      String merchant = transaction.getMerchant();
      context.send(MERCHANT_FN, merchant, CheckMerchantScore.getDefaultInstance());
    }

    if (input instanceof ReportedFraud) {
      ReportedFraud reported = (ReportedFraud) input;
      recentFraud.set(reported.getCount());

      Integer merchant = merchantScore.get();
      if (merchant != null) {
        score(context, merchant, reported.getCount());
      }
    }

    if (input instanceof ReportedMerchantScore) {
      ReportedMerchantScore reportedScore = (ReportedMerchantScore) input;
      merchantScore.set(reportedScore.getScore());

      Integer count = recentFraud.get();
      if (count != null) {
        score(context, reportedScore.getScore(), count);
      }
    }

    if (input instanceof FraudScore) {
      FraudScore fraudScore = (FraudScore) input;
      if (fraudScore.getScore() > THRESHOLD) {
        context.send(ALERT, transactionState.get());
      } else {
        context.send(PROCESS, transactionState.get());
        clean();
      }
    }

    if (input instanceof UserResponse) {
      UserResponse response = (UserResponse) input;
      if (!response.getIsFraud()) {
        context.send(PROCESS, transactionState.get());
      }

      clean();
    }
  }

  private void clean() {
    transactionState.clear();
    recentFraud.clear();
    merchantScore.clear();
  }

  private void score(Context context, Integer merchant, Integer count) {
    FeatureVector vector =
        FeatureVector.newBuilder().setFraudCount(count).setMerchantScore(merchant).build();
    context.send(MODEL_FN, transactionState.get().getAccount(), vector);
  }
}
