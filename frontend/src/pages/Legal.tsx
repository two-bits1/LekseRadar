// Политика конфиденциальности (/privacy) и условия использования (/terms). Открыты без входа.
// Тексты — src/legal/{nb,en,ru}.ts; страница грузится отдельным чанком (React.lazy в main.tsx).
import { useEffect, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ArrowUpRight } from 'lucide-react';
import { useLang } from '../i18n';
import type { Lang } from '../i18n';
import type { LegalBlock, LegalTexts } from '../legal/types';
import { nb } from '../legal/nb';
import { en } from '../legal/en';
import { ru } from '../legal/ru';

const TEXTS: Record<Lang, LegalTexts> = { nb, en, ru };
const LINK = /\[([^\]]+)\]\(([^)\s]+)\)/g;

/** Строка с ссылками [текст](/путь) или [текст](https://…): свои адреса — Link, чужие — в новой вкладке. */
export function inline(text: string): ReactNode[] {
  const out: ReactNode[] = [];
  let last = 0;
  for (const m of text.matchAll(LINK)) {
    const [all, label, href] = m;
    const at = m.index ?? 0;
    if (at > last) out.push(text.slice(last, at));
    out.push(href.startsWith('/')
      ? <Link key={at} to={href}>{label}</Link>
      : <a key={at} href={href} target="_blank" rel="noreferrer">{label}<ArrowUpRight size={12} aria-hidden="true" /></a>);
    last = at + all.length;
  }
  if (last < text.length) out.push(text.slice(last));
  return out;
}

function Block({ block }: { block: LegalBlock }) {
  if (typeof block === 'string') return <p>{inline(block)}</p>;
  return <ul>{block.list.map((item, i) => <li key={i}>{inline(item)}</li>)}</ul>;
}

export default function Legal({ doc }: { doc: 'privacy' | 'terms' }) {
  const { lang } = useLang();
  const texts = TEXTS[lang]; const d = texts[doc];
  useEffect(() => {
    const before = document.title;
    document.title = `${d.eyebrow.charAt(0)}${d.eyebrow.slice(1).toLowerCase()} — LekseRadar`;
    return () => { document.title = before; };
  }, [d]);

  return <section className="page narrow legal">
    <div className="eyebrow">{d.eyebrow}</div>
    <h1>{d.title}</h1>
    <p className="legal-intro">{inline(d.intro)}</p>
    <p className="legal-updated">{texts.updated}</p>
    <nav className="legal-toc" aria-label={texts.contents}>
      <h2>{texts.contents}</h2>
      <ol>{d.sections.map(s => <li key={s.id}><a href={`#${s.id}`}>{s.title}</a></li>)}</ol>
    </nav>
    {d.sections.map(s => <article key={s.id} className="legal-section" id={s.id} aria-labelledby={`${s.id}-title`}>
      <h2 id={`${s.id}-title`}>{s.title}</h2>
      {s.body.map((b, i) => <Block key={i} block={b} />)}
    </article>)}
  </section>;
}
