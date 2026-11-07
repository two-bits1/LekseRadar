// Подключение школьных платформ на /settings/platforms.
// Canvas и InSchool — форма; Teams — вход по коду Microsoft (или токен вручную); itslearning — вход на сайте
// школы и вставка адреса (или токен вручную); Google Classroom — вход через Google (бэкенду нужна доработка).
// Секреты не сохраняются в браузере и не пишутся в консоль: только уходят на сервер, где их шифрует бэкенд.
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Check, Copy, ExternalLink, Link2, RefreshCw } from 'lucide-react';
import { homeworkApi, platformsApi } from '../api/endpoints';
import { isApiError } from '../api/client';
import type { PlatformConnection, PlatformKey, TeamsStart } from '../api/types';
import { Field, asApiError } from '../forms';
import { useAuth } from '../auth';
import { useLang, useT, type Dict } from '../i18n';
import { PATHS } from '../paths';
import { FIELDS, cardState, missingFields, oauthReturn, toCredentials, type FieldKey, type Values } from './platformLogic';

type Manual = Exclude<PlatformKey, 'classroom'>;
const PLATFORMS: { key: PlatformKey; name: string }[] = [
  { key: 'canvas', name: 'Canvas' },
  { key: 'teams', name: 'Microsoft Teams' },
  { key: 'itslearning', name: 'itslearning' },
  { key: 'inschool', name: 'Visma InSchool' },
  { key: 'classroom', name: 'Google Classroom' },
];
const SECRET: FieldKey[] = ['token', 'refresh_token', 'access_token', 'JSESSIONID', 'Authorization'];
const PLACEHOLDER: Partial<Record<Manual, Values>> = {
  canvas: { base_url: 'https://skole.instructure.com' },
  itslearning: { base_url: 'https://sms.itslearning.com' },
  inschool: { host: 'skole.inschool.visma.no' },
};
// Куда Google вернёт ученика: этот же адрес, он должен быть зарегистрирован в Google Cloud.
const redirectUri = () => `${window.location.origin}${PATHS.platforms}`;

// Ошибки платформ: 422 — платформа не приняла доступ (текст от неё в message), 502 / 503 — сбор недоступен.
export function platformError(err: unknown, t: Dict): string {
  const p = t.platforms;
  if (isApiError(err)) {
    if (err.status === 422) return err.message ? `${p.rejected} ${err.message}` : p.rejected;
    if (err.status === 502) return p.platformDown;
    if (err.status === 503) return p.collectorDown;
    if (err.status === 429) return p.tooOften;
  }
  return asApiError(err, t.common).message;
}

const Busy = ({ busy, idle, active }: { busy: boolean; idle: string; active: string }) => <>{busy ? active : idle}</>;

// Ручная форма: Canvas, InSchool и запасной вариант для Teams / itslearning.
function ManualForm({ platform, help, onConnected }: { platform: Manual; help?: string; onConnected: (c: PlatformConnection) => void }) {
  const t = useT(); const p = t.platforms;
  const [values, setValues] = useState<Values>({});
  const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null);
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (missingFields(platform, values).length) { setError(p.required); return; }
    setBusy(true); setError(null);
    try { onConnected(await platformsApi.connect(platform, toCredentials(platform, values))); setValues({}); }
    catch (err) { setError(platformError(err, t)); }
    finally { setBusy(false); }
  };
  return <form className="platform-form" onSubmit={submit} noValidate autoComplete="off">
    {help && <p className="platform-hint">{help}</p>}
    {FIELDS[platform].map(f => <Field key={f} name={`${platform}-${f}`} label={p.fields[f]} type={SECRET.includes(f) ? 'password' : f === 'host' ? 'text' : 'url'}
      autoComplete="off" spellCheck={false} placeholder={PLACEHOLDER[platform]?.[f]} value={values[f] ?? ''}
      onChange={e => setValues(v => ({ ...v, [f]: e.target.value }))} />)}
    <small className="platform-hint">{p.secretHint}</small>
    {error && <p role="alert" className="feedback danger">{error}</p>}
    <button className="btn btn-primary btn-sm" disabled={busy}><Busy busy={busy} idle={p.connect} active={p.connecting} /></button>
  </form>;
}

