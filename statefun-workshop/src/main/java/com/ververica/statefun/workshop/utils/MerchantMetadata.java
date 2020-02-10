package com.ververica.statefun.workshop.utils;

import org.apache.flink.statefun.sdk.Address;

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
