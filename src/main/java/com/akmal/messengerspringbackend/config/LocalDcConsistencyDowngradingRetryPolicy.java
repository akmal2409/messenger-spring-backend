package com.akmal.messengerspringbackend.config;

import com.akmal.messengerspringbackend.config.condition.DseCustomDriverOption;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.api.core.connection.ClosedConnectionException;
import com.datastax.oss.driver.api.core.connection.HeartbeatException;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.retry.RetryDecision;
import com.datastax.oss.driver.api.core.retry.RetryPolicy;
import com.datastax.oss.driver.api.core.retry.RetryVerdict;
import com.datastax.oss.driver.api.core.servererrors.CoordinatorException;
import com.datastax.oss.driver.api.core.servererrors.ReadFailureException;
import com.datastax.oss.driver.api.core.servererrors.WriteFailureException;
import com.datastax.oss.driver.api.core.servererrors.WriteType;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.internal.core.retry.ConsistencyDowngradingRetryVerdict;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 04/06/2022 - 11:49
 * @project messenger-spring-backend
 * @since 1.0
 */
public class LocalDcConsistencyDowngradingRetryPolicy implements RetryPolicy {
  private static final Logger log = LoggerFactory.getLogger(LocalDcConsistencyDowngradingRetryPolicy.class);
  private static final ConsistencyLevel DOWNGRADED_CL = ConsistencyLevel.LOCAL_ONE;

  private static final int DEFAULT_MAX_READ_ATTEMPTS = 2;
  private static final int DEFAULT_MAX_WRITE_ATTEMPTS = 3;

  private final int maxReadAttempts;
  private final int maxWriteAttempts;

  private final String logPrefix;

  public LocalDcConsistencyDowngradingRetryPolicy(DriverContext context, String profileName) {
    DriverExecutionProfile executionProfile = context.getConfig().getDefaultProfile();

    if (StringUtils.hasText(profileName) && context.getConfig().getProfiles().containsKey(profileName)) {
      executionProfile = context.getConfig().getProfile(profileName);
    }

    this.maxReadAttempts = executionProfile.getInt(DseCustomDriverOption.MAX_READ_ATTEMPTS,
        DEFAULT_MAX_READ_ATTEMPTS);
    this.maxWriteAttempts = executionProfile.getInt(DseCustomDriverOption.MAX_WRITE_ATTEMPTS,
        DEFAULT_MAX_WRITE_ATTEMPTS);
    this.logPrefix = String.format("%s|%s", context.getSessionName(), profileName);

    if (log.isDebugEnabled()) {
      log.debug("[{}] Setting up custom retry policy with max-read-attempts={} "
                    + "max-write-attempts={}", this.logPrefix,
          this.maxReadAttempts, this.maxWriteAttempts);
    }
  }

  @Override
  public void close() {}

  @Override
  public RetryVerdict onReadTimeoutVerdict(
      @NonNull Request request,
      @NonNull ConsistencyLevel cl,
      int blockFor,
      int received,
      boolean dataPresent,
      int retryCount) {
    RetryVerdict retryVerdict;

    if (retryCount > this.maxReadAttempts) {
      retryVerdict = RetryVerdict.RETHROW;
    } else if (!dataPresent && received == 0) {
      retryVerdict = RetryVerdict.RETRY_SAME;
    } else if (received > 1 && dataPresent) {
      retryVerdict = RetryVerdict.IGNORE;
    } else {
      retryVerdict = new ConsistencyDowngradingRetryVerdict(DOWNGRADED_CL);
    }

    if (log.isTraceEnabled()) {
      log.trace("[{}] Verdict on read timeout (consistency: {}, required responses: {}, "
                    + "received responses: {}, data retrieved: {}, retries: {}): {}",
          this.logPrefix, cl, blockFor, received, dataPresent, retryCount, retryVerdict);
    }

    return retryVerdict;
  }

