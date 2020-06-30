package org.apache.flink.statefun.sdk.springboot;

import java.time.Duration;
import org.apache.flink.statefun.flink.core.polyglot.generated.Egress;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunction;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunctionController;
import org.apache.flink.statefun.springboot.generated.Counter;
import org.apache.flink.statefun.springboot.generated.Greeting;
import org.apache.flink.statefun.springboot.generated.Invoke;
import org.apache.flink.statefun.springboot.generated.OtherMessage;
import org.springframework.beans.factory.annotation.Autowired;

@StatefulFunctionController(path = "/statefun")
public class GreetFunctions {

  private final GreetingGenerator generator;

  @Autowired
  public GreetFunctions(GreetingGenerator generator) {
    this.generator = generator;
  }

  @StatefulFunction("remote/greeter")
  public void greet(Invoke message, Context ctx) {
    int count = ctx.get("counter", Counter.class).map(Counter::getCount).orElse(0);

    count += 1;

    Greeting greeting =
        Greeting.newBuilder().setGreeting(generator.text(ctx.self().getId(), count)).build();

    ctx.reply(greeting);
    ctx.sendAfter(ctx.self(), Duration.ofMinutes(1), greeting);
    ctx.send(Egress.newBuilder().setNamespace("test").setType("egress").build(), greeting);
    ctx.update("counter", Counter.newBuilder().setCount(count).build());
  }

  @StatefulFunction("remote/hi")
  public void hi(OtherMessage message, Context ctx) {
    ctx.reply(Greeting.newBuilder().setGreeting("hi").build());
  }
}
