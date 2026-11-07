import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, NavLink, Link, Navigate, useLocation } from 'react-router-dom';
import { CalendarDays, Layers, UserRound, Settings2, ArrowUpRight, Radar } from 'lucide-react';
import { Dashboard } from './pages/Dashboard';
import { Tutor, Flashcards } from './pages/Learning';
import { Calendar, Profile, Settings, Auth, Status, Soon, ChatSoon } from './pages/Other';
import { type ThemeChoice, loadThemeChoice, saveThemeChoice, resolveTheme, themeColor } from './theme';
import { PATHS } from './paths';
import './styles.css';
export function Orb({small=false}:{small?:boolean}) { return <span className={small?'orb small':'orb'} aria-hidden="true"><span/></span>; }
const darkQuery='(prefers-color-scheme: dark)';
function useTheme(){
 const [choice,setChoice]=useState<ThemeChoice>(loadThemeChoice);
 const [systemDark,setSystemDark]=useState(()=>matchMedia(darkQuery).matches);
 const [now,setNow]=useState(()=>new Date());
 useEffect(()=>{
  if(choice!=='auto')return;
  const mq=matchMedia(darkQuery);
  const sync=()=>{setSystemDark(mq.matches);setNow(new Date())};
  sync();
  mq.addEventListener('change',sync);
  const timer=setInterval(sync,60_000);
  return()=>{mq.removeEventListener('change',sync);clearInterval(timer)};
 },[choice]);
 const resolved=resolveTheme(choice,systemDark,now);
 useEffect(()=>{document.querySelector('meta[name="theme-color"]')?.setAttribute('content',themeColor(resolved))},[resolved]);
 return {choice,resolved,setChoice:(c:ThemeChoice)=>{setChoice(c);saveThemeChoice(c)}};
}
// Старые адреса (до API.md) ведут на новые, ?assignment=… сохраняется.
function Moved({to}:{to:string}){const {search}=useLocation();return <Navigate to={to+search} replace/>;}
function App(){
 const {choice:theme,resolved,setChoice:setTheme}=useTheme();
 const [completed,setCompleted]=useState<string[]>([]);
 const toggle=(id:string)=>setCompleted(items=>items.includes(id)?items.filter(i=>i!==id):[...items,id]);
 return <div className="app" data-theme={resolved}>
  <a className="skip" href="#main">Hopp til innhold</a>
  <header className="header"><Link to={PATHS.home} className="brand" aria-label="LekseRadar — oversikt"><Radar size={25}/><span>LekseRadar<span className="brand-dot">.</span></span></Link>
  <nav aria-label="Hovedmeny"><NavLink to={PATHS.homework}><CalendarDays/><span>Kalender</span></NavLink><NavLink to={PATHS.flashcards}><Layers/><span>Læringskort</span></NavLink><NavLink to={PATHS.chats} className="orb-link" aria-label="AI-veileder"><Orb/><span>AI-veileder</span></NavLink><NavLink to={PATHS.profile}><UserRound/><span>Profil</span></NavLink><NavLink to={PATHS.settings}><Settings2/><span>Innstillinger</span></NavLink></nav>
  <Link to={PATHS.profile} className="guest"><span className="avatar">E</span><span>Hei, elev<small>Gjestemodus</small></span></Link></header>
  <div className="demo-bar"><span className="live-dot"/>En liten smakebit av en enklere skolehverdag.<Link to={PATHS.status}>Utforsk demoen <ArrowUpRight size={13}/></Link></div>
  <main id="main"><Routes>
   {/* Страницы из API.md */}
   <Route path={PATHS.home} element={<Dashboard completed={completed} toggle={toggle}/>}/>
   <Route path={PATHS.login} element={<Auth/>}/>
   <Route path={PATHS.register} element={<Auth register/>}/>
   <Route path={PATHS.profile} element={<Profile completed={completed}/>}/>
   <Route path={PATHS.chats} element={<Tutor/>}/>
   <Route path={PATHS.chat(':chatId')} element={<ChatSoon/>}/>
   <Route path={PATHS.homework} element={<Calendar/>}/>
   <Route path={PATHS.quizzes} element={<Flashcards key="quiz" initialMode="Quiz"/>}/>
   <Route path={PATHS.quiz(':quizId')} element={<Flashcards key="quiz" initialMode="Quiz"/>}/>
   <Route path={PATHS.flashcards} element={<Flashcards key="cards"/>}/>
   <Route path={PATHS.progress} element={<Soon title="Fremgang" text="Grafer per fag og dag kommer når fremgangen lagres på kontoen din."/>}/>
   <Route path={PATHS.history} element={<Soon title="Historikk" text="Her ser du snart hva du har jobbet med, steg for steg."/>}/>
   <Route path={PATHS.platforms} element={<Settings theme={theme} setTheme={setTheme} platformsOnly/>}/>
   {/* Только фронтенд */}
   <Route path={PATHS.settings} element={<Settings theme={theme} setTheme={setTheme}/>}/>
   <Route path={PATHS.status} element={<Status/>}/>
   {/* Старые адреса */}
   <Route path="/dashboard" element={<Moved to={PATHS.home}/>}/>
   <Route path="/tutor" element={<Moved to={PATHS.chats}/>}/>
   <Route path="/calendar" element={<Moved to={PATHS.homework}/>}/>
   <Route path="*" element={<section className="page"><h1>Her var det stille.</h1><p>Denne siden finnes ikke ennå.</p><Link className="button" to={PATHS.status}>Se alle sidene</Link></section>}/>
  </Routes></main>
  <footer><Link to={PATHS.home}>LekseRadar.</Link><span>Lær litt. Forstå mer.</span><Link to={PATHS.status}>Prototype · Se utviklingsstatus <ArrowUpRight size={12}/></Link></footer>
 </div>;
}
createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter><App/></BrowserRouter></React.StrictMode>);
