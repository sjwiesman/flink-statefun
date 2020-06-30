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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunction;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunctionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Component
public class StatefulFunctionAutoConfiguration implements ApplicationContextAware {

  private static final Logger LOG =
      LoggerFactory.getLogger(StatefulFunctionAutoConfiguration.class);

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Bean
  @SuppressWarnings("unused")
  RouterFunction<ServerResponse> routers() {
    Map<String, Object> modules =
        applicationContext.getBeansWithAnnotation(StatefulFunctionController.class);
    if (modules.size() == 0) {
      LOG.info("No StatefulFunction controllers found");
      return null;
    }

    return modules.values().stream()
        .map(StatefulFunctionAutoConfiguration::createModule)
        .filter(Objects::nonNull)
        .map(StatefulFunctionAutoConfiguration::createRouter)
        .reduce(RouterFunction::and)
        .orElse(null);
  }

  private static RouterFunction<ServerResponse> createRouter(ModuleHandler module) {
    RequestPredicate predicate =
        RequestPredicates.POST(module.getPath())
            .and(RequestPredicates.accept(MediaType.APPLICATION_OCTET_STREAM));

    return RouterFunctions.route(predicate, new RequestReplyHandler(module));
  }

  static ModuleHandler createModule(Object module) {
    StatefulFunctionController controller =
        module.getClass().getAnnotation(StatefulFunctionController.class);

    Map<String, FunctionHandler> functionRegistry =
        definedMethods(module.getClass())
            .filter(method -> method.getAnnotation(StatefulFunction.class) != null)
            .map(method -> FunctionHandler.create(module, method))
            .collect(Collectors.toMap(FunctionHandler::getFunctionType, func -> func));

    if (functionRegistry.isEmpty()) {
      LOG.warn(
          "Controller {} is annotated @StatefulFunctionController but contains no methods annotated @StatefulFunction",
          module.getClass());
      return null;
    }

    return new ModuleHandler(controller.path(), functionRegistry);
  }

  private static Stream<Method> definedMethods(Class<?> javaClass) {
    if (javaClass == null || javaClass == Object.class) {
      return Stream.empty();
    }

    Stream<Method> selfMethods = Arrays.stream(javaClass.getDeclaredMethods());
    Stream<Method> superMethods = definedMethods(javaClass.getSuperclass());

    return Stream.concat(selfMethods, superMethods);
  }
}
