package com.ververica.statefun.workshop.messages;

import java.util.Objects;

public final class MerchantScore {
  public static MerchantScore score(int score) {
    return new MerchantScore(score, true);
  }

  public static MerchantScore error() {
    return new MerchantScore(-1, false);
  }

  private final int score;

  private final boolean success;

  private MerchantScore(int score, boolean success) {
    this.score = score;
    this.success = success;
  }

  public int getScore() {
    return score;
  }

  public boolean isSuccess() {
    return success;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MerchantScore that = (MerchantScore) o;
    return score == that.score && success == that.success;
  }

  @Override
  public int hashCode() {
    return Objects.hash(score, success);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("MerchantScore{");
    if (success) {
      builder.append("score=").append(score);
    } else {
      builder.append("error");
    }

    return builder.append("}").toString();
  }
}
