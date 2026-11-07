// /homework: ДЗ со всех платформ на настоящем API (API.md, «Домашка»): GET /homework, GET/PATCH /homework/{id}.
// Из задания можно сразу открыть чат с репетитором (POST /chats с assignmentId) или квиз по нему (POST /quizzes).
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronDown, ChevronUp, ExternalLink, ListChecks, RefreshCw, Sparkles, Undo2, Check } from 'lucide-react';
import { PATHS } from '../paths';
import { useAuth } from '../auth';
import { chatsApi, homeworkApi, quizzesApi } from '../api/endpoints';
import type { ApiError, Assignment, AssignmentStatus, SourceStatus, SyncResult } from '../api/types';
import { isApiError } from '../api/client';
import { platformError } from '../components/Platforms';
import { asApiError } from '../forms';
import { Md } from '../components/Md';
import { useLang, useT } from '../i18n';

type Filter = 'all' | 'todo' | 'overdue' | 'submitted';
const FILTERS: Filter[] = ['all', 'todo', 'overdue', 'submitted'];
const PLATFORM: Record<Assignment['source'], string> = {
  canvas: 'Canvas', teams: 'Teams', itslearning: 'itslearning', inschool: 'Visma InSchool', classroom: 'Google Classroom',
};
// Предмет для «Lag quiz»: курс, а если его нет (бывает у Classroom) — название платформы.
export const quizSubject = (a: Assignment) => (a.course || PLATFORM[a.source] || a.source).slice(0, 100);

export const matchesFilter = (a: Assignment, f: Filter) =>
  f === 'all' || a.status === f || (f === 'submitted' && a.status === 'graded');

// Сначала то, что надо сделать (просроченное, потом по сроку), сданное и оценённое — в конце, новые сверху.
const RANK: Record<AssignmentStatus, number> = { overdue: 0, todo: 1, submitted: 2, graded: 2 };
const time = (a: Assignment) => a.dueAt ? Date.parse(a.dueAt) : Number.POSITIVE_INFINITY;
export const sortHomework = (list: Assignment[]) => [...list].sort((a, b) =>
  RANK[a.status] - RANK[b.status] || (RANK[a.status] < 2 ? time(a) - time(b) : time(b) - time(a)) || a.id - b.id);

// Итог POST /homework/sync для строки под кнопкой «Oppdater».
export type SyncSummary = { none: true } | { none: false; added: number; updated: number; failed: SourceStatus[] };
export const syncSummary = (r: SyncResult): SyncSummary =>
  r.sources.length ? { none: false, added: r.added, updated: r.updated, failed: r.sources.filter(s => !s.ok) } : { none: true };

