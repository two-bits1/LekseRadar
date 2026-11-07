// Главная «/» после входа — только настоящие данные (API.md): профиль, чаты, квизы, прогресс, флеш-карты.
// ДЗ — GET /homework: ближайшие несданные задания.
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Inbox, ListChecks, MessageSquare, Sparkles } from 'lucide-react';
import { PATHS } from '../paths';
import { useAuth } from '../auth';
import { chatsApi, flashcardsApi, homeworkApi, quizzesApi } from '../api/endpoints';
import type { Assignment, Chat, FlashcardStats, QuizProgress, QuizSummary } from '../api/types';
import { useLang, useT } from '../i18n';

type Data = { stats: FlashcardStats | null; progress: QuizProgress[] | null; quizzes: QuizSummary[] | null; chats: Chat[] | null; homework: Assignment[] | null };

export function Dashboard() {
  const { user } = useAuth(); const t = useT(); const d = t.dashboard; const { tag } = useLang();
  const [data, setData] = useState<Data>({ stats: null, progress: null, quizzes: null, chats: null, homework: null });
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let off = false;
    Promise.allSettled([flashcardsApi.stats(), quizzesApi.progress(), quizzesApi.list(), chatsApi.list(false), homeworkApi.list()]).then(([s, p, q, c, h]) => {
      if (off) return;
      const val = <T,>(r: PromiseSettledResult<T>) => r.status === 'fulfilled' ? r.value : null;
      setData({ stats: val(s), progress: val(p), quizzes: val(q), chats: val(c), homework: val(h) });
      setFailed([s, p, q, c, h].every(r => r.status === 'rejected'));
    });
    return () => { off = true; };
  }, []);

  let today = '';
  try { today = new Intl.DateTimeFormat(tag, { weekday: 'long', day: 'numeric', month: 'long', timeZone: user?.timezone }).format(new Date()); }
  catch { today = new Intl.DateTimeFormat(tag, { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date()); }
  const inProgress = (data.quizzes ?? []).filter(q => q.status === 'in_progress').slice(0, 4);
  const completed = (data.progress ?? []).reduce((n, p) => n + p.quizzesCompleted, 0);
  const progress = [...(data.progress ?? [])].filter(p => p.bestScore !== null).sort((a, b) => b.lastActivity.localeCompare(a.lastActivity)).slice(0, 5);
  const num = (v: number | undefined | null) => v ?? '—';
  const nextHomework = (data.homework ?? []).filter(a => a.status === 'todo' || a.status === 'overdue').slice(0, 4);
  const dueFmt = (iso: string) => {
    try { return new Intl.DateTimeFormat(tag, { weekday: 'short', day: 'numeric', month: 'short', timeZone: user?.timezone }).format(new Date(iso)); }
    catch { return new Date(iso).toLocaleDateString(tag); }
  };

  return <section className="page dash">
    <header className="dash-head">
      <div>
        <p className="dash-date"><span className="cap-first">{today}</span></p>
        <h1>{d.hello(user?.displayName ?? d.student)}</h1>
        <p className="dash-sub">{d.intro}</p>
      </div>
      <div className="dash-actions">
        <Link className="btn btn-primary" to={PATHS.chats}><Sparkles size={16} />{d.askTutor}</Link>
        <Link className="btn" to={PATHS.quizzes}>{d.newQuiz}</Link>
        <Link className="btn" to={PATHS.flashcards}>{d.review}</Link>
      </div>
    </header>
    {failed && <p role="alert" className="feedback danger">{d.loadFailed}</p>}

    <div className="dash-stats">
      <Link to={PATHS.flashcards} className={`stat${data.stats?.dueNow ? ' stat-hot' : ''}`}><span>{d.due}</span><strong>{num(data.stats?.dueNow)}</strong></Link>
      <div className="stat"><span>{d.cards}</span><strong>{num(data.stats?.cards)}</strong><small>{d.learned(data.stats?.learned ?? 0)}</small></div>
      <Link to={PATHS.quizzes} className="stat"><span>{d.quizzesDone}</span><strong>{data.progress ? completed : '—'}</strong></Link>
    </div>

    <div className="dash-grid">
      <section className="panel">
        <div className="panel-head"><h2><ListChecks size={16} />{d.inProgress}</h2><Link to={PATHS.quizzes}>{d.newQuiz}</Link></div>
        {data.quizzes === null ? <p className="panel-empty">{t.common.loading}</p>
          : inProgress.length ? inProgress.map(q => <Link key={q.id} to={PATHS.quiz(q.id)} className="row">
              <span><strong>{q.title}</strong><small>{[q.subject, q.topic].filter(Boolean).join(' · ')}</small></span>
              <span className="row-meta">{d.question(q.currentPosition, q.questionCount)}<ChevronRight size={15} /></span></Link>)
          : <p className="panel-empty">{d.noInProgress}</p>}
      </section>

      <section className="panel">
        <div className="panel-head"><h2><MessageSquare size={16} />{d.recentChats}</h2><Link to={PATHS.chats}>{d.allChats}</Link></div>
        {data.chats === null ? <p className="panel-empty">{t.common.loading}</p>
          : data.chats.length ? data.chats.slice(0, 5).map(c => <Link key={c.id} to={PATHS.chat(c.id)} className="row">
              <span><strong>{c.title}</strong><small>{[c.subject, c.topic].filter(Boolean).join(' · ') || t.nav.tutor}</small></span>
              <ChevronRight size={15} className="row-meta" /></Link>)
          : <p className="panel-empty">{d.noChats} <Link to={PATHS.chats}>{d.askTutor}</Link></p>}
      </section>

      <section className="panel">
        <div className="panel-head"><h2>{d.progress}</h2></div>
        {data.progress === null ? <p className="panel-empty">{t.common.loading}</p>
          : progress.length ? progress.map(p => <div key={`${p.subject}|${p.topic}`} className="row row-static">
              <span><strong>{p.topic}</strong><small>{p.subject}</small></span>
              <span className="meter" aria-label={d.best(Math.round(p.bestScore ?? 0))}><span style={{ width: `${p.bestScore ?? 0}%` }} /></span>
              <span className="row-meta">{d.best(Math.round(p.bestScore ?? 0))}</span></div>)
          : <p className="panel-empty">{d.noProgress}</p>}
      </section>

      <section className="panel">
        <div className="panel-head"><h2><Inbox size={16} />{d.homework}</h2><Link to={PATHS.homework}>{t.hw.all}</Link></div>
        {data.homework === null ? <p className="panel-empty">{t.common.loading}</p>
          : nextHomework.length ? nextHomework.map(a => <Link key={a.id} to={PATHS.homework} className="row">
              <span><strong>{a.title}</strong><small>{[a.course, a.dueAt ? t.hw.due(dueFmt(a.dueAt)) : t.hw.noDue].filter(Boolean).join(' · ')}</small></span>
              <span className="row-meta"><span className={`pill hw-${a.status}`}>{t.hw.status[a.status]}</span><ChevronRight size={15} /></span></Link>)
          : <p className="panel-empty">{data.homework.length ? t.hw.none : t.hw.noneAll}</p>}
      </section>
    </div>
  </section>;
}
