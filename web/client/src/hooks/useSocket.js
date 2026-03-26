import { useState, useEffect, useRef } from 'react';
import io from 'socket.io-client';
import { SOCKET_URL, LOCAL_SERVER_URL, PUBLIC_SERVER_URL } from '../constants';

// How long (ms) to wait for localhost before falling back to production
const LOCAL_TIMEOUT_MS = 3000;
// Max consecutive reconnection failures on local before falling back to public
const MAX_LOCAL_RECONNECT_FAILURES = 5;

const useSocket = () => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverUrl, setServerUrl] = useState(SOCKET_URL || LOCAL_SERVER_URL);
    const socketRef = useRef(null);

    useEffect(() => {
        let cancelled = false;

        // If a URL was explicitly set at build time, use it directly — no fallback logic
        if (SOCKET_URL) {
            const s = io(SOCKET_URL, {
                reconnection: true,
                reconnectionDelay: 1000,
                reconnectionDelayMax: 5000,
                reconnectionAttempts: Infinity,
            });
            s.on('connect', () => !cancelled && setIsConnected(true));
            s.on('disconnect', () => !cancelled && setIsConnected(false));
            socketRef.current = s;
            setSocket(s);
            return () => { cancelled = true; s.disconnect(); };
        }

        // --- Local-first, then public fallback ---
        let fallbackTimer = null;
        let localConnected = false;
        let reconnectFailures = 0;

        // Attempt 1: local server (short timeout, no auto-reconnect yet)
        const local = io(LOCAL_SERVER_URL, {
            reconnection: false,
            timeout: LOCAL_TIMEOUT_MS,
        });

        const switchToPublic = () => {
            if (cancelled) return;
            local.disconnect();
            clearTimeout(fallbackTimer);
            if (!cancelled) setServerUrl(PUBLIC_SERVER_URL);

            const pub = io(PUBLIC_SERVER_URL, {
                reconnection: true,
                reconnectionDelay: 1000,
                reconnectionDelayMax: 5000,
                reconnectionAttempts: Infinity,
            });
            pub.on('connect', () => !cancelled && setIsConnected(true));
            pub.on('disconnect', () => !cancelled && setIsConnected(false));
            socketRef.current = pub;
            if (!cancelled) setSocket(pub);
        };

        local.on('connect', () => {
            if (cancelled) { local.disconnect(); return; }
            clearTimeout(fallbackTimer);
            localConnected = true;
            reconnectFailures = 0;
            if (!cancelled) setServerUrl(LOCAL_SERVER_URL);

            // Re-enable reconnection now that we know local is alive
            local.io.reconnection(true);
            local.io.reconnectionDelay(1000);
            local.io.reconnectionDelayMax(5000);
            local.io.reconnectionAttempts(MAX_LOCAL_RECONNECT_FAILURES);

            setIsConnected(true);
            socketRef.current = local;
            setSocket(local);
        });

        local.on('disconnect', () => {
            if (!cancelled) setIsConnected(false);
        });

        // Fall back if local doesn't connect within timeout
        local.on('connect_error', () => {
            if (!localConnected && !cancelled) {
                switchToPublic();
            } else if (localConnected && !cancelled) {
                // Local was connected but now failing to reconnect
                reconnectFailures++;
                if (reconnectFailures >= MAX_LOCAL_RECONNECT_FAILURES) {
                    console.warn('[useSocket] Local server unreachable after reconnect attempts, switching to public');
                    switchToPublic();
                }
            }
        });

        // Belt-and-suspenders timer fallback
        fallbackTimer = setTimeout(() => {
            if (!localConnected && !cancelled) switchToPublic();
        }, LOCAL_TIMEOUT_MS + 500);

        return () => {
            cancelled = true;
            clearTimeout(fallbackTimer);
            local.disconnect();
            if (socketRef.current && socketRef.current !== local) {
                socketRef.current.disconnect();
            }
        };
    }, []);

    return { socket, isConnected, serverUrl };
};

export default useSocket;