export function Homework() {
  const t = useT(); const h = t.hw; const { tag } = useLang(); const { user } = useAuth(); const navigate = useNavigate();
  const [items, setItems] = useState<Assignment[] | null>(null);
  const [filter, setFilter] = useState<Filter>('all');
  const [open, setOpen] = useState<number | null>(null);
  const [full, setFull] = useState<Record<number, string>>({});
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [sync, setSync] = useState<SyncSummary | null>(null); const [syncing, setSyncing] = useState(false); const [syncError, setSyncError] = useState<string | null>(null);

  useEffect(() => { homeworkApi.list().then(setItems).catch(e => { setItems([]); setError(asApiError(e, t.common)); }); }, [t.common]);

  const fmt = (iso: string) => {
    try { return new Intl.DateTimeFormat(tag, { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: user?.timezone }).format(new Date(iso)); }
    catch { return new Date(iso).toLocaleString(tag); }
  };

  const toggle = (a: Assignment) => {
    setOpen(open === a.id ? null : a.id);
    if (full[a.id] === undefined) homeworkApi.get(a.id).then(x => setFull(f => ({ ...f, [a.id]: x.description }))).catch(() => {});
  };
  const run = async (key: string, fn: () => Promise<void>) => {
    setBusy(key); setError(null);
    try { await fn(); } catch (e) { setError(asApiError(e, t.common)); } finally { setBusy(null); }
  };
  const setStatus = (a: Assignment, status: 'todo' | 'submitted') => run(`s${a.id}`, async () => {
    const updated = await homeworkApi.update(a.id, { status });
    setItems(list => list && list.map(x => x.id === a.id ? updated : x));
  });
  const askTutor = (a: Assignment) => run(`c${a.id}`, async () => {
    const chat = await chatsApi.create({ mode: 'tutor', assignmentId: a.id, subject: a.course || undefined, topic: a.title.slice(0, 100) });
    navigate(PATHS.chat(chat.id));
  });
  const makeQuiz = (a: Assignment) => run(`q${a.id}`, async () => {
    const quiz = await quizzesApi.start({ subject: quizSubject(a), topic: a.title.slice(0, 100), assignmentId: a.id, count: 8 });
    navigate(PATHS.quiz(quiz.id));
  });

  // «Oppdater»: сбор может идти до минуты; потом заново GET /homework.
  const refresh = async () => {
    setSyncing(true); setSync(null); setSyncError(null);
    try { setSync(syncSummary(await homeworkApi.sync())); setItems(await homeworkApi.list()); }
    catch (e) { setSyncError(isApiError(e) && e.status === 429 ? t.platforms.syncRecent : platformError(e, t)); }
    finally { setSyncing(false); }
  };
  const p = t.platforms;

  const shown = sortHomework((items ?? []).filter(a => matchesFilter(a, filter)));
  const count = (f: Filter) => (items ?? []).filter(a => matchesFilter(a, f)).length;
  const pillClass = (s: AssignmentStatus) => `pill hw-${s}`;

  return <section className="page narrow app-page">
    <header className="page-head hw-head"><div><h1>{h.title}</h1><p>{h.intro}</p></div>
      <button type="button" className="btn btn-sm" disabled={syncing} onClick={refresh}><RefreshCw size={15} className={syncing ? 'spin' : undefined} />{syncing ? p.syncing : p.sync}</button>
    </header>
    <div aria-live="polite">
      {sync?.none && <p className="feedback warning">{p.noPlatforms} <Link to={PATHS.platforms}>{p.toPlatforms}</Link></p>}
      {sync && !sync.none && <p className="feedback success"><Check size={16} />{p.syncDone(sync.added, sync.updated)}</p>}
      {sync && !sync.none && sync.failed.map(s => <p key={s.platform} role="alert" className="feedback danger">
        <strong>{PLATFORM[s.platform] ?? s.platform}:</strong> {s.error ?? p.failing}{s.needsReconnect && <> <Link to={PATHS.platforms}>{p.reconnectLink}</Link></>}</p>)}
      {syncError && <p role="alert" className="feedback danger">{syncError}</p>}
    </div>
    <div className="seg" role="group" aria-label={h.title}>
      {FILTERS.map(f => <button type="button" key={f} aria-pressed={filter === f} onClick={() => setFilter(f)}>{h.filters[f]}{items ? ` (${count(f)})` : ''}</button>)}
    </div>
    {error && <p role="alert" className="feedback danger">{error.message}</p>}
    <section className="panel">
      {items === null ? <p className="panel-empty">{t.common.loading}</p>
        : !items.length ? <p className="panel-empty">{h.noneAll} <Link to={PATHS.platforms}>{h.connect}</Link></p>
        : !shown.length ? <p className="panel-empty">{h.none}</p>
        : shown.map(a => <article key={a.id} className="hw-item">
            <button type="button" className="row hw-row" aria-expanded={open === a.id} onClick={() => toggle(a)}>
              <span><strong>{a.title}</strong><small>{[a.course, PLATFORM[a.source], a.dueAt ? h.due(fmt(a.dueAt)) : h.noDue].filter(Boolean).join(' · ')}</small></span>
              <span className="row-meta"><span className={pillClass(a.status)}>{h.status[a.status]}</span>{open === a.id ? <ChevronUp size={15} /> : <ChevronDown size={15} />}</span>
            </button>
            {open === a.id && <div className="hw-body">
              {full[a.id] === undefined ? <p className="panel-empty">{h.loadFull}</p> : full[a.id] ? <Md>{full[a.id]}</Md> : null}
              <div className="actions wrap">
                <button className="btn btn-primary btn-sm" disabled={!!busy} onClick={() => askTutor(a)}><Sparkles size={15} />{busy === `c${a.id}` ? h.starting : h.askTutor}</button>
                <button className="btn btn-sm" disabled={!!busy} onClick={() => makeQuiz(a)}><ListChecks size={15} />{busy === `q${a.id}` ? t.quiz.generating : h.makeQuiz}</button>
                {a.status === 'submitted'
                  ? <button className="btn btn-sm" disabled={!!busy} onClick={() => setStatus(a, 'todo')}><Undo2 size={15} />{h.markTodo}</button>
                  : a.status !== 'graded' && <button className="btn btn-sm" disabled={!!busy} onClick={() => setStatus(a, 'submitted')}><Check size={15} />{h.markDone}</button>}
                {a.url && /^https?:\/\//.test(a.url) && <a className="btn btn-sm" href={a.url} target="_blank" rel="noopener noreferrer"><ExternalLink size={15} />{h.open}</a>}
              </div>
            </div>}
          </article>)}
    </section>
  </section>;
}