// Teams: POST /platforms/teams/start → код → опрос /teams/poll раз в interval секунд, пока не connected или не истёк.
function TeamsLogin({ onConnected }: { onConnected: (c: PlatformConnection) => void }) {
  const t = useT(); const p = t.platforms;
  const [start, setStart] = useState<TeamsStart | null>(null);
  const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null);
  const [expired, setExpired] = useState(false); const [copied, setCopied] = useState(false);
  const done = useRef(onConnected); done.current = onConnected;

  useEffect(() => {
    if (!start) return;
    let stopped = false; let timer: ReturnType<typeof setTimeout>;
    const deadline = Date.now() + start.expiresIn * 1000;
    const tick = async () => {
      if (stopped) return;
      if (Date.now() > deadline) { setStart(null); setExpired(true); return; }
      try {
        const r = await platformsApi.teamsPoll(start.deviceCode);
        if (stopped) return;
        if (r.status === 'connected') { setStart(null); done.current(r.platform); return; }
      } catch (err) { if (!stopped) { setStart(null); setError(platformError(err, t)); } return; }
      timer = setTimeout(tick, Math.max(1, start.interval) * 1000);
    };
    timer = setTimeout(tick, Math.max(1, start.interval) * 1000);
    return () => { stopped = true; clearTimeout(timer); };  // ушли со страницы — опрос стоп
  }, [start, t]);

  const begin = async () => {
    setBusy(true); setError(null); setExpired(false); setCopied(false);
    try { setStart(await platformsApi.teamsStart()); } catch (err) { setError(platformError(err, t)); } finally { setBusy(false); }
  };
  const copy = () => { navigator.clipboard?.writeText(start?.userCode ?? '').then(() => setCopied(true)).catch(() => {}); };

  if (start) return <div className="platform-form">
    <p className="platform-hint">{p.teamsCode}</p>
    <div className="plat-code"><code>{start.userCode}</code><button type="button" className="btn btn-sm" onClick={copy}>{copied ? <Check size={14} /> : <Copy size={14} />}{copied ? p.copied : p.copy}</button></div>
    <a className="btn btn-primary btn-sm plat-open" href={start.verificationUri} target="_blank" rel="noopener noreferrer">{p.teamsOpen} <ExternalLink size={14} /></a>
    <p role="status" className="platform-hint plat-wait"><RefreshCw size={13} className="spin" />{p.teamsWaiting}</p>
    <button type="button" className="text-link plat-link" onClick={() => setStart(null)}>{p.cancel}</button>
  </div>;
  return <div className="platform-form">
    {expired && <p role="alert" className="feedback warning">{p.teamsExpired}</p>}
    {error && <p role="alert" className="feedback danger">{error}</p>}
    <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={begin}><Busy busy={busy} idle={expired || error ? p.retry : p.teamsStart} active={p.connecting} /></button>
  </div>;
}

// itslearning: адрес школы → GET /itslearning/url → вход в новой вкладке → вставить itsl-itslearning://… → POST.
function ItslearningLogin({ onConnected }: { onConnected: (c: PlatformConnection) => void }) {
  const t = useT(); const p = t.platforms;
  const [baseUrl, setBaseUrl] = useState(''); const [code, setCode] = useState(''); const [opened, setOpened] = useState(false);
  const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null);
  const login = async () => {
    if (!baseUrl.trim()) { setError(p.required); return; }
    setBusy(true); setError(null);
    // Вкладку открываем сразу по клику (иначе браузер заблокирует всплывающее окно), адрес подставляем после ответа.
    const tab = window.open('', '_blank');
    try { const { url } = await platformsApi.itslearningUrl(baseUrl.trim()); if (tab) { tab.opener = null; tab.location.href = url; } else window.open(url, '_blank', 'noopener'); setOpened(true); }
    catch (err) { tab?.close(); setError(platformError(err, t)); }
    finally { setBusy(false); }
  };
  const finish = async (e: FormEvent) => {
    e.preventDefault();
    if (!code.trim()) { setError(p.required); return; }
    setBusy(true); setError(null);
    try { onConnected(await platformsApi.connect('itslearning', { base_url: baseUrl.trim(), code_or_url: code.trim() })); }
    catch (err) { setError(platformError(err, t)); }
    finally { setBusy(false); }
  };
  return <form className="platform-form" onSubmit={finish} noValidate autoComplete="off">
    <Field name="its-base" label={p.fields.base_url} type="url" spellCheck={false} placeholder="https://sms.itslearning.com" value={baseUrl} onChange={e => setBaseUrl(e.target.value)} />
    <button type="button" className="btn btn-sm" disabled={busy} onClick={login}>{p.itsLogin} <ExternalLink size={14} /></button>
    {opened && <>
      <p className="platform-hint">{p.itsHelp}</p>
      <Field name="its-code" label={p.itsPaste} type="text" spellCheck={false} placeholder="itsl-itslearning://login?code=…" value={code} onChange={e => setCode(e.target.value)} />
      <button className="btn btn-primary btn-sm" disabled={busy}><Busy busy={busy} idle={p.itsFinish} active={p.connecting} /></button>
    </>}
    {error && <p role="alert" className="feedback danger">{error}</p>}
  </form>;
}

