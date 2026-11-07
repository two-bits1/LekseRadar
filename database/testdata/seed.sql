-- Тестовые данные LekseRadar: ученик, ДЗ по 10 предметам, 15 курсов (уровень первого курса университета / R2, Fysikk 2),
-- готовые квизы, учебные материалы и один пройденный квиз. Только для демо и тестов, НЕ для продакшена.
--
-- Вход: test@lekseradar.no / LekseRadar2026
-- Запуск: psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f database/testdata/seed.sql
--         или docker compose --profile testdata up   (из папки database/)
-- Повторный запуск пересоздаёт тестового ученика; сроки ДЗ всегда считаются от текущего времени.

BEGIN;

DELETE FROM users WHERE email = 'test@lekseradar.no';   -- CASCADE: ДЗ, чаты, квизы, карточки, события

CREATE TEMP TABLE seed_user ON COMMIT DROP AS
WITH u AS (
    INSERT INTO users (email, password_hash, display_name, locale, timezone)
    VALUES ('test@lekseradar.no', '$2a$10$T.wjgVDW3inSRzCuQ5mUlOxrl.S9GOjIADSuZUfxBtCEWOC.s7Y3i',
            'Test Elev', 'nb', 'Europe/Oslo')
    RETURNING id)
SELECT id FROM u;

-- ДЗ: (платформа, id, название, курс, срок от сейчас, статус, вид, текст задания)
INSERT INTO assignments (user_id, source, external_id, title, course, due_at, status, kind, description)
SELECT (SELECT id FROM seed_user), d.source::platform, d.ext, d.title, d.course,
       CASE WHEN d.due IS NULL THEN NULL ELSE date_trunc('hour', now()) + d.due::interval END,
       d.status::assignment_status, d.kind, d.description
FROM (VALUES
('canvas', 'seed-mat-1', 'Oblig 3: Taylorpolynomer og feilestimat', 'MAT1100 Kalkulus', '2 days 23:59', 'todo', 'assignment',
$t$**Oppgave 1.** Finn Taylorpolynomet av grad 4 til $f(x) = \ln(1+x)$ om $x = 0$.

**Oppgave 2.** Bruk Taylors formel med restledd til å vise at
$$\left| \ln(1.2) - T_4(0.2) \right| < 10^{-4}.$$

**Oppgave 3.** Bestem grenseverdien $\displaystyle \lim_{x\to 0} \frac{e^{x^2} - 1 - x^2}{x^4}$ ved hjelp av Taylorrekker.

**Oppgave 4.** Hvor mange ledd trengs i Maclaurinrekken til $\sin x$ for å beregne $\sin(1)$ med feil mindre enn $10^{-8}$? Begrunn.

Lever som PDF. Vis alle mellomregninger.$t$),
('canvas', 'seed-mat-2', 'Ukeoppgaver 9: Egenverdier og diagonalisering', 'MAT1120 Lineær algebra', '5 days', 'todo', 'assignment',
$t$La $$A = \begin{pmatrix} 4 & 1 \\ 2 & 3 \end{pmatrix}.$$

a) Finn egenverdiene og egenvektorene til $A$.

b) Diagonaliser $A$, altså finn $P$ og $D$ slik at $A = PDP^{-1}$.

c) Bruk b) til å regne ut $A^{10}$ uten å multiplisere matrisen med seg selv ti ganger.

d) Vis at en symmetrisk $2\times 2$-matrise alltid har reelle egenverdier.

