package com.ververica.statefun.workshop.messages;

import com.ververica.statefun.workshop.functions.solutions.FraudCount;

/**
 * This message is sent to a {@link FraudCount} instance when a user confirms a fraudulent
 * transaction against their account.
 */
public final class ConfirmFraud {

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ConfirmFraud;
  }
}
