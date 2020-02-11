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

import com.ververica.statefun.workshop.generated.CheckMerchantScore;
import com.ververica.statefun.workshop.generated.MerchantResult;
import com.ververica.statefun.workshop.generated.ReportedMerchantScore;
import com.ververica.statefun.workshop.utils.MerchantMetadata;
import com.ververica.statefun.workshop.utils.QueryService;
import org.apache.flink.statefun.sdk.AsyncOperationResult;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;

public class MerchantFunction implements StatefulFunction {

  private final QueryService client;

  public MerchantFunction(QueryService client) {
    this.client = client;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void invoke(Context context, Object input) {
    if (input instanceof CheckMerchantScore) {
      MerchantMetadata metadata = new MerchantMetadata(context.caller(), 3);
      context.registerAsyncOperation(metadata, client.query(context.self().id()));
      return;
    }

    if (input instanceof AsyncOperationResult) {
      AsyncOperationResult<MerchantMetadata, Integer> result =
          (AsyncOperationResult<MerchantMetadata, Integer>) input;

      MerchantMetadata metadata = result.metadata();
      if (result.unknown()) {
        MerchantMetadata metadata1 =
            new MerchantMetadata(metadata.getAddress(), metadata.getRemainingAttempts());
        context.registerAsyncOperation(metadata1, client.query(context.self().id()));
      } else if (result.failure()) {
        if (metadata.getRemainingAttempts() == 0) {
          ReportedMerchantScore score =
              ReportedMerchantScore.newBuilder().setStatus(MerchantResult.UNKNOWN).build();
          context.send(metadata.getAddress(), score);
        } else {
          MerchantMetadata metadata1 =
              new MerchantMetadata(metadata.getAddress(), metadata.getRemainingAttempts() - 1);
          context.registerAsyncOperation(metadata1, client.query(context.self().id()));
        }
      } else {
        ReportedMerchantScore score =
            ReportedMerchantScore.newBuilder()
                .setStatus(MerchantResult.SCORED)
                .setScore(result.value())
                .build();
        context.send(metadata.getAddress(), score);
      }
    }
  }
}
