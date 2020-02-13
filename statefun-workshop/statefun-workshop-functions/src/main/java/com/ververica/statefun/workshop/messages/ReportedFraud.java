package com.ververica.statefun.workshop.messages;

import java.util.Objects;

/**
 * This message is sent from a {@link com.ververica.statefun.workshop.functions.FraudCount} instance
 * when reporting the current count.
 */
public final class ReportedFraud {

  private final int count;

  public ReportedFraud(int count) {
    this.count = count;
  }

  public int getCount() {
    return count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReportedFraud that = (ReportedFraud) o;
    return count == that.count;
  }

  @Override
  public int hashCode() {
    return Objects.hash(count);
  }

  @Override
  public String toString() {
    return "ReportedFraud{" + "count=" + count + '}';
  }
}
