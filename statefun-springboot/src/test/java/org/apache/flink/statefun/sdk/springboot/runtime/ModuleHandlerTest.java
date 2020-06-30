package org.apache.flink.statefun.sdk.springboot.runtime;

import com.google.protobuf.Any;
import org.apache.flink.statefun.flink.core.polyglot.generated.Address;
import org.apache.flink.statefun.flink.core.polyglot.generated.FromFunction;
import org.apache.flink.statefun.flink.core.polyglot.generated.ToFunction;
import org.apache.flink.statefun.sdk.springboot.GreetFunctions;
import org.apache.flink.statefun.sdk.springboot.GreetingGenerator;
import org.apache.flink.statefun.springboot.generated.Counter;
import org.apache.flink.statefun.springboot.generated.Greeting;
import org.apache.flink.statefun.springboot.generated.Invoke;
import org.junit.Assert;
import org.junit.Test;

public class ModuleHandlerTest {

  @Test
  public void testModuleHandler() {
    ToFunction request =
        ToFunction.newBuilder()
            .setInvocation(
                ToFunction.InvocationBatchRequest.newBuilder()
                    .setTarget(
                        Address.newBuilder()
                            .setNamespace("remote")
                            .setType("greeter")
                            .setId("id")
                            .build())
                    .addState(
                        ToFunction.PersistedValue.newBuilder()
                            .setStateName("counter")
                            .setStateValue(
                                Any.pack(Counter.newBuilder().setCount(1).build()).toByteString())
                            .build())
                    .addInvocations(
                        ToFunction.Invocation.newBuilder()
                            .setCaller(
                                Address.newBuilder()
                                    .setNamespace("test")
                                    .setType("caller")
                                    .setId("id")
                                    .build())
                            .setArgument(Any.pack(Invoke.newBuilder().build()))
                            .build())
                    .build())
            .build();

    ModuleHandler handler =
        StatefulFunctionAutoConfiguration.createModule(new GreetFunctions(new TestGenerator()));
    Assert.assertNotNull(handler);

    FromFunction response = handler.handle(request);

    FromFunction expected =
        FromFunction.newBuilder()
            .setInvocationResult(
                FromFunction.InvocationResponse.newBuilder()
                    .addStateMutations(
                        FromFunction.PersistedValueMutation.newBuilder()
                            .setStateName("counter")
                            .setStateValue(
                                Any.pack(Counter.newBuilder().setCount(2).build()).toByteString())
                            .setMutationType(
                                FromFunction.PersistedValueMutation.MutationType.MODIFY)
                            .build())
                    .addOutgoingMessages(
                        FromFunction.Invocation.newBuilder()
                            .setTarget(
                                Address.newBuilder()
                                    .setNamespace("test")
                                    .setType("caller")
                                    .setId("id")
                                    .build())
                            .setArgument(Any.pack(Greeting.newBuilder().setGreeting("2").build()))
                            .build())
                    .addDelayedInvocations(
                        FromFunction.DelayedInvocation.newBuilder()
                            .setTarget(
                                Address.newBuilder()
                                    .setNamespace("remote")
                                    .setType("greeter")
                                    .setId("id")
                                    .build())
                            .setDelayInMs(60000)
                            .setArgument(Any.pack(Greeting.newBuilder().setGreeting("2").build()))
                            .build())
                    .addOutgoingEgresses(
                        FromFunction.EgressMessage.newBuilder()
                            .setEgressNamespace("test")
                            .setEgressType("egress")
                            .setArgument(Any.pack(Greeting.newBuilder().setGreeting("2").build()))
                            .build())
                    .build())
            .build();

    Assert.assertEquals(expected, response);
  }

  static class TestGenerator implements GreetingGenerator {
    @Override
    public String text(String name, int seen) {
      return Integer.toString(seen);
    }
  }
}
