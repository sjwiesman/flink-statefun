package org.apache.flink.statefun.e2e.springboot;

import static org.apache.flink.statefun.e2e.remote.generated.RemoteModuleVerification.Invoke;
import static org.apache.flink.statefun.e2e.remote.generated.RemoteModuleVerification.InvokeCount;
import static org.apache.flink.statefun.e2e.remote.generated.RemoteModuleVerification.InvokeResult;
import static org.apache.flink.statefun.sdk.springboot.EgressUtil.kafkaRecord;

import org.apache.flink.statefun.flink.core.polyglot.generated.Address;
import org.apache.flink.statefun.flink.core.polyglot.generated.Egress;
import org.apache.flink.statefun.sdk.springboot.Context;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunction;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunctionController;
import org.springframework.beans.factory.annotation.Autowired;

@StatefulFunctionController(path = "/service")
public class RemoteFunctions {

  private final FunctionConfig.RandomKey randomKey;

  @Autowired
  public RemoteFunctions(FunctionConfig.RandomKey randomKey) {
    this.randomKey = randomKey;
  }

  @StatefulFunction("org.apache.flink.statefun.e2e.remote/counter")
  public void counter(Invoke invoke, Context ctx) {
    int count = ctx.get("invoke_count", InvokeCount.class).map(InvokeCount::getCount).orElse(0);

    count++;

    ctx.update("invoke_count", InvokeCount.newBuilder().setCount(count).build());

    InvokeResult result =
        InvokeResult.newBuilder().setId(ctx.self().getId()).setInvokeCount(count).build();

    ctx.send(
        Address.newBuilder()
            .setNamespace("org.apache.flink.statefun.e2e.remote")
            .setType("forward-function")
            .setId(randomKey.key())
            .build(),
        result);
  }

  @StatefulFunction("org.apache.flink.statefun.e2e.remote/forward-function")
  public void forwardToEgress(InvokeResult result, Context ctx) {
    ctx.send(Egress.newBuilder().build(), kafkaRecord("invoke-results", result.getId(), result));
  }
}