e) Løs systemet $\mathbf{x}'(t) = A\mathbf{x}(t)$ med $\mathbf{x}(0) = (1, 0)^T$.$t$),
('canvas', 'seed-mat-3', 'Innlevering: Andre ordens lineære differensiallikninger', 'MAT1110 Kalkulus og lineær algebra', '-1 day', 'todo', 'assignment',
$t$**1.** Løs $y'' + 4y' + 13y = 0$ med $y(0) = 1$, $y'(0) = 0$.

**2.** Finn den generelle løsningen av $y'' - 3y' + 2y = e^{3x} + x$ (ubestemte koeffisienters metode).

**3.** En fjær med demping beskrives av $m\ddot{x} + c\dot{x} + kx = 0$. For $m = 1$, $k = 9$: for hvilke $c$ er systemet underdempet, kritisk dempet og overdempet? Skisser løsningene.$t$),
('teams', 'seed-fys-1', 'Lab-rapport: Dempet harmonisk svingning', 'FYS-MEK1110 Mekanikk', '3 days', 'todo', 'assignment',
$t$Dere har målt utslaget til en pendel med luftmotstand (data i `pendel.csv` på Teams).

1. Utled bevegelseslikningen for en pendel med lineær luftmotstand $F = -b v$ og vis at den for små utslag blir $\ddot\theta + \gamma\dot\theta + \omega_0^2\theta = 0$.
2. Tilpass $\theta(t) = \theta_0 e^{-\gamma t/2}\cos(\omega t + \varphi)$ til dataene og bestem $\gamma$ og $\omega$ med usikkerhet.
3. Beregn Q-faktoren og sammenlign med $\omega_0/\gamma$.
4. Diskuter feilkilder (små-vinkel-tilnærming, målefrekvens, friksjon i opphenget).

Rapporten skal følge malen: innledning, teori, metode, resultater, diskusjon, konklusjon. Maks 6 sider.$t$),
('teams', 'seed-fys-2', 'Oppgavesett 6: Gauss'' lov', 'FYS1120 Elektromagnetisme', '7 days', 'todo', 'assignment',
$t$**a)** En uendelig lang sylinder med radius $R$ har volumladningstetthet $\rho(r) = \rho_0 \, r/R$. Finn $\vec E(r)$ både innenfor og utenfor sylinderen.

**b)** Finn potensialforskjellen mellom $r = R$ og $r = 2R$.

**c)** Forklar hvorfor Gauss' lov alene ikke er nok til å finne feltet fra en endelig lang ladet stav.

