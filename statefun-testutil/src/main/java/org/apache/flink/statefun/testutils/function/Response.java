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
package org.apache.flink.statefun.testutils.function;

/** A wrapper class that tags the response from a function with the id the intended recipient. */
public class Response {

  private final String id;

  private final Object message;

  Response(String id, Object message) {
    this.id = id;
    this.message = message;
  }

  public String getId() {
    return id;
  }

  public Object getMessage() {
    return message;
  }

  @SuppressWarnings("unchecked")
  public <T> Object getMessage(Class<T> type) {
    try {
      return message;
    } catch (ClassCastException e) {
      throw new IllegalStateException(String.format("Message not of type %s", type.getName()));
    }
  }

  @Override
  public String toString() {
    return "Response{" + "id='" + id + '\'' + ", message=" + message + '}';
  }
}
