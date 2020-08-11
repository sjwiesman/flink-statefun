/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.statefun.flink.core.classloader;

import org.apache.flink.api.common.functions.Function;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.streaming.api.operators.AbstractUdfStreamOperator;

/**
 * An unimplemented stream operator used to appease Flink's API. This operator should always be
 * overridden with a concrete instance.
 */
final class UnimplementedStreamOperator<OUT, F extends Function>
    extends AbstractUdfStreamOperator<OUT, F> {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  public UnimplementedStreamOperator() {
    super((F) new UnimplementedFunction());
  }

  @Override
  public void open() throws Exception {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public void close() throws Exception {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public void dispose() throws Exception {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public MetricGroup getMetricGroup() {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public OperatorID getOperatorID() {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) throws Exception {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public void setCurrentKey(Object key) {
    throw new RuntimeException("This method should never be called");
  }

  @Override
  public Object getCurrentKey() {
    throw new RuntimeException("This method should never be called");
  }

  private static class UnimplementedFunction implements Function {
    private static final long serialVersionUID = 1L;
  }
}