**d)** En kuleskall-kondensator har indre radius $a$ og ytre radius $b$. Vis at $C = 4\pi\varepsilon_0 \frac{ab}{b-a}$.$t$),
('itslearning', 'seed-kje-1', 'Gibbs fri energi og kjemisk likevekt', 'KJM1100 Generell kjemi', '4 days', 'todo', 'assignment',
$t$For reaksjonen $\mathrm{N_2(g) + 3H_2(g) \rightleftharpoons 2NH_3(g)}$ er $\Delta H^\circ = -92{,}2$ kJ/mol og $\Delta S^\circ = -198{,}7$ J/(mol·K).

1. Beregn $\Delta G^\circ$ og likevektskonstanten $K$ ved 298 K og ved 700 K.
2. Over hvilken temperatur er reaksjonen ikke lenger spontan under standardbetingelser?
3. Forklar med Le Chateliers prinsipp hvorfor Haber–Bosch-prosessen likevel kjøres ved høy temperatur og høyt trykk.
4. Bruk van 't Hoff-likningen til å anslå $K$ ved 500 K og sammenlign med svaret fra 1.$t$),
('itslearning', 'seed-kje-2', 'Organisk kjemi: SN1 og SN2', 'KJM1101 Organisk kjemi', '-3 days', 'submitted', 'assignment',
$t$Forklar forskjellen mellom SN1- og SN2-mekanismen med reaksjonsdiagram. For hver av forbindelsene 2-brom-2-metylpropan, 1-brombutan og (R)-2-brombutan: forutsi mekanisme i henholdsvis vann/etanol og aceton med NaI, og angi stereokjemien til produktet.$t$),
('canvas', 'seed-inf-1', 'Oblig 2: Korteste vei med Dijkstra', 'IN2010 Algoritmer og datastrukturer', '6 days', 'todo', 'assignment',
$t$Implementer Dijkstras algoritme med binær haug (prioritetskø) i Java eller Python.

1. Les en vektet, rettet graf fra fil (format i oppgaveteksten på Canvas).
2. Skriv ut korteste avstand og sti fra startnoden til alle andre noder.
3. Analyser kjøretiden: vis at den er $O((V+E)\log V)$.
4. Forklar hvorfor algoritmen ikke fungerer med negative kantvekter, og gi et moteksempel med tre noder.
5. Sammenlign med Bellman–Ford på en graf med $10^5$ noder.

Koden leveres i GitHub Classroom, rapporten som PDF.$t$),
('canvas', 'seed-inf-2', 'Databaser: Normalisering og SQL', 'IN2090 Databaser og datamodellering', '9 days', 'todo', 'assignment',
$t$Gitt relasjonen `Emne(emnekode, emnenavn, studiepoeng, foreleser, foreleser_epost, semester, rom)` med de funksjonelle avhengighetene
- emnekode → emnenavn, studiepoeng
- foreleser → foreleser_epost
- emnekode, semester → foreleser, rom

a) Finn alle kandidatnøkler.
b) Hvilken normalform er relasjonen på? Begrunn.
c) Dekomponer tapsfritt til BCNF og vis at dekomposisjonen bevarer avhengighetene (eller forklar hvorfor ikke).
d) Skriv SQL som finner foreleserne som har undervist i minst tre ulike emner, sortert etter antall.$t$),
('inschool', 'seed-bio-1', 'Genregulering hos prokaryoter og CRISPR', 'BIOS1120 Cellebiologi', '8 days', 'todo', 'assignment',
$t$1. Beskriv lac-operonet og forklar hvordan både laktose og glukose påvirker transkripsjonen (repressor, CAP, cAMP).
2. Hva skjer med uttrykket hvis lacI er mutert slik at repressoren ikke binder allolaktose? Hva om operatoren er mutert?
3. Forklar hvordan CRISPR-Cas9 fungerer som immunforsvar i bakterier, og hvordan det brukes som verktøy for genredigering.
4. Drøft én etisk problemstilling ved bruk av CRISPR på mennesker (maks 300 ord).$t$),
('canvas', 'seed-oko-1', 'Konsumentteori: Nyttemaksimering', 'ECON1210 Mikroøkonomi', '10 days', 'todo', 'assignment',
$t$En konsument har nyttefunksjonen $U(x, y) = x^{0{,}4} y^{0{,}6}$, inntekt $m = 1000$ og prisene $p_x = 10$, $p_y = 20$.

a) Sett opp og løs maksimeringsproblemet med Lagrange-metoden.
b) Finn de Marshallske etterspørselsfunksjonene $x(p_x, p_y, m)$ og $y(p_x, p_y, m)$.
c) Prisen på $x$ øker til 15. Del opp endringen i etterspurt mengde i substitusjons- og inntektseffekt (Slutsky).
d) Er $x$ og $y$ substitutter eller komplementer? Begrunn med krysspriselastisiteten.$t$),
('canvas', 'seed-sta-1', 'Hypotesetesting og konfidensintervall', 'STK1100 Sannsynlighetsregning og statistikk', '1 day 12:00', 'todo', 'exam',
$t$Hjemmeeksamen (4 timer). Tillatte hjelpemidler: alle.

En produsent påstår at batteriene varer i gjennomsnitt 500 timer. Et utvalg på $n = 25$ batterier ga $\bar x = 486$ timer og $s = 30$ timer.

a) Formuler hypotesene og test påstanden på 5 % nivå. Hvilke antakelser gjør du?
b) Finn et 95 % konfidensintervall for forventet levetid.
c) Beregn p-verdien og tolk den.
d) Hvor stort utvalg trengs for at intervallet skal ha bredde under 10 timer?
e) Forklar forskjellen på type I- og type II-feil i denne sammenhengen.$t$),
('itslearning', 'seed-his-1', 'Kildeanalyse: Den kalde krigen', 'HIS1300 Moderne historie', '-10 days', 'graded', 'assignment',
$t$Analyser Churchills «Iron Curtain»-tale (1946) og Novikov-telegrammet (1946). Hvem var avsender og mottaker, hva var formålet, og hvordan kan kildene brukes til å forstå framveksten av den kalde krigen? 1500–2000 ord, Chicago-referansestil.$t$),
('teams', 'seed-eng-1', 'Argumentative essay: AI in higher education', 'ENG1100 Academic English', '14 days', 'todo', 'assignment',
$t$Write an argumentative essay (1,200–1,500 words) on the question: *Should universities allow students to use generative AI in graded coursework?*

Requirements:
- a clear thesis in the introduction;
- at least three arguments, and a fair presentation of the strongest counterargument;
- at least four academic sources, APA 7th edition;
- formal academic register, no contractions.

Submit a one-paragraph outline by Friday for feedback.$t$),
('inschool', 'seed-fil-1', 'Ex.phil: Kant mot utilitarismen', 'EXPHIL03 Examen philosophicum', '11 days', 'todo', 'assignment',
$t$Gjør rede for Kants kategoriske imperativ (særlig formelen om mennesket som mål i seg selv) og for Mills utilitarisme.
Drøft deretter følgende tilfelle: en lege kan redde fem pasienter ved å ta organene til én frisk pasient. Hva sier de to teoriene, og hvilken står sterkest? 1200–1500 ord.$t$),
('canvas', 'seed-mat-4', 'Quiz: Rekker og konvergens', 'MAT1100 Kalkulus', '20 days', 'todo', 'quiz',
$t$Digital quiz på Canvas, 30 minutter: konvergenstester (sammenligning, forholdstest, rottest, integraltest), alternerende rekker og potensrekkers konvergensradius.$t$)
) AS d(source, ext, title, course, due, status, kind, description);

