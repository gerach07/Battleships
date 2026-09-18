/**
 * Sliding window rate limiter.
 * Tracks timestamps per player and compacts when the buffer fills.
 */
class RateLimiter {
    constructor(maxRequests = 10, windowMs = 1000) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.requests = new Map(); // playerId -> { timestamps: number[], count: number }
    }

    isAllowed(playerId) {
        const now = Date.now();
        let entry = this.requests.get(playerId);

        if (!entry) {
            entry = { timestamps: new Array(this.maxRequests).fill(0), count: 0 };
            this.requests.set(playerId, entry);
        }

        // Single-pass: count valid entries and compact in one step
        let writeIdx = 0;
        for (let i = 0; i < entry.count; i++) {
            if (now - entry.timestamps[i] < this.windowMs) {
                entry.timestamps[writeIdx++] = entry.timestamps[i];
            }
        }
        entry.count = writeIdx;

        if (entry.count >= this.maxRequests) return false;

        // Grow the array if needed
        if (entry.count >= entry.timestamps.length) {
            entry.timestamps.push(0);
        }
        entry.timestamps[entry.count] = now;
        entry.count++;

        return true;
    }

    removePlayer(playerId) {
        this.requests.delete(playerId);
    }

    cleanup() {
        const now = Date.now();
        for (const [playerId, entry] of this.requests) {
            // Compact in-place — same approach as isAllowed(), avoids allocating temp arrays
            let writeIdx = 0;
            for (let i = 0; i < entry.count; i++) {
                if (now - entry.timestamps[i] < this.windowMs) {
                    entry.timestamps[writeIdx++] = entry.timestamps[i];
                }
            }
            entry.count = writeIdx;
            if (writeIdx === 0) {
                this.requests.delete(playerId);
            }
        }
    }
}

/**
 * Sliding-window timestamp tracker for IP / key-based rate limiting.
 * Replaces the repeated inline "check size > 500, filter timestamps" pattern
 * used for IP connections, PIN attempts, and room-listing endpoints.
 */
class TimestampTracker {
    constructor(maxPerWindow, windowMs, maxKeys = 500) {
        this.maxPerWindow = maxPerWindow;
        this.windowMs = windowMs;
        this.maxKeys = maxKeys;
        this.entries = new Map();
    }

    /** Returns true if the key is under the rate limit (and records the attempt). */
    isAllowed(key) {
        const now = Date.now();
        this._compactIfNeeded(now);
        let entry = this.entries.get(key);
        if (!entry) { entry = { timestamps: [] }; this.entries.set(key, entry); }
        entry.timestamps = entry.timestamps.filter(t => now - t < this.windowMs);
        if (entry.timestamps.length >= this.maxPerWindow) return false;
        entry.timestamps.push(now);
        return true;
    }

    /** Prune expired entries from every key. Called on the periodic cleanup interval. */
    cleanup() {
        const now = Date.now();
        for (const [key, entry] of this.entries) {
            entry.timestamps = entry.timestamps.filter(t => now - t < this.windowMs);
            if (entry.timestamps.length === 0) this.entries.delete(key);
        }
    }

    /** When the tracker grows beyond maxKeys, do an eager full sweep. */
    _compactIfNeeded(now) {
        if (this.entries.size <= this.maxKeys) return;
        for (const [key, entry] of this.entries) {
            entry.timestamps = entry.timestamps.filter(t => now - t < this.windowMs);
            if (entry.timestamps.length === 0) this.entries.delete(key);
        }
    }
}

module.exports = RateLimiter;
module.exports.TimestampTracker = TimestampTracker;
