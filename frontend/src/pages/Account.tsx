// Страницы аккаунта на настоящем API: /login, /register, /profile (API.md).
import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Check, LogOut } from 'lucide-react';
import { PATHS } from '../paths';
import { useAuth, initialOf } from '../auth';
import { usersApi } from '../api/endpoints';
import type { ApiError, User } from '../api/types';
import { Field, asApiError, fieldError, generalError } from '../forms';
import { Orb } from '../components/Orb';
import { useT } from '../i18n';

// POST /auth/login | /auth/register → токен → на «/».
// Ошибки: 400 → fields под полями, 409 → message у поля email, остальное → над кнопкой.
export function Auth({register=false}:{register?:boolean}){
 const auth=useAuth();const t=useT();const a=t.auth;
 if(auth.token)return <Navigate to={PATHS.home} replace/>;
 return <section className="page auth-page"><Orb size={56}/><div className="eyebrow">{a.eyebrow}</div><h1>{register?a.titleRegister:a.titleLogin}</h1><p>{a.intro}</p><AuthForm register={register}/><Link className="text-link" to={register?PATHS.login:PATHS.register}>{register?a.toLogin:a.toRegister}</Link></section>;
}

// Сама форма — та же на /login, /register и в центре лендинга.
export function AuthForm({register=false}:{register?:boolean}){
 const auth=useAuth();const navigate=useNavigate();const t=useT();const a=t.auth;
 const [form,setForm]=useState({email:'',password:'',displayName:''});
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);
 const set=(k:keyof typeof form)=>(e:ChangeEvent<HTMLInputElement>)=>setForm(f=>({...f,[k]:e.target.value}));
 const errorFor=(k:string)=>fieldError(error,k)??(error?.status===409&&k==='email'?error.message:undefined);
 const general=error?.status===409?null:generalError(error);
 const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError(null);
  try{if(register)await auth.register(form);else await auth.login({email:form.email,password:form.password});navigate(PATHS.home,{replace:true})}
  catch(err){setError(asApiError(err,t.common))}
  finally{setBusy(false)}};
 return <form className="lesson-card auth-form" onSubmit={submit} noValidate><h2>{register?a.register:a.login}</h2>
  {register&&<Field name="displayName" label={a.name} type="text" autoComplete="name" required placeholder={a.namePlaceholder} value={form.displayName} onChange={set('displayName')} error={errorFor('displayName')}/>}
  <Field name="email" label={a.email} type="email" autoComplete="email" required placeholder={a.emailPlaceholder} value={form.email} onChange={set('email')} error={errorFor('email')}/>
  <Field name="password" label={a.password} type="password" autoComplete={register?'new-password':'current-password'} required minLength={register?8:undefined} placeholder={register?a.passwordHint:''} value={form.password} onChange={set('password')} error={errorFor('password')}/>
  {general&&<p role="alert" className="feedback danger">{general}</p>}
  <button className="button dark-button" disabled={busy}>{busy?(register?a.registering:a.loggingIn):(register?a.register:a.login)}</button>
 </form>;
}

// /profile: GET/PATCH /users/me, PUT /users/me/password, «Logg ut» (= удалить токен).
export function Profile(){
 const {user,logout}=useAuth();const t=useT();const p=t.profile;
 return <section className="page narrow"><div className="page-intro"><span className="avatar large">{initialOf(user)}</span><div className="eyebrow">{p.eyebrow}</div><h1>{user?p.titleNamed(user.displayName):p.title}</h1><p>{user?user.email:p.loading}</p></div>
  {user&&<ProfileForm key={user.id} user={user}/>}
  <PasswordForm/>
  <article className="lesson-card"><h2>{t.settings.logout}</h2><p>{p.logoutText}</p><button className="button" onClick={logout}><LogOut size={16}/>{t.settings.logout}</button></article>
 </section>;
}

const timezones=():string[]=>{try{return Intl.supportedValuesOf('timeZone')}catch{return []}};

function ProfileForm({user}:{user:User}){
 const {updateUser}=useAuth();const t=useT();const p=t.profile;
 // Язык здесь не меняется — только в настройках (для всего сайта и ИИ).
 const [form,setForm]=useState({displayName:user.displayName,timezone:user.timezone});
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);const [saved,setSaved]=useState(false);
 const zones=timezones();if(!zones.includes(form.timezone))zones.unshift(form.timezone);
 // PATCH: «меняются только переданные поля» — шлём только изменённые.
 const changes=Object.fromEntries(Object.entries(form).filter(([k,v])=>v!==user[k as keyof typeof form]));
 const set=<K extends keyof typeof form>(k:K)=>(e:ChangeEvent<HTMLInputElement|HTMLSelectElement>)=>{setSaved(false);setForm(f=>({...f,[k]:e.target.value}))};
 const submit=async(e:FormEvent)=>{e.preventDefault();if(!Object.keys(changes).length)return;setBusy(true);setError(null);
  try{updateUser(await usersApi.update(changes));setSaved(true)}catch(err){setError(asApiError(err,t.common))}finally{setBusy(false)}};
 const selectError=(k:string)=>fieldError(error,k);
 return <form className="lesson-card" onSubmit={submit} noValidate><h2>{p.about}</h2>
  <Field name="displayName" label={t.auth.name} type="text" autoComplete="name" required maxLength={100} value={form.displayName} onChange={set('displayName')} error={fieldError(error,'displayName')}/>
  <label className="field">{p.timezone}<select name="timezone" value={form.timezone} onChange={set('timezone')} aria-invalid={!!selectError('timezone')}>{zones.map(z=><option key={z} value={z}>{z.replace(/_/g,' ')}</option>)}</select>{selectError('timezone')&&<span className="field-error">{selectError('timezone')}</span>}</label>
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
