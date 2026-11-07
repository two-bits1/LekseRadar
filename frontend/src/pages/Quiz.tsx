// Квиз на настоящем API (API.md, «Квизы»): /quiz — новый квиз и мои квизы, /quiz/:quizId — прохождение.
import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, ChevronRight, CircleAlert, CircleCheck, CircleX, Layers, Sparkles } from 'lucide-react';
import { PATHS } from '../paths';
import { quizzesApi, subjectsApi } from '../api/endpoints';
import type { AnswerResult, ApiError, Level, Quiz, QuizSummary, Subject } from '../api/types';
import { Field, asApiError, fieldError, generalError } from '../forms';
import { Md } from '../components/Md';
import { useT } from '../i18n';
import { correctAnswers, encodeAnswer } from './quizLogic';

const LEVELS: Level[] = ['beginner', 'intermediate', 'advanced'];

// Общая форма «предмет + тема + уровень» — для квиза и для «Карточек от ИИ».
export function SubjectTopicForm({ submitLabel, busyLabel, onSubmit, withCount = false }: {
  submitLabel: string; busyLabel: string; withCount?: boolean;
  onSubmit: (v: { subject: string; topic: string; level: Level; count?: number }) => Promise<void>;
}) {
  const t = useT(); const q = t.quiz;
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [form, setForm] = useState({ subject: '', topic: '', level: 'intermediate' as Level, count: 8 });
  const [error, setError] = useState<ApiError | null>(null); const [busy, setBusy] = useState(false);
  useEffect(() => { subjectsApi.list().then(setSubjects).catch(() => {}); }, []);
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setError(null);
    try { await onSubmit({ subject: form.subject.trim(), topic: form.topic.trim(), level: form.level, ...(withCount ? { count: form.count } : {}) }); }
    catch (err) { setError(asApiError(err, t.common)); }
    finally { setBusy(false); }
  };
  return <form className="panel form-panel" onSubmit={submit} noValidate>
    <div className="form-row">
      <Field name="subject" label={q.subject} list="subjects" required maxLength={100} placeholder={q.subjectPlaceholder} value={form.subject} onChange={e => setForm(f => ({ ...f, subject: e.target.value }))} error={fieldError(error, 'subject')} />
      <datalist id="subjects">{subjects.map(s => <option key={s.id} value={s.name} />)}</datalist>
      <Field name="topic" label={q.topic} required maxLength={100} placeholder={q.topicPlaceholder} value={form.topic} onChange={e => setForm(f => ({ ...f, topic: e.target.value }))} error={fieldError(error, 'topic')} />
    </div>
    <div className="form-row">
      <fieldset className="seg"><legend>{q.level}</legend>{LEVELS.map(l => <button type="button" key={l} aria-pressed={form.level === l} onClick={() => setForm(f => ({ ...f, level: l }))}>{q.levels[l]}</button>)}</fieldset>
      {withCount && <label className="field">{q.count}<input type="number" min={1} max={30} value={form.count} onChange={e => setForm(f => ({ ...f, count: Math.min(30, Math.max(1, Number(e.target.value) || 1)) }))} /></label>}
    </div>
    {generalError(error) && <p role="alert" className="feedback danger">{generalError(error)}</p>}
    {busy && <p role="status" className="feedback hint"><span className="dots" aria-hidden="true"><i /><i /><i /></span>{busyLabel}</p>}
    <button className="btn btn-primary" disabled={busy || !form.subject.trim() || !form.topic.trim()}><Sparkles size={16} />{submitLabel}</button>
  </form>;
}

