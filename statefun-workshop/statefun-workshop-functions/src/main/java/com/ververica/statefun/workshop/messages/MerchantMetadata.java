package com.ververica.statefun.workshop.messages;

import com.ververica.statefun.workshop.functions.solutions.MerchantFunction;
import org.apache.flink.statefun.sdk.Address;

/**
 * The metadata stored along with each async request in the {@link MerchantFunction}.
 *
 * <p>It tracks the {@link Address} where the final result should be sent along with the number of
 * remaining attempts in case of failure.
 */
public final class MerchantMetadata {

  private final Address address;

  private final int remainingAttempts;

  public MerchantMetadata(Address address, int remainingAttempts) {
    this.address = address;
    this.remainingAttempts = remainingAttempts;
  }

  public Address getAddress() {
    return address;
  }

  public int getRemainingAttempts() {
    return remainingAttempts;
  }
}
