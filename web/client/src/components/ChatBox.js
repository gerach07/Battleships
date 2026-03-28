import React, { useState, useRef, useEffect, useCallback, memo } from 'react';
import { useI18n } from '../i18n/I18nContext';

const ChatBox = memo(({ messages, onSend, isOpen, onToggle, unread }) => {
    const { t } = useI18n();
    const [text, setText] = useState('');
    const listRef = useRef(null);

    useEffect(() => {
        if (listRef.current && isOpen) {
            listRef.current.scrollTop = listRef.current.scrollHeight;
        }
    }, [messages, isOpen]);

    const handleSend = useCallback(() => {
        if (text.trim()) {
            const trimmed = text.trim();
            const impMatch = trimmed.match(/^\/imp\s+(.+)/i);
            if (impMatch) {
                onSend(impMatch[1], true);
            } else {
                onSend(trimmed, false);
            }
            setText('');
        }
    }, [text, onSend]);

    return (
        <div className={`fixed bottom-4 right-4 z-50 transition-all duration-300 ${isOpen ? 'w-[calc(100vw-2rem)] sm:w-80 max-w-80' : ''}`}>
            {isOpen ? (
                <div className="flex flex-col h-96 overflow-hidden rounded-2xl border border-slate-600/30 shadow-2xl shadow-black/40" style={{ background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.97), rgba(30, 41, 59, 0.97))', backdropFilter: 'blur(24px)', animation: 'chatSlideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards' }}>
                    {/* Header */}
                    <div className="flex items-center justify-between px-4 py-2.5 bg-gradient-to-r from-blue-500/10 to-indigo-500/10 border-b border-slate-700/40">
                        <div className="flex items-center gap-2">
                            <span className="text-base">💬</span>
                            <span className="font-bold text-sm text-white">{t('chat.title')}</span>
                            {messages.length > 0 && (
                                <span className="text-[0.6rem] text-slate-500 bg-slate-800/60 px-1.5 py-0.5 rounded-full font-mono">{messages.length}</span>
                            )}
                        </div>
                        <button onClick={onToggle} className="w-6 h-6 flex items-center justify-center rounded-lg text-slate-500 hover:text-white hover:bg-white/10 transition text-sm leading-none">✕</button>
                    </div>

                    {/* Messages */}
                    <div ref={listRef} className="flex-1 overflow-y-auto px-3 py-3 space-y-2 text-sm scrollbar-thin">
                        {messages.length === 0 && (
                            <div className="flex flex-col items-center justify-center h-full gap-2 opacity-50">
                                <span className="text-3xl">🫧</span>
                                <p className="text-slate-500 text-xs">{t('chat.noMessages')}</p>
                            </div>
                        )}
                        {messages.map((m, i) => {
                            const showName = !m.isSystem && (i === 0 || messages[i-1].isSystem || messages[i-1].senderName !== m.senderName);
                            const displayName = m.isMine ? t('waiting.you') : m.senderName;
                            return (
                            <div key={m.id || i} className={m.isSystem ? 'text-center mt-2' : `flex ${m.isMine ? 'justify-end' : 'justify-start'} ${!showName ? 'mt-0.5' : 'mt-2'}`}>
                                {m.isSystem ? (
                                    <p className="text-slate-500 text-[0.65rem] italic bg-slate-800/40 rounded-full px-3 py-1 inline-block">{m.text}</p>
                                ) : (
                                    <div className={`rounded-2xl px-3 py-2 max-w-[80%] break-words text-[0.8rem] leading-relaxed shadow-sm ${
                                        m.isImportant
                                            ? 'bg-gradient-to-br from-cyan-600/90 to-blue-700/90 text-white border border-cyan-400/30 rounded-br-md'
                                            : m.isMine 
                                                ? 'bg-gradient-to-br from-blue-600 to-blue-700 text-white rounded-br-md' 
                                                : 'bg-slate-700/70 text-slate-200 border border-slate-600/30 rounded-bl-md'
                                    }`}>
                                        {showName && (
                                            <span className={`text-[0.6rem] font-bold block mb-0.5 ${m.isImportant ? 'text-cyan-200/80' : m.isMine ? 'text-blue-200/70' : 'text-yellow-300/90'}`}>{m.isImportant && '📢 '}{displayName}</span>
                                        )}
                                        {m.text}
                                    </div>
                                )}
                            </div>
                        )})}
                    </div>

                    {/* Input */}
                    <div className="p-2.5 border-t border-slate-700/40 bg-slate-900/40 flex gap-2">
                        <input
                            type="text"
                            value={text}
                            onChange={e => setText(e.target.value.slice(0, 200))}
                            onKeyDown={e => e.key === 'Enter' && handleSend()}
                            placeholder={t('chat.placeholder')}
                            className="flex-1 px-3.5 py-2 bg-slate-800/80 border border-slate-600/40 rounded-xl text-white text-sm placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/40 focus:border-blue-500/40 transition-all"
                            maxLength={200}
                            autoFocus
                        />
                        <button
                            onClick={handleSend}
                            disabled={!text.trim()}
                            className="w-9 h-9 flex items-center justify-center bg-gradient-to-br from-blue-500 to-indigo-600 hover:from-blue-400 hover:to-indigo-500 disabled:opacity-30 disabled:cursor-not-allowed text-white font-bold rounded-xl transition-all hover:scale-105 active:scale-95 shadow-lg shadow-blue-900/30 text-sm"
                        >
                            ➤
                        </button>
                    </div>
                </div>
            ) : (
                <button
                    onClick={onToggle}
                    className="group w-12 h-12 flex items-center justify-center bg-gradient-to-br from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white rounded-2xl shadow-xl shadow-blue-900/40 transition-all hover:scale-110 hover:shadow-2xl active:scale-95 relative border border-blue-400/20"
                >
                    <span className="text-lg group-hover:scale-110 transition-transform">💬</span>
                    {unread > 0 && (
                        <span className="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-[0.55rem] font-black min-w-[1.25rem] h-5 flex items-center justify-center rounded-full animate-bounce shadow-lg shadow-red-900/40 border-2 border-slate-900">
                            {unread > 9 ? '9+' : unread}
                        </span>
                    )}
                </button>
            )}
        </div>
    );
});

ChatBox.displayName = 'ChatBox';
export default ChatBox;
