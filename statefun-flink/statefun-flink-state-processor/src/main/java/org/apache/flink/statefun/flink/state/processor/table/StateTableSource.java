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
import java.util.Comparator;
import java.util.List;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.state.MapState;
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
import org.apache.flink.statefun.flink.common.SetContextClassLoader;
import org.apache.flink.statefun.flink.core.StatefulFunctionsJobConstants;
import org.apache.flink.statefun.flink.core.state.PersistedValues;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.StatefulFunctionProvider;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.sources.InputFormatTableSource;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

/**
 * A {@link org.apache.flink.table.sources.TableSource} for reading stateful functions into a Flink
 * table job. Each {@link org.apache.flink.statefun.sdk.StatefulFunction} is treated as a single
 * table within the universe of Flink state and persisted values are the columns. Each table
 * contains a special column <b>function_id</b> which is the {@link Address#id()} of the current
 * function instance.
 */
public class StateTableSource extends InputFormatTableSource<Row> {

  private static final String FUNCTION_ID_COLUMN_NAME = "function_id";

  private final List<PersistedValue<Object>> values;

  private final OperatorState state;

  private final StateBackend stateBackend;

  private final FunctionType functionType;

  private final boolean disableMultiplexedState;

  private StateTableSource(
      List<PersistedValue<Object>> values,
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
    TableSchema.Builder schema = TableSchema.builder().field(FUNCTION_ID_COLUMN_NAME, Types.STRING);
    for (PersistedValue<Object> entry : values) {
      schema = schema.field(entry.name(), TypeInformation.of(entry.type()));
    }

    return schema.primaryKey(FUNCTION_ID_COLUMN_NAME).build();
  }

  /**
   * Entry point for creating a new {@link StateTableSource}.
   *
   * <p>This method loads the savepoint metadata for underlying savepoint and so the proper
   * credentials and fileystems must be available on the classpath.
   *
   * @param savepointPath The path to a savepoint for a Stateful Function Flink application.
   * @return A {@code Builder} for configuring the source.
   * @throws IOException If the savepoint does not exist or is unreachable.
   */
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

    private List<PersistedValue<Object>> values;

    private StateBackend stateBackend;

    private boolean disableMultiplexedState = false;

    private Builder(OperatorState state) {
      this.state = state;
    }

    /**
     * @param functionType The {@link FunctionType} for the stateful function that will be read.
     * @param provider the provider to bind.
     * @return A {@code Builder} for configuring the source.
     */
    public Builder forFunction(FunctionType functionType, StatefulFunctionProvider provider) {
      Preconditions.checkNotNull(functionType, "The function type cannot be null");
      Preconditions.checkNotNull(provider, "The stateful function provider must not be null");

      this.functionType = functionType;

      try (SetContextClassLoader ignored = new SetContextClassLoader(provider)) {
        StatefulFunction function = provider.functionOfType(functionType);
        this.values = PersistedValues.findReflectively(function);
      }
      return this;
    }

    /**
     * Set's the state backend to {@link RocksDBStateBackend}.
     *
     * <p>This affects the format of the generated savepoint, and should therefore be the same as
     * what is configured by the Stateful Functions application that savepoint being analyzed.
     *
     * @return A {@code Builder} for configuring the source.
     */
    public Builder withRocksDBStateBackend() {
      stateBackend = new RocksDBStateBackend((StateBackend) new MemoryStateBackend());
      return this;
    }
    /**
     * Set's the state backend to {@link FsStateBackend}.
     *
     * <p>This affects the format of the generated savepoint, and should therefore be the same as
     * what is configured by the Stateful Functions application that savepoint being analyzed.
     *
     * @return A {@code Builder} for configuring the source.
     */
    public Builder withFsStateBackend() {
      stateBackend = new FsStateBackend("file:///tmp/ignore");
      return this;
    }

    /**
     * Disables multiplexing different function types and persisted state vales as a single Flink
     * {@link MapState}. By default, multiplexing is enabled.
     *
     * <p>This affects the format of the generated savepoint, and should therefore be the same as
     * what is configured by the Stateful Functions application that savepoint being analyzed.
     *
     * @see StatefulFunctionsJobConstants#MULTIPLEX_FLINK_STATE
     */
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
