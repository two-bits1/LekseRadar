// /progress и /history на настоящем API (API.md, «Дашборд и история»): GET /dashboard, GET /quizzes/progress, GET /history.
import { useEffect, useState } from 'react';
import { useAuth } from '../auth';
import { dashboardApi, quizzesApi } from '../api/endpoints';
import type { ApiError, Dashboard, HistoryEvent, QuizProgress } from '../api/types';
import { asApiError } from '../forms';
import { useLang, useT } from '../i18n';

export function Progress() {
  const t = useT(); const p = t.prog; const d = t.dashboard; const { tag } = useLang();
  const [data, setData] = useState<Dashboard | null>(null);
  const [quizzes, setQuizzes] = useState<QuizProgress[] | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  useEffect(() => {
    dashboardApi.get().then(setData).catch(e => setError(asApiError(e, t.common)));
    quizzesApi.progress().then(setQuizzes).catch(() => setQuizzes([]));
  }, [t.common]);

  const days = (data?.days ?? []).slice(-30);
  const maxEvents = Math.max(1, ...days.map(x => x.events));
  const dayLabel = (iso: string) => new Intl.DateTimeFormat(tag, { day: 'numeric', month: 'short' }).format(new Date(iso + 'T12:00:00'));

  return <section className="page narrow app-page">
    <header className="page-head"><h1>{p.title}</h1><p>{p.intro}</p></header>
    {error && <p role="alert" className="feedback danger">{error.message}</p>}
    {!data && !error && <p className="panel-empty">{t.common.loading}</p>}
    {data && <>
      <div className="dash-stats">
        <div className="stat"><span>{p.homework}</span><strong>{data.homeworkTodo + data.homeworkOverdue}</strong>
          <small>{data.homeworkOverdue} {p.overdue} · {data.homeworkDone} {p.done}</small></div>
        <div className="stat"><span>{p.quizzes}</span><strong>{data.quizzesCompleted}</strong>
          <small>{p.avg}: {data.avgQuizScore === null ? '—' : `${Math.round(data.avgQuizScore)} %`}</small></div>
        <div className="stat"><span>{p.cards}</span><strong>{data.flashcards.cards}</strong><small>{d.learned(data.flashcards.learned)}</small></div>
      </div>
      <div className="dash-stats">
        <div className="stat"><span>{p.answered}</span><strong>{data.exercisesAnswered}</strong></div>
        <div className="stat"><span>{p.asked}</span><strong>{data.questionsAsked}</strong></div>
        <div className="stat"><span>{p.activeDays}</span><strong>{data.activeDays30}</strong></div>
      </div>

      <h2 className="section-label">{p.topics}</h2>
      <section className="panel">
        {data.topics.length ? data.topics.map(x => <div key={x.topic} className="row row-static">
            <span><strong>{x.topic}</strong><small>{p.attempts(x.attempts)}</small></span>
            <span className="meter" aria-label={p.mastery(Math.round(x.masteryPct ?? 0))}><span style={{ width: `${x.masteryPct ?? 0}%` }} /></span>
            <span className="row-meta">{p.mastery(Math.round(x.masteryPct ?? 0))}</span></div>)
          : <p className="panel-empty">{p.noTopics}</p>}
      </section>

      {quizzes && quizzes.length > 0 && <><h2 className="section-label">{d.progress}</h2>
        <section className="panel">{quizzes.map(q => <div key={`${q.subject}|${q.topic}`} className="row row-static">
          <span><strong>{q.topic}</strong><small>{q.subject}</small></span>
          <span className="meter" aria-label={d.best(Math.round(q.bestScore ?? 0))}><span style={{ width: `${q.bestScore ?? 0}%` }} /></span>
          <span className="row-meta">{d.best(Math.round(q.bestScore ?? 0))}</span></div>)}</section></>}

      <h2 className="section-label">{p.days}</h2>
      <section className="panel">
        {days.length ? <div className="day-bars" role="list">{days.map(x => <div key={x.day} role="listitem" className="day-bar" title={`${dayLabel(x.day)}: ${x.events}`}>
            <span style={{ height: `${Math.max(6, (x.events / maxEvents) * 100)}%` }} /><small>{dayLabel(x.day)}</small></div>)}</div>
          : <p className="panel-empty">{p.noDays}</p>}
      </section>
    </>}
  </section>;
}

export function History() {
  const t = useT(); const hs = t.hist; const { tag } = useLang(); const { user } = useAuth();
  const [items, setItems] = useState<HistoryEvent[] | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  useEffect(() => { dashboardApi.history(100).then(setItems).catch(e => { setItems([]); setError(asApiError(e, t.common)); }); }, [t.common]);
  const fmt = (iso: string) => {
    try { return new Intl.DateTimeFormat(tag, { dateStyle: 'medium', timeStyle: 'short', timeZone: user?.timezone }).format(new Date(iso)); }
    catch { return new Date(iso).toLocaleString(tag); }
  };
  return <section className="page narrow app-page">
    <header className="page-head"><h1>{hs.title}</h1><p>{hs.intro}</p></header>
    {error && <p role="alert" className="feedback danger">{error.message}</p>}
    <section className="panel">
      {items === null ? <p className="panel-empty">{t.common.loading}</p>
        : items.length ? items.map(e => <div key={e.id} className="row row-static">
            <span><strong>{hs.types[e.type] ?? hs.other}</strong><small>{fmt(e.createdAt)}</small></span></div>)
        : <p className="panel-empty">{hs.none}</p>}
    </section>
  </section>;
}
