import React, { useState } from 'react';
import { useI18n } from '../i18n/I18nContext';
import { logOut } from '../firebase';

const ProfileModal = ({ onClose, user, setUser, SOCKET_URL, firebaseAuthToken }) => {
    const { t } = useI18n();
    const [name, setName] = useState(user?.name || '');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);

    const handleSave = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(false);

        try {
            const url = (SOCKET_URL.startsWith('http') ? SOCKET_URL : `http://${SOCKET_URL}`) + '/api/profile';
            const res = await fetch(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${firebaseAuthToken}`
                },
                body: JSON.stringify({ name })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to update profile');
            
            setUser(data);
            setSuccess(true);
            setTimeout(() => setSuccess(false), 3000);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = async () => {
        try {
            await logOut();
            setUser(null);
            onClose();
        } catch (err) {
            console.error(err);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-sm animate-fade-in">
            <div className="glass-card w-full max-w-md shadow-2xl shadow-emerald-900/20 border-slate-700">
                <div className="p-5 border-b border-slate-700 flex justify-between items-center bg-slate-800/50 rounded-t-2xl">
                    <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        <span>👤</span> Edit Profile
                    </h2>
                    <button onClick={onClose} className="p-2 bg-slate-700/50 hover:bg-slate-600 rounded-xl transition-colors">
                        ✕
                    </button>
                </div>
                
                <div className="p-5 space-y-6">
                    <div className="flex items-center gap-4 bg-slate-800/50 p-4 rounded-xl border border-slate-700/50">
                        {user?.photoUrl ? (
                            <img src={user.photoUrl} alt="Avatar" className="w-16 h-16 rounded-full border-2 border-emerald-500/50 shadow-lg" />
                        ) : (
                            <div className="w-16 h-16 rounded-full bg-slate-700 flex items-center justify-center text-2xl border-2 border-slate-600">👤</div>
                        )}
                        <div>
                            <p className="font-bold text-white text-lg">{user?.name || 'Anonymous'}</p>
                            <p className="text-slate-400 text-sm">{user?.email}</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-slate-800/50 p-3 rounded-xl border border-slate-700/50 text-center">
                            <p className="text-xs text-slate-400 font-bold uppercase tracking-widest mb-1">Total Wins</p>
                            <p className="text-2xl font-black text-yellow-400">{user?.wins || 0}</p>
                        </div>
                        <div className="bg-slate-800/50 p-3 rounded-xl border border-slate-700/50 text-center">
                            <p className="text-xs text-slate-400 font-bold uppercase tracking-widest mb-1">Matches</p>
                            <p className="text-2xl font-black text-blue-400">{user?.gamesPlayed || 0}</p>
                        </div>
                    </div>

                    <form onSubmit={handleSave} className="space-y-4">
                        <div>
                            <label className="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-1 ml-1">Display Name</label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full bg-slate-900/50 border border-slate-600 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all"
                                maxLength={50}
                                required
                            />
                        </div>
                        {error && <p className="text-red-400 text-sm font-bold bg-red-500/10 p-2 rounded-lg text-center border border-red-500/20">{error}</p>}
                        {success && <p className="text-emerald-400 text-sm font-bold bg-emerald-500/10 p-2 rounded-lg text-center border border-emerald-500/20">Profile updated successfully!</p>}

                        <div className="flex gap-3 pt-2">
                            <button
                                type="button"
                                onClick={handleLogout}
                                className="flex-1 py-3 bg-red-500/20 hover:bg-red-500/30 text-red-400 font-bold rounded-xl transition-all border border-red-500/30"
                            >
                                Sign Out
                            </button>
                            <button
                                type="submit"
                                disabled={loading}
                                className="flex-[2] py-3 bg-gradient-to-r from-emerald-500 to-green-600 hover:from-emerald-400 hover:to-green-500 text-white font-bold rounded-xl transition-all shadow-lg shadow-emerald-900/50 disabled:opacity-50"
                            >
                                {loading ? 'Saving...' : 'Save Profile'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default ProfileModal;
