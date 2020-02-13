package com.ververica.statefun.workshop.messages;

/**
 * This message is sent to a {@link com.ververica.statefun.workshop.functions.FraudCount} instance
 * when another entity wants to query the current count for a particular account.
 */
public final class QueryFraud {

  @Override
  public boolean equals(Object obj) {
    return obj instanceof QueryFraud;
  }
}
