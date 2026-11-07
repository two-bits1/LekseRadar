import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ArrowUpRight, Check, Clock3, Sparkles, Layers, ChevronRight, CalendarDays, MessageSquare } from 'lucide-react';
import { assignments } from '../data';
import { Orb } from '../components/Orb';
import { PATHS, chatAbout, homeworkItem } from '../paths';
import { useAuth } from '../auth';
import { chatsApi } from '../api/endpoints';
import type { Chat } from '../api/types';
import { useLang, useT } from '../i18n';

// Демо-неделя 46: понедельник 9 ноября 2026. Даты выводятся на языке интерфейса.
const demoDay = (day: number) => new Date(2026, 10, day);
const SUBJECTS = ['Matematikk', 'Norsk', 'Naturfag', 'Engelsk'];  // названия предметов — из демо-данных норвежской школы

// «Последние чаты» на дашборде — GET /chats?archived=false (API.md, страница «/»).
function RecentChats(){
 const t=useT();const d=t.dashboard;
 const [chats,setChats]=useState<Chat[]|null>(null);const [failed,setFailed]=useState(false);
 useEffect(()=>{let off=false;chatsApi.list(false).then(c=>{if(!off)setChats(c.slice(0,3))}).catch(()=>{if(!off)setFailed(true)});return()=>{off=true}},[]);
 return <section className="reminders"><div className="section-title"><h3>{d.recentChats}</h3><Link to={PATHS.chats} aria-label={d.allChats}><MessageSquare size={16}/></Link></div>
  {failed?<p className="quiet-note">{d.chatsFailed}</p>
   :chats===null?<p className="quiet-note">{t.common.loading}</p>
   :chats.length?chats.map(c=><Link key={c.id} to={PATHS.chat(c.id)} className="reminder"><span className="day-number"><MessageSquare size={16}/></span><span><strong>{c.title}</strong><small>{c.topic??t.nav.tutor}</small></span><ChevronRight size={14}/></Link>)
   :<p className="quiet-note">{d.noChats} <Link className="text-link" to={PATHS.chats}>{d.askTutor}</Link></p>}
 </section>;
}

export function Dashboard({completed,toggle}:{completed:string[],toggle:(id:string)=>void}){
 const [subject,setSubject]=useState('all');
 const {user}=useAuth();const t=useT();const d=t.dashboard;const {tag}=useLang();
 const longDate=new Intl.DateTimeFormat(tag,{weekday:'long',day:'numeric',month:'long'}).format(demoDay(9));
 const shortDate=(day:number)=>new Intl.DateTimeFormat(tag,{day:'numeric',month:'short'}).format(demoDay(day));
 const monthShort=new Intl.DateTimeFormat(tag,{month:'short'}).format(demoDay(9)).replace('.','').toUpperCase();
 return <section className="page dashboard"><div className="greeting"><div><div className="eyebrow">{d.eyebrow}</div><h1>{d.title1}<br/><span>{d.title2}</span></h1><p>{d.hello(user?.displayName??d.student)}</p></div><div className="date-note"><CalendarDays size={18}/><span><span className="cap-first">{longDate}</span><small>{d.newWeek}</small></span></div></div>
 <div className="dashboard-grid"><div className="primary-column"><div className="focus-card"><div><div className="eyebrow"><span className="live-dot"/>{d.focusEyebrow}</div><h2>{d.focusTitle}</h2><p>{d.focusText}</p><Link className="button dark-button" to={chatAbout('likninger')}>{d.focusCta} <ArrowUpRight size={16}/></Link></div><div className="focus-art"><Orb/><span>{d.focusArt1}<br/>{d.focusArt2}</span></div></div>
 <div className="section-title"><h2>{d.planTitle} <span className="count">{assignments.length-completed.length}</span></h2><Link to={PATHS.homework}>{d.seeCalendar} <ArrowUpRight size={14}/></Link></div>
 <div className="filters" aria-label={d.filterLabel}>{['all',...SUBJECTS].map(s=><button key={s} aria-pressed={s===subject} className={s===subject?'selected':''} onClick={()=>setSubject(s)}>{s==='all'?d.allSubjects:s}</button>)}</div>
 <div className="assignments">{assignments.filter(a=>subject==='all'||a.subject===subject).map(a=>{const done=completed.includes(a.id);return <article className={`assignment ${done?'done':''}`} key={a.id}><div className="assignment-top"><span className={`subject-icon ${a.color}`}>{a.icon}</span><span className="subject-name">{a.subject}<small>{d.from(a.source)}</small></span><span className={a.urgent?'due urgent':'due'}>{a.urgent?`${shortDate(a.day)} · ${d.soon}`:shortDate(a.day)}</span></div><h3>{a.title}</h3><p>{a.description}</p><div className="assignment-bottom"><span><Clock3 size={13}/>{a.duration}</span><Link to={chatAbout(a.id)}><Sparkles size={14}/>{d.learnWithAi}</Link><button className="completion" aria-label={done?d.undo(a.title):d.complete(a.title)} aria-pressed={done} onClick={()=>toggle(a.id)}>{done&&<Check size={14}/>}</button></div></article>})}</div></div>
 <aside><section className="progress-card"><div className="section-title"><h3>{d.progressTitle}</h3><span className="sage mini-icon"><Sparkles size={15}/></span></div><div className="progress-number">{completed.length}<span> / 4</span></div><p>{d.progressText}</p><div className="progress-track"><span style={{width:`${completed.length/4*100}%`}}/></div><small>{completed.length===4?d.progressDone:d.progressCalm}</small></section>
 <RecentChats/>
 <section className="reminders"><div className="section-title"><h3>{d.upcoming}</h3><CalendarDays size={16}/></div>{assignments.slice(0,3).map(a=><Link key={a.id} to={homeworkItem(a.id)} className="reminder"><span className="day-number">{a.day}<small>{monthShort}</small></span><span><strong>{a.subject}</strong><small>{a.title}</small></span><ChevronRight size={14}/></Link>)}</section>
 <Link to={PATHS.flashcards} className="deck-preview"><div className="mini-cards"><span/><span/><span><Layers size={23}/></span></div><div className="eyebrow">{d.deckEyebrow}</div><h3>{d.deckTitle}</h3><p>{d.deckText}</p><span className="text-link">{d.deckCta} <ArrowRight size={16}/></span></Link>
 <p className="quiet-note">{d.demoNote1}<br/>{d.demoNote2}</p></aside></div></section>;
}
