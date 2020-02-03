package org.apache.flink.statefun.flink.state.processor.table;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.runtime.checkpoint.OperatorState;
import org.apache.flink.runtime.checkpoint.savepoint.Savepoint;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.state.api.input.KeyedStateInputFormat;
import org.apache.flink.state.api.runtime.SavepointLoader;
import org.apache.flink.state.api.runtime.metadata.SavepointMetadata;
import org.apache.flink.statefun.flink.core.StatefulFunctionsJobConstants;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.sources.InputFormatTableSource;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

public class StateTableSource extends InputFormatTableSource<Row> {

  private final Map<String, Class<?>> values;

  private final OperatorState state;

  private final StateBackend stateBackend;

  private final FunctionType functionType;

  private final boolean disableMultiplexedState;

  private StateTableSource(
      Map<String, Class<?>> values,
      OperatorState state,
      StateBackend stateBackend,
      FunctionType functionType,
      boolean disableMultiplexedState) {
    this.values = values;
    this.state = state;
    this.stateBackend = stateBackend;
    this.functionType = functionType;
    this.disableMultiplexedState = disableMultiplexedState;
  }

  @Override
  public InputFormat<Row, ?> getInputFormat() {
    return new KeyedStateInputFormat<>(
        state,
        stateBackend,
        new StatefulFunctionStateReaderOperator(functionType, values, disableMultiplexedState));
  }

  @Override
  @SuppressWarnings("deprecation")
  public TableSchema getTableSchema() {
    TableSchema.Builder schema = TableSchema.builder().field("function_id", Types.STRING);
    for (Map.Entry<String, Class<?>> entry : values.entrySet()) {
      schema = schema.field(entry.getKey(), TypeInformation.of(entry.getValue()));
    }

    return schema.build();
  }

  public static Builder analyze(String savepointPath) throws IOException {
    Savepoint savepoint = SavepointLoader.loadSavepoint(savepointPath);
    int maxParallelism =
        savepoint.getOperatorStates().stream()
            .map(OperatorState::getMaxParallelism)
            .max(Comparator.naturalOrder())
            .orElseThrow(
                () -> new RuntimeException("Savepoint must contain at least one operator state."));

    SavepointMetadata metadata =
        new SavepointMetadata(
            maxParallelism, savepoint.getMasterStates(), savepoint.getOperatorStates());

    try {
      OperatorState state =
          metadata.getOperatorState(StatefulFunctionsJobConstants.FUNCTION_OPERATOR_UID);
      return new Builder(state);
    } catch (IOException e) {
      throw new RuntimeException(
          "This savepoint does not appear to be from a Stateful Functions application");
    }
  }

  public static class Builder {

    private final OperatorState state;

    private FunctionType functionType;

    private Map<String, Class<?>> values = new TreeMap<>();

    private StateBackend stateBackend;

    private boolean disableMultiplexedState = false;

    private Builder(OperatorState state) {
      this.state = state;
    }

    public Builder forFunction(FunctionType functionType) {
      this.functionType = functionType;
      return this;
    }

    public Builder withPersistedValue(String name, Class<?> type) {
      values.put(name, type);
      return this;
    }

    public Builder withRocksDBStateBackend() {
      stateBackend = new RocksDBStateBackend((StateBackend) new MemoryStateBackend());
      return this;
    }

    public Builder withFsStateBackend() {
      stateBackend = new FsStateBackend("file:///tmp/ignore");
      return this;
    }

    public Builder disableMultiplexedState() {
      this.disableMultiplexedState = true;
      return this;
    }

    public StateTableSource build() {
      Preconditions.checkArgument(!values.isEmpty());
      Preconditions.checkNotNull(stateBackend);

      return new StateTableSource(
          values, state, stateBackend, functionType, disableMultiplexedState);
    }
  }
}
