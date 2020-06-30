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

import java.util.Map;
import org.apache.flink.statefun.flink.core.polyglot.generated.FromFunction;
import org.apache.flink.statefun.flink.core.polyglot.generated.ToFunction;

class ModuleHandler {

  private final String path;

  private final Map<String, FunctionHandler> handlersByType;

  private final BatchContext ctx;

  ModuleHandler(String path, Map<String, FunctionHandler> handlersByType) {
    this.path = path;
    this.handlersByType = handlersByType;
    this.ctx = new BatchContext();
  }

  String getPath() {
    return path;
  }

  FromFunction handle(ToFunction request) {
    ToFunction.InvocationBatchRequest invocation = request.getInvocation();

    String functionType = FunctionType.fromAddress(invocation.getTarget());
    FunctionHandler handler = handlersByType.get(functionType);

    ctx.setup(invocation.getTarget(), invocation.getStateList());

    for (ToFunction.Invocation payload : invocation.getInvocationsList()) {
      ctx.setCaller(payload.getCaller());
      handler.invoke(payload.getArgument(), ctx);
    }

    FromFunction.InvocationResponse response = ctx.getResponse();
    return FromFunction.newBuilder().setInvocationResult(response).build();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("ModuleHandler[");

    String functions = String.join(", ", handlersByType.keySet());

    return builder.append(functions).append("]").toString();
  }
}
