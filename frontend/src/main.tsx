import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import { ArrowUpRight } from 'lucide-react';
import { Dashboard } from './pages/Dashboard';
import { Flashcards } from './pages/Learning';
import { ChatList, ChatView } from './pages/Chats';
import { Calendar, Settings, Status, Soon } from './pages/Other';
import { Auth, Profile } from './pages/Account';
import { type ThemeChoice, loadThemeChoice, saveThemeChoice, resolveTheme, themeColor } from './theme';
import { PATHS } from './paths';
import { AuthProvider, RequireAuth } from './auth';
import { AppHeader } from './components/AppHeader';
import { I18nProvider, useT } from './i18n';
import './styles.css';
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
 const t=useT();
 const [completed,setCompleted]=useState<string[]>([]);
 const toggle=(id:string)=>setCompleted(items=>items.includes(id)?items.filter(i=>i!==id):[...items,id]);
 return <div className="app" data-theme={resolved}>
  <a className="skip" href="#main">{t.header.skip}</a>
  <AppHeader/>
  <div className="demo-bar"><span className="live-dot"/>{t.demoBar.text}<Link to={PATHS.status}>{t.demoBar.link} <ArrowUpRight size={13}/></Link></div>
  <main id="main"><Routes>
   {/* 🔓 Страницы из API.md */}
   <Route path={PATHS.login} element={<Auth/>}/>
   <Route path={PATHS.register} element={<Auth register/>}/>
   {/* 🔒 Нет токена → /login */}
   <Route element={<RequireAuth/>}>
    <Route path={PATHS.home} element={<Dashboard completed={completed} toggle={toggle}/>}/>
    <Route path={PATHS.profile} element={<Profile completed={completed}/>}/>
    <Route path={PATHS.chats} element={<ChatList/>}/>
    <Route path={PATHS.chat(':chatId')} element={<ChatView/>}/>
    <Route path={PATHS.homework} element={<Calendar/>}/>
    <Route path={PATHS.quizzes} element={<Flashcards key="quiz" initialMode="quiz"/>}/>
    <Route path={PATHS.quiz(':quizId')} element={<Flashcards key="quiz" initialMode="quiz"/>}/>
    <Route path={PATHS.flashcards} element={<Flashcards key="cards"/>}/>
    <Route path={PATHS.progress} element={<Soon title={t.soon.progressTitle} text={t.soon.progressText}/>}/>
    <Route path={PATHS.history} element={<Soon title={t.soon.historyTitle} text={t.soon.historyText}/>}/>
    <Route path={PATHS.platforms} element={<Settings theme={theme} setTheme={setTheme} platformsOnly/>}/>
    {/* Только фронтенд, тоже 🔒 */}
    <Route path={PATHS.settings} element={<Settings theme={theme} setTheme={setTheme}/>}/>
   </Route>
   {/* 🔓 Только фронтенд */}
   <Route path={PATHS.status} element={<Status/>}/>
   {/* Старые адреса */}
   <Route path="/dashboard" element={<Moved to={PATHS.home}/>}/>
   <Route path="/tutor" element={<Moved to={PATHS.chats}/>}/>
   <Route path="/calendar" element={<Moved to={PATHS.homework}/>}/>
   <Route path="*" element={<section className="page"><h1>{t.notFound.title}</h1><p>{t.notFound.text}</p><Link className="button" to={PATHS.status}>{t.notFound.link}</Link></section>}/>
  </Routes></main>
  <footer><Link to={PATHS.home}>LekseRadar.</Link><span>{t.footer.tagline}</span><Link to={PATHS.status}>{t.footer.status} <ArrowUpRight size={12}/></Link></footer>
 </div>;
}
createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter><AuthProvider><I18nProvider><App/></I18nProvider></AuthProvider></BrowserRouter></React.StrictMode>);
