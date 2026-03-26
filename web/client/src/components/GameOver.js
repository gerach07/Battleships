import React, { memo } from 'react';
import { useI18n } from '../i18n/I18nContext';

const GameOver = memo(({
    winner,
    playerId,
    opponentName,
    opponentWantsPlayAgain,
    playAgainPending,
    handlePlayAgain,
    handleBackToMenu,
    handleDeclinePlayAgain,
    isSpectator
}) => {
    const { t } = useI18n();
    const isWinner = winner === playerId;

    return (
        <div className="max-w-md mx-auto pt-6 px-2 text-center space-y-6 animate-fade-in" role="status" aria-label={isSpectator ? 'Game over' : (isWinner ? 'Victory' : 'Defeat')}>
            <div className={`glass-card p-7 space-y-5 relative overflow-hidden ${
                isSpectator ? 'border-slate-600/40'
                : isWinner ? 'border-yellow-500/50 shadow-2xl shadow-yellow-900/20'
                : 'border-red-500/30 shadow-xl shadow-red-900/15'
            }`}>
                {isWinner && !isSpectator && (
                    <div className="absolute inset-0 bg-gradient-to-br from-yellow-500/10 via-transparent to-amber-500/10 pointer-events-none" />
                )}
                <div className={`text-8xl ${!isSpectator && isWinner ? 'animate-bounce drop-shadow-2xl' : ''}`} style={!isSpectator && isWinner ? { animationDuration: '1.5s', filter: 'drop-shadow(0 0 20px rgba(251,191,36,0.5))' } : {}}>
                    {isSpectator ? '🏁' : (isWinner ? '🏆' : '💀')}
                </div>
                <div className="relative">
                    <h2 className={`text-4xl sm:text-5xl font-black ${
                        isSpectator ? 'text-white'
                        : isWinner ? 'bg-gradient-to-r from-yellow-300 via-amber-300 to-yellow-400 bg-clip-text text-transparent drop-shadow-lg'
                        : 'text-red-400'
                    }`}>
                        {isSpectator ? t('gameover.title') : (isWinner ? t('gameover.victory') : t('gameover.defeat'))}
                    </h2>
                    <p className="text-slate-400 text-sm mt-3 leading-relaxed">
                        {isSpectator
                            ? t('gameover.spectatorSubtitle')
                            : (isWinner ? t('gameover.winSubtitle') : t('gameover.loseSubtitle', opponentName))}
                    </p>
                </div>

                {isWinner && !isSpectator && (
                    <div className="flex justify-center gap-2">
                        {['⭐', '⭐', '⭐'].map((s, i) => (
                            <span 
                                key={i} 
                                className="text-3xl animate-bounce" 
                                style={{ 
                                    animationDelay: `${i * 150}ms`, 
                                    animationDuration: '1s',
                                    filter: 'drop-shadow(0 0 8px rgba(251,191,36,0.6))'
                                }}
                            >
                                {s}
                            </span>
                        ))}
                    </div>
                )}

            {!isSpectator && (
                <>
                    {/* Opponent wants to play again — show Accept/Decline */}
                    {opponentWantsPlayAgain && (
                        <div className="bg-blue-900/40 border border-blue-500/50 rounded-2xl p-4 space-y-3">
                            <p className="font-bold text-blue-200">{t('gameover.rematchOffer', opponentName)}</p>
                            <div className="flex gap-2">
                                <button
                                    onClick={handlePlayAgain}
                                    className="flex-1 py-2.5 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-400 hover:to-emerald-500 text-white font-bold rounded-xl transition"
                                >
                                    {t('gameover.accept')}
                                </button>
                                <button
                                    onClick={handleDeclinePlayAgain}
                                    className="flex-1 py-2.5 bg-red-700 hover:bg-red-600 text-white font-bold rounded-xl transition"
                                >
                                    {t('gameover.decline')}
                                </button>
                            </div>
                            <button onClick={handleBackToMenu} className="w-full py-2 bg-slate-700 hover:bg-slate-600 text-slate-300 font-semibold rounded-xl transition text-sm">
                                {t('gameover.mainMenu')}
                            </button>
                        </div>
                    )}

                    {/* I requested, waiting for opponent */}
                    {playAgainPending && !opponentWantsPlayAgain && (
                        <div className="bg-slate-800 border border-slate-600 rounded-2xl p-4">
                            <div className="flex justify-center gap-2 mb-2">
                                {[0, 150, 300].map(d => <span key={d} className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: `${d}ms` }} />)}
                            </div>
                            <p className="text-slate-300 text-sm">{t('gameover.waitingAccept', opponentName)}</p>
                            <button
                                onClick={handleDeclinePlayAgain}
                                className="mt-2 text-xs text-slate-500 hover:text-slate-300 transition underline"
                            >
                                {t('gameover.cancelRequest')}
                            </button>
                            <button onClick={handleBackToMenu} className="w-full mt-3 py-2 bg-slate-700 hover:bg-slate-600 text-slate-300 font-semibold rounded-xl transition text-sm">
                                {t('gameover.mainMenu')}
                            </button>
                        </div>
                    )}

                    {/* Neither side is in play-again flow yet */}
                    {!playAgainPending && !opponentWantsPlayAgain && (
                        <div className="space-y-2">
                            <button onClick={handlePlayAgain} className="w-full py-3.5 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-500 hover:to-purple-500 text-white font-bold rounded-2xl transition hover:scale-[1.02] shadow-lg shadow-purple-900/20">
                                {t('gameover.playAgain')}
                            </button>
                            <button onClick={handleBackToMenu} className="w-full py-2.5 bg-slate-700 hover:bg-slate-600 text-slate-200 font-semibold rounded-2xl transition text-sm">
                                {t('gameover.backToMenu')}
                            </button>
                        </div>
                    )}
                </>
            )}

            {isSpectator && (
                <button onClick={handleBackToMenu} className="w-full py-3.5 bg-slate-700 hover:bg-slate-600 text-white font-bold rounded-2xl transition hover:scale-[1.02] shadow-lg">
                    {t('gameover.backToMenu')}
                </button>
            )}
            </div>
        </div>
    );
});

GameOver.displayName = 'GameOver';
export default GameOver;
