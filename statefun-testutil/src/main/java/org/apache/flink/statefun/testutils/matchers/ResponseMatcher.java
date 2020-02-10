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

import java.util.Objects;
import org.apache.flink.statefun.testutils.function.Response;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** A matcher for checking the value of a {@link Response}. */
public class ResponseMatcher extends TypeSafeMatcher<Response> {

  private final String id;

  private final Matcher<Object> messageMatcher;

  ResponseMatcher(String id, Matcher<Object> messageMatcher) {
    this.id = Objects.requireNonNull(id);
    this.messageMatcher = Objects.requireNonNull(messageMatcher);
  }

  @Override
  protected boolean matchesSafely(Response item) {
    return id.equals(item.getId()) && messageMatcher.matches(item.getMessage());
  }

  @Override
  public void describeTo(Description description) {
    description
        .appendText("response containing [")
        .appendText(id)
        .appendText("->")
        .appendDescriptionOf(messageMatcher)
        .appendText("]");
  }
}
