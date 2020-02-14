package com.ververica.statefun.workshop.functions.exercises;

import com.ververica.statefun.workshop.messages.ConfirmFraud;
import com.ververica.statefun.workshop.messages.ExpireFraud;
import com.ververica.statefun.workshop.messages.QueryFraud;
import com.ververica.statefun.workshop.messages.ReportedFraud;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;

/**
 * This function tracks the total number of reported fraudulent transactions made against an account
 * on a rolling 30 day period. It supports three three message types:
 *
 * <p>1) {@link ConfirmFraud}: When a customer reports a fraudulent transaction the function will
 * receive this message. It will increment it's internal count and set a 30 day expiration timer.
 *
 * <p>2) {@link ExpireFraud}: After 30 days, the function will receive an expiration message. At
 * this time, it will decrement its internal count.
 *
 * <p>3) {@link QueryFraud}: Any other entity in the system may query the current count by sending
 * this message. The function will reply with the current count wrapped in a {@link ReportedFraud}
 * message.
 */
public class FraudCount implements StatefulFunction {

  @Override
  public void invoke(Context context, Object input) {}
}
