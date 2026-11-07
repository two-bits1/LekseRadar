// ИИ-чат на настоящем API (API.md, «ИИ-чат»): /chat — список, /chat/:chatId — переписка.
import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import Markdown from 'react-markdown';
import { Archive, ArchiveRestore, ArrowLeft, ArrowUp, ArrowUpRight, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { Orb } from '../components/Orb';
import { setOrbThinking } from '../components/orbState';
import { assignments } from '../data';
import { PATHS } from '../paths';
import { useAuth } from '../auth';
import { chatsApi } from '../api/endpoints';
import type { ApiError, Chat, CreateChatRequest, Message, UpdateChatRequest } from '../api/types';
import { Field, asApiError } from '../forms';
import { useLang, useT } from '../i18n';

const MAX_MESSAGE = 8000;  // как @Size в SendMessageRequest
const MAX_TITLE = 200;

// Даты на языке интерфейса и в часовом поясе из профиля.
function useDateFormat() {
  const { user } = useAuth();
  const { tag } = useLang();
  return (iso: string) => {
    try { return new Intl.DateTimeFormat(tag, { dateStyle: 'medium', timeStyle: 'short', timeZone: user?.timezone }).format(new Date(iso)); }
    catch { return new Date(iso).toLocaleString(tag); }
  };
}

// /chat: GET /chats?archived=false|true, «Ny samtale» → POST /chats.
// /chat?assignment=… (кнопка «Lær med AI» у задания) → сразу новый чат по заданию.
export function ChatList() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const fmt = useDateFormat();
  const t = useT(); const c = t.chat;
  const [archived, setArchived] = useState(false);
  const [chats, setChats] = useState<Chat[] | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let off = false;
    setChats(null); setError(null);
    chatsApi.list(archived).then(c => { if (!off) setChats(c); }).catch(e => { if (!off) setError(asApiError(e, t.common)); });
    return () => { off = true; };
  }, [archived]);

  const start = async (body: CreateChatRequest = {}, replace = false) => {
    setCreating(true); setError(null);
    try { const chat = await chatsApi.create(body); navigate(PATHS.chat(chat.id), { replace }); }
    catch (e) { setError(asApiError(e, t.common)); setCreating(false); }
  };

  const assignment = assignments.find(a => a.id === params.get('assignment'));
  const started = useRef(false);  // StrictMode вызывает эффект дважды — чат должен создаться один раз
  useEffect(() => {
    if (!assignment || started.current) return;
    started.current = true;
    start({ title: assignment.title, topic: assignment.subject }, true);
  }, [assignment]);

  return <section className="page narrow">
    <div className="page-intro"><Orb /><div className="eyebrow">{c.eyebrow}</div><h1>{c.title}</h1><p>{c.intro}</p></div>
    <div className="study-toolbar">
      <div className="filters">{[false, true].map(a => <button key={String(a)} aria-pressed={archived === a} className={archived === a ? 'selected' : ''} onClick={() => setArchived(a)}>{a ? c.archive : c.active}</button>)}</div>
      <button className="button dark-button" onClick={() => start()} disabled={creating}><Plus size={16} />{creating ? c.starting : c.newChat}</button>
    </div>
    {assignment && creating && <p role="status" className="quiet-note">{c.startingAbout(assignment.title)}</p>}
    {error && <p role="alert" className="feedback danger">{error.message}</p>}
    <article className="lesson-card">
      {chats === null ? !error && <p>{c.loadingList}</p>
        : chats.length ? chats.map(c => <Link key={c.id} className="route-row chat-row" to={PATHS.chat(c.id)}><span><strong>{c.title}</strong>{c.topic && <small>{c.topic}</small>}</span><span>{fmt(c.updatedAt)}<ArrowUpRight size={15} /></span></Link>)
        : <p>{archived ? c.emptyArchive : c.empty}</p>}
    </article>
  </section>;
}

