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

package org.apache.flink.statefun.sdk.springboot.runtime;

import static org.apache.flink.statefun.sdk.springboot.runtime.ProtoUtil.maybePack;
import static org.apache.flink.statefun.sdk.springboot.runtime.ProtoUtil.maybeUnpack;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.flink.statefun.flink.core.polyglot.generated.Address;
import org.apache.flink.statefun.flink.core.polyglot.generated.Egress;
import org.apache.flink.statefun.flink.core.polyglot.generated.FromFunction;
import org.apache.flink.statefun.flink.core.polyglot.generated.ToFunction;
import org.apache.flink.statefun.sdk.springboot.Context;

class BatchContext implements Context {

  private FromFunction.InvocationResponse.Builder response;

  private Address self;

  private Address caller;

  private final Map<String, Any> persistedValues;

  private final Set<String> updated;

  BatchContext() {
    this.self = null;
    this.caller = null;
    this.persistedValues = new HashMap<>();
    this.updated = new HashSet<>();
  }

  void setup(Address self, List<ToFunction.PersistedValue> states) {
    this.response = FromFunction.InvocationResponse.newBuilder();
    this.self = self;

    for (ToFunction.PersistedValue value : states) {
      try {
        persistedValues.put(value.getStateName(), Any.parseFrom(value.getStateValue()));
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException("Failed to deserialize state " + value.getStateName(), e);
      }
    }
  }

  @Override
  public Address self() {
    return self;
  }

  void setCaller(Address caller) {
    this.caller = caller;
  }

  @Override
  public Address caller() {
    return caller;
  }

  @Override
  public <T extends Message> void send(Address target, T message) {
    FromFunction.Invocation invocation =
        FromFunction.Invocation.newBuilder()
            .setTarget(target)
            .setArgument(maybePack(message))
            .build();

    response.addOutgoingMessages(invocation);
  }

  @Override
  public <T extends Message> void send(Egress target, T message) {
    FromFunction.EgressMessage egressMessage =
        FromFunction.EgressMessage.newBuilder()
            .setEgressNamespace(target.getNamespace())
            .setEgressType(target.getType())
            .setArgument(maybePack(message))
            .build();

    response.addOutgoingEgresses(egressMessage);
  }

  @Override
  public <T extends Message> void reply(T message) {
    send(caller, message);
  }

  @Override
  public <T extends Message> void sendAfter(Address target, Duration duration, T message) {
    FromFunction.DelayedInvocation invocation =
        FromFunction.DelayedInvocation.newBuilder()
            .setTarget(target)
            .setArgument(maybePack(message))
            .setDelayInMs(duration.toMillis())
            .build();

    response.addDelayedInvocations(invocation);
  }

  @Override
  public <T extends Message> Optional<T> get(String name, Class<T> type) {
    if (!persistedValues.containsKey(name) && !updated.contains(name)) {
      throw new IllegalStateException("Unknown state " + name);
    }

    Any value = persistedValues.get(name);
    if (value == null) {
      return Optional.empty();
    }

    try {
      return Optional.ofNullable(maybeUnpack(value, type));
    } catch (InvalidTypeException | InvalidProtocolBufferException e) {
      throw new RuntimeException(
          String.format(
              "Failed to access state %s in function %s/%s",
              name, self.getNamespace(), self.getType()),
          e);
    }
  }

  @Override
  public <T extends Message> void update(String name, T value) {
    if (!persistedValues.containsKey(name) && !updated.contains(name)) {
      throw new IllegalStateException("Unknown state " + name);
    }

    updated.add(name);

    if (value == null) {
      persistedValues.remove(name);
      return;
    }

    persistedValues.put(name, maybePack(value));
  }

  @Override
  public void clear(String name) {
    update(name, null);
  }

  FromFunction.InvocationResponse getResponse() {
    for (String state : updated) {
      FromFunction.PersistedValueMutation.Builder mutation =
          FromFunction.PersistedValueMutation.newBuilder().setStateName(state);

      Any value = persistedValues.get(state);
      if (value == null) {
        mutation.setMutationType(FromFunction.PersistedValueMutation.MutationType.DELETE);
      } else {
        mutation.setMutationType(FromFunction.PersistedValueMutation.MutationType.MODIFY);
        mutation.setStateValue(value.toByteString());
      }

      response.addStateMutations(mutation);
    }

    persistedValues.clear();
    updated.clear();

    return response.build();
  }
}
