package com.sap.sse.gwt.client.async;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.Duration;

/**
 * A result returned by a {@link RetryableAsyncAction} that can be empty and may require re-trying. The service that
 * returned this result is then expected to provide instructions for retrying, in particular how long to wait until the
 * next retry. Use in conjunction with {@link RetryableAsyncAction}, such that the client specifies an
 * {@link AsyncCallback}{@code <T>} to the {@link AsyncActionsExecutor#execute(AsyncAction, AsyncCallback)} or
 * {@link AsyncActionsExecutor#execute(AsyncAction, String, AsyncCallback)} method, whereas the actual RPC method to be
 * invoked by the action has a result type of {@link RetryableActionResult}{@code <T>}.
 * <p>
 * 
 * Timeout behavior should be controlled by the client, e.g., the {@link RetryableAsyncAction} itself that provides the
 * {@link AsyncActionsExecutor} with timeout information.
 * 
 * @author Axel Uhl (d043530)
 *
 * @param <T>
 *            the actual result type; in other words, the type of the "payload"
 */
public class RetryableActionResult<T> implements Serializable {
    private static final long serialVersionUID = -927895877316370853L;
    private boolean needsRetry;
    private T result;
    private Duration durationUntilNextRetry;
    
    @Deprecated
    RetryableActionResult() { // for GWT serialization only
    }
    
    public static <T> RetryableActionResult<T> withResult(T result) {
        return new RetryableActionResult<>(/* needs retry */ false, result, /* duration until next retry */ null);
    }
    
    public static <T> RetryableActionResult<T> retry(Duration durationUntilNextRetry) {
        return new RetryableActionResult<>(/* needs retry */ true, /* result */ null, durationUntilNextRetry);
    }
    
    private RetryableActionResult(boolean needsRetry, T result, Duration durationUntilNextRetry) {
        super();
        this.needsRetry = needsRetry;
        this.result = result;
        this.durationUntilNextRetry = durationUntilNextRetry;
    }

    public boolean needsRetry() {
        return needsRetry;
    }
    
    /**
     * Precondition: {@code !}{@link #needsRetry()}
     * 
     * @return the actual result of the service invocation; undefined if {@link #needsRetry()} returns {@code true}
     */
    public T get() {
        return result;
    }
    
    /**
     * Precondition: {@link #needsRetry()}{@code == true}
     * 
     * @return the duration that {@link AsyncActionsExecutor} shall wait before it tries to re-try the request
     */
    public Duration getDurationUntilNextRetry() {
        return durationUntilNextRetry;
    }
}