// /quiz: POST /quizzes (готовый набор — мгновенно, новый от ИИ — 3–30 с) и GET /quizzes.
export function QuizStart() {
  const t = useT(); const q = t.quiz; const navigate = useNavigate();
  const [mine, setMine] = useState<QuizSummary[] | null>(null);
  useEffect(() => { quizzesApi.list().then(setMine).catch(() => setMine([])); }, []);
  return <section className="page narrow app-page">
    <header className="page-head"><h1>{q.title}</h1><p>{q.intro}</p></header>
    <h2 className="section-label">{q.newTitle}</h2>
    <SubjectTopicForm withCount submitLabel={q.start} busyLabel={q.generating}
      onSubmit={async v => { const quiz = await quizzesApi.start(v); navigate(PATHS.quiz(quiz.id)); }} />
    <h2 className="section-label">{q.mine}</h2>
    <section className="panel">
      {mine === null ? <p className="panel-empty">{t.common.loading}</p>
        : mine.length ? mine.map(s => <Link key={s.id} to={PATHS.quiz(s.id)} className="row">
            <span><strong>{s.title}</strong><small>{[s.subject, s.topic, q.levels[s.level]].filter(Boolean).join(' · ')}</small></span>
            <span className="row-meta">{s.status === 'completed' && s.score !== null ? `${Math.round(s.score)} %` : q.status[s.status]}<ChevronRight size={15} /></span></Link>)
        : <p className="panel-empty">{q.none}</p>}
    </section>
  </section>;
}

