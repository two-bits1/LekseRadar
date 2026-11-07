// Страницы аккаунта на настоящем API: /login, /register, /profile (API.md).
import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { ArrowUpRight, Check, LogOut } from 'lucide-react';
import { assignments } from '../data';
import { PATHS } from '../paths';
import { useAuth, initialOf } from '../auth';
import { usersApi } from '../api/endpoints';
import type { ApiError, Locale, User } from '../api/types';
import { Field, LOCALES, asApiError, fieldError, generalError } from '../forms';

// POST /auth/login | /auth/register → токен → на «/».
// Ошибки: 400 → fields под полями, 409 → message у поля email, остальное → над кнопкой.
export function Auth({register=false}:{register?:boolean}){
 const auth=useAuth();const navigate=useNavigate();
 const [form,setForm]=useState({email:'',password:'',displayName:''});
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);
 if(auth.token)return <Navigate to={PATHS.home} replace/>;
 const set=(k:keyof typeof form)=>(e:ChangeEvent<HTMLInputElement>)=>setForm(f=>({...f,[k]:e.target.value}));
 const errorFor=(k:string)=>fieldError(error,k)??(error?.status===409&&k==='email'?error.message:undefined);
 const general=error?.status===409?null:generalError(error);
 const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError(null);
  try{if(register)await auth.register(form);else await auth.login({email:form.email,password:form.password});navigate(PATHS.home,{replace:true})}
  catch(err){setError(asApiError(err))}
  finally{setBusy(false)}};
 return <section className="page auth-page"><div className="eyebrow">VELKOMMEN TIL LEKSERADAR</div><h1>{register?'En litt enklere skolehverdag.':'Fint å ha deg her.'}</h1><p>Mer oversikt. Mindre stress. Plass til å forstå.</p><form className="lesson-card" onSubmit={submit} noValidate><h2>{register?'Opprett konto':'Logg inn'}</h2>
  {register&&<Field name="displayName" label="Navn" type="text" autoComplete="name" required placeholder="Fornavn" value={form.displayName} onChange={set('displayName')} error={errorFor('displayName')}/>}
  <Field name="email" label="E-post" type="email" autoComplete="email" required placeholder="deg@skole.no" value={form.email} onChange={set('email')} error={errorFor('email')}/>
  <Field name="password" label="Passord" type="password" autoComplete={register?'new-password':'current-password'} required minLength={register?8:undefined} placeholder={register?'Minst 8 tegn':''} value={form.password} onChange={set('password')} error={errorFor('password')}/>
  {general&&<p role="alert" className="feedback danger">{general}</p>}
  <button className="button dark-button" disabled={busy}>{busy?(register?'Oppretter konto…':'Logger inn…'):(register?'Opprett konto':'Logg inn')}</button>
 </form><Link className="text-link" to={register?PATHS.login:PATHS.register}>{register?'Har du allerede en konto? Logg inn':'Ny her? Opprett en konto'}</Link></section>}

// /profile: GET/PATCH /users/me, PUT /users/me/password, «Logg ut» (= удалить токен).
export function Profile({completed}:{completed:string[]}){
 const {user,logout}=useAuth();
 return <section className="page narrow"><div className="page-intro"><span className="avatar large">{initialOf(user)}</span><div className="eyebrow">DIN PROFIL</div><h1>{user?`Du er på vei, ${user.displayName}.`:'Du er på vei.'}</h1><p>{user?user.email:'Henter profilen …'}</p></div>
  <div className="profile-stats"><article><strong>{completed.length}</strong><span>fullførte oppgaver</span></article><article><strong>4</strong><span>fag å utforske</span></article><article><strong>{Math.round(completed.length/4*100)}%</strong><span>av ukas oppgaver</span></article></div>
  <article className="lesson-card"><h2>Det du har fått til</h2>{completed.length?assignments.filter(a=>completed.includes(a.id)).map(a=><p key={a.id} className="text-link"><Check size={16}/>{a.title}</p>):<p>Her dukker oppgavene opp når du markerer dem som fullført.</p>}<Link className="text-link" to={PATHS.home}>Til oppgavene <ArrowUpRight size={16}/></Link></article>
  {user&&<ProfileForm key={user.id} user={user}/>}
  <PasswordForm/>
  <article className="lesson-card"><h2>Logg ut</h2><p>Du logges ut på denne enheten.</p><button className="button" onClick={logout}><LogOut size={16}/>Logg ut</button></article>
  <p className="quiet-note">Oppgavene og fremgangen over er fortsatt demodata og nullstilles når siden lastes på nytt.</p>
 </section>;
}

