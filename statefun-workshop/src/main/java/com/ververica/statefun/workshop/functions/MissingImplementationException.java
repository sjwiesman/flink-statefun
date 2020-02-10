package com.ververica.statefun.workshop.functions;

public class MissingImplementationException extends RuntimeException {
  public MissingImplementationException() {
    super("This method has not been implementd yet");
  }
}
