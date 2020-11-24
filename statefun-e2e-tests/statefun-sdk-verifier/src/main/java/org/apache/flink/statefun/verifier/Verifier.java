package org.apache.flink.statefun.verifier;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class Verifier {

  public static void main(String[] args) {
    JUnitCore core = new JUnitCore();
    core.addListener(new TextListener(System.out));
    Result results = core.run(SdkTests.class);
  }
}
