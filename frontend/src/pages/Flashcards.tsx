// Флеш-карты на настоящем API (API.md, «Флеш-карты»): к повторению → переворот → оценка 0–5 (SM-2).
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, ListChecks, RotateCcw } from 'lucide-react';
import { PATHS } from '../paths';
import { flashcardsApi } from '../api/endpoints';
import type { ApiError, Flashcard, FlashcardStats } from '../api/types';
import { asApiError } from '../forms';
import { Md } from '../components/Md';
import { ResultCard } from '../components/ResultCard';
import { useT } from '../i18n';
import { GRADES } from './quizLogic';
import { SubjectTopicForm } from './Quiz';

type GradeKey = keyof typeof GRADES;

export function Flashcards() {
  const t = useT(); const c = t.cards;
  const [stats, setStats] = useState<FlashcardStats | null>(null);
  const [due, setDue] = useState<Flashcard[] | null>(null);
  const [index, setIndex] = useState(0); const [flipped, setFlipped] = useState(false);
  const [busy, setBusy] = useState(false); const [error, setError] = useState<ApiError | null>(null);
  const [generated, setGenerated] = useState<number | null>(null);
  const [session, setSession] = useState<Record<GradeKey, number>>({ again: 0, hard: 0, good: 0, easy: 0 });  // оценки за это занятие

  const load = useCallback(() => {
    setIndex(0); setFlipped(false); setSession({ again: 0, hard: 0, good: 0, easy: 0 });
    flashcardsApi.stats().then(setStats).catch(() => {});
    flashcardsApi.due().then(setDue).catch(e => { setDue([]); setError(asApiError(e, t.common)); });
  }, []);
  useEffect(load, [load]);

  const card = due?.[index];
  const reviewed = session.again + session.hard + session.good + session.easy;
  const grade = async (g: GradeKey) => {
    if (!card) return;
    setBusy(true); setError(null);
    try {
      await flashcardsApi.review(card.exerciseId, GRADES[g]);
      setSession(x => ({ ...x, [g]: x[g] + 1 }));
      // «Не помню» — карточка вернётся в конце этого же занятия (в базе она тоже снова к повторению через 10 минут)
      if (g === 'again') setDue(d => d ? [...d, card] : d);
      setIndex(i => i + 1); setFlipped(false);
      flashcardsApi.stats().then(setStats).catch(() => {});
    } catch (e) { setError(asApiError(e, t.common)); } finally { setBusy(false); }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!card || (e.target as HTMLElement)?.closest('input,textarea,select')) return;
      if (e.key === ' ') { e.preventDefault(); setFlipped(f => !f); }
      if (flipped && !busy && ['1', '2', '3', '4'].includes(e.key)) grade((['again', 'hard', 'good', 'easy'] as GradeKey[])[Number(e.key) - 1]);
    };
    window.addEventListener('keydown', onKey); return () => window.removeEventListener('keydown', onKey);
  });

  return <section className="page narrow app-page">
    <header className="page-head"><h1>{c.title}</h1><p>{c.intro}</p></header>
    <div className="mini-stats">
      <span><strong>{stats?.cards ?? '—'}</strong>{c.statsCards}</span>
      <span className={stats?.dueNow ? 'hot' : ''}><strong>{stats?.dueNow ?? '—'}</strong>{c.statsDue}</span>
      <span><strong>{stats?.learned ?? '—'}</strong>{c.statsLearned}</span>
      <span><strong>{stats?.reviews ?? '—'}</strong>{c.statsReviews}</span>
    </div>
    {error && <p role="alert" className="feedback danger">{error.message}</p>}

    {due === null ? <p className="quiet-note">{t.common.loading}</p>
      : card ? <>
        <p className="dash-date">{[card.subject, card.topic].filter(Boolean).join(' · ')} · {c.progress(index + 1, due.length)}</p>
        <button className={`fcard${flipped ? ' flipped' : ''}`} onClick={() => setFlipped(f => !f)} aria-label={`${flipped ? c.back : c.front}. ${c.flipHint}`}>
          <span className="fcard-inner">
            <span className="fcard-face fcard-front"><span className="eyebrow">{c.front}</span><span className="fcard-text"><Md>{card.front}</Md></span><span className="flip-hint"><RotateCcw size={14} />{c.flipHint}</span></span>
            <span className="fcard-face fcard-back"><span className="eyebrow">{c.back}</span><span className="fcard-text"><Md>{card.back}</Md></span>
              {card.explanation && <span className="fcard-expl"><Md>{card.explanation}</Md></span>}</span>
          </span>
        </button>
        {!flipped ? <button className="btn btn-primary fcard-show" onClick={() => setFlipped(true)}>{c.showAnswer}</button>
          : <div className="grades" role="group">{(['again', 'hard', 'good', 'easy'] as GradeKey[]).map((g, i) =>
              <button key={g} className={`grade grade-${g}`} disabled={busy} onClick={() => grade(g)}><span>{c.grades[g]}</span><kbd>{i + 1}</kbd></button>)}</div>}
      </>
      : reviewed > 0 ? <ResultCard eyebrow={c.sessionEyebrow} title={c.sessionTitle(Math.max(1, (due?.length ?? 0) - session.again))} text={c.sessionText(stats?.learned ?? 0)}
          pct={(session.good + session.easy) / reviewed * 100} ringLabel={c.remembered}
          tallies={[
            { label: c.grades.easy, value: session.easy, tone: 'info' },
            { label: c.grades.good, value: session.good, tone: 'success' },
            { label: c.grades.hard, value: session.hard, tone: 'warning' },
            { label: c.grades.again, value: session.again, tone: 'danger' },
          ]}
          footer={<><button className="btn btn-primary" onClick={load}><RotateCcw size={16} />{c.more}</button>
            <Link className="btn" to={PATHS.quizzes}><ListChecks size={16} />{t.quiz.newTitle}</Link>
            <Link className="btn" to={PATHS.home}>{c.toHome}</Link></>} />
      : <p className="feedback success"><Check size={17} />{stats && stats.cards > 0 ? (due.length ? c.done : c.none) : c.empty}</p>}

    <h2 className="section-label">{c.generateTitle}</h2>
    <p className="section-sub">{c.generateIntro}</p>
    {generated !== null && <p role="status" className="feedback success"><Check size={17} />{c.generated(generated)}</p>}
    <SubjectTopicForm submitLabel={c.generate} busyLabel={c.generating}
      onSubmit={async v => { const deck = await flashcardsApi.generate(v); setGenerated(deck.added); load(); }} />
    <p className="quiet-note"><Link className="text-link" to={PATHS.quizzes}>{t.quiz.newTitle}</Link></p>
  </section>;
}
