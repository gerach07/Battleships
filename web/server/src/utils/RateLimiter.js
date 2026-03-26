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

module.exports = RateLimiter;
