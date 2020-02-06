package org.apache.flink.statefun.examples.modelserving.util;

public class UnsupportedTypeException extends IllegalStateException {

  public UnsupportedTypeException(Object object) {
    super(String.format("Unsupported type %s", object.getClass().getName()));
  }
}
