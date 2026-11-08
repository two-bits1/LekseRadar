// Страницы аккаунта на настоящем API: /login, /register, /profile (API.md).
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Award, Brain, CalendarCheck, Check, CircleCheckBig, Layers, Lightbulb, ListChecks, Lock, LogOut, MessageCircleQuestion, PenLine, ShieldCheck, Star, Sunrise, Trophy, type LucideIcon } from 'lucide-react';
import { PATHS } from '../paths';
import { useAuth, initialOf } from '../auth';
import { dashboardApi, quizzesApi, usersApi } from '../api/endpoints';
import type { ApiError, Dashboard, User } from '../api/types';
import { type AchievementId, achievements, bestQuizScore } from '../components/achievements';
import { Field, asApiError, fieldError, generalError } from '../forms';
import { Radar } from 'lucide-react';
import { useLang, useT } from '../i18n';

// POST /auth/login | /auth/register → токен → на «/».
// Ошибки: 400 → fields под полями, 409 → message у поля email, остальное → над кнопкой.
export function Auth({register=false}:{register?:boolean}){
 const auth=useAuth();const t=useT();const a=t.auth;
 if(auth.token)return <Navigate to={PATHS.home} replace/>;
 return <section className="page auth-page"><span className="auth-logo" aria-hidden="true"><Radar size={28}/></span><div className="eyebrow">{a.eyebrow}</div><h1>{register?a.titleRegister:a.titleLogin}</h1><p>{a.intro}</p><AuthForm register={register}/><Link className="text-link" to={register?PATHS.login:PATHS.register}>{register?a.toLogin:a.toRegister}</Link></section>;
}

// Сама форма — та же на /login, /register и в центре лендинга.
export function AuthForm({register=false}:{register?:boolean}){
 const auth=useAuth();const navigate=useNavigate();const t=useT();const a=t.auth;
 const [form,setForm]=useState({email:'',password:'',displayName:''});const [accept,setAccept]=useState(false);
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);
 const [sentTo,setSentTo]=useState<string|null>(null);
 const set=(k:keyof typeof form)=>(e:ChangeEvent<HTMLInputElement>)=>setForm(f=>({...f,[k]:e.target.value}));
 const errorFor=(k:string)=>fieldError(error,k)??(error?.status===409&&k==='email'?error.message:undefined);
 const general=error?.status===409?null:generalError(error);
 const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError(null);
  try{if(register)setSentTo((await auth.register({...form,acceptTerms:accept})).email);else{await auth.login({email:form.email,password:form.password});navigate(PATHS.home,{replace:true})}}
  catch(err){setError(asApiError(err,t.common))}
  finally{setBusy(false)}};
 if(register&&sentTo)return <VerifyEmailForm email={sentTo} resend={()=>auth.register({...form,acceptTerms:true})} back={()=>setSentTo(null)}/>;
 return <form className="lesson-card auth-form" onSubmit={submit} noValidate><h2>{register?a.register:a.login}</h2>
  {register&&<Field name="displayName" label={a.name} type="text" autoComplete="name" required placeholder={a.namePlaceholder} value={form.displayName} onChange={set('displayName')} error={errorFor('displayName')}/>}
  <Field name="email" label={a.email} type="email" autoComplete="email" required placeholder={a.emailPlaceholder} value={form.email} onChange={set('email')} error={errorFor('email')}/>
  <Field name="password" label={a.password} type="password" autoComplete={register?'new-password':'current-password'} required minLength={register?8:undefined} placeholder={register?a.passwordHint:''} value={form.password} onChange={set('password')} error={errorFor('password')}/>
  {register&&<label className="consent"><input type="checkbox" name="acceptTerms" checked={accept} onChange={e=>setAccept(e.target.checked)} required/>
   <span>{a.consentBefore}<Link to={PATHS.terms} target="_blank">{a.consentTerms}</Link>{a.consentAnd}<Link to={PATHS.privacy} target="_blank">{a.consentPrivacy}</Link>{a.consentAfter}</span></label>}
  {register&&fieldError(error,'acceptTerms')&&<p role="alert" className="field-error">{fieldError(error,'acceptTerms')}</p>}
  {general&&<p role="alert" className="feedback danger">{general}</p>}
  <button className="button dark-button" disabled={busy||(register&&!accept)}>{busy?(register?a.registering:a.loggingIn):(register?a.register:a.login)}</button>
 </form>;
}

