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
package com.ververica.statefun.workshop.functions.solutions;

import com.ververica.statefun.workshop.messages.MerchantMetadata;
import com.ververica.statefun.workshop.messages.MerchantScore;
import com.ververica.statefun.workshop.messages.QueryMerchantScore;
import com.ververica.statefun.workshop.utils.MerchantScoreService;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.AsyncOperationResult;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;

/**
 * Our application relies on a 3rd party service that returns a trustworthiness score for each
 * merchant.
 *
 * <p>This function, when it receives a {@link QueryMerchantScore} message, will make up to <b>3
 * attempts</b> to query the service and return a score. If the service does not successfully return
 * a result within 3 tries it will return back an error.
 *
 * <p>All cases will result in a {@link MerchantScore} message be sent back to the caller function.
 */
public class MerchantFunction implements StatefulFunction {

  private final MerchantScoreService client;

  public MerchantFunction(MerchantScoreService client) {
    this.client = client;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void invoke(Context context, Object input) {
    if (input instanceof QueryMerchantScore) {
      queryService(context, context.caller(), 2);
    }

    if (input instanceof AsyncOperationResult) {
      AsyncOperationResult<MerchantMetadata, Integer> result =
          (AsyncOperationResult<MerchantMetadata, Integer>) input;

      MerchantMetadata metadata = result.metadata();
      if (result.unknown()) {
        queryService(context, metadata.getAddress(), metadata.getRemainingAttempts());
      } else if (result.failure()) {
        if (metadata.getRemainingAttempts() == 0) {
          context.send(metadata.getAddress(), MerchantScore.error());
        } else {
          queryService(context, metadata.getAddress(), metadata.getRemainingAttempts() - 1);
        }
      } else {
        MerchantScore score = MerchantScore.score(result.value());
        context.send(metadata.getAddress(), score);
      }
    }
  }

  /**
   * Query the external service and register the future as a callback.
   *
   * @param context The function context.
   * @param address The address where the final result should be sent.
   * @param attempts The number of remaining attempts.
   */
  private void queryService(Context context, Address address, int attempts) {
    MerchantMetadata metadata = new MerchantMetadata(address, attempts);
    context.registerAsyncOperation(metadata, client.query(context.self().id()));
  }
}
