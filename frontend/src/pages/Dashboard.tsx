import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ArrowUpRight, Check, Clock3, Sparkles, Layers, ChevronRight, CalendarDays, MessageSquare } from 'lucide-react';
import { assignments } from '../data';
import { Orb } from '../components/Orb';
import { PATHS, chatAbout, homeworkItem } from '../paths';
import { useAuth } from '../auth';
import { chatsApi } from '../api/endpoints';
import type { Chat } from '../api/types';

// «Последние чаты» на дашборде — GET /chats?archived=false (API.md, страница «/»).
function RecentChats(){
 const [chats,setChats]=useState<Chat[]|null>(null);const [failed,setFailed]=useState(false);
 useEffect(()=>{let off=false;chatsApi.list(false).then(c=>{if(!off)setChats(c.slice(0,3))}).catch(()=>{if(!off)setFailed(true)});return()=>{off=true}},[]);
 return <section className="reminders"><div className="section-title"><h3>Siste samtaler</h3><Link to={PATHS.chats} aria-label="Alle samtaler"><MessageSquare size={16}/></Link></div>
  {failed?<p className="quiet-note">Kunne ikke hente samtalene.</p>
   :chats===null?<p className="quiet-note">Henter …</p>
   :chats.length?chats.map(c=><Link key={c.id} to={PATHS.chat(c.id)} className="reminder"><span className="day-number"><MessageSquare size={16}/></span><span><strong>{c.title}</strong><small>{c.topic??'AI-veileder'}</small></span><ChevronRight size={14}/></Link>)
   :<p className="quiet-note">Ingen samtaler ennå. <Link className="text-link" to={PATHS.chats}>Spør AI-veilederen</Link></p>}
 </section>;
}
export function Dashboard({completed,toggle}:{completed:string[],toggle:(id:string)=>void}){
 const [subject,setSubject]=useState('Alle fag');
 const {user}=useAuth();
 return <section className="page dashboard"><div className="greeting"><div><div className="eyebrow">DIN LILLE OVERSIKT · DEMOUKE 46</div><h1>Plass til å lære.<br/><span>Og til alt det andre.</span></h1><p>Hei, {user?.displayName??'elev'}. Vi tar én ting om gangen.</p></div><div className="date-note"><CalendarDays size={18}/><span>Mandag 9. november<small>En ny uke, nye muligheter.</small></span></div></div>
 <div className="dashboard-grid"><div className="primary-column"><div className="focus-card"><div><div className="eyebrow"><span className="live-dot"/>LITT FOKUS NÅ, LITT MER FRI SENERE</div><h2>En god start på uka.</h2><p>Begynn med matematikk. Vi hjelper deg<br className="desktop-break"/> å forstå, ikke bare finne svaret.</p><Link className="button dark-button" to={chatAbout('likninger')}>La oss se på det <ArrowUpRight size={16}/></Link></div><div className="focus-art"><Orb/><span>Din nysgjerrighet.<br/>En liten hjelpende hånd.</span></div></div>
 <div className="section-title"><h2>På planen din <span className="count">{assignments.length-completed.length}</span></h2><Link to={PATHS.homework}>Se kalender <ArrowUpRight size={14}/></Link></div>
 <div className="filters" aria-label="Filtrer etter fag">{['Alle fag','Matematikk','Norsk','Naturfag','Engelsk'].map(s=><button key={s} aria-pressed={s===subject} className={s===subject?'selected':''} onClick={()=>setSubject(s)}>{s}</button>)}</div>
 <div className="assignments">{assignments.filter(a=>subject==='Alle fag'||a.subject===subject).map(a=><article className={`assignment ${completed.includes(a.id)?'done':''}`} key={a.id}><div className="assignment-top"><span className={`subject-icon ${a.color}`}>{a.icon}</span><span className="subject-name">{a.subject}<small>Fra {a.source} · demo</small></span><span className={a.urgent?'due urgent':'due'}>{a.urgent?'9. nov. · snart':a.date}</span></div><h3>{a.title}</h3><p>{a.description}</p><div className="assignment-bottom"><span><Clock3 size={13}/>{a.duration}</span><Link to={chatAbout(a.id)}><Sparkles size={14}/>Lær med AI</Link><button className="completion" aria-label={`${completed.includes(a.id)?'Angre fullføring':'Fullfør'}: ${a.title}`} aria-pressed={completed.includes(a.id)} onClick={()=>toggle(a.id)}>{completed.includes(a.id)&&<Check size={14}/>}</button></div></article>)}</div></div>
 <aside><section className="progress-card"><div className="section-title"><h3>Små steg teller</h3><span className="sage mini-icon"><Sparkles size={15}/></span></div><div className="progress-number">{completed.length}<span> / 4</span></div><p>oppgaver fullført denne uka</p><div className="progress-track"><span style={{width:`${completed.length/4*100}%`}}/></div><small>{completed.length===4?'Du er i mål. Godt jobbet!':'Du trenger ikke gjøre alt på én gang.'}</small></section>
 <RecentChats/>
 <section className="reminders"><div className="section-title"><h3>Rett rundt hjørnet</h3><CalendarDays size={16}/></div>{assignments.slice(0,3).map(a=><Link key={a.id} to={homeworkItem(a.id)} className="reminder"><span className="day-number">{a.day}<small>NOV</small></span><span><strong>{a.subject}</strong><small>{a.title}</small></span><ChevronRight size={14}/></Link>)}</section>
 <Link to={PATHS.flashcards} className="deck-preview"><div className="mini-cards"><span/><span/><span><Layers size={23}/></span></div><div className="eyebrow">LITT REPETISJON, STOR FORSKJELL</div><h3>Det sitter snart.</h3><p>Seks små kort. Et litt klarere hode.</p><span className="text-link">Prøv læringskort <ArrowRight size={16}/></span></Link>
 <p className="quiet-note">Dette er demonstrasjonsdata.<br/>Ingen skolekonto er koblet til.</p></aside></div></section>;
}
