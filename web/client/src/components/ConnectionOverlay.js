import React, { memo } from 'react';
import { useI18n } from '../i18n/I18nContext';

const ConnectionOverlay = memo(({ isConnected }) => {
    const { t } = useI18n();
    if (isConnected) return null;

    return (
        <div className="fixed inset-0 z-[100] bg-black/70 backdrop-blur-sm flex items-center justify-center" role="alertdialog" aria-modal="true" aria-label="Connection lost">
            <div className="glass-card p-8 max-w-sm text-center shadow-2xl">
                <div className="text-5xl mb-4">📡</div>
                <h2 className="text-xl font-bold text-white mb-2">{t('connection.title')}</h2>
                <p className="text-slate-400 text-sm mb-4">{t('connection.reconnecting')}</p>
                <div className="flex justify-center gap-2">
                    {[0, 150, 300].map(d => (
                        <span key={d} className="w-2.5 h-2.5 bg-red-400 rounded-full animate-bounce" style={{ animationDelay: `${d}ms` }} />
                    ))}
                </div>
                <button onClick={() => window.location.reload()} className="mt-4 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-colors">
                    🔄 {t('connection.refresh')}
                </button>
            </div>
        </div>
    );
});

ConnectionOverlay.displayName = 'ConnectionOverlay';
export default ConnectionOverlay;
