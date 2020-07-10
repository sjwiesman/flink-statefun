package org.apache.flink.statefun.sdk.springboot.runtime;

import com.google.protobuf.Any;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.apache.flink.statefun.sdk.springboot.Context;
import org.apache.flink.statefun.sdk.springboot.annotations.StatefulFunction;
import org.junit.Test;

public class FunctionHandlerTest {

  static final StatefulFunction ANNOTATION = new StatefulFunction() {

    @Override
    public Class<? extends Annotation> annotationType() {
      return StatefulFunction.class;
    }

    public String value() {
      return "remote/func";
    }
  };

  @Test(expected = IllegalStateException.class)
  public void testProperArgumentLength() throws Exception {
    Method method = this.getClass().getMethod("func1");
    FunctionHandler.create(this, ANNOTATION, method);
  }

  @Test(expected = IllegalStateException.class)
  public void testMessageTypeValidation() throws Exception {
    Method method = this.getClass().getMethod("func2", String.class, Context.class);
    FunctionHandler.create(this, ANNOTATION, method);
  }

  @Test(expected = IllegalStateException.class)
  public void testContextTypeValidation() throws Exception {
    Method method = this.getClass().getMethod("func3", Any.class, String.class);
    FunctionHandler.create(this, ANNOTATION, method);
  }

  public void testStatefulSignatureValidation() throws Exception {
    Method method = this.getClass().getMethod("func4", Any.class, Context.class);
    FunctionHandler.create(this, ANNOTATION, method);
  }

  public void func1() {}

  public void func2(String message, Context ctx) {}

  public void func3(Any message, String ctx) {}

  public void func4(Any message, Context ctx) {}
}
