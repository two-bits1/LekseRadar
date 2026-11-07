import { useState } from 'react';
import { useAuth } from '../auth';
import { usersApi } from '../api/endpoints';
import { asApiError } from '../forms';
import { Link } from 'react-router-dom';
import { ArrowUpRight, Link2, Sun, Moon, SunMoon, Languages, Bell, BellOff, Send, Check } from 'lucide-react';
import { type ThemeChoice, EVENING_START, MORNING_END } from '../theme';
import { routes } from '../data';
import { PATHS } from '../paths';
import { PlatformsCard } from '../components/Platforms';
import { LANGS, LANG_NAMES, useLang, useT } from '../i18n';
export function Settings({theme,setTheme,platformsOnly=false}:{theme:ThemeChoice,setTheme:(t:ThemeChoice)=>void,platformsOnly?:boolean}){
 const t=useT();const s=t.settings;const {lang,setLang}=useLang();
  return <section className="page narrow"><div className="page-heading"><div><div className="eyebrow">{s.eyebrow}</div><h1>{s.title}</h1><p>{s.intro}</p></div></div>
  {!platformsOnly&&<article className="lesson-card"><h2>{s.appearance}</h2>
   <div className="settings-row"><span>{s.theme}<small>{s.themeHint(EVENING_START,String(MORNING_END).padStart(2,'0'))}</small></span><div className="actions"><button aria-pressed={theme==='auto'} onClick={()=>setTheme('auto')}><SunMoon size={16}/>{s.auto}</button><button aria-pressed={theme==='light'} onClick={()=>setTheme('light')}><Sun size={16}/>{s.light}</button><button aria-pressed={theme==='dark'} onClick={()=>setTheme('dark')}><Moon size={16}/>{s.dark}</button></div></div>
   <div className="settings-row"><span>{s.language}<small>{s.languageHint}</small></span><div className="actions" role="group" aria-label={s.language}>{LANGS.map(l=><button key={l} lang={l} aria-pressed={lang===l} onClick={()=>setLang(l)}><Languages size={16}/>{LANG_NAMES[l]}</button>)}</div></div>
   <RemindersRow/>
  </article>}
  {platformsOnly?<PlatformsCard/>:<article className="lesson-card"><h2>{s.platformsTitle}</h2><p>{s.platformsIntro}</p><Link className="btn btn-sm" to={PATHS.platforms}><Link2 size={15}/>{t.platforms.toPlatforms} <ArrowUpRight size={14}/></Link></article>}
 </section>}
export function Soon({title,text}:{title:string,text:string}){const t=useT();return <section className="page narrow"><div className="eyebrow">{t.soon.eyebrow}</div><h1>{title}</h1><p>{text}</p><article className="lesson-card"><p>{t.soon.text}</p><Link className="text-link" to={PATHS.home}>{t.soon.toOverview} <ArrowUpRight size={16}/></Link></article></section>}
export function Status(){const t=useT();const s=t.status;const [copied,setCopied]=useState(false);return <section className="page narrow"><div className="eyebrow">{s.eyebrow}</div><h1>{s.title1}<br/>{s.title2}</h1><p>{s.intro}</p><article className="lesson-card"><div className="section-title"><h2>{s.sitemap}</h2><span className="pill">{s.version}</span></div>{routes.map(([url,page,state])=><Link className="route-row" key={url} to={url}><span><strong>{s.pages[page]}</strong><code>{url}</code></span><span>{s.states[state]}<ArrowUpRight size={15}/></span></Link>)}</article><button className="button dark-button" onClick={async()=>{try{await navigator.clipboard.writeText(`${window.location.origin}/status`);setCopied(true)}catch{setCopied(false)}}}>{copied?s.copied:s.copy}<Link2 size={16}/></button><p className="quiet-note">{s.note1}<br/>{s.note2}</p></section>}

// Напоминания о ДЗ на почту из регистрации: вкл/выкл (PATCH /users/me) и пробное письмо (POST /users/me/reminders/test).
function RemindersRow(){
 const {user,updateUser}=useAuth();const t=useT();const s=t.settings;
 const [busy,setBusy]=useState<'toggle'|'test'|null>(null);const [note,setNote]=useState<{ok:boolean;text:string}|null>(null);
 const on=user?.emailReminders!==false;
 const toggle=async(v:boolean)=>{if(v===on||busy)return;setBusy('toggle');setNote(null);
  try{updateUser(await usersApi.update({emailReminders:v}))}catch(e){setNote({ok:false,text:asApiError(e,t.common).message})}finally{setBusy(null)}};
 const test=async()=>{setBusy('test');setNote(null);
  try{const r=await usersApi.testReminder();setNote({ok:true,text:s.testSent(r.email,r.count)})}catch(e){setNote({ok:false,text:asApiError(e,t.common).message})}finally{setBusy(null)}};
 return <div className="settings-row reminders-row"><span>{s.reminders}<small>{s.remindersHint(user?.email??'…')}</small></span>
  <div className="actions" role="group" aria-label={s.reminders}>
   <button aria-pressed={on} disabled={busy!==null} onClick={()=>toggle(true)}><Bell size={16}/>{s.on}</button>
   <button aria-pressed={!on} disabled={busy!==null} onClick={()=>toggle(false)}><BellOff size={16}/>{s.off}</button>
   <button disabled={busy!==null} onClick={test}><Send size={16}/>{busy==='test'?s.testSending:s.testReminder}</button>
  </div>
  {note&&<p role={note.ok?'status':'alert'} className={`feedback ${note.ok?'success':'danger'}`}>{note.ok&&<Check size={17}/>}{note.text}</p>}
 </div>;
}
