package com.groupf.dating.config;

import com.groupf.dating.exception.ClaudeApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Retry configuration for handling retries when calling external services, especially the Claude API.
 */
@Slf4j
@Configuration
@EnableRetry
public class RetryConfig {

  /**
   * Claude API retry template with specific retry logic: - maxAttempts: 3
   * - retryableExceptions: ClaudeApiException (only if isRetryable() returns true
   * - backOffPolicy: Exponential backoff starting at 1 second, doubling each time, max 10 seconds
   * -retryListener: Logs each retry attempt and the exception message
   */
  @Bean
  public RetryTemplate claudeApiRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    // Retry logic
    Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
    retryableExceptions.put(ClaudeApiException.class, true);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions) {
      @Override
      public boolean canRetry(RetryContext context) {
        Throwable lastThrowable = context.getLastThrowable();
        if (lastThrowable == null) {
          return true; // allow first attempt
        }
        if (lastThrowable instanceof ClaudeApiException claudeEx) {
          return claudeEx.isRetryable() && super.canRetry(context);
        }
        return false;
      }
    };
    retryTemplate.setRetryPolicy(retryPolicy);

    // Configure exponential backoff policy
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(1000);      // Initial delay 1 second
    backOffPolicy.setMultiplier(2.0);            // Multiplier 2
    backOffPolicy.setMaxInterval(10000);         // Max delay 10 seconds
    retryTemplate.setBackOffPolicy(backOffPolicy);

    // Add retry listener
    retryTemplate.registerListener(new RetryListener() {
      @Override
      public <T, E extends Throwable> boolean open(
          RetryContext context,
          RetryCallback<T, E> callback) {
        // Called before first retry attempt
        return true;
      }

      @Override
      public <T, E extends Throwable> void onError(
          RetryContext context,
          RetryCallback<T, E> callback,
          Throwable throwable) {
        log.warn("Claude API call failed (attempt {}/{}): {}",
            context.getRetryCount() + 1,
            3,
            throwable.getMessage());
      }

      @Override
      public <T, E extends Throwable> void close(
          RetryContext context,
          RetryCallback<T, E> callback,
          Throwable throwable) {
        // Called when retry block exits
      }
    });

    return retryTemplate;
  }

  /**
   * Default retry template (for other external services)
   * - Max attempts: 2
   * - Initial delay: 500 milliseconds
   */
  @Bean
  public RetryTemplate defaultRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(2);
    retryTemplate.setRetryPolicy(retryPolicy);

    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(500);
    backOffPolicy.setMultiplier(1.5);
    backOffPolicy.setMaxInterval(5000);
    retryTemplate.setBackOffPolicy(backOffPolicy);

    return retryTemplate;
  }
}
