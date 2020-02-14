/*
 * Licensed to Ververica GmbH under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ververica.statefun.workshop.functions;

import static com.ververica.statefun.workshop.identifiers.FRAUD_FN;
import static org.apache.flink.statefun.testutils.matchers.StatefulFunctionMatchers.*;
import static org.hamcrest.Matchers.equalTo;

import com.ververica.statefun.workshop.functions.exercises.FraudCount;
import com.ververica.statefun.workshop.messages.ConfirmFraud;
import com.ververica.statefun.workshop.messages.QueryFraud;
import com.ververica.statefun.workshop.messages.ReportedFraud;
import java.time.Duration;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.testutils.function.FunctionTestHarness;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FraudCountTest {

  private static final Address SENDER = new Address(new FunctionType("ververica", "sender"), "id");

  @Test
  @Ignore
  public void testCounting() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new FraudCount(), FRAUD_FN, "user1");

    Assert.assertThat(
        "When reporting fraud, the function should not respond",
        harness.invoke(SENDER, new ConfirmFraud()),
        sentNothing());

    Assert.assertThat(
        "When querying fraud, the function should return the current count",
        harness.invoke(SENDER, new QueryFraud()),
        sent(messagesTo(SENDER, equalTo(new ReportedFraud(1)))));
  }

  @Test
  @Ignore
  public void testRollingCount() {
    FunctionTestHarness harness =
        FunctionTestHarness.test(ignore -> new FraudCount(), FRAUD_FN, "user1");

    Assert.assertThat(
        "When reporting fraud, the function should not respond",
        harness.invoke(SENDER, new ConfirmFraud()),
        sentNothing());

    Assert.assertThat(
        "When querying fraud, the function should return the current count",
        harness.invoke(SENDER, new QueryFraud()),
        sent(messagesTo(SENDER, equalTo(new ReportedFraud(1)))));

    Assert.assertThat(
        "In 15 days the count should not change the function should not respond",
        harness.tick(Duration.ofDays(15)),
        sentNothing());

    Assert.assertThat(
        "In 15 days the count should not change the function should not respond",
        harness.invoke(SENDER, new QueryFraud()),
        sent(messagesTo(SENDER, equalTo(new ReportedFraud(1)))));

    Assert.assertThat(
        "When reporting fraud, the function should not respond",
        harness.invoke(SENDER, new ConfirmFraud()),
        sentNothing());

    Assert.assertThat(
        "When querying fraud, the function should return the current count",
        harness.invoke(SENDER, new QueryFraud()),
        sent(messagesTo(SENDER, equalTo(new ReportedFraud(2)))));

    Assert.assertThat(
        "After 30 days the count should decrement, but this won't return anything",
        harness.tick(Duration.ofDays(15)),
        sentNothing());

    Assert.assertThat(
        "When querying fraud, the function should return the current count",
        harness.invoke(SENDER, new QueryFraud()),
        sent(messagesTo(SENDER, equalTo(new ReportedFraud(1)))));
  }
}
