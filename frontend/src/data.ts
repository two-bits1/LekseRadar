import { PATHS } from './paths';
export const assignments = [
  {id:'likninger', subject:'Matematikk', title:'Likninger med én ukjent', description:'Jobb med oppgave 4.1–4.8 på side 92. Vis hvordan du tenker.', date:'9. nov.', day:9, source:'itslearning', color:'sage', icon:'x', duration:'25 min', urgent:true},
  {id:'novelle', subject:'Norsk', title:'En liten tekst, en stor historie', description:'Les «Karens jul» og finn kjennetegn på en novelle.', date:'10. nov.', day:10, source:'Canvas', color:'peach', icon:'Aa', duration:'30 min', urgent:false},
  {id:'energi', subject:'Naturfag', title:'Energi som aldri forsvinner', description:'Utforsk energikjeder og energibevaring i kapittel 3.', date:'11. nov.', day:11, source:'Teams', color:'blue', icon:'e', duration:'20 min', urgent:false},
  {id:'english', subject:'Engelsk', title:'A voice of your own', description:'Forbered en kort presentasjon om en person som inspirerer deg.', date:'12. nov.', day:12, source:'Canvas', color:'sand', icon:'En', duration:'35 min', urgent:false},
];
export const decks = [
  ['Hva er en ukjent?', 'Et tall vi ikke kjenner ennå. Vi bruker ofte bokstaven x.'],
  ['Hva betyr likhetstegnet?', 'At uttrykkene på begge sider har samme verdi.'],
  ['Hvordan holder du en likning i balanse?', 'Du gjør den samme operasjonen på begge sider.'],
  ['Hva er en koeffisient?', 'Tallet som multipliseres med en variabel. I 3x er koeffisienten 3.'],
  ['Hva er et konstantledd?', 'Et tall uten variabel, for eksempel 5 i uttrykket 2x + 5.'],
  ['Hvordan sjekker du løsningen?', 'Sett verdien inn i den opprinnelige likningen og sammenlign sidene.'],
];
// Сайт-карта для /status — те же адреса, что в API.md (см. src/paths.ts).
export const routes = [
  [PATHS.home,'Oversikt','Interaktiv demo'], [PATHS.homework,'Lekser og kalender','Interaktiv demo'], [PATHS.chats,'AI-veileder','Forhåndsskrevet demo'], [PATHS.chat(':chatId'),'Samtale','Kommer senere'], [PATHS.quizzes,'Quiz','Interaktiv demo'], [PATHS.flashcards,'Læringskort','Interaktiv demo'], [PATHS.progress,'Fremgang','Kommer senere'], [PATHS.history,'Historikk','Kommer senere'], [PATHS.profile,'Profil','Demodata'], [PATHS.settings,'Innstillinger','Delvis klar'], [PATHS.platforms,'Plattformer','Kommer senere'], [PATHS.login,'Logg inn','Kun gjestetilgang'], [PATHS.register,'Opprett konto','Kommer senere'],
];
