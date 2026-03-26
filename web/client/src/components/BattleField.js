import React, { memo, useMemo } from 'react';
import GameBoard from './GameBoard';
import GameTimer from './GameTimer';
import { useI18n } from '../i18n/I18nContext';

const BattleField = memo(({
    isMyTurn,
    opponentName,
    handleForfeit,
    dispPlayer,
    dispOpponent,
    handleShoot,
    explosionCells,
    noop,
    playerTimeLeft,
    turnStartedAt,
    myId,
    currentTurn,
    spectatorCount,
    isSpectator,
    lastShot,
    handleLeave,
    mySunkCount,
    theirSunkCount,
}) => {
    const { t } = useI18n();
    // Memoize filtered props to avoid breaking child React.memo
    const playerExplosions = useMemo(() => explosionCells.filter(e => e.board === 'player'), [explosionCells]);
    const opponentExplosions = useMemo(() => explosionCells.filter(e => e.board === 'opponent'), [explosionCells]);
    const playerLastShot = useMemo(() => lastShot?.board === 'player' ? lastShot : null, [lastShot]);
    const opponentLastShot = useMemo(() => lastShot?.board === 'opponent' ? lastShot : null, [lastShot]);

    return (
        <div className="space-y-4 sm:space-y-5 animate-fade-in" role="region" aria-label="Battle phase">
            {playerTimeLeft && Object.keys(playerTimeLeft).length > 0 && (
                <GameTimer
                    playerTimeLeft={playerTimeLeft}
                    turnStartedAt={turnStartedAt}
                    currentTurn={currentTurn}
                    myId={myId}
                    opponentName={opponentName}
                />
            )}

            <div className={`glass-card p-4 sm:p-5 text-center transition-all duration-500 relative overflow-hidden
                ${isSpectator
                    ? 'border-purple-500/40 shadow-purple-900/20 shadow-lg'
                    : isMyTurn
                        ? 'border-emerald-500/40 your-turn-card shadow-emerald-900/20 shadow-lg'
                        : 'border-slate-700/50'
                }`}>
                {isMyTurn && !isSpectator && (
                    <>
                        <div className="absolute inset-0 bg-gradient-to-r from-emerald-500/8 via-transparent to-emerald-500/8 animate-pulse" />
                        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-emerald-400/50 to-transparent" />
                    </>
                )}
                <div className="flex items-center justify-between gap-3 relative">
                    <div className="flex-1 min-w-0 text-left">
                        <p className="text-base sm:text-lg font-black truncate flex items-center gap-2">
                            {isSpectator ? (
                                <><span className="text-purple-400">👁️</span> {t('battle.spectating')}</>
                            ) : isMyTurn ? (
                                <><span className="text-emerald-400 animate-pulse">⚡</span> {t('battle.yourTurn')}</>
                            ) : (
                                <><span className="text-slate-500">⏳</span> {t('battle.opTurn', opponentName)}</>
                            )}
                        </p>
                        {/* Always render to prevent layout shift; hide via visibility */}
                        <p className={`text-[0.65rem] mt-0.5 font-medium transition-opacity duration-200 ${!isSpectator && isMyTurn ? 'text-emerald-300/70 opacity-100' : 'opacity-0 pointer-events-none select-none'}`}>
                            {t('battle.extraShot')}
                        </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                        {spectatorCount > 0 && (
                            <span className="px-2.5 py-1.5 bg-purple-900/40 text-purple-300 text-[0.65rem] font-bold rounded-lg border border-purple-700/40 shadow-sm">
                                👁️ {spectatorCount}
                            </span>
                        )}
                        <button
                            onClick={isSpectator ? handleLeave : handleForfeit}
                            className="px-3 py-1.5 text-red-400/80 hover:text-red-300 hover:bg-red-500/15 text-xs font-semibold rounded-lg transition-all active:scale-95 border border-red-500/20 hover:border-red-500/40"
                            title={isSpectator ? t('battle.leaveTooltip') : t('battle.surrenderTooltip')}
                        >
                            {isSpectator ? t('battle.leave') : '🏳️'}
                        </button>
                    </div>
                </div>
            </div>

            {/* Ship scoreboard — always visible to avoid layout shift */}
            {!isSpectator && (
                <div className="glass-card px-4 py-3 flex items-center justify-center gap-6 text-xs">
                    <div className="flex items-center gap-2">
                        <span className="text-emerald-400 font-bold text-sm">{t('battle.yourHits')}</span>
                        <div className="flex gap-1">
                            {Array.from({ length: 5 }, (_, i) => (
                                <span 
                                    key={i} 
                                    className={`w-2.5 h-2.5 rounded-full transition-all duration-300 ${
                                        i < mySunkCount 
                                        ? 'bg-orange-400 shadow-md shadow-orange-400/60 scale-110' 
                                        : 'bg-slate-700/60 scale-90'
                                    }`} 
                                />
                            ))}
                        </div>
                    </div>
                    <span className="text-slate-600 text-lg">│</span>
                    <div className="flex items-center gap-2">
                        <div className="flex gap-1">
                            {Array.from({ length: 5 }, (_, i) => (
                                <span 
                                    key={i} 
                                    className={`w-2.5 h-2.5 rounded-full transition-all duration-300 ${
                                        i < theirSunkCount 
                                        ? 'bg-red-500 shadow-md shadow-red-500/60 scale-110' 
                                        : 'bg-slate-700/60 scale-90'
                                    }`} 
                                />
                            ))}
                        </div>
                        <span className="text-red-400 font-bold text-sm">{t('battle.theirHits')}</span>
                    </div>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
                <div className={`glass-card p-3 sm:p-4 transition-all animate-board-entry ${isSpectator ? '' : isMyTurn ? '' : 'border-yellow-500/15'}`}>
                    <GameBoard board={dispPlayer} isYourBoard={!isSpectator} onCellClick={noop} isYourTurn={false} label={isSpectator ? `🛡️ ${opponentName || t('battle.player1')}` : t('battle.yourFleet')} explosionCells={playerExplosions} lastShot={playerLastShot} />
                </div>
                <div className={`glass-card p-3 sm:p-4 transition-all animate-board-entry ${isSpectator ? '' : isMyTurn ? 'border-emerald-500/20 ring-1 ring-emerald-500/10' : ''}`}>
                    <GameBoard board={dispOpponent} isYourBoard={false} onCellClick={isSpectator ? noop : handleShoot} isYourTurn={!isSpectator && isMyTurn} label={t('battle.enemyWaters', opponentName || t('battle.player2'))} explosionCells={opponentExplosions} lastShot={opponentLastShot} />
                </div>
            </div>
        </div>
    );
});

BattleField.displayName = 'BattleField';

export default BattleField;
