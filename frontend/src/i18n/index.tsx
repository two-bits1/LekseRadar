// Перевод интерфейса: useT() — строки текущего языка, useLang() — язык и его смена (только в настройках).
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { useAuth } from '../auth';
import { usersApi } from '../api/endpoints';
import { nb, type Dict } from './nb';
import { en } from './en';
import { ru } from './ru';
import { type Lang, langFromLocale, loadLang, localeTag, saveLang, shouldSyncProfile } from './lang';

export * from './lang';
export type { Dict };

const DICTS: Record<Lang, Dict> = { nb, en, ru };
const I18nContext = createContext<{ lang: Lang; setLang: (lang: Lang) => void } | null>(null);

export function I18nProvider({ children }: { children: ReactNode }) {
  const { token, user, updateUser } = useAuth();
  const [lang, setLangState] = useState<Lang>(loadLang);

  // После входа главный — язык из профиля (на новом устройстве сайт сразу на языке ученика).
  useEffect(() => {
    if (!user) return;
    const fromProfile = langFromLocale(user.locale);
    setLangState(fromProfile); saveLang(fromProfile);
  }, [user?.id, user?.locale]);

  useEffect(() => { document.documentElement.lang = lang; }, [lang]);

  const setLang = (next: Lang) => {
    setLangState(next); saveLang(next);
    // Тот же язык — ИИ-наставнику: PATCH /users/me { locale }. Без токена не шлём.
    if (shouldSyncProfile(token, next, user)) usersApi.update({ locale: next }).then(updateUser).catch(() => {});
  };

  return <I18nContext.Provider value={{ lang, setLang }}>{children}</I18nContext.Provider>;
}

export function useLang() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useLang() должен вызываться внутри <I18nProvider>');
  return { ...ctx, tag: localeTag(ctx.lang) };
}

export const useT = (): Dict => DICTS[useLang().lang];
