/*
 * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.flink.statefun.flink.core.functions;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.apache.flink.statefun.flink.core.StatefulFunctionsConfig;
import org.apache.flink.statefun.flink.core.StatefulFunctionsUniverse;
import org.apache.flink.statefun.flink.core.StatefulFunctionsUniverses;
import org.apache.flink.statefun.flink.core.classloader.ModuleClassLoader;
import org.apache.flink.statefun.flink.core.message.Message;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.streaming.api.operators.*;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.SerializedValue;

public final class FunctionGroupDispatchFactory
    implements OneInputStreamOperatorFactory<Message, Message>, YieldingOperatorFactory<Message> {

  private static final long serialVersionUID = 1;

  public static FunctionGroupDispatchFactory create(
      StatefulFunctionsConfig configuration,
      Map<EgressIdentifier<?>, OutputTag<Object>> sideOutputs) {
    try {
      SerializedValue<Map<EgressIdentifier<?>, OutputTag<Object>>> serializedSideOutputs =
          new SerializedValue<>(sideOutputs);
      return new FunctionGroupDispatchFactory(configuration, serializedSideOutputs);
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialized egress side outputs", e);
    }
  }

  private final StatefulFunctionsConfig configuration;

  private final SerializedValue<Map<EgressIdentifier<?>, OutputTag<Object>>> serializedSideOutputs;

  private transient MailboxExecutor mailboxExecutor;

  private FunctionGroupDispatchFactory(
      StatefulFunctionsConfig configuration,
      SerializedValue<Map<EgressIdentifier<?>, OutputTag<Object>>> serializedSideOutputs) {
    this.configuration = configuration;
    this.serializedSideOutputs = serializedSideOutputs;
  }

  @Override
  public void setMailboxExecutor(MailboxExecutor mailboxExecutor) {
    this.mailboxExecutor =
        Objects.requireNonNull(mailboxExecutor, "Mailbox executor can't be NULL");
  }

  @Override
  public <T extends StreamOperator<Message>> T createStreamOperator(
      StreamOperatorParameters<Message> streamOperatorParameters) {

    ClassLoader moduleClassLoader =
        ModuleClassLoader.createModuleClassLoader(
            configuration, streamOperatorParameters.getContainingTask().getUserCodeClassLoader());
    StatefulFunctionsUniverse universe =
        StatefulFunctionsUniverses.get(moduleClassLoader, configuration);

    Map<EgressIdentifier<?>, OutputTag<Object>> sideOutputs = null;
    try {
      sideOutputs = serializedSideOutputs.deserializeValue(moduleClassLoader);
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Failed to deserialize egress side outputs", e);
    }
    FunctionGroupOperator fn =
        new FunctionGroupOperator(
            universe,
            sideOutputs,
            configuration,
            mailboxExecutor,
            ChainingStrategy.ALWAYS,
            streamOperatorParameters.getProcessingTimeService());
    fn.setup(
        streamOperatorParameters.getContainingTask(),
        streamOperatorParameters.getStreamConfig(),
        streamOperatorParameters.getOutput());

    return (T) fn;
  }

  @Override
  public void setChainingStrategy(ChainingStrategy chainingStrategy) {
    // We ignore the chaining strategy, because we only use ChainingStrategy.ALWAYS
  }

  @Override
  public ChainingStrategy getChainingStrategy() {
    return ChainingStrategy.ALWAYS;
  }

  @Override
  public Class<? extends StreamOperator> getStreamOperatorClass(ClassLoader classLoader) {
    return FunctionGroupOperator.class;
  }
}
