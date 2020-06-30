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
package org.apache.flink.statefun.sdk.springboot;

import com.google.protobuf.Message;
import java.time.Duration;
import java.util.Optional;
import org.apache.flink.statefun.flink.core.polyglot.generated.Address;
import org.apache.flink.statefun.flink.core.polyglot.generated.Egress;

public interface Context {

  Address self();

  Address caller();

  <T extends Message> void send(Address target, T message);

  <T extends Message> void send(Egress target, T message);

  <T extends Message> void sendToKafka(Egress target, String topic, String key, T message);

  <T extends Message> void sendToKinesis(
      Egress target, String stream, String partitionKey, String explicitHash, T message);

  <T extends Message> void reply(T message);

  <T extends Message> void sendAfter(Address target, Duration duration, T message);

  <T extends Message> Optional<T> get(String name, Class<T> type);

  <T extends Message> void update(String name, T value);

  void clear(String name);
}
