package org.apache.flink.statefun.sdk.springboot.runtime;

import com.google.protobuf.Any;
import org.apache.flink.statefun.springboot.generated.Counter;
import org.junit.Assert;
import org.junit.Test;

public class ProtoUtilTest {

  @Test
  public void testPackAny() {
    Any message = Any.pack(Counter.getDefaultInstance());
    Any packed = ProtoUtil.maybePack(message);

    Assert.assertSame(message, packed);
  }

  @Test
  public void testUnPackAny() throws Exception {
    Any message = Any.pack(Counter.getDefaultInstance());
    Counter unpacked = ProtoUtil.maybeUnpack(message, Counter.class);

    Assert.assertEquals(Counter.getDefaultInstance(), unpacked);
  }
}
