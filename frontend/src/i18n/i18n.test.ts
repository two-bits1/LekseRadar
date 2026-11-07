import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { langFromLocale, loadLang, saveLang, localeTag, shouldSyncProfile } from './lang';
import { nb } from './nb';
import { en } from './en';
import { ru } from './ru';
import type { User } from '../api/types';

const store = new Map<string, string>();
beforeEach(() => {
  store.clear();
  vi.stubGlobal('localStorage', {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => { store.set(k, v); },
    removeItem: (k: string) => { store.delete(k); },
  });
});
afterEach(() => { vi.unstubAllGlobals(); });

describe('langFromLocale', () => {
  it.each([['nb', 'nb'], ['nn', 'nb'], ['uk', 'en'], ['en', 'en'], ['ru', 'ru'], [undefined, 'nb'], [null, 'nb'], ['xx', 'nb']] as const)(
    '%s → %s', (locale, lang) => { expect(langFromLocale(locale)).toBe(lang); });
});

describe('выбор языка на устройстве', () => {
  it('по умолчанию nb', () => { expect(loadLang()).toBe('nb'); });
  it('мусор в localStorage → nb', () => { store.set('lekseradar-lang', 'xx'); expect(loadLang()).toBe('nb'); });
  it('сохранённый выбор читается обратно', () => { saveLang('ru'); expect(loadLang()).toBe('ru'); });
});

it('localeTag для дат', () => {
  expect([localeTag('nb'), localeTag('en'), localeTag('ru')]).toEqual(['nb-NO', 'en-GB', 'ru-RU']);
});

describe('shouldSyncProfile', () => {
  const user = { locale: 'nb' } as User;
  it('без токена — никогда (иначе 401 выкинет на /login)', () => { expect(shouldSyncProfile(null, 'en', user)).toBe(false); });
  it('с токеном и другим языком — да', () => { expect(shouldSyncProfile('t', 'en', user)).toBe(true); });
  it('с токеном и тем же языком — нет', () => { expect(shouldSyncProfile('t', 'nb', user)).toBe(false); });
  it('профиль nn и выбран nb — нет (nn показывается как nb)', () => { expect(shouldSyncProfile('t', 'nb', { locale: 'nn' } as User)).toBe(false); });
});

// Словари: en и ru повторяют форму nb, без пустых строк.
type Tree = { [k: string]: string | ((...a: never[]) => string) | Tree };
const shape = (t: Tree): unknown => Object.fromEntries(Object.entries(t).map(([k, v]) => [k, typeof v === 'object' ? shape(v) : typeof v]));
const strings = (t: Tree): string[] => Object.values(t).flatMap(v => typeof v === 'object' ? strings(v) : typeof v === 'string' ? [v] : []);

describe('словари', () => {
  it.each([['en', en], ['ru', ru]] as const)('%s повторяет ключи nb', (_n, dict) => {
    expect(shape(dict as unknown as Tree)).toEqual(shape(nb as unknown as Tree));
  });
  it.each([['nb', nb], ['en', en], ['ru', ru]] as const)('%s без пустых строк', (_n, dict) => {
    expect(strings(dict as unknown as Tree).filter(s => !s.trim())).toEqual([]);
  });
});