// Шаг 2 регистрации: 6 цифр из письма → аккаунт создан и сразу вход.
function VerifyEmailForm({email,resend,back}:{email:string;resend:()=>Promise<unknown>;back:()=>void}){
 const auth=useAuth();const navigate=useNavigate();const t=useT();const a=t.auth;
 const [code,setCode]=useState('');const [note,setNote]=useState<string|null>(null);
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);
 const run=async(action:()=>Promise<void>)=>{setBusy(true);setError(null);setNote(null);
  try{await action()}catch(err){setError(asApiError(err,t.common))}finally{setBusy(false)}};
 const submit=(e:FormEvent)=>{e.preventDefault();
  run(async()=>{await auth.verifyEmail({email,code:code.trim()});navigate(PATHS.home,{replace:true})})};
 return <form className="lesson-card auth-form" onSubmit={submit} noValidate><h2>{a.codeTitle}</h2>
  <p>{a.codeSent(email)}</p>
  <Field name="code" label={a.code} type="text" inputMode="numeric" autoComplete="one-time-code" required maxLength={6}
   placeholder="123456" value={code} onChange={e=>setCode(e.target.value.replace(/D/g,''))} error={fieldError(error,'code')}/>
  {note&&<p role="status" className="feedback success">{note}</p>}
  {generalError(error)&&<p role="alert" className="feedback danger">{generalError(error)}</p>}
  <button className="button dark-button" disabled={busy||code.length!==6}>{busy?a.verifying:a.verify}</button>
  <div className="auth-code-links">
   <button type="button" className="text-link" disabled={busy} onClick={()=>run(async()=>{await resend();setNote(a.resent)})}>{a.resend}</button>
   <button type="button" className="text-link" disabled={busy} onClick={back}>{a.back}</button>
  </div>
 </form>;
}

// /profile: GET/PATCH /users/me, PUT /users/me/password, ачивки (из GET /dashboard), «Logg ut» (= удалить токен).
const ACH_ICONS: Record<AchievementId, LucideIcon> = {
 firstDay: Sunrise, firstQuestion: MessageCircleQuestion, firstQuiz: ListChecks, firstReview: Layers, firstHomework: CircleCheckBig,
 weekRhythm: CalendarCheck, curious: Lightbulb, quizFive: Trophy, perfect: Star, learnedTen: Brain, answersFifty: PenLine, noDebts: ShieldCheck,
};

export function Profile(){
 const {user,logout}=useAuth();const t=useT();const p=t.profile;const {tag}=useLang();
 const [dash,setDash]=useState<Dashboard|null>(null);const [best,setBest]=useState(0);const [failed,setFailed]=useState(false);
 useEffect(()=>{
  let off=false;
  dashboardApi.get().then(d=>{if(!off)setDash(d)}).catch(()=>{if(!off)setFailed(true)});
  quizzesApi.progress().then(x=>{if(!off)setBest(bestQuizScore(x))}).catch(()=>{});
  return()=>{off=true};
 },[]);
 const list=dash?achievements(dash,best):null;const got=list?list.filter(a=>a.done).length:0;
 const since=user?.createdAt?new Intl.DateTimeFormat(tag,{month:'long',year:'numeric'}).format(new Date(user.createdAt)):null;
 return <section className="page narrow app-page profile">
  <header className="profile-hero">
   <span className="profile-avatar" aria-hidden="true"><span>{initialOf(user)}</span></span>
   <div className="profile-id"><div className="eyebrow">{p.eyebrow}</div><h1>{user?p.titleNamed(user.displayName):p.title}</h1>
    <p>{user?user.email:p.loading}{since&&<span className="profile-since">{p.memberSince(since)}</span>}</p></div>
  </header>
  {dash&&<div className="profile-stats">
   <div className="stat"><strong>{dash.quizzesCompleted}</strong><span>{p.statQuizzes}</span></div>
   <div className="stat"><strong>{dash.flashcards.learned}</strong><span>{p.statLearned}</span></div>
   <div className="stat"><strong>{dash.activeDays30}</strong><span>{p.statDays}</span></div>
   <div className="stat"><strong>{dash.questionsAsked}</strong><span>{p.statAsked}</span></div>
  </div>}

  {!failed&&<section className="ach" aria-labelledby="ach-title">
   <div className="ach-head"><h2 id="ach-title" className="section-label">{p.achievements}</h2>{list&&<span className="ach-count"><Award size={14}/>{p.achCount(got,list.length)}</span>}</div>
   {!list?<p className="panel-empty">{p.achLoading}</p>
    :<ul className="ach-grid">{list.map(a=>{const Icon=ACH_ICONS[a.id];const [title,text]=p.ach[a.id];
      return <li key={a.id} className={a.done?'ach-item done':'ach-item'}>
       <span className="ach-icon" aria-hidden="true">{a.done?<Icon size={20}/>:<Lock size={17}/>}</span>
       <span className="ach-body"><strong>{title}</strong><small>{text}</small>
        {a.done?<span className="ach-badge"><Check size={12}/>{p.achDone}</span>
         :<span className="ach-meter"><span className="meter"><span style={{width:`${Math.round(a.value/a.target*100)}%`}}/></span><em>{a.value}/{a.target}</em></span>}</span>
      </li>})}</ul>}
  </section>}

  <div className="profile-forms">
   {user&&<ProfileForm key={user.id} user={user}/>}
   <PasswordForm/>
  </div>
  <article className="account-row">
   <span className="account-icon" aria-hidden="true"><LogOut size={18}/></span>
   <div><strong>{t.settings.account}</strong><small>{user?t.settings.loggedInAs(user.email):'…'} {p.logoutText}</small></div>
   <button className="btn btn-sm btn-logout" onClick={logout}><LogOut size={15}/>{t.settings.logout}</button>
  </article>
 </section>;
}

