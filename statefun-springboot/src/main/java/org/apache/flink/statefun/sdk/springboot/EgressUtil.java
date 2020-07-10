package org.apache.flink.statefun.sdk.springboot;

import static org.apache.flink.statefun.sdk.springboot.runtime.ProtoUtil.maybePack;

import com.google.protobuf.Message;
import org.apache.flink.statefun.flink.io.generated.KafkaProducerRecord;
import org.apache.flink.statefun.flink.io.generated.KinesisEgressRecord;

public class EgressUtil {
  private EgressUtil() {
    throw new UnsupportedOperationException();
  }

  public static <T extends Message> KafkaProducerRecord kafkaRecord(String topic, T message) {
    return KafkaProducerRecord.newBuilder()
        .setTopic(topic)
        .setValueBytes(maybePack(message).toByteString())
        .build();
  }

  public static <T extends Message> KafkaProducerRecord kafkaRecord(
      String topic, String key, T message) {
    return KafkaProducerRecord.newBuilder()
        .setTopic(topic)
        .setKey(key)
        .setValueBytes(maybePack(message).toByteString())
        .build();
  }

  public static <T extends Message> KinesisEgressRecord kinesisRecord(String stream, T message) {
    return KinesisEgressRecord.newBuilder()
        .setStream(stream)
        .setValueBytes(maybePack(message).toByteString())
        .build();
  }

  public static <T extends Message> KinesisEgressRecord kinesisRecord(
      String stream, String partitionKey, T message) {
    return KinesisEgressRecord.newBuilder()
        .setStream(stream)
        .setPartitionKey(partitionKey)
        .setValueBytes(maybePack(message).toByteString())
        .build();
  }

  public static <T extends Message> KinesisEgressRecord kinesisRecord(
      String stream, String partitionKey, String explicitHash, T message) {
    return KinesisEgressRecord.newBuilder()
        .setStream(stream)
        .setPartitionKey(partitionKey)
        .setExplicitHashKey(explicitHash)
        .setValueBytes(maybePack(message).toByteString())
        .build();
  }
}