-- Ученик отметил одно задание сданным, учитель оценил другое — то же, что сделал бы API
INSERT INTO user_events (user_id, type, ref_table, ref_id, created_at)
SELECT a.user_id, 'homework_done', 'assignments', a.id, now() - interval '3 days'
FROM assignments a WHERE a.user_id = (SELECT id FROM seed_user) AND a.external_id = 'seed-kje-2';

-- Готовые квизы (общие, created_by NULL): ИИ не нужен, POST /quizzes по этой теме отвечает сразу
SELECT import_quiz_json(q::jsonb) FROM (VALUES
($q${"subject":"Matematikk","topic":"Taylorpolynomer","level":"advanced","language":"nb","title":"Taylorpolynomer og restledd","model":"testdata","questions":[
 {"type":"single_choice","question":"Hva er Taylorpolynomet av grad 3 til $e^x$ om $x=0$?","options":["$1 + x + \\frac{x^2}{2} + \\frac{x^3}{6}$","$1 + x + x^2 + x^3$","$x + \\frac{x^3}{6}$","$1 + x + \\frac{x^2}{2} + \\frac{x^3}{3}$"],"correct_answer":"$1 + x + \\frac{x^2}{2} + \\frac{x^3}{6}$","explanation":"Koeffisienten foran $x^n$ er $f^{(n)}(0)/n! = 1/n!$. En vanlig feil er å glemme fakultetet."},
 {"type":"single_choice","question":"Hva er $\\lim_{x\\to 0} \\frac{\\sin x - x}{x^3}$?","options":["$-\\frac{1}{6}$","$0$","$\\frac{1}{6}$","Grensen eksisterer ikke"],"correct_answer":"$-\\frac{1}{6}$","explanation":"$\\sin x = x - x^3/6 + O(x^5)$, så telleren er $-x^3/6 + O(x^5)$."},
 {"type":"multiple_choice","question":"Hvilke påstander om Lagranges restledd $R_n(x) = \\frac{f^{(n+1)}(c)}{(n+1)!}(x-a)^{n+1}$ stemmer? (flere riktige)","options":["$c$ ligger mellom $a$ og $x$","Restleddet er alltid positivt","Det kan brukes til å gi en øvre grense for feilen","Det krever at $f$ er $n+1$ ganger deriverbar"],"correct_answer":["$c$ ligger mellom $a$ og $x$","Det kan brukes til å gi en øvre grense for feilen","Det krever at $f$ er $n+1$ ganger deriverbar"],"explanation":"Fortegnet avhenger av $f^{(n+1)}(c)$ og av $(x-a)^{n+1}$, så det er ikke alltid positivt."},
 {"type":"single_choice","question":"Konvergensradien til $\\sum_{n=1}^{\\infty} \\frac{x^n}{n\\,2^n}$ er","options":["$2$","$1$","$\\frac{1}{2}$","$\\infty$"],"correct_answer":"$2$","explanation":"Forholdstesten gir $|x|/2 < 1$. Faktoren $1/n$ påvirker ikke radien, bare endepunktene."},
 {"type":"text","question":"Skriv opp de tre første leddene (forskjellige fra null) i Maclaurinrekken til $\\cos x$.","correct_answer":"$1 - \\frac{x^2}{2} + \\frac{x^4}{24}$","explanation":"Bare like potenser, med vekslende fortegn og $(2n)!$ i nevneren."}]}$q$),
($q${"subject":"Matematikk","topic":"Egenverdier","level":"advanced","language":"nb","title":"Egenverdier og diagonalisering","model":"testdata","questions":[
 {"type":"single_choice","question":"Egenverdiene til $\\begin{pmatrix} 2 & 1 \\\\ 1 & 2 \\end{pmatrix}$ er","options":["$1$ og $3$","$2$ og $2$","$0$ og $4$","$-1$ og $3$"],"correct_answer":"$1$ og $3$","explanation":"$\\det(A-\\lambda I) = (2-\\lambda)^2 - 1 = 0$ gir $\\lambda = 1, 3$. Sporet 4 og determinanten 3 stemmer."},
 {"type":"single_choice","question":"En $n \\times n$-matrise er diagonaliserbar hvis og bare hvis","options":["den har $n$ lineært uavhengige egenvektorer","alle egenverdiene er forskjellige","den er inverterbar","den er symmetrisk"],"correct_answer":"den har $n$ lineært uavhengige egenvektorer","explanation":"Forskjellige egenverdier og symmetri er tilstrekkelige, men ikke nødvendige vilkår. Inverterbarhet har ingenting med saken å gjøre."},
 {"type":"multiple_choice","question":"Hvilke er alltid sanne for en reell symmetrisk matrise? (flere riktige)","options":["Alle egenverdier er reelle","Egenvektorer til ulike egenverdier er ortogonale","Den er inverterbar","Den kan diagonaliseres ortogonalt"],"correct_answer":["Alle egenverdier er reelle","Egenvektorer til ulike egenverdier er ortogonale","Den kan diagonaliseres ortogonalt"],"explanation":"Spektralteoremet. Nullmatrisen er symmetrisk, men ikke inverterbar."},
 {"type":"text","question":"Hvis $A = PDP^{-1}$, hva er $A^k$ uttrykt med $P$ og $D$?","correct_answer":"$A^k = PD^kP^{-1}$","explanation":"$P^{-1}P = I$ forsvinner mellom faktorene, og $D^k$ får man ved å opphøye diagonalelementene."}]}$q$),
($q${"subject":"Kjemi","topic":"Termodynamikk","level":"advanced","language":"nb","title":"Gibbs fri energi og likevekt","model":"testdata","questions":[
 {"type":"single_choice","question":"Hvilken sammenheng gjelder mellom $\\Delta G^\\circ$ og likevektskonstanten $K$?","options":["$\\Delta G^\\circ = -RT\\ln K$","$\\Delta G^\\circ = RT\\ln K$","$\\Delta G^\\circ = -RT K$","$\\Delta G^\\circ = \\Delta H^\\circ \\cdot K$"],"correct_answer":"$\\Delta G^\\circ = -RT\\ln K$","explanation":"$K > 1$ gir negativ $\\Delta G^\\circ$. Fortegnsfeil er den vanligste feilen her."},
 {"type":"single_choice","question":"En reaksjon har $\\Delta H < 0$ og $\\Delta S < 0$. Når er den spontan?","options":["Ved lav temperatur","Ved høy temperatur","Alltid","Aldri"],"correct_answer":"Ved lav temperatur","explanation":"$\\Delta G = \\Delta H - T\\Delta S$: leddet $-T\\Delta S$ er positivt og vokser med $T$."},
 {"type":"multiple_choice","question":"Hva øker utbyttet av NH$_3$ i $\\mathrm{N_2 + 3H_2 \\rightleftharpoons 2NH_3}$, $\\Delta H<0$? (flere riktige)","options":["Høyere trykk","Lavere temperatur","Katalysator","Fjerne NH$_3$ fortløpende"],"correct_answer":["Høyere trykk","Lavere temperatur","Fjerne NH$_3$ fortløpende"],"explanation":"En katalysator gjør at likevekten innstilles raskere, men flytter den ikke."},
 {"type":"text","question":"Ved hvilken temperatur er $\\Delta G = 0$, uttrykt med $\\Delta H$ og $\\Delta S$?","correct_answer":"$T = \\Delta H / \\Delta S$","explanation":"Sett $\\Delta H - T\\Delta S = 0$. Husk å bruke samme energienhet (J eller kJ)."}]}$q$),
($q${"subject":"Informatikk","topic":"Grafalgoritmer","level":"advanced","language":"nb","title":"Korteste vei og grafsøk","model":"testdata","questions":[
 {"type":"single_choice","question":"Kjøretiden til Dijkstra med binær haug er","options":["$O((V+E)\\log V)$","$O(V^2 E)$","$O(V+E)$","$O(E \\log E \\cdot V)$"],"correct_answer":"$O((V+E)\\log V)$","explanation":"Hver node tas ut én gang og hver kant kan gi én decrease-key, begge $O(\\log V)$."},
 {"type":"single_choice","question":"Hvilken algoritme finner korteste vei med negative kantvekter (uten negative sykler)?","options":["Bellman–Ford","Dijkstra","BFS","Prims algoritme"],"correct_answer":"Bellman–Ford","explanation":"Dijkstra antar at en node er ferdig når den tas ut av køen. Det holder ikke med negative kanter."},
 {"type":"multiple_choice","question":"Hvilke kan løses i lineær tid $O(V+E)$? (flere riktige)","options":["Topologisk sortering","Korteste vei i uvektet graf","Sterkt sammenhengende komponenter","Minimalt spenntre"],"correct_answer":["Topologisk sortering","Korteste vei i uvektet graf","Sterkt sammenhengende komponenter"],"explanation":"MST med Kruskal eller Prim krever sortering eller en prioritetskø, altså $O(E\\log V)$."},
 {"type":"text","question":"Hvilken datastruktur bruker BFS for å bestemme rekkefølgen nodene besøkes i?","correct_answer":"kø (FIFO)","explanation":"DFS bruker stakk (eller rekursjon). BFS bruker kø."}]}$q$),
($q${"subject":"Statistikk","topic":"Hypotesetesting","level":"advanced","language":"nb","title":"t-test og konfidensintervall","model":"testdata","questions":[
 {"type":"single_choice","question":"Hva betyr en p-verdi på 0,03?","options":["Sannsynligheten for et minst like ekstremt resultat er 3 % hvis $H_0$ er sann","Det er 3 % sannsynlig at $H_0$ er sann","Det er 97 % sannsynlig at $H_1$ er sann","Effekten er 3 %"],"correct_answer":"Sannsynligheten for et minst like ekstremt resultat er 3 % hvis $H_0$ er sann","explanation":"p-verdien er en betinget sannsynlighet gitt $H_0$, ikke sannsynligheten for at en hypotese er sann."},
 {"type":"single_choice","question":"Med $n=25$, ukjent $\\sigma$ og normalfordelte data bruker vi","options":["t-fordeling med 24 frihetsgrader","standard normalfordeling","t-fordeling med 25 frihetsgrader","kji-kvadrat med 24 frihetsgrader"],"correct_answer":"t-fordeling med 24 frihetsgrader","explanation":"Vi estimerer $\\sigma$ med $s$, og det gir $n-1$ frihetsgrader."},
 {"type":"multiple_choice","question":"Hva gjør et 95 % konfidensintervall smalere? (flere riktige)","options":["Større utvalg","Mindre standardavvik i dataene","Høyere konfidensnivå (99 %)","Lavere konfidensnivå (90 %)"],"correct_answer":["Større utvalg","Mindre standardavvik i dataene","Lavere konfidensnivå (90 %)"],"explanation":"Bredden er proporsjonal med $t^* \\cdot s/\\sqrt n$. Høyere konfidensnivå gir større $t^*$."},
 {"type":"text","question":"Hva kalles feilen å forkaste en sann nullhypotese?","correct_answer":"type I-feil","explanation":"Sannsynligheten for type I-feil er signifikansnivået $\\alpha$."}]}$q$)
) AS t(q);