// Часовой пояс не выбирается вручную: берём из браузера и сохраняем вместе с именем.
const browserZone=():string|undefined=>{try{return Intl.DateTimeFormat().resolvedOptions().timeZone||undefined}catch{return undefined}};

function ProfileForm({user}:{user:User}){
 const {updateUser}=useAuth();const t=useT();const p=t.profile;
 // Язык здесь не меняется — только в настройках (для всего сайта и ИИ).
 const [form,setForm]=useState({displayName:user.displayName});
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);const [saved,setSaved]=useState(false);
 // PATCH: «меняются только переданные поля» — шлём только изменённые.
 const changes:Record<string,string>=form.displayName!==user.displayName?{displayName:form.displayName}:{};
 const set=(k:keyof typeof form)=>(e:ChangeEvent<HTMLInputElement>)=>{setSaved(false);setForm(f=>({...f,[k]:e.target.value}))};
 const submit=async(e:FormEvent)=>{e.preventDefault();if(!Object.keys(changes).length)return;setBusy(true);setError(null);
  const zone=browserZone();const body=zone&&zone!==user.timezone?{...changes,timezone:zone}:changes;
  try{updateUser(await usersApi.update(body));setSaved(true)}catch(err){setError(asApiError(err,t.common))}finally{setBusy(false)}};
 return <form className="lesson-card" onSubmit={submit} noValidate><h2>{p.about}</h2>
  <Field name="displayName" label={t.auth.name} type="text" autoComplete="name" required maxLength={100} value={form.displayName} onChange={set('displayName')} error={fieldError(error,'displayName')}/>
  {generalError(error)&&<p role="alert" className="feedback danger">{generalError(error)}</p>}
  {saved&&<p role="status" className="feedback success"><Check size={17}/>{t.common.saved}</p>}
  <button className="button dark-button" disabled={busy||!Object.keys(changes).length}>{busy?t.common.saving:p.save}</button>
 </form>;
}

function PasswordForm(){
 const t=useT();const p=t.profile;
 const [form,setForm]=useState({currentPassword:'',newPassword:''});
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);const [done,setDone]=useState(false);
 const set=(k:keyof typeof form)=>(e:ChangeEvent<HTMLInputElement>)=>{setDone(false);setForm(f=>({...f,[k]:e.target.value}))};
 const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError(null);
  try{await usersApi.changePassword(form);setForm({currentPassword:'',newPassword:''});setDone(true)}catch(err){setError(asApiError(err,t.common))}finally{setBusy(false)}};
 return <form className="lesson-card" onSubmit={submit} noValidate><h2>{p.changePassword}</h2>
  <Field name="currentPassword" label={p.currentPassword} type="password" autoComplete="current-password" required value={form.currentPassword} onChange={set('currentPassword')} error={fieldError(error,'currentPassword')}/>
  <Field name="newPassword" label={p.newPassword} type="password" autoComplete="new-password" required minLength={8} placeholder={t.auth.passwordHint} value={form.newPassword} onChange={set('newPassword')} error={fieldError(error,'newPassword')}/>
  {generalError(error)&&<p role="alert" className="feedback danger">{generalError(error)}</p>}
  {done&&<p role="status" className="feedback success"><Check size={17}/>{p.passwordChanged}</p>}
  <button className="button" disabled={busy||!form.currentPassword||!form.newPassword}>{busy?t.common.saving:p.changePassword}</button>
 </form>;
}