const timezones=():string[]=>{try{return Intl.supportedValuesOf('timeZone')}catch{return []}};

function ProfileForm({user}:{user:User}){
 const {updateUser}=useAuth();
 const [form,setForm]=useState({displayName:user.displayName,locale:user.locale,timezone:user.timezone});
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);const [saved,setSaved]=useState(false);
 const zones=timezones();if(!zones.includes(form.timezone))zones.unshift(form.timezone);
 // PATCH: «меняются только переданные поля» — шлём только изменённые.
 const changes=Object.fromEntries(Object.entries(form).filter(([k,v])=>v!==user[k as keyof typeof form]));
 const set=<K extends keyof typeof form>(k:K)=>(e:ChangeEvent<HTMLInputElement|HTMLSelectElement>)=>{setSaved(false);setForm(f=>({...f,[k]:e.target.value}))};
 const submit=async(e:FormEvent)=>{e.preventDefault();if(!Object.keys(changes).length)return;setBusy(true);setError(null);
  try{updateUser(await usersApi.update(changes));setSaved(true)}catch(err){setError(asApiError(err))}finally{setBusy(false)}};
 const selectError=(k:string)=>fieldError(error,k);
 return <form className="lesson-card" onSubmit={submit} noValidate><h2>Om deg</h2><p>Språket gjelder også AI-veilederen.</p>
  <Field name="displayName" label="Navn" type="text" autoComplete="name" required maxLength={100} value={form.displayName} onChange={set('displayName')} error={fieldError(error,'displayName')}/>
  <label className="field">Språk<select name="locale" value={form.locale} onChange={set('locale')} aria-invalid={!!selectError('locale')}>{(Object.keys(LOCALES) as Locale[]).map(l=><option key={l} value={l}>{LOCALES[l]}</option>)}</select>{selectError('locale')&&<span className="field-error">{selectError('locale')}</span>}</label>
  <label className="field">Tidssone<select name="timezone" value={form.timezone} onChange={set('timezone')} aria-invalid={!!selectError('timezone')}>{zones.map(z=><option key={z} value={z}>{z.replace(/_/g,' ')}</option>)}</select>{selectError('timezone')&&<span className="field-error">{selectError('timezone')}</span>}</label>
  {generalError(error)&&<p role="alert" className="feedback danger">{generalError(error)}</p>}
  {saved&&<p role="status" className="feedback success"><Check size={17}/>Lagret.</p>}
  <button className="button dark-button" disabled={busy||!Object.keys(changes).length}>{busy?'Lagrer…':'Lagre endringer'}</button>
 </form>;
}

function PasswordForm(){
 const [form,setForm]=useState({currentPassword:'',newPassword:''});
 const [error,setError]=useState<ApiError|null>(null);const [busy,setBusy]=useState(false);const [done,setDone]=useState(false);
 const set=(k:keyof typeof form)=>(e:ChangeEvent<HTMLInputElement>)=>{setDone(false);setForm(f=>({...f,[k]:e.target.value}))};
 const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError(null);
  try{await usersApi.changePassword(form);setForm({currentPassword:'',newPassword:''});setDone(true)}catch(err){setError(asApiError(err))}finally{setBusy(false)}};
 return <form className="lesson-card" onSubmit={submit} noValidate><h2>Bytt passord</h2>
  <Field name="currentPassword" label="Nåværende passord" type="password" autoComplete="current-password" required value={form.currentPassword} onChange={set('currentPassword')} error={fieldError(error,'currentPassword')}/>
  <Field name="newPassword" label="Nytt passord" type="password" autoComplete="new-password" required minLength={8} placeholder="Minst 8 tegn" value={form.newPassword} onChange={set('newPassword')} error={fieldError(error,'newPassword')}/>
  {generalError(error)&&<p role="alert" className="feedback danger">{generalError(error)}</p>}
  {done&&<p role="status" className="feedback success"><Check size={17}/>Passordet er endret.</p>}
  <button className="button" disabled={busy||!form.currentPassword||!form.newPassword}>{busy?'Lagrer…':'Bytt passord'}</button>
 </form>;
}
