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

import java.util.regex.Pattern;
import org.apache.flink.statefun.flink.core.polyglot.generated.Address;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunction;

class FunctionType {

  private static final Pattern PATTERN =
      Pattern.compile("(?<namespace>[a-zA-Z0-9]+)/(?<type>[a-zA-Z0-9]+)");

  static String fromAnnotation(StatefulFunction annotation) {
    if (!PATTERN.matcher(annotation.value()).matches()) {
      throw new RuntimeException(
          "Invalid function type ["
              + annotation.value()
              + "]. FunctionTypes must be in the form <namespace>/<type>");
    }

    return annotation.value();
  }

  static String fromAddress(Address address) {
    return String.format("%s/%s", address.getNamespace(), address.getType());
  }
}
