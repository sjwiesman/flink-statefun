package org.apache.flink.statefun.verifier.util;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.flink.statefun.flink.core.httpfn.HttpRequestReplyClient;
import org.apache.flink.statefun.flink.core.httpfn.OkHttpUtils;
import org.apache.flink.statefun.flink.core.metrics.RemoteInvocationMetrics;
import org.apache.flink.statefun.flink.core.polyglot.generated.FromFunction;
import org.apache.flink.statefun.flink.core.polyglot.generated.ToFunction;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.flink.core.reqreply.RequestReplyClient;
import org.apache.flink.statefun.flink.core.reqreply.ToFunctionRequestSummary;
import org.junit.rules.ExternalResource;

import java.util.concurrent.TimeUnit;

public class SdkClient extends ExternalResource {

  private RequestReplyClient client;

  private OkHttpClient httpClient;

  private volatile boolean shutdown = false;

  @Override
  protected void before() {
    httpClient = OkHttpUtils.newClient();
    client = new HttpRequestReplyClient(
            HttpUrl.get("http://localhost:8000"),
            httpClient,
            () -> shutdown);
  }

  @Override
  protected void after() {
    shutdown = true;
    OkHttpUtils.closeSilently(httpClient);
  }

  public FromFunction call(ToFunction toFunction, Address target) throws Exception {
    ToFunctionRequestSummary requestSummary =
            new ToFunctionRequestSummary(
                    target,
                    toFunction.getSerializedSize(),
                    toFunction.getInvocation().getStateCount(),
                    toFunction.getInvocation().getInvocationsCount());
    return client.call(requestSummary, new MockMetrics(), toFunction).get(1, TimeUnit.MINUTES);
  }

  private static class MockMetrics implements RemoteInvocationMetrics {

    @Override
    public void remoteInvocationFailures() {

    }

    @Override
    public void remoteInvocationLatency(long elapsed) {

    }
  }
}