-- Учебные материалы (общие): по ним тьютор и квиз ищут текст для промпта
SELECT add_material(NULL, d.subject, d.topic, d.title, d.content, d.file, 'text/markdown', 'teacher', 'nb')
FROM (VALUES
('Matematikk', 'Taylorpolynomer', 'Sammendrag: Taylorpolynomer', 'taylor.md',
$m$Taylorpolynomet av grad n til f om x = a er T_n(x) = sum_{k=0}^{n} f^(k)(a)/k! (x-a)^k.
Lagranges restledd: R_n(x) = f^(n+1)(c)/(n+1)! (x-a)^(n+1) for en c mellom a og x.
Feilestimat: finn en øvre grense M for |f^(n+1)| på intervallet, da er |R_n(x)| <= M |x-a|^(n+1)/(n+1)!.
Viktige Maclaurinrekker: e^x = 1 + x + x^2/2! + ..., sin x = x - x^3/3! + x^5/5! - ..., cos x = 1 - x^2/2! + x^4/4! - ..., ln(1+x) = x - x^2/2 + x^3/3 - ... for |x| < 1.
Grenseverdier av typen 0/0 løses ofte ved å sette inn rekkene og sammenligne laveste potens.$m$),
('Matematikk', 'Egenverdier', 'Sammendrag: Egenverdier og diagonalisering', 'egenverdier.md',
$m$λ er en egenverdi til A hvis det finnes v ≠ 0 med Av = λv. Egenverdiene er røttene til det(A − λI) = 0.
Sporet er summen av egenverdiene, determinanten er produktet.
A er diagonaliserbar hvis den har n lineært uavhengige egenvektorer; da er A = PDP^{-1} med egenvektorene som kolonner i P.
Potenser: A^k = P D^k P^{-1}. Systemer x' = Ax løses med x(t) = sum c_i e^{λ_i t} v_i.
Spektralteoremet: en reell symmetrisk matrise har reelle egenverdier og kan diagonaliseres med en ortogonal matrise.$m$),
('Kjemi', 'Termodynamikk', 'Sammendrag: Gibbs fri energi', 'gibbs.md',
$m$ΔG = ΔH − TΔS. Reaksjonen er spontan når ΔG < 0.
ΔG° = −RT ln K, R = 8,314 J/(mol·K). K > 1 betyr at produktene er favorisert.
van 't Hoff: ln(K2/K1) = −ΔH°/R (1/T2 − 1/T1).
Le Chatelier: systemet motvirker en ytre endring. Økt trykk favoriserer siden med færre gassmolekyler; for en eksoterm reaksjon gir høyere temperatur lavere K.
En katalysator endrer hastigheten, ikke likevekten.$m$),
('Informatikk', 'Grafalgoritmer', 'Sammendrag: Korteste vei', 'korteste-vei.md',
$m$BFS finner korteste vei i uvektede grafer på O(V+E) og bruker en kø.
Dijkstra: velg alltid noden med minst foreløpig avstand fra en prioritetskø; O((V+E) log V) med binær haug. Krever ikke-negative kantvekter.
Bellman–Ford: relakser alle kanter V−1 ganger, O(VE); oppdager negative sykler med én ekstra runde.
Topologisk sortering (DFS eller Kahns algoritme) gir korteste vei i DAG på lineær tid.$m$),
('Statistikk', 'Hypotesetesting', 'Sammendrag: t-test', 't-test.md',
$m$Ensidig/tosidig t-test for forventning: T = (x̄ − μ0)/(s/√n) er t-fordelt med n−1 frihetsgrader under H0.
Forkast H0 når |T| > t_{α/2, n−1} (tosidig). 95 % konfidensintervall: x̄ ± t_{0,025, n−1} · s/√n.
p-verdi: sannsynligheten for et minst like ekstremt resultat gitt at H0 er sann.
Type I-feil: forkaste en sann H0 (sannsynlighet α). Type II-feil: beholde en usann H0.
Nødvendig utvalgsstørrelse for bredde B: n ≈ (2 z s / B)^2.$m$)
) AS d(subject, topic, title, file, content);

