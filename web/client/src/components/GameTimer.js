import React, { useState, useEffect, useRef, memo } from 'react';
import { useI18n } from '../i18n/I18nContext';

const fmt = (secs) => {
    const s = Math.max(0, Math.ceil(secs ?? 0));
    return `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`;
};

const GameTimer = memo(({ playerTimeLeft, turnStartedAt, currentTurn, myId, opponentName }) => {
    const { t } = useI18n();
    const [, setTick] = useState(0);
    const lastDisplayRef = useRef('');

    useEffect(() => {
        if (!currentTurn || !turnStartedAt) return;
        // Don't tick when tab is hidden — saves CPU and prevents stale renders
        let id = null;
        const tick = () => {
            // Compute what the display would show; only trigger re-render if it changed
            const ids = Object.keys(playerTimeLeft);
            const display = ids.map(pid => {
                const isActive = pid === currentTurn;
                const stored = playerTimeLeft[pid];
                const live = isActive && turnStartedAt
                    ? Math.max(0, stored - (Date.now() - turnStartedAt) / 1000)
                    : stored;
                return fmt(live);
            }).join('|');
            if (display !== lastDisplayRef.current) {
                lastDisplayRef.current = display;
                setTick(prev => prev + 1);
            }
        };
        const start = () => { if (!id) id = setInterval(tick, 500); };
        const stop = () => { if (id) { clearInterval(id); id = null; } };
        const onVisibility = () => { document.hidden ? stop() : start(); };
        if (!document.hidden) start();
        document.addEventListener('visibilitychange', onVisibility);
        return () => { stop(); document.removeEventListener('visibilitychange', onVisibility); };
    }, [currentTurn, turnStartedAt, playerTimeLeft]);

    if (!playerTimeLeft || Object.keys(playerTimeLeft).length === 0) return null;

    const ids = Object.keys(playerTimeLeft);
    const ordered = myId && ids.includes(myId)
        ? [myId, ids.find(id => id !== myId)].filter(Boolean)
        : ids;

    return (
        <div className="flex gap-2">
            {ordered.map(pid => {
                const isActive = pid === currentTurn;
                const stored = playerTimeLeft[pid];
                const live = isActive && turnStartedAt
                    ? Math.max(0, stored - (Date.now() - turnStartedAt) / 1000)
                    : stored;
                const isCritical = isActive && live <= 10;
                const isLow = isActive && live <= 30;
                const label = pid === myId ? t('timer.you') : (opponentName || t('timer.opponent'));

                return (
                    <div key={pid} className={`flex-1 flex flex-col items-center py-1.5 px-3 rounded-xl border font-mono font-bold transition-all
                        ${isActive
                            ? isCritical
                                ? 'bg-red-900/60 border-red-500/60 text-red-300 animate-pulse'
                                : isLow
                                    ? 'bg-orange-900/40 border-orange-500/40 text-orange-300'
                                    : 'bg-green-900/30 border-green-500/40 text-green-300'
                            : 'bg-slate-800/50 border-slate-700 text-slate-500'}`}>
                        <span className="text-[0.65rem] uppercase tracking-wider font-semibold mb-0.5 opacity-80">{label}</span>
                        <span className="text-xl leading-none">{fmt(live)}</span>
                        {isActive && <span className="text-[0.6rem] mt-0.5 opacity-60">{t('timer.ticking')}</span>}
                    </div>
                );
            })}
        </div>
    );
});

GameTimer.displayName = 'GameTimer';
export default GameTimer;