// Google Classroom: GET /platforms/classroom/url → Google → назад на /settings/platforms?code=…&state=… → POST /platforms/classroom.
function ClassroomLogin({ error }: { error: string | null }) {
  const t = useT(); const p = t.platforms;
  const [busy, setBusy] = useState(false); const [own, setOwn] = useState<string | null>(null);
  const begin = async () => {
    setBusy(true); setOwn(null);
    try { const { url } = await platformsApi.classroomUrl(redirectUri()); window.location.assign(url); }
    catch (err) { setOwn(classroomError(err, t)); setBusy(false); }
  };
  const shown = own ?? error;
  return <div className="platform-form">
    <p className="platform-hint">{p.classroomHelp}</p>
    {shown && <p role="alert" className="feedback danger">{shown}</p>}
    <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={begin}><Busy busy={busy} idle={p.classroomStart} active={p.connecting} /></button>
  </div>;
}
// Бэкенд без Classroom ответит 400 «platform: canvas | …» или 404 — это «ещё не поддерживается», а не ошибка ученика.
const classroomError = (err: unknown, t: Dict) =>
  isApiError(err) && [400, 404, 405, 501].includes(err.status) ? t.platforms.classroomNotReady : platformError(err, t);

function PlatformCard({ pl, conn, classroomError: cErr, onChange }: {
  pl: typeof PLATFORMS[number]; conn: PlatformConnection | undefined; classroomError: string | null; onChange: (c: PlatformConnection) => void;
}) {
  const t = useT(); const p = t.platforms; const { tag } = useLang(); const { user } = useAuth();
  const state = cardState(conn);
  const [open, setOpen] = useState(state === 'reconnect' || (pl.key === 'classroom' && !!cErr));
  const [manual, setManual] = useState(false);
  const [confirm, setConfirm] = useState(false); const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null); const [saved, setSaved] = useState(false);
  const fmt = (iso: string) => { try { return new Intl.DateTimeFormat(tag, { dateStyle: 'medium', timeStyle: 'short', timeZone: user?.timezone }).format(new Date(iso)); } catch { return new Date(iso).toLocaleString(tag); } };
  const connected = (c: PlatformConnection) => { onChange(c); setOpen(false); setManual(false); setSaved(true); };
  const off: PlatformConnection = { platform: pl.key, connected: false, needsReconnect: false, lastSyncedAt: null, lastError: null };
  const disconnect = async () => {
    setBusy(true); setError(null);
    try { await platformsApi.disconnect(pl.key); onChange(off); setConfirm(false); setSaved(false); } catch (err) { setError(platformError(err, t)); } finally { setBusy(false); }
  };
  const retry = async () => {
    setBusy(true); setError(null);
    try {
      const r = await homeworkApi.sync(); const s = r.sources.find(x => x.platform === pl.key);
      onChange({ ...(conn ?? off), lastError: s && !s.ok ? s.error : null, needsReconnect: !!s?.needsReconnect, lastSyncedAt: s?.ok ? new Date().toISOString() : conn?.lastSyncedAt ?? null });
    } catch (err) { setError(platformError(err, t)); } finally { setBusy(false); }
  };

  const pill = state === 'on' ? <span className="pill pill-ok"><Check size={12} />{p.connected}</span>
    : state === 'reconnect' ? <span className="pill pill-warn">{p.reconnect}</span>
    : state === 'error' ? <span className="pill pill-danger">{p.failing}</span> : null;
  const form = pl.key === 'canvas' ? <ManualForm platform="canvas" help={p.canvasHelp} onConnected={connected} />
    : pl.key === 'inschool' ? <ManualForm platform="inschool" help={p.inschoolHelp} onConnected={connected} />
    : pl.key === 'classroom' ? <ClassroomLogin error={cErr} />
    : <>{manual ? <ManualForm platform={pl.key} onConnected={connected} /> : pl.key === 'teams' ? <TeamsLogin onConnected={connected} /> : <ItslearningLogin onConnected={connected} />}
      <button type="button" className="text-link plat-link" onClick={() => setManual(m => !m)}>{manual ? p.hideManual : p.manual}</button></>;

  return <div className={`plat-card plat-${state}`}>
    <div className="plat-head">
      <span className="plat-icon" aria-hidden="true"><Link2 size={16} /></span>
      <div className="plat-title"><strong>{pl.name}</strong>{pl.key === 'itslearning' && <span className="pill">{p.unofficial}</span>}{pill}
        <small>{state === 'off' ? p.about[pl.key] : conn?.lastSyncedAt ? p.lastSynced(fmt(conn.lastSyncedAt)) : p.never}</small></div>
      <div className="plat-actions">
        {(state === 'on' || state === 'error') && (confirm
          ? <><button className="btn btn-sm btn-danger" disabled={busy} onClick={disconnect}>{p.confirmDisconnect}</button><button className="btn btn-sm" disabled={busy} onClick={() => setConfirm(false)}>{p.cancel}</button></>
          : <button className="btn btn-sm" onClick={() => setConfirm(true)}>{p.disconnect}</button>)}
        {(state === 'off' || state === 'reconnect') && <button className="btn btn-sm" aria-expanded={open} onClick={() => setOpen(o => !o)}>{open ? p.cancel : p.connect}</button>}
      </div>
    </div>
    {saved && state === 'on' && <p role="status" className="feedback success"><Check size={16} />{p.saved}</p>}
    {(state === 'reconnect' || state === 'error') && conn?.lastError && <p role="alert" className="feedback danger">{conn.lastError}</p>}
    {state === 'error' && <button className="btn btn-sm" disabled={busy} onClick={retry}><RefreshCw size={14} />{busy ? p.syncing : p.retry}</button>}
    {error && <p role="alert" className="feedback danger">{error}</p>}
    {open && (state === 'off' || state === 'reconnect') && form}
  </div>;
}

