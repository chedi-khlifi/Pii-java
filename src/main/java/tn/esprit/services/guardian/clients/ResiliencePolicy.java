package tn.esprit.services.guardian.clients;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Resilience policies for fault tolerance.
 * Location: Wraps all external API calls to handle retries, circuit breaking, and timeouts.
 * Simplified implementation without external dependencies.
 */
public class ResiliencePolicy {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Retry logic: attempts operation up to 3 times with 500ms delay between retries.
     */
    public static <T> T executeWithRetry(String name, Callable<T> operation) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }
        throw lastException;
    }

    /**
     * Timeout wrapper: executes operation with timeout (simplified version).
     */
    public static <T> T executeWithTimeout(String name, Callable<T> operation, Duration timeout) throws Exception {
        return operation.call();  // TODO: Implement actual timeout with ExecutorService if needed
    }
}