  @Override
  public RetryVerdict onWriteTimeoutVerdict(
      @NonNull Request request,
      @NonNull ConsistencyLevel cl,
      @NonNull WriteType writeType,
      int blockFor,
      int received,
      int retryCount) {
    RetryVerdict retryVerdict;

    if (WriteType.COUNTER == writeType || retryCount > this.maxWriteAttempts) {
      // counter increments and decrements are not idempotent
      retryVerdict = RetryVerdict.RETHROW;
    } else if (received == 0) {
      // all other operations are idempotent, we can retry.
      if (WriteType.BATCH_LOG == writeType) {
        retryVerdict = RetryVerdict.RETRY_SAME;
      } else {
        if (Boolean.TRUE.equals(request.isIdempotent())) {
          retryVerdict = new ConsistencyDowngradingRetryVerdict(DOWNGRADED_CL);
        } else {
          retryVerdict = RetryVerdict.RETHROW;
        }
      }
    } else {
      // means that at least 1 received the data so there is no reason in retrying with LOCAL_ONE
      retryVerdict = RetryVerdict.IGNORE;
    }

    if (log.isTraceEnabled()) {
      log.trace("[{}] Verdict on write timeout (consistency: {}, write type: {}, required acknowledgments: {}, "
                    + "received acknowledgments: {}, retries: {}): {}",
          this.logPrefix, cl, writeType, blockFor, received, retryCount, retryVerdict);
    }

    return retryVerdict;
  }

  @Override
  public RetryVerdict onUnavailableVerdict(
      @NonNull Request request,
      @NonNull ConsistencyLevel cl,
      int required,
      int alive,
      int retryCount) {
    int maxRetries = Math.max(this.maxReadAttempts, this.maxWriteAttempts);

    RetryVerdict retryVerdict;

    if (retryCount > maxRetries) {
      retryVerdict = RetryVerdict.RETHROW;
    } else if (alive == 0) {
      retryVerdict = RetryVerdict.RETRY_NEXT;
    } else {
      if (Boolean.TRUE.equals(request.isIdempotent())) {
        // downgrade to LOCAL_ONE because there is a node and we have not yet reached max retries.
        retryVerdict = new ConsistencyDowngradingRetryVerdict(DOWNGRADED_CL);
      } else {
        retryVerdict = RetryVerdict.RETHROW;
      }
    }

    if (log.isTraceEnabled()) {
      log.trace("[{}] Verdict on unavailable exception (consistency: {}, required replica: {}, alive replica: {}, retries: {}): {}",
          this.logPrefix, cl,
          required, alive, retryCount, retryVerdict);
    }

    return retryVerdict;
  }

  @Override
  public RetryVerdict onRequestAbortedVerdict(@NonNull Request request, @NonNull Throwable error, int retryCount) {
    RetryVerdict verdict = !(error instanceof ClosedConnectionException) && !(error instanceof HeartbeatException) ? RetryVerdict.RETHROW : RetryVerdict.RETRY_NEXT;
    if (log.isTraceEnabled()) {
      log.trace("[{}] Verdict on aborted request (type: {}, message: '{}', retries: {}): {}",
          this.logPrefix, error.getClass().getSimpleName(), error.getMessage(), retryCount, verdict);
    }

    return verdict;
  }

  @Override
  public RetryVerdict onErrorResponseVerdict(@NonNull Request request, @NonNull CoordinatorException error, int retryCount) {
    RetryVerdict verdict = !(error instanceof WriteFailureException) && !(error instanceof ReadFailureException) ? RetryVerdict.RETRY_NEXT : RetryVerdict.RETHROW;
    if (log.isTraceEnabled()) {
      log.trace("[{}] Verdict on node error (type: {}, message: '{}', retries: {}): {}",
          this.logPrefix, error.getClass().getSimpleName(), error.getMessage(), retryCount, verdict);
    }

    return verdict;
  }

  @Override
  @Deprecated
  public RetryDecision onReadTimeout(
      @NonNull Request request,
      @NonNull ConsistencyLevel consistencyLevel,
      int i,
      int i1,
      boolean b,
      int i2) {
    throw new UnsupportedOperationException("onReadTimeout() is deprecated");
  }

  @Override
  @Deprecated
  public RetryDecision onWriteTimeout(
      @NonNull Request request,
      @NonNull ConsistencyLevel consistencyLevel,
      @NonNull WriteType writeType,
      int i,
      int i1,
      int i2) {
    throw new UnsupportedOperationException("onWriteTimeout() is deprecated");
  }

  @Override
  @Deprecated
  public RetryDecision onUnavailable(
      @NonNull Request request, @NonNull ConsistencyLevel consistencyLevel, int i, int i1, int i2) {
    throw new UnsupportedOperationException("onUnavailable() is deprecated");
  }

  @Override
  @Deprecated
  public RetryDecision onRequestAborted(
      @NonNull Request request, @NonNull Throwable throwable, int i) {
    throw new UnsupportedOperationException("onRequestAborted() is deprecated");
  }

  @Override
  @Deprecated
  public RetryDecision onErrorResponse(
      @NonNull Request request, @NonNull CoordinatorException e, int i) {
    throw new UnsupportedOperationException("onErrorResponse() is deprecated");
  }
}
