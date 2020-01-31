package org.apache.flink.statefun.flink.state.processor.table;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.flink.api.common.functions.AbstractRichFunction;
import org.apache.flink.api.common.functions.RichFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.KeyedStateBackend;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.apache.flink.state.api.input.operator.StateReaderOperator;
import org.apache.flink.state.api.runtime.SavepointRuntimeContext;
import org.apache.flink.statefun.flink.core.message.MessageFactoryType;
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

public class StatefulFunctionStateReaderOperator
    extends StateReaderOperator<RichFunction, String, VoidNamespace, Row> {

  private final Map<String, Class<?>> persistedValues;

  private transient FunctionType functionType;

  private transient State state;

  private transient List<Accessor<?>> accessors;

  StatefulFunctionStateReaderOperator(
      FunctionType functionType, Map<String, Class<?>> persistedValues) {
    super(new RuntimeCapture(), Types.STRING, VoidNamespaceSerializer.INSTANCE);
    this.functionType = functionType;
    this.persistedValues = persistedValues;
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

    state =
        new MultiplexedState(
            function.getRuntimeContext(),
            (KeyedStateBackend<Object>) (Object) getKeyedStateBackend(),
            new DynamicallyRegisteredTypes(
                new StaticallyRegisteredTypes(MessageFactoryType.WITH_RAW_PAYLOADS)));

    accessors = new ArrayList<>();
    for (Map.Entry<String, Class<?>> entry : persistedValues.entrySet()) {
      accessors.add(
          state.createFlinkStateAccessor(
              functionType, PersistedValue.of(entry.getKey(), entry.getValue())));
    }
  }

  @Override
  public void processElement(String key, VoidNamespace namespace, Collector<Row> collector) {
    state.setCurrentKey(new Address(functionType, key));

    Row row = new Row(accessors.size() + 1);
    row.setField(0, key);

    int i = 1;
    for (Accessor<?> accessor : accessors) {
      row.setField(i++, accessor.get());
    }

    collector.collect(row);
  }

  @Override
  public Iterator<Tuple2<String, VoidNamespace>> getKeysAndNamespaces(SavepointRuntimeContext ctx) {
    Iterator<String> keys =
        getKeyedStateBackend().getKeys("state", VoidNamespace.INSTANCE).iterator();
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
      String key = this.keys.next();
      return Tuple2.of(key, VoidNamespace.INSTANCE);
    }

    public void remove() {
      this.keys.remove();
    }
  }

  private static class RuntimeCapture extends AbstractRichFunction {}
}
