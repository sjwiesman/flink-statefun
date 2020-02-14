package com.ververica.statefun.workshop.messages;

import com.ververica.statefun.workshop.functions.solutions.FraudCount;

/**
 * This message is sent to a {@link FraudCount} instance when another entity wants to query the
 * current count for a particular account.
 */
public final class QueryFraud {

  @Override
  public boolean equals(Object obj) {
    return obj instanceof QueryFraud;
  }
}
