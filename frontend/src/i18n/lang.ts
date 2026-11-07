// Язык интерфейса: nb (по умолчанию), en, ru. Выбирается только в настройках.
import type { User } from '../api/types';

export type Lang = 'nb' | 'en' | 'ru';
export const LANGS: Lang[] = ['nb', 'en', 'ru'];
export const LANG_NAMES: Record<Lang, string> = { nb: 'Norsk', en: 'English', ru: 'Русский' };  // каждый — на своём языке

const STORAGE_KEY = 'lekseradar-lang';
const TAGS: Record<Lang, 'nb-NO' | 'en-GB' | 'ru-RU'> = { nb: 'nb-NO', en: 'en-GB', ru: 'ru-RU' };

// Профиль хранит nb / nn / en / ru; nn и всё неизвестное показываем как nb.
export const langFromLocale = (locale: string | null | undefined): Lang =>
  locale === 'en' || locale === 'ru' ? locale : locale === 'uk' ? 'en' : 'nb';  // uk → en: украинского интерфейса нет

export function loadLang(): Lang {
  try { const v = localStorage.getItem(STORAGE_KEY); if (v === 'nb' || v === 'en' || v === 'ru') return v; } catch {}
  return 'nb';
}

export function saveLang(lang: Lang) {
  try { localStorage.setItem(STORAGE_KEY, lang); } catch {}
}

export const localeTag = (lang: Lang) => TAGS[lang];

// Писать язык в профиль можно только с токеном: без него PATCH /users/me вернёт 401 и выкинет на /login.
export const shouldSyncProfile = (token: string | null, lang: Lang, user: User | null) =>
  !!token && !!user && langFromLocale(user.locale) !== lang;
