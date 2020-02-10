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
package org.apache.flink.statefun.testutils.matchers;

import java.util.List;
import java.util.Map;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.testutils.function.Response;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** A matcher for checking all the responses sent to a particlar function type. */
public class FullResponseMatcher extends TypeSafeMatcher<Map<FunctionType, List<Response>>> {

  private final FunctionType type;

  private final List<Matcher<?>> messageMatchers;

  FullResponseMatcher(FunctionType type, List<Matcher<?>> messageMatchers) {
    this.type = type;
    this.messageMatchers = messageMatchers;
  }

  @Override
  protected boolean matchesSafely(Map<FunctionType, List<Response>> item) {
    List<Response> responses = item.get(type);
    if (responses == null) {
      return false;
    }

    if (responses.size() != messageMatchers.size()) {
      return false;
    }

    for (int i = 0; i < responses.size(); i++) {
      if (!messageMatchers.get(i).matches(responses.get(i))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void describeTo(Description description) {
    description
        .appendText("Messaged " + type.toString() + " -> ")
        .appendValueList("[", ",", "]", messageMatchers);
  }
}
