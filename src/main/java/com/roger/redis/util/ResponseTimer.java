package com.roger.redis.util;

/**
 * Lightweight utility for measuring server-side response times with sub-millisecond precision.
 *
 * <p>Usage: call {@link #start()} at the beginning of request processing,
 * then {@link #elapsedMs()} to get the elapsed time as a formatted string
 * suitable for the {@code X-Response-Time-Ms} header.</p>
 */
public final class ResponseTimer {

    private final long startNanos;

    private ResponseTimer() {
        this.startNanos = System.nanoTime();
    }

    public static ResponseTimer start() {
        return new ResponseTimer();
    }

    /**
     * Returns elapsed time in milliseconds with sub-ms precision.
     */
    public double elapsedMs() {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    /**
     * Returns elapsed time as a string for use in response headers.
     */
    public String elapsedMsString() {
        return String.valueOf(elapsedMs());
    }
}
