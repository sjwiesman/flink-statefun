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
package org.apache.flink.statefun.flink.core.translation;

import java.io.IOException;
import org.apache.flink.statefun.flink.core.StatefulFunctionsConfig;
import org.apache.flink.statefun.flink.core.StatefulFunctionsUniverse;
import org.apache.flink.statefun.flink.core.StatefulFunctionsUniverses;
import org.apache.flink.statefun.flink.core.classloader.ModuleClassLoader;
import org.apache.flink.statefun.flink.core.message.Message;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamOperatorParameters;
import org.apache.flink.util.SerializedValue;

public class IngressRouterFactory<IN> implements OneInputStreamOperatorFactory<IN, Message> {

  private static final long serialVersionUID = 1L;

  public static <IN> IngressRouterFactory<IN> create(
      StatefulFunctionsConfig config, IngressIdentifier<IN> id) {
    try {
      SerializedValue<IngressIdentifier<IN>> serializedValue = new SerializedValue<>(id);
      return new IngressRouterFactory<>(config, serializedValue);
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize ingress identifier " + id.toString(), e);
    }
  }

  private final StatefulFunctionsConfig config;

  private final SerializedValue<IngressIdentifier<IN>> serializedIdentifier;

  private ChainingStrategy strategy = ChainingStrategy.ALWAYS;

  private IngressRouterFactory(
      StatefulFunctionsConfig config, SerializedValue<IngressIdentifier<IN>> serializedIdentifier) {
    this.config = config;
    this.serializedIdentifier = serializedIdentifier;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends StreamOperator<Message>> T createStreamOperator(
      StreamOperatorParameters<Message> parameters) {
    ClassLoader moduleClassLoader =
        ModuleClassLoader.createModuleClassLoader(
            config, parameters.getContainingTask().getUserCodeClassLoader());
    IngressIdentifier<IN> id;
    try {
      id = serializedIdentifier.deserializeValue(moduleClassLoader);
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Failed to deserialize Ingress identifier", e);
    }
    StatefulFunctionsUniverse universe = StatefulFunctionsUniverses.get(moduleClassLoader, config);
    IngressRouterOperator<IN> operator = new IngressRouterOperator<>(universe, id);
    operator.setup(
        parameters.getContainingTask(), parameters.getStreamConfig(), parameters.getOutput());
    return (T) operator;
  }

  @Override
  public void setChainingStrategy(ChainingStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public ChainingStrategy getChainingStrategy() {
    return strategy;
  }

  @Override
  public Class<? extends StreamOperator> getStreamOperatorClass(ClassLoader classLoader) {
    return IngressRouterOperator.class;
  }
}
