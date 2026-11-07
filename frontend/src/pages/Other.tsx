import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowUpRight, Link2, Sun, Moon, SunMoon, Languages } from 'lucide-react';
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
   <div className="settings-row"><span>{s.reminders}<small>{s.remindersHint}</small></span><span className="pill">{s.soon}</span></div>
  </article>}
  {platformsOnly?<PlatformsCard/>:<article className="lesson-card"><h2>{s.platformsTitle}</h2><p>{s.platformsIntro}</p><Link className="btn btn-sm" to={PATHS.platforms}><Link2 size={15}/>{t.platforms.toPlatforms} <ArrowUpRight size={14}/></Link></article>}
 </section>}
export function Soon({title,text}:{title:string,text:string}){const t=useT();return <section className="page narrow"><div className="eyebrow">{t.soon.eyebrow}</div><h1>{title}</h1><p>{text}</p><article className="lesson-card"><p>{t.soon.text}</p><Link className="text-link" to={PATHS.home}>{t.soon.toOverview} <ArrowUpRight size={16}/></Link></article></section>}
export function Status(){const t=useT();const s=t.status;const [copied,setCopied]=useState(false);return <section className="page narrow"><div className="eyebrow">{s.eyebrow}</div><h1>{s.title1}<br/>{s.title2}</h1><p>{s.intro}</p><article className="lesson-card"><div className="section-title"><h2>{s.sitemap}</h2><span className="pill">{s.version}</span></div>{routes.map(([url,page,state])=><Link className="route-row" key={url} to={url}><span><strong>{s.pages[page]}</strong><code>{url}</code></span><span>{s.states[state]}<ArrowUpRight size={15}/></span></Link>)}</article><button className="button dark-button" onClick={async()=>{try{await navigator.clipboard.writeText(`${window.location.origin}/status`);setCopied(true)}catch{setCopied(false)}}}>{copied?s.copied:s.copy}<Link2 size={16}/></button><p className="quiet-note">{s.note1}<br/>{s.note2}</p></section>}
