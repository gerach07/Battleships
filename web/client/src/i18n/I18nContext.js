import React, { createContext, useContext, useState, useCallback, useEffect, useMemo } from 'react';
import en from './en.json';
import lv from './lv.json';
import ru from './ru.json';

const locales = { en, lv, ru };
const LANG_KEY = 'battleships-lang';

const I18nContext = createContext();

/**
 * Simple template — replaces {0}, {1}, … with args.
 */
function formatString(template, ...args) {
  if (!template) return '';
  return template.replace(/\{(\d+)\}/g, (_, i) => args[+i] ?? '');
}

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(() => {
    const saved = localStorage.getItem(LANG_KEY);
    if (saved && locales[saved]) return saved;
    // Auto-detect from browser
    const browserLang = navigator.language?.slice(0, 2);
    if (locales[browserLang]) return browserLang;
    return 'en';
  });

  useEffect(() => {
    localStorage.setItem(LANG_KEY, lang);
  }, [lang]);

  const setLang = useCallback((l) => {
    if (locales[l]) setLangState(l);
  }, []);

  const t = useCallback((key, ...args) => {
    const str = locales[lang]?.[key] || locales.en[key] || key;
    return args.length ? formatString(str, ...args) : str;
  }, [lang]);

  const value = useMemo(() => ({ lang, setLang, t }), [lang, setLang, t]);

  return (
    <I18nContext.Provider value={value}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  return useContext(I18nContext);
}

export default I18nContext;