// /quiz/:quizId: вопрос → ответ (POST answers) → пояснение → … → итог (POST finish) → «Учить карточками».
export function QuizRun() {
  const { quizId } = useParams(); const id = Number(quizId);
  const t = useT(); const q = t.quiz; const navigate = useNavigate();
  const [quiz, setQuiz] = useState<Quiz | null>(null); const [loadError, setLoadError] = useState<ApiError | null>(null);
  const [pos, setPos] = useState(1);
  const [answers, setAnswers] = useState<Record<number, string | string[]>>({});
  const [results, setResults] = useState<Record<number, AnswerResult>>({});
  const [busy, setBusy] = useState(false); const [error, setError] = useState<ApiError | null>(null);
  const [score, setScore] = useState<number | null>(null); const [added, setAdded] = useState<number | null>(null);

  useEffect(() => {
    // Переход с одного квиза на другой (назад/вперёд в браузере) — начинаем с чистого состояния.
    setQuiz(null); setLoadError(null); setAnswers({}); setResults({}); setError(null); setScore(null); setAdded(null);
    let off = false;
    quizzesApi.get(id).then(qz => { if (off) return; setQuiz(qz); setPos(Math.min(Math.max(qz.currentPosition, 1), qz.questions.length)); })
      .catch(e => { if (!off) setLoadError(asApiError(e, t.common)); });
    return () => { off = true; };
  }, [id]);

  if (loadError) return <section className="page narrow app-page"><h1>{loadError.status === 404 ? q.notFound : loadError.message}</h1><Link className="text-link" to={PATHS.quizzes}><ArrowLeft size={16} />{q.back}</Link></section>;
  if (!quiz) return <section className="page narrow app-page"><p className="quiet-note">{t.common.loading}</p></section>;

  const total = quiz.questions.length;
  const question = quiz.questions[pos - 1];
  const answer = answers[pos] ?? (question.type === 'multiple_choice' ? [] : '');
  const result = results[pos];
  const hasAnswer = Array.isArray(answer) ? answer.length > 0 : answer.trim().length > 0;
  const setAnswer = (v: string | string[]) => setAnswers(a => ({ ...a, [pos]: v }));
  const toggle = (opt: string) => setAnswer(Array.isArray(answer) && answer.includes(opt) ? answer.filter(x => x !== opt) : [...(answer as string[]), opt]);

  const check = async () => {
    setBusy(true); setError(null);
    try { const r = await quizzesApi.answer(quiz.id, { position: pos, answer: encodeAnswer(question.type, answer) }); setResults(rs => ({ ...rs, [pos]: r })); }
    catch (e) { setError(asApiError(e, t.common)); } finally { setBusy(false); }
  };
  const finish = async () => {
    setBusy(true); setError(null);
    try { setScore((await quizzesApi.finish(quiz.id)).score); } catch (e) { setError(asApiError(e, t.common)); } finally { setBusy(false); }
  };
  const toCards = async () => {
    setBusy(true); setError(null);
    try { setAdded((await quizzesApi.setToFlashcards(quiz.quizSetId)).added); } catch (e) { setError(asApiError(e, t.common)); } finally { setBusy(false); }
  };

  if (score !== null || quiz.status === 'completed') return <section className="page narrow app-page">
    <header className="page-head"><p className="dash-date">{quiz.title}</p><h1>{q.resultTitle}</h1>{score !== null && <p className="score">{q.result(Math.round(score))}</p>}</header>
    {error && <p role="alert" className="feedback danger">{error.message}</p>}
    {added !== null && <p role="status" className="feedback success"><Check size={17} />{q.toCardsDone(added)}</p>}
    <div className="dash-actions">
      {added === null ? <button className="btn btn-primary" onClick={toCards} disabled={busy}><Layers size={16} />{q.toCards}</button>
        : <Link className="btn btn-primary" to={PATHS.flashcards}><Layers size={16} />{t.nav.flashcards}</Link>}
      <Link className="btn" to={PATHS.quizzes}>{q.again}</Link>
    </div>
  </section>;

  const VerdictIcon = result?.verdict === 'correct' ? CircleCheck : result?.verdict === 'partial' ? CircleAlert : CircleX;
  const tone = result?.verdict === 'correct' ? 'success' : result?.verdict === 'partial' ? 'warning' : 'danger';

  return <section className="page narrow app-page quiz">
    <Link className="text-link" to={PATHS.quizzes}><ArrowLeft size={16} />{q.back}</Link>
    <header className="quiz-head"><p className="dash-date">{[quiz.subject, quiz.topic, q.levels[quiz.level]].filter(Boolean).join(' · ')}</p>
      <div className="quiz-steps" aria-label={q.question(pos, total)}>{quiz.questions.map(x => <span key={x.position} className={x.position === pos ? 'on' : results[x.position] ? `done ${results[x.position].verdict}` : ''} />)}</div></header>
    <article className="panel question">
      <p className="dash-date">{q.question(pos, total)} · {question.type === 'multiple_choice' ? q.chooseMany : question.type === 'single_choice' ? q.chooseOne : q.yourAnswer}</p>
      <div className="question-text"><Md>{question.question}</Md></div>
      {question.type === 'text'
        ? <label className="field"><span className="sr-only">{q.yourAnswer}</span><input value={answer as string} disabled={!!result} onChange={e => setAnswer(e.target.value)} onKeyDown={e => { if (e.key === 'Enter' && hasAnswer && !result) check(); }} placeholder={q.yourAnswer} /></label>
        : <div className="options" role={question.type === 'single_choice' ? 'radiogroup' : 'group'}>{(question.options ?? []).map(opt => {
            const picked = Array.isArray(answer) ? answer.includes(opt) : answer === opt;
            const right = result && correctAnswers(question.type, result.correctAnswer).includes(opt);
            return <button key={opt} type="button" role={question.type === 'single_choice' ? 'radio' : 'checkbox'} aria-checked={picked} disabled={!!result}
              className={`option${picked ? ' picked' : ''}${right ? ' right' : ''}`} onClick={() => question.type === 'single_choice' ? setAnswer(opt) : toggle(opt)}>
              <span className="option-mark" aria-hidden="true">{picked && <Check size={13} />}</span><Md inline>{opt}</Md></button>;
          })}</div>}
      {result && <div role="status" className={`feedback ${tone} verdict`}><VerdictIcon size={18} /><div>
        <strong>{q.verdict[result.verdict]}</strong>
        {result.verdict !== 'correct' && <p>{q.correctAnswer}: {correctAnswers(question.type, result.correctAnswer).map((a, i) => <span key={i}>{i > 0 && ', '}<Md inline>{a}</Md></span>)}</p>}
        {result.explanation && <div className="explanation"><Md>{result.explanation}</Md></div>}
      </div></div>}
      {error && <p role="alert" className="feedback danger">{error.message}</p>}
      <div className="quiz-actions">
        {pos > 1 && <button className="btn" onClick={() => setPos(pos - 1)}>{q.prev}</button>}
        {!result ? <button className="btn btn-primary" onClick={check} disabled={!hasAnswer || busy}>{busy ? q.checking : q.check}</button>
          : pos < total ? <button className="btn btn-primary" onClick={() => setPos(pos + 1)}>{q.next}<ChevronRight size={16} /></button>
          : <button className="btn btn-primary" onClick={finish} disabled={busy}>{busy ? q.finishing : q.finish}</button>}
      </div>
    </article>
  </section>;
}
