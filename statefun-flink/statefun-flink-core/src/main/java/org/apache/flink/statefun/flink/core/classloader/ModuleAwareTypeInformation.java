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
package org.apache.flink.statefun.flink.core.classloader;

import java.io.IOException;
import java.util.Objects;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.statefun.flink.core.StatefulFunctionsConfig;
import org.apache.flink.util.SerializedValue;

/** TypeInformation for classes whose implementations are only available on the module classpath. */
public class ModuleAwareTypeInformation<T> extends TypeInformation<T> {

  private static final long serialVersionUID = 1L;

  public static <T> TypeInformation<T> of(
      StatefulFunctionsConfig config, TypeInformation<T> inner) {
    try {
      SerializedValue<TypeInformation<T>> serialized = new SerializedValue<>(inner);
      String name = "ModuleAwareTypeInformation(" + inner.toString() + ")";
      return new ModuleAwareTypeInformation<>(config, name, serialized);
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize inner type information");
    }
  }

  private final StatefulFunctionsConfig config;

  private final String name;

  private final SerializedValue<TypeInformation<T>> serializedTypeInfo;

  private ModuleAwareTypeInformation(
      StatefulFunctionsConfig config,
      String name,
      SerializedValue<TypeInformation<T>> serializedTypeInfo) {
    this.config = config;
    this.name = name;
    this.serializedTypeInfo = serializedTypeInfo;
  }

  @Override
  public TypeSerializer<T> createSerializer(ExecutionConfig config) {
    try {
      ClassLoader moduleClassLoader =
          ModuleClassLoader.createModuleClassLoader(
              this.config, Thread.currentThread().getContextClassLoader());
      return serializedTypeInfo.deserializeValue(moduleClassLoader).createSerializer(config);
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Failed to deserialize inner type information");
    }
  }

  @Override
  public boolean isBasicType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isTupleType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getArity() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getTotalFields() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<T> getTypeClass() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isKeyType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModuleAwareTypeInformation<?> that = (ModuleAwareTypeInformation<?>) o;
    return serializedTypeInfo.equals(that.serializedTypeInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serializedTypeInfo);
  }

  @Override
  public boolean canEqual(Object obj) {
    throw new UnsupportedOperationException();
  }
}
