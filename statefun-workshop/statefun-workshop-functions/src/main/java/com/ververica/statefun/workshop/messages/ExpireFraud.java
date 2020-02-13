package com.ververica.statefun.workshop.messages;

import com.ververica.statefun.workshop.functions.solutions.FraudCount;

/**
 * This message is sent to a {@link FraudCount} instance
 * when 30 days have elapsed and its internal count should be decremented by 1.
 */
public final class ExpireFraud {

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ExpireFraud;
  }
}
