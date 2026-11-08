// Флеш-карты на настоящем API (API.md, «Флеш-карты»): выбор колоды (к повторению, трудные, уже знаю, новые, все;
// тема; перемешать) → переворот → оценка 0–5 (SM-2). Какие карточки попадут в колоду — flashcardDeck.ts.
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, ListChecks, RotateCcw, Shuffle } from 'lucide-react';
import { PATHS } from '../paths';
import { flashcardsApi } from '../api/endpoints';
import type { ApiError, Flashcard, FlashcardStats } from '../api/types';
import { asApiError } from '../forms';
import { Md } from '../components/Md';
import { ResultCard } from '../components/ResultCard';
import { useT } from '../i18n';
import { GRADES } from './quizLogic';
import { SubjectTopicForm } from './Quiz';
import { ALL_TOPICS, buildDeck, counts, topicKey, topics, type DeckMode } from './flashcardDeck';

/** Счётчик над карточкой → какую колоду он открывает. */
const STAT_MODES = [['all', 'statsCards'], ['due', 'statsDue'], ['learned', 'statsLearned'], ['hard', 'statsHard'], ['new', 'statsNew']] as const satisfies readonly (readonly [DeckMode, string])[];

type GradeKey = keyof typeof GRADES;

export function Flashcards() {
  const t = useT(); const c = t.cards;
  const [stats, setStats] = useState<FlashcardStats | null>(null);
  const [all, setAll] = useState<Flashcard[] | null>(null);   // вся колода с прогрессом
  const [due, setDue] = useState<Flashcard[] | null>(null);   // карточки этого занятия
  const [mode, setMode] = useState<DeckMode>('due');
  const [topic, setTopic] = useState<string>(ALL_TOPICS);
  const [shuffle, setShuffle] = useState(false);
  const [index, setIndex] = useState(0); const [flipped, setFlipped] = useState(false);
  const [busy, setBusy] = useState(false); const [error, setError] = useState<ApiError | null>(null);
  const [generated, setGenerated] = useState<number | null>(null);
  const [session, setSession] = useState<Record<GradeKey, number>>({ again: 0, hard: 0, good: 0, easy: 0 });  // оценки за это занятие

  // Новое занятие: колода заново с сервера (прогресс после оценок свежий), потом отбор по режиму и теме.
  const load = useCallback((m: DeckMode = mode, tp: string = topic, sh: boolean = shuffle) => {
    setIndex(0); setFlipped(false); setSession({ again: 0, hard: 0, good: 0, easy: 0 });
    flashcardsApi.stats().then(setStats).catch(() => {});
    flashcardsApi.list().then(cards => {
      const tps = topics(cards).map(x => x.key);
      const tp2 = tp === ALL_TOPICS || tps.includes(tp) ? tp : ALL_TOPICS;
      setAll(cards); setTopic(tp2); setDue(buildDeck(cards, m, tp2, sh));
    }).catch(e => { setAll([]); setDue([]); setError(asApiError(e, t.common)); });
  }, [mode, topic, shuffle]);
  useEffect(() => { load(); }, []);  // eslint-disable-line react-hooks/exhaustive-deps
  const choose = (next: { mode?: DeckMode; topic?: string; shuffle?: boolean }) => {
    const m = next.mode ?? mode, tp = next.topic ?? topic, sh = next.shuffle ?? shuffle;
    setMode(m); setTopic(tp); setShuffle(sh); setError(null); load(m, tp, sh);
  };
  const modeCounts = useMemo(() => all ? counts(all, topic) : null, [all, topic]);
  // Повторений — по выбранной теме (по всей колоде — как в статистике сервера)
  const reviews = useMemo(() => !all || topic === ALL_TOPICS ? stats?.reviews
    : all.filter(x => topicKey(x) === topic).reduce((n, x) => n + (x.reviews ?? 0), 0), [all, topic, stats]);
  const topicList = useMemo(() => all ? topics(all) : [], [all]);

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
    {/* Счётчики — они же выбор колоды: «выучено» — повторить то, что знаешь, «трудных» — то, что не запоминается */}
    <div className="mini-stats deck-stats" role="group" aria-label={c.statsPick}>
      {STAT_MODES.map(([m, label]) => {
        const n = modeCounts ? modeCounts[m] : m === 'all' ? stats?.cards : m === 'due' ? stats?.dueNow : m === 'learned' ? stats?.learned : undefined;
        return <button key={m} type="button" className={`stat-pick${m === 'due' && n ? ' hot' : ''}`} aria-pressed={mode === m}
          disabled={!all?.length} onClick={() => choose({ mode: m })}><strong>{n ?? '—'}</strong>{c[label]}</button>;
      })}
      <span><strong>{reviews ?? '—'}</strong>{c.statsReviews}</span>
    </div>
    {error && <p role="alert" className="feedback danger">{error.message}</p>}

    {all && all.length > 0 && <div className="deck-picker">
      <div className="deck-head">
        <h2>{c.practice}</h2>
        <div className="deck-tools">
          {topicList.length > 1 && <label className="deck-topic"><span className="sr-only">{c.topic}</span>
            <select value={topic} onChange={e => choose({ topic: e.target.value })}>
              <option value={ALL_TOPICS}>{c.allTopics} ({all.length})</option>
              {topicList.map(x => <option key={x.key} value={x.key}>{x.key || '—'} ({x.count})</option>)}
            </select></label>}
          <button type="button" className="deck-shuffle" aria-pressed={shuffle} onClick={() => choose({ shuffle: !shuffle })}><Shuffle size={15} />{c.shuffle}</button>
        </div>
      </div>
      <p className="deck-hint">{c.modeHints[mode]}</p>
    </div>}

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
          footer={<><button className="btn btn-primary" onClick={() => load()}><RotateCcw size={16} />{mode === 'due' ? c.more : c.again}</button>
            <Link className="btn" to={PATHS.quizzes}><ListChecks size={16} />{t.quiz.newTitle}</Link>
            <Link className="btn" to={PATHS.home}>{c.toHome}</Link></>} />
      : all && all.length > 0 ? <p className={`feedback ${mode === 'due' ? 'success' : 'hint'}`}><Check size={17} />{due.length ? c.done : c.modeEmpty[mode]}</p>
      : <p className="feedback success"><Check size={17} />{c.empty}</p>}

    <h2 className="section-label">{c.generateTitle}</h2>
    <p className="section-sub">{c.generateIntro}</p>
    {generated !== null && <p role="status" className="feedback success"><Check size={17} />{c.generated(generated)}</p>}
    <SubjectTopicForm submitLabel={c.generate} busyLabel={c.generating}
      onSubmit={async v => { const deck = await flashcardsApi.generate(v); setGenerated(deck.added); load(); }} />
    <p className="quiet-note"><Link className="text-link" to={PATHS.quizzes}>{t.quiz.newTitle}</Link></p>
  </section>;
}
