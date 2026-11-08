import { ISSUES_URL, SECURITY_URL, type LegalTexts } from './types';

export const nb: LegalTexts = {
  updated: 'Versjon av 8. november 2026',
  contents: 'Innhold',

  privacy: {
    eyebrow: 'PERSONVERNERKLÆRING',
    title: 'Slik behandler LekseRadar personopplysningene dine',
    intro: 'Her forklarer vi hvilke personopplysninger LekseRadar behandler, hvorfor, på hvilket rettslig grunnlag etter GDPR, hvem som får dem, hvor lenge de lagres og hvilke rettigheter du har.',
    sections: [
      { id: 'short', title: 'Kort fortalt', body: [{ list: [
        'Vi behandler bare det LekseRadar trenger for å fungere: kontoen din, plattformene du har koblet til, leksene dine, samtalene med AI-veilederen og fremgangen din.',
        'Tilgangen til skoleplattformene lagres kryptert. Passord lagres bare som hash.',
        'AI-tjenesten får teksten i samtalene dine og i oppgaven du spør om – ikke navnet eller e-postadressen din.',
        'Ingen reklame, ingen sporing, ingen salg av data.',
        'Du kan selv laste ned alle dataene dine og slette kontoen i [Innstillinger](/settings).',
        'En konto uten innlogging på 24 måneder slettes automatisk med alle dataene.',
      ] }] },
      { id: 'who', title: 'Hvem som er ansvarlig', body: [
        'LekseRadar drives av LekseRadar-teamet, som startet prosjektet på hackathonet Oslo Sprint (6.–8. november 2026). Teamet er behandlingsansvarlig for kontoer som elever oppretter selv.',
        'Hvis skolen din gir deg LekseRadar, er skolen behandlingsansvarlig, og teamet behandler dataene på vegne av skolen etter en databehandleravtale. Da kan du også kontakte skolen.',
      ] },
      { id: 'what', title: 'Hva vi behandler', body: [{ list: [
        'Konto: navn, e-postadresse, passord-hash (aldri selve passordet), språk, tidssone, innstilling for påminnelser, når du registrerte deg, sist logget inn og når du godtok disse vilkårene.',
        'Tilkoblede plattformer: hvilke plattformer du har koblet til, tilgangsnøkler eller øktinformasjonskapsler for dem (kryptert med AES-256) og tidspunkt og resultat for siste oppdatering.',
        'Lekser fra plattformene: tittel, fag eller emne, frist, status, lenke og beskrivelse, og om du har markert leksen som levert.',
        'Samtaler med AI-veilederen: tittel, tema og alle meldinger du og AI-veilederen skriver.',
        'Læring: quizer og læringskort laget til deg, svarene dine og om de var riktige, repetisjonsfremgang, en aktivitetshistorikk (for eksempel «logget inn», «quiz fullført») og prestasjoner.',
        'Påminnelser på e-post: hvilke oppgaver vi allerede har minnet deg på.',
      ] },
        'Mens du bekrefter e-postadressen, ligger navnet, passord-hashen og en hash av koden bare i serverens minne, i høyst 15 minutter. IP-adressen din brukes bare i minnet for å begrense registreringskoder og innloggingsforsøk; vi lagrer den ikke.',
      ] },
      { id: 'why', title: 'Hvorfor, og på hvilket rettslig grunnlag', body: [{ list: [
        'For å levere LekseRadar slik det står i [brukervilkårene](/terms): kontoen, innsamling av lekser, AI-veilederen, quizer, læringskort og fremgang. Rettslig grunnlag: avtale med deg (GDPR art. 6 nr. 1 bokstav b).',
        'Påminnelser på e-post 24 timer før en frist: vår berettigede interesse i å hjelpe deg å rekke fristene (art. 6 nr. 1 bokstav f). De er på som standard, og du kan slå dem av når som helst i Innstillinger.',
        'Sikkerhet og hindring av misbruk (grenser for innloggingsforsøk og AI-forespørsler, tekniske logger): berettiget interesse i å holde tjenesten og dataene dine trygge (art. 6 nr. 1 bokstav f).',
        'Registrering av når du godtok vilkårene: for å kunne vise hva du har sagt ja til (art. 7 nr. 1).',
      ] }, 'Vi bruker ikke dataene dine til reklame, profilering eller til å trene AI-modeller.'] },
      { id: 'share', title: 'Hvem som får data', body: [
        'Vi bruker disse tjenestene for å drive LekseRadar. De behandler data på våre vegne og bare til dette formålet:',
        { list: [
          'AI-tjeneste (en OpenAI-kompatibel leverandør, for eksempel Groq i USA): meldingene dine og samtalehistorikken; tittel, frist og tekst for oppgaven du spør om; fag, tema, nivå og språk; temaene du sliter mest med; og tekstsvar som skal sjekkes. Ikke navnet eller e-postadressen din.',
          'E-posttjeneste (for eksempel Gmail): e-postadressen og navnet ditt, registreringskoden, og i påminnelser titler, frister og lenker til oppgavene dine.',
          'Google: hvis du kobler til Google Classroom, logger du inn hos Google, og vi får lesetilgang til fagene og oppgavene dine. Når du kobler fra eller sletter kontoen, trekkes tilgangen tilbake hos Google.',
          'Skoleplattformene dine (Canvas, Microsoft Teams, itslearning, Visma InSchool, Google Classroom): serveren vår leser oppgavene dine med din egen tilgang.',
          'Drift: serverne som kjører LekseRadars backend og database, og Vercel for nettsiden. De kan lagre tekniske logger, for eksempel IP-adresser, i en begrenset periode.',
        ] },
        'Noen av leverandørene er utenfor EU/EØS, de fleste i USA. Slike overføringer skjer med garantiene i leverandørenes databehandlervilkår: EUs standardkontraktsbestemmelser eller rammeverket EU–US Data Privacy Framework.',
      ] },
      { id: 'browser', title: 'Informasjonskapsler og lagring i nettleseren', body: [
        'LekseRadar bruker ikke informasjonskapsler til sporing eller reklame. Nettleseren lagrer bare det som trengs for at siden skal virke: innloggingen, temaet og språket ditt (i «local storage»). Skrifttypen er en del av nettsiden og lastes ikke fra Google.',
      ] },
      { id: 'keep', title: 'Hvor lenge vi lagrer data', body: [{ list: [
        'Dataene dine lagres så lenge du bruker LekseRadar.',
        'Hvis du ikke logger inn på 24 måneder, slettes kontoen automatisk med alle dataene.',
        'Du kan slette kontoen når som helst i [Innstillinger](/settings). Alt slettes med en gang, og tilgangen til Google Classroom trekkes tilbake.',
        'Når du kobler fra en plattform, slettes tilgangen, leksene fra den og samtalene, quizene, læringskortene og historikken du har laget til disse leksene.',
        'Når du sletter en samtale, slettes alle meldingene i den.',
        'En registrering som ikke fullføres, glemmes etter 15 minutter.',
      ] }] },
      { id: 'rights', title: 'Rettighetene dine', body: [{ list: [
        'Innsyn og dataportabilitet (art. 15 og 20): last ned alle dataene dine som en fil i [Innstillinger](/settings).',
        'Retting (art. 16): endre navn, språk og passord i profilen og innstillingene.',
        'Sletting (art. 17): slett kontoen i [Innstillinger](/settings).',
        'Begrensning og protest (art. 18 og 21): slå av påminnelser i Innstillinger, eller kontakt oss.',
        'Klage: du kan klage til [Datatilsynet](https://www.datatilsynet.no).',
      ] }, 'Vi svarer på henvendelser innen én måned.'] },
      { id: 'ai', title: 'AI og automatiserte avgjørelser', body: [
        'AI-veilederen, quizer og læringskort lages av en AI-tjeneste, og tekstsvar i quizer sjekkes av den. Resultatene er bare til øving. LekseRadar tar ingen avgjørelser om deg som har rettsvirkning eller på tilsvarende måte påvirker deg i betydelig grad (GDPR art. 22).',
      ] },
      { id: 'children', title: 'Barn og unge', body: [
        'LekseRadar er laget for elever. Når du oppretter en konto, bekrefter du at du er minst 13 år, eller at en forelder eller foresatt samtykker. I Norge er aldersgrensen for selv å samtykke til nettjenester 13 år (personopplysningsloven § 5).',
        'Hvis skolen din bruker LekseRadar, bestemmer skolen hvordan elever og foreldre informeres.',
      ] },
      { id: 'security', title: 'Sikkerhet', body: [
        'Vi beskytter dataene dine med krypterte forbindelser, kryptert plattformtilgang, hashing av passord, grenser for innloggingsforsøk og AI-forespørsler og begrenset tilgang til serverne. Hvis det skjer et brudd på personopplysningssikkerheten, varsler vi Datatilsynet innen 72 timer og deg uten ugrunnet opphold når loven krever det. Har du funnet et sikkerhetsproblem? [Meld det fra privat](' + SECURITY_URL + ').',
      ] },
      { id: 'changes', title: 'Endringer i erklæringen', body: [
        'Hvis vi endrer erklæringen, oppdaterer vi versjonsdatoen øverst. Hvis en endring påvirker bruken av dataene dine på en viktig måte, sier vi fra på nettsiden før den gjelder.',
      ] },
      { id: 'contact', title: 'Kontakt', body: [
        'Det meste kan du gjøre selv i [Innstillinger](/settings). For andre spørsmål, [opprett en sak på GitHub](' + ISSUES_URL + ') – ikke skriv navn, e-postadresse, skole eller andre personopplysninger i den; vi svarer der med hvordan vi går videre privat.',
      ] },
    ],
  },

  terms: {
    eyebrow: 'BRUKERVILKÅR',
    title: 'Reglene for å bruke LekseRadar',
    intro: 'Når du oppretter en konto, godtar du disse vilkårene. Les dem sammen med [personvernerklæringen](/privacy).',
    sections: [
      { id: 'about', title: 'Hva LekseRadar er', body: [
        'LekseRadar samler leksene fra skoleplattformene dine på ett sted og hjelper deg å lære med en AI-veileder, quizer og læringskort. Det er gratis.',
        'LekseRadar ble startet av et elevteam på hackathonet Oslo Sprint (6.–8. november 2026) og utvikles fortsatt. Funksjoner kan endres, og tjenesten kan settes på pause eller legges ned. Hvis vi legger den ned, sletter vi alle personopplysninger.',
      ] },
      { id: 'account', title: 'Kontoen din', body: [{ list: [
        'Du må være minst 13 år, eller ha samtykke fra en forelder eller foresatt, for å opprette en konto.',
        'Bruk din egen e-postadresse og hold passordet for deg selv.',
        'Én konto per person. Ikke bruk andres konto.',
        'Du er ansvarlig for det som skjer på kontoen din. Tror du noen andre har brukt den, bytt passord.',
      ] }] },
      { id: 'platforms', title: 'Tilkobling av skoleplattformer', body: [{ list: [
        'Koble bare til dine egne kontoer, og følg reglene til skolen og plattformen.',
        'Tilkoblingene til itslearning og Visma InSchool er uoffisielle. De kan slutte å virke når som helst.',
        'Du kan koble fra en plattform når som helst. Da slettes tilgangen, leksene og samtalene, quizene og læringskortene du har laget til dem.',
        'Leksene tilhører skolen og lærerne dine. LekseRadar viser deg bare en kopi.',
      ] }] },
      { id: 'ai', title: 'AI-veilederen, quizer og læringskort', body: [{ list: [
        'Svar, quizer og læringskort lages av en AI. De kan være feil eller ufullstendige. Sjekk viktige ting med læreren eller læreboka.',
        'AI-veilederen skal hjelpe deg å forstå, ikke gjøre leksene for deg. Følg skolens regler for hjelp med skolearbeid.',
        'Quizresultater er til øving. De er ikke karakterer.',
        'Ikke skriv sensitive personopplysninger i samtalen, som adresse, telefonnummer eller helse. Teksten sendes til AI-tjenesten (se [personvernerklæringen](/privacy)).',
      ] }] },
      { id: 'rules', title: 'Ikke lov', body: [{ list: [
        'Å bryte loven, mobbe eller trakassere noen, eller dele andres personopplysninger.',
        'Å prøve å komme inn på andres kontoer eller å bryte eller omgå sikkerheten eller grensene i LekseRadar.',
        'Å overbelaste tjenesten, for eksempel med automatiske forespørsler.',
        'Å prøve å få AI-en til å lage skadelig innhold.',
      ] }, 'Vi kan stenge eller begrense en konto som bryter disse reglene.'] },
      { id: 'content', title: 'Innholdet ditt', body: [
        'Det du skriver (samtaler og svar), er ditt. Vi bruker det bare til å drive LekseRadar for deg, slik det står i [personvernerklæringen](/privacy), og aldri til å trene AI-modeller.',
      ] },
      { id: 'warranty', title: 'Ingen garantier', body: [
        'LekseRadar leveres «som den er». Vi gjør vårt beste, men kan ikke love at den virker uten feil eller avbrudd, at påminnelser alltid kommer frem, eller at AI-svar er riktige. Så langt loven tillater det, er teamet ikke ansvarlig for oversittede frister, tapte data eller valg du tar basert på AI-svar. Rettighetene dine som forbruker etter norsk lov påvirkes ikke.',
      ] },
      { id: 'end', title: 'Avslutning', body: [
        'Du kan slutte når som helst og slette kontoen i [Innstillinger](/settings); alle dataene dine slettes. En konto uten innlogging på 24 måneder slettes automatisk.',
      ] },
      { id: 'changes', title: 'Endringer i vilkårene', body: [
        'Vi kan endre vilkårene. Versjonsdatoen øverst viser gjeldende versjon. Er endringen viktig, sier vi fra på nettsiden før den gjelder.',
      ] },
      { id: 'contact', title: 'Kontakt', body: [
        'Spørsmål: [opprett en sak på GitHub](' + ISSUES_URL + ') uten personopplysninger. Sikkerhetsproblemer: [meld dem fra privat](' + SECURITY_URL + ').',
      ] },
    ],
  },
};
