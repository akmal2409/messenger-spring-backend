package com.akmal.messengerspringbackend.shared;

import com.akmal.messengerspringbackend.config.ProjectConfigurationProperties;
import com.akmal.messengerspringbackend.snowflake.SnowflakeGenerator;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The class manages all snowflake/timestamp conversions to time buckets and vice versa.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 02/06/2022 - 17:32
 * @project messenger-spring-backend
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "project.bucketing")
@RequiredArgsConstructor
public class BucketingManager {

  private final SnowflakeGenerator snowflakeGenerator;
  private final ProjectConfigurationProperties projectProps;
  /** Specifies in milliseconds how big the bucket should be */
  @Setter private long bucketSize;

  /**
   * Creates the bucket based on the current timestamp. The calculation is following:
   * timestamp/bucketSize, where both variables are in milliseconds.
   *
   * @return bucket number.
   */
  public int makeBucket() {
    long msSinceEpoch =
        Instant.now().minusMillis(this.projectProps.getCustomEpochMilli()).toEpochMilli();

    return (int) (msSinceEpoch / bucketSize);
  }

  /**
   * Computes the bucket based on the snowflake (which contains timestamp). Firstly, the timestamp
   * is extracted from the snowflake (41 bits) which is already adjusted to the project specified
   * epoch. Thereafter, we apply the following calculation: bucket = timestamp / bucketSize where
   * both timestamp and bucket size are ms from an epoch.
   *
   * @param snowflake - 64bit id that contains timestamp.
   * @return bucket number.
   */
  public int makeBucket(long snowflake) {
    long timestamp = this.snowflakeGenerator.toTimestampMilli(snowflake);
    return (int) (timestamp / bucketSize);
  }

  public int makeBucketForTimestamp(long timestamp) {
    return (int) (timestamp / bucketSize);
  }

  public long adjustTimestampToCustomEpoch(long timestamp) {
    return timestamp - this.projectProps.getCustomEpochMilli();
  }

  /**
   * Produces a list of buckets between given two snowflakes. For example, suppose we want to get
   * all buckets between the messageId (snowflake) and threadId (which is again snowflake and
   * contains creation time of the thread). Then the function will generate all buckets including
   * the start and end.
   *
   * @param startSnowflake - 64 bit snowflake that contains a timestamp
   * @param endSnowflake - 64 bit snowflake that contains a timestamp
   * @return return list of buckets between two given ranges
   */
  public List<Integer> makeBuckets(long startSnowflake, long endSnowflake) {
    return IntStream.range(this.makeBucket(startSnowflake), this.makeBucket(endSnowflake) + 1)
        .boxed()
        .toList();
  }

  public List<Integer> makeBucketsFromTimestampTillBucket(long timestamp, int bucket) {
    return IntStream.range(this.makeBucketForTimestamp(timestamp), bucket + 1).boxed().toList();
  }
}
