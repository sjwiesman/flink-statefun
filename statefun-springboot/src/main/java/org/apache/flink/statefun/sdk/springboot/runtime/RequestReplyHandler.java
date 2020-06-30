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

import org.apache.flink.statefun.flink.core.polyglot.generated.FromFunction;
import org.apache.flink.statefun.flink.core.polyglot.generated.ToFunction;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

public class RequestReplyHandler implements HandlerFunction<ServerResponse> {

  @NonNull private final ModuleHandler moduleHandler;

  public RequestReplyHandler(@NonNull ModuleHandler moduleHandler) {
    this.moduleHandler = moduleHandler;
  }

  @Override
  public ServerResponse handle(ServerRequest request) throws Exception {

    ToFunction toFunction = ToFunction.parseFrom(request.body(byte[].class));
    FromFunction response = moduleHandler.handle(toFunction);

    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(response.toByteArray());
  }

  @Override
  public String toString() {
    return "RequestReplyHandler{" + "moduleHandler=" + moduleHandler + '}';
  }
}
