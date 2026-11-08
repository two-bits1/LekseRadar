import { describe, expect, it } from 'vitest';
import { isValidElement } from 'react';
import { nb } from './nb';
import { en } from './en';
import { ru } from './ru';
import type { LegalTexts } from './types';
import { inline } from '../pages/Legal';
import { PATHS } from '../paths';

const ALL: [string, LegalTexts][] = [['nb', nb], ['en', en], ['ru', ru]];
const strings = (t: LegalTexts) => [t.privacy, t.terms].flatMap(d => [d.intro, ...d.sections.flatMap(s =>
  s.body.flatMap(b => typeof b === 'string' ? [b] : b.list))]);

describe('тексты /privacy и /terms', () => {
  it.each(['privacy', 'terms'] as const)('%s: одинаковые разделы на всех языках', doc => {
    const ids = (t: LegalTexts) => t[doc].sections.map(s => s.id);
    expect(ids(nb)).toEqual(ids(en));
    expect(ids(ru)).toEqual(ids(en));
  });

  it.each(ALL)('%s: ссылки ведут на свои страницы или на https', (_, t) => {
    const pages = new Set<string>(Object.values(PATHS).flatMap(v => typeof v === 'string' ? [v] : []));
    const hrefs = strings(t).flatMap(s => [...s.matchAll(/\[[^\]]+\]\(([^)\s]+)\)/g)].map(m => m[1]));
    expect(hrefs.length).toBeGreaterThan(0);
    for (const href of hrefs) expect(href.startsWith('https://') || pages.has(href), href).toBe(true);
  });

  it.each(ALL)('%s: нет незакрытых ссылок', (_, t) => {
    for (const s of strings(t)) expect(s.replace(/\[[^\]]+\]\([^)\s]+\)/g, ''), s).not.toMatch(/\]\(/);
  });
});

describe('inline()', () => {
  it('текст без ссылок — как есть', () => {
    expect(inline('Bare tekst.')).toEqual(['Bare tekst.']);
  });

  it('свой адрес — Link, чужой — новая вкладка', () => {
    const parts = inline('Se [personvern](/privacy) og [GitHub](https://github.com).');
    expect(parts[0]).toBe('Se ');
    expect(isValidElement(parts[1]) && (parts[1].props as { to: string }).to).toBe('/privacy');
    expect(parts[2]).toBe(' og ');
    expect(isValidElement(parts[3]) && (parts[3].props as { href: string; target: string }).target).toBe('_blank');
    expect(parts[4]).toBe('.');
  });
});
