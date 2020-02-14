package com.ververica.statefun.workshop.functions;

import static com.ververica.statefun.workshop.identifiers.MERCHANT_FN;
import static org.apache.flink.statefun.testutils.matchers.StatefulFunctionMatchers.*;
import static org.hamcrest.core.IsEqual.equalTo;

import com.ververica.statefun.workshop.functions.exercises.MerchantFunction;
import com.ververica.statefun.workshop.messages.MerchantScore;
import com.ververica.statefun.workshop.messages.QueryMerchantScore;
import com.ververica.statefun.workshop.utils.MerchantScoreService;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.StatefulFunctionProvider;
import org.apache.flink.statefun.testutils.function.FunctionTestHarness;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MerchantFunctionTest {

  private static String SELF_ID = "my-id";

  private static Address CALLER = new Address(new FunctionType("ververica", "caller"), "id");

  @Test
  public void testAsyncOperation() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(new TestProvider(), MERCHANT_FN, SELF_ID);

    Assert.assertThat(
        harness.invoke(CALLER, new QueryMerchantScore()),
        sent(messagesTo(CALLER, equalTo(MerchantScore.score(1)))));
  }

  @Test
  public void testSingleFailureOperation() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(new TestProviderWithSingleFailure(), MERCHANT_FN, SELF_ID);

    Assert.assertThat(
        harness.invoke(CALLER, new QueryMerchantScore()),
        sent(messagesTo(CALLER, equalTo(MerchantScore.score(1)))));
  }

  @Test
  public void testAsyncFailure() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(new TestProviderWithMultipleFailures(), MERCHANT_FN, SELF_ID);

    Assert.assertThat(
        harness.invoke(CALLER, new QueryMerchantScore()),
        sent(messagesTo(CALLER, equalTo(MerchantScore.error()))));
  }

  private static class TestProvider implements StatefulFunctionProvider {

    @Override
    public StatefulFunction functionOfType(FunctionType type) {
      MerchantScoreService client = MockMerchantScoreService.builder().withResponse(1).build();

      return new MerchantFunction(client);
    }
  }

  private static class TestProviderWithSingleFailure implements StatefulFunctionProvider {

    @Override
    public StatefulFunction functionOfType(FunctionType type) {
      MerchantScoreService client =
          MockMerchantScoreService.builder()
              .withResponse(new Throwable("error"))
              .withResponse(1)
              .build();

      return new MerchantFunction(client);
    }
  }

  private static class TestProviderWithMultipleFailures implements StatefulFunctionProvider {

    @Override
    public StatefulFunction functionOfType(FunctionType type) {
      MerchantScoreService client =
          MockMerchantScoreService.builder()
              .withResponse(new Throwable("error"))
              .withResponse(new Throwable("error"))
              .withResponse(new Throwable("error"))
              .build();

      return new MerchantFunction(client);
    }
  }
}
