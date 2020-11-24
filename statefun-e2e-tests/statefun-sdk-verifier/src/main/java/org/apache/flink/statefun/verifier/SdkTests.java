package org.apache.flink.statefun.verifier;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.apache.flink.statefun.flink.core.polyglot.generated.FromFunction;
import org.apache.flink.statefun.flink.core.polyglot.generated.ToFunction;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.verifier.util.SdkClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class SdkTests {

    @ClassRule
    public static final SdkClient client = new SdkClient();

    public static final Address TARGET = new Address(new FunctionType("verification", "test"), "id");

    @Test
    public void testInitialStateRegistration() throws Exception {
        ToFunction toFunction = ToFunction.newBuilder().setInvocation(
                ToFunction.InvocationBatchRequest.newBuilder()
                        .addInvocations(ToFunction.Invocation
                                .newBuilder()
                                .setArgument(Any.newBuilder().build())))
                .build();

        FromFunction fromFunction = client.call(toFunction, TARGET);

        Assert.assertTrue(
                "Function invocation should return an incomplete invocation response when no states are provided",
                fromFunction.hasIncompleteInvocationContext());

        MatcherAssert.assertThat(
                "Function invocation should return 3 state specifications",
                fromFunction.getIncompleteInvocationContext().getMissingValuesList(),
                Matchers.containsInAnyOrder(
                        FromFunction.PersistedValueSpec.newBuilder()
                                .setStateName("A")
                                .setExpirationSpec(FromFunction.ExpirationSpec.newBuilder()
                                        .setMode(FromFunction.ExpirationSpec.ExpireMode.NONE)),
                        FromFunction.PersistedValueSpec.newBuilder()
                                .setStateName("B")
                                .setExpirationSpec(FromFunction.ExpirationSpec.newBuilder()
                                        .setMode(FromFunction.ExpirationSpec.ExpireMode.AFTER_WRITE)
                                        .setExpireAfterMillis(60000)),
                        FromFunction.PersistedValueSpec.newBuilder()
                                .setStateName("C")
                                .setExpirationSpec(FromFunction.ExpirationSpec.newBuilder()
                                        .setMode(FromFunction.ExpirationSpec.ExpireMode.AFTER_INVOKE)
                                        .setExpireAfterMillis(60000))
                                .build()
                ));
    }

    @Test
    public void testUpdatedStateRegistration() throws Exception {
        ToFunction toFunction = ToFunction.newBuilder().setInvocation(
                ToFunction.InvocationBatchRequest.newBuilder()
                        .addState(ToFunction.PersistedValue.newBuilder()
                                .setStateName("B")
                                .setStateValue(ByteString.copyFromUtf8("state-b")))
                        .addState(ToFunction.PersistedValue.newBuilder()
                                .setStateName("C")
                                .setStateValue(ByteString.copyFromUtf8("state-c")))
                        .addInvocations(ToFunction.Invocation
                                .newBuilder()
                                .setArgument(Any.newBuilder().build())))
                .build();

        FromFunction fromFunction = client.call(toFunction, TARGET);

        Assert.assertTrue(
                "Function invocation should return an incomplete invocation response a state is missing",
                fromFunction.hasIncompleteInvocationContext());

        MatcherAssert.assertThat(
                "Function invocation should return 1 missing state specifications",
                fromFunction.getIncompleteInvocationContext().getMissingValuesList(),
                Matchers.containsInAnyOrder(
                        FromFunction.PersistedValueSpec.newBuilder()
                                .setStateName("A")
                                .setExpirationSpec(FromFunction.ExpirationSpec.newBuilder()
                                        .setMode(FromFunction.ExpirationSpec.ExpireMode.NONE))));
    }

    @Test
    public void testFunctionInvocation() throws Exception {
        Any message = Any.newBuilder()
                .setTypeUrl("message")
                .build();

        ToFunction toFunction = ToFunction.newBuilder().setInvocation(
                ToFunction.InvocationBatchRequest.newBuilder()
                        .addState(ToFunction.PersistedValue.newBuilder()
                            .setStateName("A")
                            .setStateValue(ByteString.copyFromUtf8("state-a")))
                        .addState(ToFunction.PersistedValue.newBuilder()
                                .setStateName("B")
                                .setStateValue(ByteString.copyFromUtf8("state-b")))
                        .addState(ToFunction.PersistedValue.newBuilder()
                                .setStateName("C")
                                .setStateValue(ByteString.copyFromUtf8("state-c")))
                        .addInvocations(ToFunction.Invocation
                                .newBuilder()
                                .setArgument(message)))
                .build();

        FromFunction fromFunction = client.call(toFunction, TARGET);

        Assert.assertTrue(
                "Function invocation should return a successful invocation response",
                fromFunction.hasInvocationResult());

        FromFunction.InvocationResponse response = fromFunction.getInvocationResult();
        MatcherAssert.assertThat(
                "Function invocation should send the input message to verification/target id",
                response.getOutgoingMessagesList(),
                Matchers.contains(FromFunction.Invocation.newBuilder()
                    .setArgument(message)
                    .setTarget(org.apache.flink.statefun.flink.core.polyglot.generated.Address.newBuilder()
                            .setNamespace("verification")
                            .setType("target")
                            .setId("id"))));

        MatcherAssert.assertThat(
                "Function invocation should send the input message to verificationtarget id with a 1 ms delay",
                response.getDelayedInvocationsList(),
                Matchers.contains(FromFunction.DelayedInvocation.newBuilder()
                        .setArgument(message)
                        .setDelayInMs(1)
                        .setTarget(org.apache.flink.statefun.flink.core.polyglot.generated.Address.newBuilder()
                                .setNamespace("verification")
                                .setType("target")
                                .setId("id"))));

        MatcherAssert.assertThat(
                "Function invocation should send the input message to verification/egress",
                response.getDelayedInvocationsList(),
                Matchers.contains(FromFunction.EgressMessage.newBuilder()
                        .setArgument(message)
                        .setEgressNamespace("verification")
                        .setEgressType("egress")));

        MatcherAssert.assertThat(
                "Function invocation should set state B to the input message and delete state C",
                response.getStateMutationsList(),
                Matchers.containsInAnyOrder(
                        FromFunction.PersistedValueMutation.newBuilder()
                                .setStateName("B")
                                .setMutationType(FromFunction.PersistedValueMutation.MutationType.MODIFY)
                                .setStateValue(message.toByteString())
                                .build(),
                        FromFunction.PersistedValueMutation.newBuilder()
                                .setStateName("C")
                                .setMutationType(FromFunction.PersistedValueMutation.MutationType.DELETE)
                                .build()));
    }
}
