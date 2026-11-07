// Итог квиза и занятия с карточками: кольцо с процентом, короткий вывод, счётчики и кнопки.
import type { ReactNode } from 'react';

const R = 54, C = 2 * Math.PI * R;

export function ScoreRing({ pct, label }: { pct: number; label: string }) {
  const v = Math.max(0, Math.min(100, Math.round(pct)));
  return <div className="score-ring" role="img" aria-label={`${v} % ${label}`}>
    <svg viewBox="0 0 128 128" aria-hidden="true">
      <defs><linearGradient id="ring-grad" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#5fd3c0" /><stop offset=".6" stopColor="#7a9cff" /><stop offset="1" stopColor="#b69cff" /></linearGradient></defs>
      <circle className="ring-track" cx="64" cy="64" r={R} />
      <circle className="ring-value" cx="64" cy="64" r={R} stroke="url(#ring-grad)" strokeDasharray={C}
        style={{ strokeDashoffset: C * (1 - v / 100), ['--ring-full' as string]: C }} />
    </svg>
    <span className="ring-text"><strong>{v}<small>%</small></strong><em>{label}</em></span>
  </div>;
}

export type Tally = { label: string; value: number; tone: 'success' | 'warning' | 'danger' | 'info' };

export function ResultCard({ eyebrow, title, text, pct, ringLabel, tallies, children, footer }: {
  eyebrow: string; title: string; text: string; pct: number | null; ringLabel: string;
  tallies?: Tally[]; children?: ReactNode; footer?: ReactNode;
}) {
  return <article className="result-card">
    <div className="result-main">
      {pct !== null && <ScoreRing pct={pct} label={ringLabel} />}
      <div className="result-copy"><p className="dash-date">{eyebrow}</p><h1>{title}</h1><p>{text}</p>
        {tallies && tallies.length > 0 && <ul className="result-tallies">{tallies.map(x =>
          <li key={x.label} className={`tally tally-${x.tone}`}><strong>{x.value}</strong><span>{x.label}</span></li>)}</ul>}
      </div>
    </div>
    {children}
    {footer && <div className="result-actions">{footer}</div>}
  </article>;
}

// Короткий вывод по проценту: ≥ 90 — отлично, ≥ 60 — хорошо, иначе — есть над чем поработать.
export const tier = (pct: number): 'great' | 'good' | 'keep' => pct >= 90 ? 'great' : pct >= 60 ? 'good' : 'keep';
