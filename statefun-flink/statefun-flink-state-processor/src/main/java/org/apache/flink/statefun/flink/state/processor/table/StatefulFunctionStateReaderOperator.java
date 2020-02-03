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
package org.apache.flink.statefun.flink.state.processor.table;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.flink.api.common.functions.AbstractRichFunction;
import org.apache.flink.api.common.functions.RichFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.KeyedStateBackend;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.apache.flink.state.api.input.MultiStateKeyIterator;
import org.apache.flink.state.api.input.operator.StateReaderOperator;
import org.apache.flink.state.api.runtime.SavepointRuntimeContext;
import org.apache.flink.statefun.flink.core.message.MessageFactoryType;
import org.apache.flink.statefun.flink.core.state.FlinkState;
import org.apache.flink.statefun.flink.core.state.MultiplexedState;
import org.apache.flink.statefun.flink.core.state.State;
import org.apache.flink.statefun.flink.core.types.DynamicallyRegisteredTypes;
import org.apache.flink.statefun.flink.core.types.StaticallyRegisteredTypes;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.state.Accessor;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

/** An operator for reading persisted values from a stateful functions application. */
public class StatefulFunctionStateReaderOperator
    extends StateReaderOperator<RichFunction, String, VoidNamespace, Row> {

  private final List<PersistedValue<Object>> persistedValues;

  private final boolean disableMultiplexedState;

  private transient FunctionType functionType;

  private transient State state;

  private transient List<Accessor<?>> accessors;

  StatefulFunctionStateReaderOperator(
      FunctionType functionType,
      List<PersistedValue<Object>> persistedValues,
      boolean disableMultiplexedState) {
    super(new RuntimeCapture(), Types.STRING, VoidNamespaceSerializer.INSTANCE);
    this.functionType = functionType;
    this.persistedValues = persistedValues;
    this.disableMultiplexedState = disableMultiplexedState;
  }

  @SuppressWarnings("unused")
  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    oos.writeUTF(functionType.namespace());
    oos.writeUTF(functionType.name());
  }

  @SuppressWarnings("unused")
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    functionType = new FunctionType(ois.readUTF(), ois.readUTF());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void open() throws Exception {
    super.open();

    if (disableMultiplexedState) {
      state =
          new FlinkState(
              function.getRuntimeContext(),
              (KeyedStateBackend<Object>) (Object) getKeyedStateBackend(),
              new DynamicallyRegisteredTypes(
                  new StaticallyRegisteredTypes(MessageFactoryType.WITH_RAW_PAYLOADS)));
    } else {
      state =
          new MultiplexedState(
              function.getRuntimeContext(),
              (KeyedStateBackend<Object>) (Object) getKeyedStateBackend(),
              new DynamicallyRegisteredTypes(
                  new StaticallyRegisteredTypes(MessageFactoryType.WITH_RAW_PAYLOADS)));
    }

    accessors = new ArrayList<>();
    for (PersistedValue<Object> persistedValue : persistedValues) {
      accessors.add(state.createFlinkStateAccessor(functionType, persistedValue));
    }
  }

  @Override
  public void processElement(String key, VoidNamespace namespace, Collector<Row> collector) {
    state.setCurrentKey(new Address(functionType, key));

    Row row = new Row(accessors.size() + 1);
    row.setField(0, key);

    int i = 1;
    boolean nonNull = false;
    for (Accessor<?> accessor : accessors) {
      Object field = accessor.get();
      if (field != null) {
        nonNull = true;
      }
      row.setField(i++, accessor.get());
    }

    // When states are multiplexed
    // the only way to know if a particular
    // key is valid for the current function
    // type is to check if it contains a non null
    // persisted value.
    if (nonNull) {
      collector.collect(row);
    }
  }

  @Override
  public Iterator<Tuple2<String, VoidNamespace>> getKeysAndNamespaces(SavepointRuntimeContext ctx) {
    Iterator<String> keys =
        new MultiStateKeyIterator<>(ctx.getStateDescriptors(), getKeyedStateBackend());
    return new NamespaceDecorator(keys);
  }

  private static class NamespaceDecorator implements Iterator<Tuple2<String, VoidNamespace>> {
    private final Iterator<String> keys;

    private NamespaceDecorator(Iterator<String> keys) {
      this.keys = keys;
    }

    public boolean hasNext() {
      return this.keys.hasNext();
    }

    public Tuple2<String, VoidNamespace> next() {
      String key = keys.next();
      return Tuple2.of(key, VoidNamespace.INSTANCE);
    }

    public void remove() {
      keys.remove();
    }
  }

  private static class RuntimeCapture extends AbstractRichFunction {}
}
