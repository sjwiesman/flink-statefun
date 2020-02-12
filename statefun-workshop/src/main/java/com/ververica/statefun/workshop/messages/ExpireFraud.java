package com.ververica.statefun.workshop.messages;

/**
 * This message is sent to a {@link com.ververica.statefun.workshop.functions.FraudCount} instance
 * when 30 days have elapsed and its internal count should be decremented by 1.
 */
public final class ExpireFraud {

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ExpireFraud;
  }
}
