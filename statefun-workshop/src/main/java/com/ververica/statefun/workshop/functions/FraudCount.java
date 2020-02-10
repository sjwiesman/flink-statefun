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

import com.ververica.statefun.workshop.generated.ExpireFraud;
import com.ververica.statefun.workshop.generated.QueryFraud;
import com.ververica.statefun.workshop.generated.ReportFraud;
import com.ververica.statefun.workshop.generated.ReportedFraud;
import java.time.Duration;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

public class FraudCount implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("ververica", "fraud-count");

  @Persisted
  private final PersistedValue<Integer> count = PersistedValue.of("count", Integer.class);

  @Override
  public void invoke(Context context, Object input) {
    if (input instanceof ReportFraud) {
      int current = count.getOrDefault(0);
      count.set(current + 1);

      context.sendAfter(Duration.ofDays(30), context.self(), ExpireFraud.getDefaultInstance());
      return;
    }

    if (input instanceof ExpireFraud) {
      int current = count.getOrDefault(0);
      if (current == 0 || current == 1) {
        count.clear();
      }

      count.set(current - 1);
    }

    if (input instanceof QueryFraud) {
      int current = count.getOrDefault(0);
      ReportedFraud response = ReportedFraud.newBuilder().setCount(current).build();
      context.reply(response);
    }
  }
}
