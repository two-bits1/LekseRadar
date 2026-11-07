import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, ArrowRight, RotateCcw, Check, CircleCheck, CircleAlert, Trophy } from 'lucide-react';
import { decks } from '../data';
import { PATHS } from '../paths';
import { useT } from '../i18n';

// Содержание упражнений (карточки, вопрос квиза, задача) — демо-материал норвежской школы, не переводится.
const QUIZ_ANSWER='Gjør det samme på begge sider';
type Mode='cards'|'quiz'|'practice';
const MODES:Mode[]=['cards','quiz','practice'];

export function Flashcards({initialMode='cards'}:{initialMode?:Mode}){
 const t=useT();const l=t.learning;
 const [index,setIndex]=useState(0);const [flipped,setFlipped]=useState(false);const [known,setKnown]=useState<number[]>([]);const [mode,setMode]=useState<Mode>(initialMode);const [answer,setAnswer]=useState<string|null>(null);const [practice,setPractice]=useState('');const [checked,setChecked]=useState(false);
 const next=(step:number)=>{setIndex(i=>(i+step+decks.length)%decks.length);setFlipped(false)};
 return <section className="page narrow"><div className="page-intro"><div className="eyebrow">{l.eyebrow}</div><h1>{l.title}</h1><p lang="nb">Matematikk · Likninger med én ukjent</p></div>
  <div className="study-toolbar"><div className="filters">{MODES.map(m=><button key={m} aria-pressed={mode===m} className={mode===m?'selected':''} onClick={()=>setMode(m)}>{l.modes[m]}</button>)}</div><span>{l.learned(known.length,decks.length)}</span></div>
  {mode==='cards'?<><button className={`flip-card ${flipped?'flipped':''}`} onClick={()=>setFlipped(!flipped)} aria-label={`${flipped?l.answer:l.question}: ${decks[index][flipped?1:0]}. ${l.flip}`}><span className="eyebrow">{flipped?l.answerEyebrow:l.think} · {index+1} / {decks.length}</span><strong lang="nb">{decks[index][flipped?1:0]}</strong><span className="flip-hint"><RotateCcw size={14}/>{l.flipHint}</span></button>
   <div className="card-controls"><button aria-label={l.prev} onClick={()=>next(-1)}><ArrowLeft size={18}/></button><div className="actions"><button onClick={()=>{setKnown(k=>k.filter(n=>n!==index));next(1)}}>{l.practiceMore}</button><button className="button dark-button" onClick={()=>{setKnown(k=>k.includes(index)?k:[...k,index]);next(1)}}><Check size={16}/>{l.iKnowThis}</button></div><button aria-label={l.next} onClick={()=>next(1)}><ArrowRight size={18}/></button></div>
   <div className="progress-track"><span style={{width:`${known.length/decks.length*100}%`}}/></div>{known.length===decks.length&&<p role="status" className="feedback reward"><Trophy size={17}/>{l.allLearned}</p>}</>
  :mode==='quiz'?<article className="lesson-card"><div className="eyebrow">{l.quizEyebrow}</div><h2 lang="nb">Hva gjør du for å bevare balansen i en likning?</h2><div className="quiz-options" lang="nb">{['Endrer bare venstre side','Gjør det samme på begge sider','Fjerner likhetstegnet'].map(a=><button key={a} onClick={()=>setAnswer(a)} aria-pressed={answer===a} className={answer===a?`selected ${a===QUIZ_ANSWER?'correct':'wrong'}`:''}>{a}</button>)}</div>{answer&&(answer===QUIZ_ANSWER?<p role="status" lang="nb" className="feedback success"><CircleCheck size={17}/>Riktig! Begge sider må fortsatt ha samme verdi.</p>:<p role="status" lang="nb" className="feedback danger"><CircleAlert size={17}/>Prøv igjen. Tenk på en vekt som skal være i balanse.</p>)}</article>
  :<article className="lesson-card"><div className="eyebrow">{l.practiceEyebrow}</div><h2 lang="nb">En eske og tre blyanter gir åtte blyanter.</h2><p lang="nb">Hvor mange blyanter er det i esken?</p><form onSubmit={e=>{e.preventDefault();setChecked(true)}}><label className="field">{l.yourAnswer}<input type="number" required value={practice} onChange={e=>{setPractice(e.target.value);setChecked(false)}}/></label><button className="button dark-button">{l.check}</button></form>{checked&&(Number(practice)===5?<p role="status" lang="nb" className="feedback success"><CircleCheck size={17}/>Riktig! Åtte minus tre er fem.</p>:<p role="status" lang="nb" className="feedback warning"><CircleAlert size={17}/>Ikke helt. Trekk de tre løse blyantene fra totalen.</p>)}</article>}
  <p className="quiet-note">{l.demoNote1}<br/>{l.demoNote2}</p><Link className="text-link" to={PATHS.chats}>{t.dashboard.askTutor} <ArrowRight size={14}/></Link></section>;
}
