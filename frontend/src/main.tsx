import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, NavLink, Link, Navigate } from 'react-router-dom';
import { CalendarDays, Layers, UserRound, Settings2, ArrowUpRight, Radar } from 'lucide-react';
import { Dashboard } from './pages/Dashboard';
import { Tutor, Flashcards } from './pages/Learning';
import { Calendar, Profile, Settings, Auth, Status } from './pages/Other';
import { type ThemeChoice, loadThemeChoice, saveThemeChoice, resolveTheme, themeColor } from './theme';
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
function App(){
 const {choice:theme,resolved,setChoice:setTheme}=useTheme();
 const [completed,setCompleted]=useState<string[]>([]);
 const toggle=(id:string)=>setCompleted(items=>items.includes(id)?items.filter(i=>i!==id):[...items,id]);
 return <div className="app" data-theme={resolved}>
  <a className="skip" href="#main">Hopp til innhold</a>
  <header className="header"><Link to="/dashboard" className="brand" aria-label="LekseRadar — oversikt"><Radar size={25}/><span>LekseRadar<span className="brand-dot">.</span></span></Link>
  <nav aria-label="Hovedmeny"><NavLink to="/calendar"><CalendarDays/><span>Kalender</span></NavLink><NavLink to="/flashcards"><Layers/><span>Læringskort</span></NavLink><NavLink to="/tutor" className="orb-link" aria-label="AI-veileder"><Orb/><span>AI-veileder</span></NavLink><NavLink to="/profile"><UserRound/><span>Profil</span></NavLink><NavLink to="/settings"><Settings2/><span>Innstillinger</span></NavLink></nav>
  <Link to="/profile" className="guest"><span className="avatar">E</span><span>Hei, elev<small>Gjestemodus</small></span></Link></header>
  <div className="demo-bar"><span className="live-dot"/>En liten smakebit av en enklere skolehverdag.<Link to="/status">Utforsk demoen <ArrowUpRight size={13}/></Link></div>
  <main id="main"><Routes><Route path="/" element={<Navigate to="/dashboard" replace/>}/><Route path="/dashboard" element={<Dashboard completed={completed} toggle={toggle}/>}/><Route path="/tutor" element={<Tutor/>}/><Route path="/flashcards" element={<Flashcards/>}/><Route path="/calendar" element={<Calendar/>}/><Route path="/profile" element={<Profile completed={completed}/>}/><Route path="/settings" element={<Settings theme={theme} setTheme={setTheme}/>}/><Route path="/login" element={<Auth/>}/><Route path="/register" element={<Auth register/>}/><Route path="/status" element={<Status/>}/><Route path="*" element={<section className="page"><h1>Her var det stille.</h1><p>Denne siden finnes ikke ennå.</p><Link className="button" to="/status">Se alle sidene</Link></section>}/></Routes></main>
  <footer><Link to="/dashboard">LekseRadar.</Link><span>Lær litt. Forstå mer.</span><Link to="/status">Prototype · Se utviklingsstatus <ArrowUpRight size={12}/></Link></footer>
 </div>;
}
createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter><App/></BrowserRouter></React.StrictMode>);
