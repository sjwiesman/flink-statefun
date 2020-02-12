package com.ververica.statefun.workshop.messages;

/**
 * This message is sent to a {@link com.ververica.statefun.workshop.functions.FraudCount} instance
 * when a user confirms a fraudulent transaction against their account.
 */
public final class ConfirmFraud {

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ConfirmFraud;
  }
}