-- Один пройденный квиз (Taylor): 3 из 5 верно — ошибки база сама добавит в карточки, прогресс виден на дашборде
DO $$
DECLARE
    v_user BIGINT := (SELECT id FROM seed_user);
    v_set  BIGINT := (SELECT s.id FROM quiz_sets s JOIN subjects sb ON sb.id = s.subject_id
                      WHERE sb.key = norm_key('Matematikk') AND s.topic_key = norm_key('Taylorpolynomer')
                        AND s.level = 'advanced' AND s.language = 'nb' ORDER BY s.id LIMIT 1);
    v_quiz BIGINT;
BEGIN
    v_quiz := start_quiz(v_user, v_set);
    PERFORM save_quiz_answer(v_quiz, 1, '$1 + x + \frac{x^2}{2} + \frac{x^3}{6}$');
    PERFORM save_quiz_answer(v_quiz, 2, '$0$');
    PERFORM save_quiz_answer(v_quiz, 3, '["$c$ ligger mellom $a$ og $x$","Det kan brukes til å gi en øvre grense for feilen","Det krever at $f$ er $n+1$ ganger deriverbar"]');
    PERFORM save_quiz_answer(v_quiz, 4, '$1$');
    PERFORM save_quiz_answer(v_quiz, 5, '1 - x^2/2 + x^4/24', 'correct', 'Riktig: like potenser med vekslende fortegn.');
    PERFORM finish_quiz(v_quiz);
END $$;

COMMIT;

-- Что получилось
SELECT u.email,
       (SELECT count(*) FROM assignments a WHERE a.user_id = u.id)        AS homework,
       (SELECT count(*) FROM quiz_sets WHERE model = 'testdata')           AS quiz_sets,
       (SELECT count(*) FROM materials WHERE owner_id IS NULL)             AS shared_materials,
       (SELECT count(*) FROM flashcard_progress f WHERE f.user_id = u.id)  AS flashcards
FROM users u WHERE u.email = 'test@lekseradar.no';
