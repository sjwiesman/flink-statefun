package com.ververica.statefun.workshop.functions;

import static com.ververica.statefun.workshop.identifiers.*;
import static org.apache.flink.statefun.testutils.matchers.StatefulFunctionMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.google.protobuf.Timestamp;
import com.ververica.statefun.workshop.generated.CheckMerchantScore;
import com.ververica.statefun.workshop.generated.FeatureVector;
import com.ververica.statefun.workshop.generated.FraudScore;
import com.ververica.statefun.workshop.generated.QueryFraud;
import com.ververica.statefun.workshop.generated.ReportedFraud;
import com.ververica.statefun.workshop.generated.ReportedMerchantScore;
import com.ververica.statefun.workshop.generated.Transaction;
import com.ververica.statefun.workshop.generated.UserResponse;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.testutils.function.FunctionTestHarness;
import org.junit.Assert;
import org.junit.Test;

public class TransactionManagerTest {

  private static final String ACCOUNT = "account-id";

  private static final String MERCHANT = "merchant-id";

  private static final Transaction TRANSACTION =
      Transaction.newBuilder()
          .setAccount(ACCOUNT)
          .setMerchant(MERCHANT)
          .setTimestamp(Timestamp.getDefaultInstance())
          .setAmount(100)
          .build();

  @Test
  public void initialTransactionTest() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new TransactionManager(), MANAGER_FN, "id");

    Assert.assertThat(
        harness.invoke(TRANSACTION),
        sent(
            messagesTo(new Address(FRAUD_FN, ACCOUNT), equalTo(QueryFraud.getDefaultInstance())),
            messagesTo(
                new Address(MERCHANT_FN, MERCHANT),
                equalTo(CheckMerchantScore.getDefaultInstance()))));
  }

  @Test
  public void reportFraudThenMerchantTest() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new TransactionManager(), MANAGER_FN, "id");

    harness.invoke(TRANSACTION);

    Assert.assertThat(
        harness.invoke(ReportedFraud.newBuilder().setCount(1).build()), sentNothing());

    Assert.assertThat(
        harness.invoke(ReportedMerchantScore.newBuilder().setScore(1).build()),
        sent(
            messagesTo(
                new Address(MODEL_FN, ACCOUNT),
                equalTo(FeatureVector.newBuilder().setFraudCount(1).setMerchantScore(1).build()))));
  }

  @Test
  public void reportMerchantThenFraudTest() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new TransactionManager(), MANAGER_FN, "id");

    harness.invoke(TRANSACTION);

    Assert.assertThat(
        harness.invoke(ReportedMerchantScore.newBuilder().setScore(1).build()), sentNothing());

    Assert.assertThat(
        harness.invoke(ReportedFraud.newBuilder().setCount(1).build()),
        sent(
            messagesTo(
                new Address(MODEL_FN, ACCOUNT),
                equalTo(FeatureVector.newBuilder().setFraudCount(1).setMerchantScore(1).build()))));
  }

  @Test
  public void noSuspectedFraudTest() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new TransactionManager(), MANAGER_FN, "id");
    harness.invoke(TRANSACTION);

    Assert.assertThat(harness.invoke(FraudScore.newBuilder().setScore(50).build()), sentNothing());

    Assert.assertThat(harness.getResults(PROCESS), contains(equalTo(TRANSACTION)));
  }

  @Test
  public void discoveredFraudTest() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new TransactionManager(), MANAGER_FN, "id");
    harness.invoke(TRANSACTION);

    Assert.assertThat(harness.invoke(FraudScore.newBuilder().setScore(81).build()), sentNothing());

    Assert.assertThat(harness.getResults(ALERT), contains(equalTo(TRANSACTION)));

    Assert.assertThat(
        harness.invoke(UserResponse.newBuilder().setIsFraud(true).build()), sentNothing());

    Assert.assertThat(harness.getResults(PROCESS), empty());
  }

  @Test
  public void falseFraudTest() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new TransactionManager(), MANAGER_FN, "id");
    harness.invoke(TRANSACTION);

    Assert.assertThat(harness.invoke(FraudScore.newBuilder().setScore(81).build()), sentNothing());

    Assert.assertThat(harness.getResults(ALERT), contains(equalTo(TRANSACTION)));

    Assert.assertThat(
        harness.invoke(UserResponse.newBuilder().setIsFraud(false).build()), sentNothing());

    Assert.assertThat(harness.getResults(PROCESS), contains(equalTo(TRANSACTION)));
  }
}