export function PlatformsCard() {
  const t = useT(); const s = t.settings; const p = t.platforms;
  const location = useLocation(); const navigate = useNavigate();
  const [list, setList] = useState<PlatformConnection[]>([]);
  const [cErr, setCErr] = useState<string | null>(null);
  const handled = useRef(false);
  useEffect(() => { platformsApi.list().then(setList).catch(() => {}); }, []);

  // Вернулись от Google: обменять code на подключение и убрать code из адреса.
  useEffect(() => {
    const ret = oauthReturn(location.search);
    if (!ret || handled.current) return;
    handled.current = true;
    navigate(PATHS.platforms, { replace: true });
    if ('error' in ret) { setCErr(p.classroomDenied); return; }
    platformsApi.connect('classroom', { code: ret.code, state: ret.state, redirect_uri: redirectUri() })
      .then(c => setList(l => [...l.filter(x => x.platform !== 'classroom'), c]))
      .catch(err => setCErr(classroomError(err, t)));
  }, [location.search, navigate, p.classroomDenied, t]);

  const update = (c: PlatformConnection) => setList(l => [...l.filter(x => x.platform !== c.platform), c]);
  return <article className="lesson-card"><h2>{s.platformsTitle}</h2><p>{s.platformsIntro}</p>
    <div className="plat-list">
      {PLATFORMS.map(pl => <PlatformCard key={pl.key + (pl.key === 'classroom' ? cErr ?? '' : '')} pl={pl} conn={list.find(c => c.platform === pl.key)}
        classroomError={pl.key === 'classroom' ? cErr : null} onChange={update} />)}
    </div>
  </article>;
}
