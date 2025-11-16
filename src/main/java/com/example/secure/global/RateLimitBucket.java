package com.example.secure.global;

import java.time.Instant;

class RateLimitBucket {
    final int capacity;
    Instant lastRefillTime;
    int tokens;

    RateLimitBucket(int capacity, int initialTokens) {
        this.capacity = capacity;
        this.tokens = initialTokens;
        this.lastRefillTime = Instant.now();
    }
}
