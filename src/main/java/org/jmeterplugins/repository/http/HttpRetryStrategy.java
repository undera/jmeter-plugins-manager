package org.jmeterplugins.repository.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

public class HttpRetryStrategy implements ServiceUnavailableRetryStrategy {

    /**
     * Maximum number of allowed retries if the server responds with a HTTP code
     * in our retry code list. Default value is 1.
     */
    private final int maxRetries;

    /**
     * Retry interval between subsequent requests, in milliseconds. Default
     * value is 1 second.
     */
    private final long retryInterval;

    public HttpRetryStrategy(int maxRetries, int retryInterval) {
        super();
        if (maxRetries < 1) {
            throw new IllegalArgumentException("MaxRetries must be greater than 1");
        }
        if (retryInterval < 1) {
            throw new IllegalArgumentException("Retry interval must be greater than 1");
        }
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    public HttpRetryStrategy() {
        this(1, 1000);
    }

    public boolean retryRequest(final HttpResponse response, int executionCount, final HttpContext context) {
        return executionCount <= maxRetries &&
                !isSuccessStatusCode(response.getStatusLine().getStatusCode());
    }

    private boolean isSuccessStatusCode(int code) {
        return code >= 200 && code < 300;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
