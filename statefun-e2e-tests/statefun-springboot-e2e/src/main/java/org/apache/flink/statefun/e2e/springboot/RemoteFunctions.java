package org.apache.flink.statefun.e2e.springboot;

import static org.apache.flink.statefun.e2e.remote.generated.RemoteModuleVerification.Invoke;
import static org.apache.flink.statefun.e2e.remote.generated.RemoteModuleVerification.InvokeCount;
import static org.apache.flink.statefun.e2e.remote.generated.RemoteModuleVerification.InvokeResult;

import java.util.Random;
import org.apache.flink.statefun.flink.core.polyglot.generated.Address;
import org.apache.flink.statefun.flink.core.polyglot.generated.Egress;
import org.apache.flink.statefun.sdk.springboot.Context;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunction;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunctionController;

@StatefulFunctionController(path = "/service")
public class RemoteFunctions {

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
            .setId(randomKey())
            .build(),
        result);
  }

  @StatefulFunction("org.apache.flink.statefun.e2e.remote/forward-function")
  public void forwardToEgress(InvokeResult result, Context ctx) {
    ctx.sendToKafka(Egress.newBuilder().build(), "invoke-results", result.getId(), result);
  }

  private String randomKey() {
    return Integer.toHexString(new Random().nextInt(0xFFFF));
  }
}