// /chat/:chatId: GET /chats/{id} + /messages, отправка, переименовать / в архив / удалить.
export function ChatView() {
  const { chatId } = useParams();
  const id = Number(chatId);
  const navigate = useNavigate();
  const fmt = useDateFormat();
  const t = useT(); const c = t.chat;
  const [chat, setChat] = useState<Chat | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loadError, setLoadError] = useState<ApiError | null>(null);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState<string | null>(null);  // вопрос, на который ждём ответ
  const [sendError, setSendError] = useState<{ content: string; error: ApiError } | null>(null);
  const [actionError, setActionError] = useState<ApiError | null>(null);
  const [renaming, setRenaming] = useState<string | null>(null);
  const bottom = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setChat(null); setMessages([]); setLoadError(null); setSendError(null); setActionError(null); setRenaming(null);
    if (!Number.isInteger(id)) { setLoadError({ status: 404, message: c.notFound, fields: null, timestamp: '' }); return; }
    let off = false;
    Promise.all([chatsApi.get(id), chatsApi.messages(id)])
      .then(([c, m]) => { if (!off) { setChat(c); setMessages(m); } })
      .catch(e => { if (!off) setLoadError(asApiError(e, t.common)); });
    return () => { off = true; };
  }, [id]);

  useEffect(() => { bottom.current?.scrollIntoView({ block: 'end' }); }, [messages.length, sending, sendError]);
  useEffect(() => () => setOrbThinking(false), []);  // ушли со страницы, пока ждали ответ — шар успокаивается

  // Ответ ИИ идёт 2–15 с. 502 → вопрос уже сохранён на сервере, подтягиваем историю и даём «Prøv igjen».
  // NB: повтор по API.md = тот же POST ещё раз, поэтому в истории будет дубль вопроса (пункт 12 аудита, нужен фикс на бэкенде).
  const send = async (content: string) => {
    setSending(content); setSendError(null); setOrbThinking(true);
    try {
      const reply = await chatsApi.send(id, content);
      setChat(reply.chat);
      setMessages(ms => [...ms, reply.userMessage, reply.assistantMessage]);
    } catch (e) {
      const error = asApiError(e, t.common);
      setSendError({ content, error });
      if (error.status === 502) chatsApi.messages(id).then(setMessages).catch(() => {});
      else setDraft(d => d || content);  // вопрос не сохранился — возвращаем текст в поле
    } finally { setSending(null); setOrbThinking(false); }
  };

  const submit = (e?: FormEvent) => {
    e?.preventDefault();
    const content = draft.trim();
    if (!content || sending || content.length > MAX_MESSAGE) return;
    setDraft(''); send(content);
  };
  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => { if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) { e.preventDefault(); submit(); } };

  const update = async (body: UpdateChatRequest) => {
    setActionError(null);
    try { setChat(await chatsApi.update(id, body)); setRenaming(null); } catch (e) { setActionError(asApiError(e, t.common)); }
  };
  const remove = async () => {
    if (!chat || !window.confirm(c.confirmDelete(chat.title))) return;
    try { await chatsApi.remove(id); navigate(PATHS.chats, { replace: true }); } catch (e) { setActionError(asApiError(e, t.common)); }
  };

  if (loadError) return <section className="page narrow"><div className="eyebrow">{c.tutorEyebrow}</div><h1>{loadError.status === 404 ? c.notFound : c.failed}</h1><p>{loadError.message}</p><Link className="text-link" to={PATHS.chats}><ArrowLeft size={16} />{c.allChats}</Link></section>;
  if (!chat) return <section className="page narrow"><p className="quiet-note">{c.loading}</p></section>;

  return <section className="page narrow chat-page">
    <Link className="text-link" to={PATHS.chats}><ArrowLeft size={16} />{c.allChats}</Link>
    <div className="chat-header">
      {renaming === null
        ? <div><div className="eyebrow">{c.tutorEyebrow}{chat.archived && ` · ${c.archived}`}</div><h1 className="chat-title">{chat.title}</h1>{chat.topic && <p>{chat.topic}</p>}</div>
        : <form className="rename" onSubmit={e => { e.preventDefault(); if (renaming.trim()) update({ title: renaming.trim() }); }}>
            <Field name="title" label={c.newName} value={renaming} maxLength={MAX_TITLE} autoFocus onChange={e => setRenaming(e.target.value)} error={actionError?.fields?.title} />
            <div className="actions"><button type="submit">{c.save}</button><button type="button" onClick={() => setRenaming(null)}>{c.cancel}</button></div>
          </form>}
      {renaming === null && <div className="actions wrap">
        <button onClick={() => setRenaming(chat.title)}><Pencil size={15} />{c.rename}</button>
        <button onClick={() => update({ archived: !chat.archived })}>{chat.archived ? <><ArchiveRestore size={15} />{c.unarchive}</> : <><Archive size={15} />{c.archiveAction}</>}</button>
        <button onClick={remove}><Trash2 size={15} />{c.remove}</button>
      </div>}
    </div>
    {actionError && !actionError.fields && <p role="alert" className="feedback danger">{actionError.message}</p>}

    <div className="chat-thread" aria-live="polite">
      {messages.length === 0 && !sending && <p className="quiet-note">{c.emptyThread}</p>}
      {messages.map(m => <div key={m.id} className={`msg ${m.role}`}>
        {m.role === 'tutor'
          ? <Markdown components={{ a: ({ node, ...props }) => <a {...props} target="_blank" rel="noopener noreferrer" /> }}>{m.content}</Markdown>
          : <p>{m.content}</p>}
        <time dateTime={m.createdAt}>{m.role === 'tutor' ? `${t.nav.tutor} · ` : ''}{fmt(m.createdAt)}</time>
      </div>)}
      {sending && <><div className="msg student pending"><p>{sending}</p></div><div className="msg tutor typing" role="status"><span className="dots" aria-hidden="true"><i /><i /><i /></span>{c.typing}</div></>}
      {sendError && <div role="alert" className="feedback danger"><span>{sendError.error.message}</span>{sendError.error.status === 502 && <button onClick={() => send(sendError.content)}><RotateCcw size={14} />{c.retry}</button>}</div>}
      <div ref={bottom} className="chat-end" />
    </div>

    <form className="composer" onSubmit={submit}>
      <textarea aria-label={c.inputLabel} placeholder={c.placeholder} rows={2} maxLength={MAX_MESSAGE} value={draft} onChange={e => setDraft(e.target.value)} onKeyDown={onKeyDown} />
      <button className="button dark-button" aria-label={c.send} disabled={!draft.trim() || !!sending}><ArrowUp size={18} /></button>
    </form>
    <p className="composer-hint">{c.hint(draft.length, MAX_MESSAGE)}</p>
  </section>;
}
