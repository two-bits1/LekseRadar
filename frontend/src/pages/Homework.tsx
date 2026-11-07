// /homework: ДЗ со всех платформ на настоящем API (API.md, «Домашка»): GET /homework, GET/PATCH /homework/{id}.
// Из задания можно сразу открыть чат с репетитором (POST /chats с assignmentId) или квиз по нему (POST /quizzes).
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronDown, ChevronUp, ExternalLink, ListChecks, Sparkles, Undo2, Check } from 'lucide-react';
import { PATHS } from '../paths';
import { useAuth } from '../auth';
import { chatsApi, homeworkApi, quizzesApi } from '../api/endpoints';
import type { ApiError, Assignment, AssignmentStatus } from '../api/types';
import { asApiError } from '../forms';
import { Md } from '../components/Md';
import { useLang, useT } from '../i18n';

type Filter = 'all' | 'todo' | 'overdue' | 'submitted';
const FILTERS: Filter[] = ['all', 'todo', 'overdue', 'submitted'];
const PLATFORM: Record<Assignment['source'], string> = { canvas: 'Canvas', teams: 'Teams', itslearning: 'itslearning', inschool: 'Visma InSchool' };

export const matchesFilter = (a: Assignment, f: Filter) =>
  f === 'all' || a.status === f || (f === 'submitted' && a.status === 'graded');

export function Homework() {
  const t = useT(); const h = t.hw; const { tag } = useLang(); const { user } = useAuth(); const navigate = useNavigate();
  const [items, setItems] = useState<Assignment[] | null>(null);
  const [filter, setFilter] = useState<Filter>('all');
  const [open, setOpen] = useState<number | null>(null);
  const [full, setFull] = useState<Record<number, string>>({});
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<ApiError | null>(null);

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
    const quiz = await quizzesApi.start({ subject: (a.course || PLATFORM[a.source]).slice(0, 100), topic: a.title.slice(0, 100), assignmentId: a.id, count: 8 });
    navigate(PATHS.quiz(quiz.id));
  });

  const shown = (items ?? []).filter(a => matchesFilter(a, filter));
  const count = (f: Filter) => (items ?? []).filter(a => matchesFilter(a, f)).length;
  const pillClass = (s: AssignmentStatus) => `pill hw-${s}`;

  return <section className="page narrow app-page">
    <header className="page-head"><h1>{h.title}</h1><p>{h.intro}</p></header>
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
