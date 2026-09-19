import React, { useEffect, useState } from 'react';
import { useI18n } from '../i18n/I18nContext';

const Leaderboard = ({ onClose, SOCKET_URL }) => {
    const { t } = useI18n();
    const [leaders, setLeaders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchLeaderboard = async () => {
            try {
                // Ensure http:// or https:// prefix
                const url = (SOCKET_URL.startsWith('http') ? SOCKET_URL : `http://${SOCKET_URL}`) + '/api/leaderboard';
                const res = await fetch(url);
                if (!res.ok) throw new Error('Failed to load leaderboard');
                const data = await res.json();
                setLeaders(data.leaderboard || []);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };
        fetchLeaderboard();
    }, [SOCKET_URL]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-sm animate-fade-in">
            <div className="glass-card w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl shadow-blue-900/20 border-slate-700">
                <div className="p-5 border-b border-slate-700 flex justify-between items-center bg-slate-800/50 rounded-t-2xl">
                    <h2 className="text-2xl font-black bg-gradient-to-r from-yellow-300 to-yellow-500 bg-clip-text text-transparent flex items-center gap-2">
                        <span>🏆</span> Leaderboard
                    </h2>
                    <button onClick={onClose} className="p-2 bg-slate-700/50 hover:bg-slate-600 rounded-xl transition-colors">
                        ✕
                    </button>
                </div>
                
                <div className="flex-1 overflow-y-auto p-4 space-y-2">
                    {loading ? (
                        <div className="text-center py-10 text-slate-400">Loading rankings...</div>
                    ) : error ? (
                        <div className="text-center py-10 text-red-400">{error}</div>
                    ) : leaders.length === 0 ? (
                        <div className="text-center py-10 text-slate-400">No ranked players yet. Be the first to win a game!</div>
                    ) : (
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="text-xs uppercase tracking-widest text-slate-500 border-b border-slate-700/50">
                                    <th className="pb-3 px-2 text-center w-12">Rank</th>
                                    <th className="pb-3 px-2">Player</th>
                                    <th className="pb-3 px-2">Player ID</th>
                                    <th className="pb-3 px-2 text-right">Wins</th>
                                    <th className="pb-3 px-2 text-right hidden sm:table-cell">Played</th>
                                </tr>
                            </thead>
                            <tbody>
                                {leaders.map((player) => (
                                    <tr key={player.playerId || player.name} className="border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors">
                                        <td className="py-3 px-2 text-center font-mono font-bold text-slate-300">
                                            {player.rank === 1 ? '🥇' : player.rank === 2 ? '🥈' : player.rank === 3 ? '🥉' : `#${player.rank}`}
                                        </td>
                                        <td className="py-3 px-2">
                                            <div className="flex items-center gap-3">
                                                {player.photoUrl ? (
                                                    <img src={player.photoUrl} alt="Avatar" className="w-8 h-8 rounded-full shadow-sm" />
                                                ) : (
                                                    <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center text-xs">👤</div>
                                                )}
                                                <span className="font-bold text-white">{player.name}</span>
                                            </div>
                                        </td>
                                        <td className="py-3 px-2 font-mono text-xs text-blue-300">
                                            {player.playerId || '—'}
                                        </td>
                                        <td className="py-3 px-2 text-right font-black text-yellow-400">
                                            {player.wins}
                                        </td>
                                        <td className="py-3 px-2 text-right font-mono text-sm text-slate-400 hidden sm:table-cell">
                                            {player.gamesPlayed}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Leaderboard;
