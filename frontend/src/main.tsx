import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import { ArrowUpRight } from 'lucide-react';
import { Dashboard } from './pages/Dashboard';
import { Flashcards } from './pages/Flashcards';
import { QuizStart, QuizRun } from './pages/Quiz';
import { ChatList, ChatView } from './pages/Chats';
import { Settings, Status } from './pages/Other';
import { Homework } from './pages/Homework';
import { Progress, History } from './pages/Progress';
import { Auth, Profile } from './pages/Account';
import { type ThemeChoice, loadThemeChoice, saveThemeChoice, resolveTheme, themeColor } from './theme';
import { PATHS } from './paths';
import { AuthProvider, RequireAuth, useAuth } from './auth';
import { Landing } from './pages/Landing';
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
 const t=useT();const {token}=useAuth();const {pathname}=useLocation();
 const landing=!token&&pathname===PATHS.home;  // лендинг всегда тёмный, как референс
 return <div className={landing?'app is-landing':'app'} data-theme={landing?'dark':resolved}>
  <a className="skip" href="#main">{t.header.skip}</a>
  <AppHeader/>
  <main id="main"><Routes>
   {/* «/»: гость — лендинг с регистрацией, вошедший — главная (адрес тот же, как в API.md) */}
   <Route path={PATHS.home} element={token?<Dashboard/>:<Landing/>}/>
   {/* 🔓 Страницы из API.md */}
   <Route path={PATHS.login} element={<Auth/>}/>
   <Route path={PATHS.register} element={<Auth register/>}/>
   {/* 🔒 Нет токена → /login */}
   <Route element={<RequireAuth/>}>
    <Route path={PATHS.profile} element={<Profile/>}/>
    <Route path={PATHS.chats} element={<ChatList/>}/>
    <Route path={PATHS.chat(':chatId')} element={<ChatView/>}/>
    <Route path={PATHS.homework} element={<Homework/>}/>
    <Route path={PATHS.quizzes} element={<QuizStart/>}/>
    <Route path={PATHS.quiz(':quizId')} element={<QuizRun/>}/>
    <Route path={PATHS.flashcards} element={<Flashcards/>}/>
    <Route path={PATHS.progress} element={<Progress/>}/>
    <Route path={PATHS.history} element={<History/>}/>
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
