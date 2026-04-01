/**
 * Background Music Manager
 * Plays phase-specific OGG background music files (same files used in the Android app).
 * Each phase gets its own track; calling `playPhaseMusic(phase)` cross-fades
 * from the current track to the new one.
 *
 * Policy: Music is PAUSED when the tab is hidden and resumed when visible.
 *         SFX (sounds.js) are unaffected — they are short one-shots.
 */

let _audio = null;        // current HTMLAudioElement
let _currentPhase = null;  // phase name of what's playing
let _enabled = false;     // disabled by default
let _volume = 0.35;
let _paused = false;       // true when paused due to tab hidden
let _fadeInterval = null;  // track active fade to prevent leaks
let _fadingAudio = null;   // audio element currently being faded out
let _currentTrackName = null;  // display name of the playing track
let _onTrackChange = null;     // optional callback(name|null)

/** Map phase → display name */
const TRACK_NAMES = {
    menu:      'The Price of Freedom',
    waiting:   'The Price of Freedom',
    placement: 'Beyond New Horizons',
    battle:    'Honor and Sword',
    victory:   'Victory',
    defeat:    'Waves Crash',
};

/** Register a callback invoked whenever the track name changes */
export function onTrackChange(cb) {
    _onTrackChange = cb;
}

/** Returns the current display name, or null if nothing is playing */
export function getCurrentTrackName() {
    return _currentTrackName;
}

function setTrackName(name) {
    _currentTrackName = name;
    _onTrackChange?.(name);
}

/** Map phase → { src, loop } — mirrors Android res/raw */
const TRACKS = {
    menu:      { src: '/assets/audio/bgm_menu.ogg',      loop: true },
    waiting:   { src: '/assets/audio/bgm_menu.ogg',      loop: true },   // same calm track as menu
    placement: { src: '/assets/audio/bgm_placement.ogg', loop: true },
    battle:    { src: '/assets/audio/bgm_battle.ogg',    loop: true },
    victory:   { src: '/assets/audio/bgm_victory.ogg',   loop: false },
    defeat:    { src: '/assets/audio/bgm_defeat.ogg',    loop: false },
};

/** Safely dispose of an audio element */
function disposeAudio(audio) {
    if (!audio) return;
    try { audio.pause(); } catch { /* */ }
    // Remove from DOM if attached
    try { audio.parentNode?.removeChild(audio); } catch { /* */ }
    // Release media resource
    audio.src = '';
}

/** Fade out then destroy an audio element */
function fadeOut(audio, durationMs = 400) {
    if (!audio) return;
    // If there's a previous fade in progress, kill the old audio element immediately
    if (_fadeInterval) {
        clearInterval(_fadeInterval);
        _fadeInterval = null;
        if (_fadingAudio && _fadingAudio !== audio) {
            disposeAudio(_fadingAudio);
        }
    }
    _fadingAudio = audio;
    const steps = 20;
    const stepMs = durationMs / steps;
    const startVol = audio.volume;
    let step = 0;
    _fadeInterval = setInterval(() => {
        step++;
        try { audio.volume = Math.max(0, startVol * (1 - step / steps)); } catch { /* audio may be disposed */ }
        if (step >= steps) {
            clearInterval(_fadeInterval);
            _fadeInterval = null;
            _fadingAudio = null;
            disposeAudio(audio);
        }
    }, stepMs);
    // Safety: force cleanup if interval somehow survives past expected duration
    setTimeout(() => {
        if (_fadeInterval && _fadingAudio === audio) {
            clearInterval(_fadeInterval);
            _fadeInterval = null;
            _fadingAudio = null;
            disposeAudio(audio);
        }
    }, durationMs + 200);
}

/** Stop whatever is currently playing */
function stopMusic(fade = true) {
    if (_audio) {
        if (fade) {
            fadeOut(_audio);
        } else {
            disposeAudio(_audio);
        }
        _audio = null;
    }
    setTrackName(null);
}

/**
 * Play the music for a given game phase.
 * Noop if the same phase is already playing or music is disabled.
 */
export function playPhaseMusic(phase) {
    if (!_enabled) {
        // Remember desired phase so re-enabling can start it
        _currentPhase = phase;
        stopMusic(false);
        return;
    }

    // Don't restart the same track
    if (_currentPhase === phase && _audio && !_audio.paused && !_audio.ended) {
        return;
    }

    stopMusic(true);
    _currentPhase = phase;
    setTrackName(TRACK_NAMES[phase] || null);

    const track = TRACKS[phase];
    if (!track) {
        return;
    }

    try {
        const audio = document.createElement('audio');
        audio.src = track.src;
        audio.loop = track.loop;
        audio.volume = _volume;
        
        // Muted autoplay: browsers allow muted audio to autoplay without user interaction
        audio.muted = true;
        audio.autoplay = true; // Use autoplay attribute instead of calling play()
        audio.preload = 'auto';
        
        // Add to DOM (required by some browsers for autoplay)
        audio.style.display = 'none';
        document.body.appendChild(audio);
        
        audio.addEventListener('playing', () => {
            // Unmute after playback starts
            setTimeout(() => {
                audio.muted = false;
            }, 100);
        });
        
        audio.addEventListener('error', () => {
            console.warn('[Music] Failed to load audio:', track.src);
            setTrackName(null);
        });
        
        // Load the audio - autoplay will handle starting it
        audio.load();
        
        _audio = audio;

        // If tab is currently hidden, immediately pause the new track
        if (_paused) {
            try { audio.pause(); } catch { /* */ }
        }
    } catch (err) {
        console.error('[Music] Exception creating audio:', err);
    }
}

/** Stop music entirely (e.g. when leaving the game) */
export function stopAllMusic() {
    _currentPhase = null;
    // Cancel any in-progress fade before stopping
    if (_fadeInterval) {
        clearInterval(_fadeInterval);
        _fadeInterval = null;
        if (_fadingAudio) {
            disposeAudio(_fadingAudio);
            _fadingAudio = null;
        }
    }
    stopMusic(false);
}

/**
 * Enable / disable background music.
 * On re-enable, restarts the music for the current phase.
 */
export function setMusicEnabled(enabled) {

    _enabled = enabled;
    if (!enabled) {
        // Keep _currentPhase so we know what to restart on re-enable
        stopMusic(false);
    } else {
        // Re-enable: restart the current phase's music
        if (_currentPhase) {
            const phase = _currentPhase;
            _currentPhase = null; // clear so playPhaseMusic doesn't noop
            playPhaseMusic(phase);
        }
    }
}

/** Pause music (e.g. tab hidden) — pauses the <audio>, does NOT stop */
export function pauseMusic() {
    _paused = true;
    if (_audio) {
        try { _audio.pause(); } catch { /* */ }
    }
}

/** Resume music (e.g. tab visible) */
export function resumeMusic() {
    _paused = false;
    if (_enabled && _audio) {
        try { _audio.play().catch(() => {}); } catch { /* */ }
    }
}
