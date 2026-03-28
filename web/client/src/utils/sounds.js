const AudioCtx = window.AudioContext || window.webkitAudioContext;
let ctx = null;
let _soundEnabled = false;
/** Track pending multi-tone timeouts so they can be cleared on dispose */
const _pendingTimeouts = new Set();

/** Called from App.js whenever the sound toggle changes */
export function setSoundEnabled(enabled) {
    _soundEnabled = !!enabled;
}

/** Close the AudioContext to free system audio resources */
export function disposeSounds() {
    // Cancel all pending multi-tone timeouts to prevent leaked AudioContexts
    for (const tid of _pendingTimeouts) clearTimeout(tid);
    _pendingTimeouts.clear();
    if (ctx) { ctx.close().catch(() => {}); ctx = null; }
}

function getCtx() {
    if (!ctx) {
        try { ctx = new AudioCtx(); } catch { return null; }
    }
    if (ctx.state === 'suspended') ctx.resume();
    return ctx;
}

function tone(freq, duration, type = 'sine', vol = 0.3) {
    try {
        const c = getCtx();
        if (!c) return;
        const osc = c.createOscillator();
        const gain = c.createGain();
        osc.type = type;
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(vol, c.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, c.currentTime + duration);
        osc.connect(gain);
        gain.connect(c.destination);
        osc.start();
        osc.stop(c.currentTime + duration);
        // Clean up AudioNodes to prevent memory leaks
        osc.onended = () => { osc.disconnect(); gain.disconnect(); };
    } catch { /* audio not available */ }
}

/** Schedule a timeout that is auto-tracked for cleanup */
function trackedTimeout(fn, delay) {
    const tid = setTimeout(() => {
        _pendingTimeouts.delete(tid);
        fn();
    }, delay);
    _pendingTimeouts.add(tid);
}

export function playSound(type) {
    // Don't play SFX when sound is disabled or the tab is hidden
    if (!_soundEnabled) return;
    if (document.hidden) return;

    switch (type) {
        case 'hit':
            tone(800, 0.15, 'square', 0.2);
            trackedTimeout(() => tone(600, 0.1, 'square', 0.15), 80);
            break;
        case 'miss':
            tone(300, 0.25, 'sine', 0.12);
            break;
        case 'sunk':
            tone(200, 0.3, 'sawtooth', 0.2);
            trackedTimeout(() => tone(150, 0.4, 'sawtooth', 0.25), 150);
            trackedTimeout(() => tone(100, 0.5, 'sawtooth', 0.2), 350);
            break;
        case 'victory':
            [523, 659, 784, 1047].forEach((f, i) =>
                trackedTimeout(() => tone(f, 0.3, 'sine', 0.25), i * 150)
            );
            break;
        case 'defeat':
            [400, 350, 300, 250].forEach((f, i) =>
                trackedTimeout(() => tone(f, 0.35, 'sine', 0.2), i * 200)
            );
            break;
        case 'turn':
            tone(880, 0.1, 'sine', 0.15);
            trackedTimeout(() => tone(1100, 0.15, 'sine', 0.15), 100);
            break;
        case 'chat':
            tone(1200, 0.05, 'sine', 0.08);
            break;
        case 'place':
            tone(500, 0.08, 'sine', 0.1);
            break;
        default:
            break;
    }
}
