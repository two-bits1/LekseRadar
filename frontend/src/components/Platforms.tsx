// Подключение школьных платформ (/settings и /settings/platforms).
// Поля — как ждёт homework-collector; отправка: POST /api/v1/platforms/{platform} (в API.md 🕓 «будет позже»).
// Токены не сохраняются в браузере: только уходят на сервер, где их шифрует бэкенд.
import { useEffect, useState, type FormEvent } from 'react';
import { ArrowUpRight, Check, Link2 } from 'lucide-react';
import { platformsApi } from '../api/endpoints';
import type { ApiError, PlatformCredentials, PlatformKey } from '../api/types';
import { Field, asApiError } from '../forms';
import { useT } from '../i18n';

type FieldKey = 'base_url' | 'token' | 'refresh_token' | 'host' | 'JSESSIONID' | 'Authorization' | 'access_token' | 'url';
const PLATFORMS: { key: PlatformKey; name: string; fields: FieldKey[]; placeholder?: Partial<Record<FieldKey, string>> }[] = [
  { key: 'canvas', name: 'Canvas', fields: ['base_url', 'token'], placeholder: { base_url: 'https://skole.instructure.com' } },
  { key: 'itslearning', name: 'itslearning', fields: ['base_url', 'refresh_token'], placeholder: { base_url: 'https://sms.itslearning.com' } },
  { key: 'inschool', name: 'Visma InSchool', fields: ['host', 'JSESSIONID', 'Authorization'], placeholder: { host: 'skole.inschool.visma.no' } },
  { key: 'teams', name: 'Microsoft Teams', fields: ['access_token', 'refresh_token'] },
  { key: 'ics', name: 'ICS', fields: ['url'], placeholder: { url: 'https://…/calendar.ics' } },
];
const SECRET: FieldKey[] = ['token', 'refresh_token', 'JSESSIONID', 'Authorization', 'access_token'];
// Бэкенд ещё без эндпоинта: 404 / 405 / 501 — «API не готово», а не «что-то сломалось».
const NOT_READY = [404, 405, 501];

// Тело запроса в формате homework-collector (у InSchool — cookies внутри).
export function toCredentials(key: PlatformKey, values: Partial<Record<FieldKey, string>>): PlatformCredentials {
  const v = (k: FieldKey) => (values[k] ?? '').trim();
  if (key === 'inschool') return { host: v('host'), cookies: { JSESSIONID: v('JSESSIONID'), Authorization: v('Authorization') } };
  return Object.fromEntries(PLATFORMS.find(p => p.key === key)!.fields.map(f => [f, v(f)]));
}

function PlatformForm({ platform, onConnected }: { platform: typeof PLATFORMS[number]; onConnected: () => void }) {
  const t = useT(); const p = t.platforms;
  const [values, setValues] = useState<Partial<Record<FieldKey, string>>>({});
  const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null); const [notReady, setNotReady] = useState(false);
  const complete = platform.fields.every(f => (values[f] ?? '').trim());
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!complete) { setError(p.required); return; }
    setBusy(true); setError(null); setNotReady(false);
    try { await platformsApi.connect(platform.key, toCredentials(platform.key, values)); setValues({}); onConnected(); }
    catch (err) { const a: ApiError = asApiError(err, t.common); if (NOT_READY.includes(a.status)) setNotReady(true); else setError(a.message); }
    finally { setBusy(false); }
  };
  return <form className="example platform-form" onSubmit={submit} noValidate autoComplete="off">
    {platform.fields.map(f => <Field key={f} name={`${platform.key}-${f}`} label={p.fields[f]} type={SECRET.includes(f) ? 'password' : f === 'host' ? 'text' : 'url'}
      autoComplete="off" spellCheck={false} placeholder={platform.placeholder?.[f]} value={values[f] ?? ''}
      onChange={e => setValues(v => ({ ...v, [f]: e.target.value }))} />)}
    <small className="platform-hint">{p.secretHint}</small>
    {notReady && <p role="status" className="feedback warning">{p.notReady}</p>}
    {error && <p role="alert" className="feedback danger">{error}</p>}
    <button className="btn btn-primary btn-sm" disabled={busy}>{busy ? p.connecting : p.connect}</button>
  </form>;
}

export function PlatformsCard() {
  const t = useT(); const s = t.settings; const p = t.platforms;
  const [open, setOpen] = useState<PlatformKey | null>(null);
  const [connected, setConnected] = useState<PlatformKey[]>([]);
  const [justSaved, setJustSaved] = useState<PlatformKey | null>(null);
  const load = () => platformsApi.list().then(list => setConnected(list.filter(c => c.connected).map(c => c.platform))).catch(() => {});  // 🕓 пока нет — просто без статуса
  useEffect(() => { load(); }, []);
  const disconnect = async (key: PlatformKey) => { try { await platformsApi.disconnect(key); setConnected(c => c.filter(k => k !== key)); } catch { /* бэкенд без эндпоинта */ } };

  return <article className="lesson-card"><h2>{s.platformsTitle}</h2><p>{s.platformsIntro}</p>
    {PLATFORMS.map(pl => <div key={pl.key}>
      <div className="settings-row">
        <span className="text-link"><Link2 size={17} />{pl.name}{connected.includes(pl.key) && <span className="pill pill-ok"><Check size={12} />{p.connected}</span>}</span>
        {connected.includes(pl.key)
          ? <button onClick={() => disconnect(pl.key)}>{p.disconnect}</button>
          : <button aria-expanded={open === pl.key} onClick={() => setOpen(open === pl.key ? null : pl.key)}>{p.setup} <ArrowUpRight size={14} /></button>}
      </div>
      {justSaved === pl.key && <p role="status" className="feedback success"><Check size={17} />{p.saved}</p>}
      {open === pl.key && !connected.includes(pl.key) && <PlatformForm platform={pl} onConnected={() => { setOpen(null); setJustSaved(pl.key); load(); setConnected(c => [...c, pl.key]); }} />}
    </div>)}
  </article>;
}
