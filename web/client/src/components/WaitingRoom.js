import React, { useState, memo } from 'react';
import { useI18n } from '../i18n/I18nContext';

/** Fallback clipboard copy for non-HTTPS / older browsers */
function fallbackCopy(text) {
    try {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.cssText = 'position:fixed;opacity:0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        return true;
    } catch { return false; }
}

const WaitingRoom = memo(({ gameId, roomPassword, handleBackToMenu, timeLimit, isHost, opponentName, handleStartGame, handleKickPlayer }) => {
    const { t } = useI18n();
    const [copied, setCopied] = useState(null);

    const hasOpponent = Boolean(opponentName);

    const copyWith = (text, label) => {
        const onSuccess = () => { setCopied(label); setTimeout(() => setCopied(null), 2000); };
        if (navigator.clipboard?.writeText) {
            navigator.clipboard.writeText(text).then(onSuccess).catch(() => {
                fallbackCopy(text) && onSuccess();
            });
        } else {
            fallbackCopy(text) && onSuccess();
        }
    };

    return (
        <div className="max-w-md mx-auto pt-4 px-2 text-center space-y-6 animate-fade-in">
            {/* ── Room code focal card with corner brackets + scanline ── */}
            <div className="glass-card corner-brackets px-6 py-7 relative overflow-hidden border-yellow-400/50 shadow-2xl shadow-yellow-500/10 hover:shadow-yellow-500/15 transition-all duration-500">
                <div className="scanline-sweep" />
                <div className="absolute inset-0 bg-gradient-to-br from-yellow-500/8 via-transparent to-yellow-500/8 pointer-events-none" />
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_120%,rgba(250,204,21,0.1),transparent_50%)] pointer-events-none" />
                <div className="relative space-y-3">
                    <p className="text-[0.6rem] text-yellow-400/80 uppercase tracking-[0.35em] font-bold drop-shadow">{t('waiting.roomCode')}</p>
                    <p
                        className="text-5xl sm:text-6xl font-black font-mono text-yellow-300 tracking-[0.25em] drop-shadow-lg select-all"
                        style={{ textShadow: '0 0 32px rgba(253,224,71,0.4), 0 0 16px rgba(253,224,71,0.3)' }}
                    >
                        {gameId}
                    </p>
                    <div className="flex items-center justify-center gap-3 mt-3 flex-wrap">
                        {roomPassword && (
                            <span className="text-xs text-orange-300 bg-orange-500/15 rounded-full py-1.5 px-3.5 border border-orange-500/30 backdrop-blur-sm shadow-sm transition-transform hover:scale-105">
                                🔒 PIN: <span className="font-mono font-bold">{roomPassword}</span>
                            </span>
                        )}
                        {timeLimit && (
                            <span className="text-xs text-blue-300 bg-blue-500/15 rounded-full py-1.5 px-3.5 border border-blue-500/30 backdrop-blur-sm shadow-sm transition-transform hover:scale-105">
                                ⏱️ {Math.round(timeLimit / 60)} {t('waiting.minGame')}
                            </span>
                        )}
                    </div>
                </div>
            </div>

            {/* ── Player slots ── */}
            <div className="grid grid-cols-2 gap-4">
                {/* You */}
                <div className="glass-card slot-ready p-5 space-y-2.5 cursor-default relative">
                    {isHost && (
                        <div className="absolute top-2 right-2 bg-gradient-to-r from-yellow-500 to-amber-500 text-[0.5rem] font-black text-slate-900 px-2 py-0.5 rounded-full shadow-md">
                            👑 HOST
                        </div>
                    )}
                    <div className="w-12 h-12 mx-auto rounded-full bg-gradient-to-br from-emerald-400/20 to-emerald-600/20 flex items-center justify-center text-xl border border-emerald-500/40 shadow-lg shadow-emerald-500/20">
                        <span className="drop-shadow">✓</span>
                    </div>
                    <p className="text-sm font-bold text-emerald-300 truncate">{t('waiting.you')}</p>
                    <p className="text-[0.6rem] text-emerald-400/70 uppercase tracking-widest font-semibold">{t('waiting.ready')}</p>
                </div>
                {/* Opponent */}
                <div className={`glass-card p-5 space-y-2.5 cursor-default ${hasOpponent ? 'slot-ready' : 'slot-waiting'}`}>
                    <div className={`w-12 h-12 mx-auto rounded-full flex items-center justify-center text-xl border ${hasOpponent ? 'bg-gradient-to-br from-emerald-400/20 to-emerald-600/20 border-emerald-500/40 shadow-lg shadow-emerald-500/20' : 'bg-gradient-to-br from-blue-500/15 to-blue-600/15 border-blue-500/30'}`}>
                        {hasOpponent ? (
                            <span className="drop-shadow">✓</span>
                        ) : (
                            <span className="flex gap-1">
                                {[0, 150, 300].map(d => (
                                    <span key={d} className="w-1.5 h-1.5 bg-blue-400 rounded-full animate-bounce shadow-sm shadow-blue-400/50" style={{ animationDelay: `${d}ms`, animationDuration: '1s' }} />
                                ))}
                            </span>
                        )}
                    </div>
                    <p className={`text-sm font-bold truncate ${hasOpponent ? 'text-emerald-300' : 'text-slate-400'}`}>
                        {hasOpponent ? opponentName : t('waiting.waitingFor')}
                    </p>
                    <p className={`text-[0.6rem] uppercase tracking-widest font-semibold ${hasOpponent ? 'text-emerald-400/70' : 'text-slate-500'}`}>
                        {hasOpponent ? t('waiting.ready') : t('waiting.notJoined')}
                    </p>
                </div>
            </div>

            {/* ── Host Controls ── */}
            {isHost && hasOpponent && (
                <div className="glass-card p-4 space-y-3 border-yellow-500/30 shadow-xl shadow-yellow-900/20">
                    <div className="flex items-center gap-2 mb-2">
                        <span className="text-lg">👑</span>
                        <h3 className="text-sm font-bold text-yellow-300">{t('waiting.hostControls')}</h3>
                    </div>
                    <div className="flex gap-3">
                        <button
                            onClick={handleStartGame}
                            className="flex-1 py-3 bg-gradient-to-r from-emerald-500 to-green-600 hover:from-emerald-400 hover:to-green-500 text-white font-bold rounded-xl transition-all hover:scale-105 shadow-lg shadow-emerald-900/30 active:scale-95"
                        >
                            🎮 {t('waiting.startGame')}
                        </button>
                        <button
                            onClick={handleKickPlayer}
                            className="px-4 py-3 bg-red-600 hover:bg-red-500 text-white font-bold rounded-xl transition-all hover:scale-105 shadow-lg shadow-red-900/30 active:scale-95"
                            title={t('waiting.kickPlayer')}
                        >
                            ⛔
                        </button>
                    </div>
                    <p className="text-[0.6rem] text-slate-400 text-center leading-relaxed">
                        {t('waiting.hostHint')}
                    </p>
                </div>
            )}

            {/* ── Waiting for host (non-host player) ── */}
            {!isHost && hasOpponent && (
                <div className="glass-card p-4 border-blue-500/30 shadow-xl shadow-blue-900/20">
                    <div className="flex items-center justify-center gap-3">
                        <span className="text-lg animate-pulse">⏳</span>
                        <p className="text-sm font-semibold text-blue-300">{t('waiting.waitingForHost')}</p>
                    </div>
                    <p className="text-[0.6rem] text-slate-400 text-center mt-2 leading-relaxed">
                        {t('waiting.waitingForHostHint')}
                    </p>
                </div>
            )}

            {/* ── Share title ── */}
            {!hasOpponent && (
                <>
                    <div className="pt-2">
                        <h2 className="text-lg font-bold text-white">{t('waiting.title')}</h2>
                        <p className="text-slate-400 text-xs mt-1.5 leading-relaxed">{t('waiting.shareHint')}</p>
                    </div>

                    {/* ── Copy buttons ── */}
                    <div className="flex gap-3 justify-center flex-wrap px-2">
                        <button
                            onClick={() => copyWith(gameId, 'code')}
                            className={`group px-6 py-3 text-sm rounded-xl transition-all duration-300 font-semibold shadow-lg ${
                                copied === 'code' 
                                ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white shadow-green-900/40 scale-105' 
                                : 'glass-card !rounded-xl hover:border-slate-400/40 text-slate-200 hover:scale-105 hover:shadow-xl active:scale-95'
                            }`}
                        >
                            <span className="flex items-center gap-2">
                                <span className={copied === 'code' ? '' : 'group-hover:scale-110 transition-transform'}>
                                    {copied === 'code' ? '✓' : '📋'}
                                </span>
                                {copied === 'code' ? t('waiting.copied') : t('waiting.copyCode')}
                            </span>
                        </button>
                        <button
                            onClick={() => {
                                const path = roomPassword ? `${gameId}/${roomPassword}` : gameId;
                                const url = `${window.location.origin}/${path}`;
                                copyWith(url, 'link');
                            }}
                            className={`group px-6 py-3 text-sm rounded-xl transition-all duration-300 font-semibold shadow-lg ${
                                copied === 'link' 
                                ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white shadow-green-900/40 scale-105' 
                                : 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white hover:scale-105 hover:shadow-xl shadow-blue-900/30 active:scale-95'
                            }`}
                        >
                            <span className="flex items-center gap-2">
                                <span className={copied === 'link' ? '' : 'group-hover:scale-110 transition-transform'}>
                                    {copied === 'link' ? '✓' : '🔗'}
                                </span>
                                {copied === 'link' ? t('waiting.copied') : `${t('waiting.copyLink')}${roomPassword ? ` ${t('waiting.plusPin')}` : ''}`}
                            </span>
                        </button>
                    </div>
                </>
            )}

            {/* ── Leave button ── */}
            <button
                onClick={handleBackToMenu}
                className="mt-3 px-6 py-2.5 text-red-400/80 hover:text-red-300 hover:bg-red-500/10 text-sm font-semibold transition-all rounded-lg active:scale-95"
            >
                ← {t('waiting.leaveRoom')}
            </button>
        </div>
    );
});

WaitingRoom.displayName = 'WaitingRoom';
export default WaitingRoom;
