package cn.hsine.mem0.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token bucket rate limiting service.
 *
 * @author MoBai

 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final long defaultCapacity = 100;
    private final long defaultRefillRate = 100;
    private final long refillPeriodMs = 60000; // 1 minute

    /**
     * Checks if a request is allowed for the given key.
     *
     * @param key the rate limit key (user ID or API key)
     * @return true if the request is allowed
     */
    public boolean allowRequest(String key) {
        return allowRequest(key, defaultCapacity, defaultRefillRate);
    }

    /**
     * Checks if a request is allowed for the given key with custom limits.
     *
     * @param key the rate limit key
     * @param capacity the bucket capacity
     * @param refillRate the refill rate per period
     * @return true if the request is allowed
     */
    public boolean allowRequest(String key, long capacity, long refillRate) {
        TokenBucket bucket = buckets.computeIfAbsent(key,
            k -> new TokenBucket(capacity, refillRate, refillPeriodMs));

        boolean allowed = bucket.tryConsume();
        if (!allowed) {
            log.debug("Rate limit exceeded for key: {}", key);
        }
        return allowed;
    }

    /**
     * Gets the remaining tokens for a key.
     *
     * @param key the rate limit key
     * @return the remaining tokens
     */
    public long getRemainingTokens(String key) {
        TokenBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getRemainingTokens() : defaultCapacity;
    }

    /**
     * Resets the rate limit for a key.
     *
     * @param key the rate limit key
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * Cleans up expired buckets.
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry ->
            now - entry.getValue().lastRefillTime > refillPeriodMs * 2
        );
    }

    private static class TokenBucket {
        private final long capacity;
        private final long refillRate;
        private final long refillPeriodMs;
        private final AtomicLong tokens;
        private volatile long lastRefillTime;

        TokenBucket(long capacity, long refillRate, long refillPeriodMs) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.refillPeriodMs = refillPeriodMs;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            long current = tokens.get();
            if (current > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            if (elapsed >= refillPeriodMs) {
                long periods = elapsed / refillPeriodMs;
                long tokensToAdd = periods * refillRate;
                long current = tokens.get();
                long newTokens = Math.min(current + tokensToAdd, capacity);
                tokens.set(newTokens);
                lastRefillTime = now;
            }
        }

        long getRemainingTokens() {
            refill();
            return tokens.get();
        }
    }
}
