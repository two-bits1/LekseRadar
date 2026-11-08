import { ISSUES_URL, SECURITY_URL, type LegalTexts } from './types';

export const nb: LegalTexts = {
  updated: 'Sist oppdatert 8. november 2026',
  contents: 'Innhold',

  privacy: {
    eyebrow: 'PERSONVERNERKLÆRING',
    title: 'Slik behandler LekseRadar dataene dine',
    intro: 'Her forklarer vi hva vi lagrer, hvorfor vi trenger det, hvem andre som får det, og hvordan du får det slettet. Vi har prøvd å skrive kort og enkelt.',
    sections: [
      { id: 'short', title: 'Kort fortalt', body: [{ list: [
        'Vi lagrer bare det LekseRadar trenger for å fungere: kontoen din, plattformene du har koblet til, oppgavene dine, samtalene med AI-veilederen og fremgangen din.',
        'Tilgangen til skoleplattformene dine lagres kryptert.',
        'AI-tjenesten får teksten i samtalene dine og i oppgaven du spør om. Den får ikke navnet eller e-postadressen din.',
        'Vi selger ikke dataene dine, viser ikke reklame og bruker ikke analyse- eller sporingsverktøy.',
        'Når du kobler fra en plattform, slettes tilgangen og alle oppgavene fra den.',
        'Du kan ikke slette hele kontoen på nettsiden ennå. Til det er på plass, kan du be oss om å slette den.',
      ] }] },
      { id: 'who', title: 'Hvem som er ansvarlig', body: [
        'LekseRadar er et elevprosjekt laget av LekseRadar-teamet på hackathonet Oslo Sprint (6.–8. november 2026).',
        'Hvis skolen din gir deg LekseRadar, er det vanligvis skolen som har ansvaret for dataene dine («behandlingsansvarlig»). Da kan du også kontakte skolen.',
      ] },
      { id: 'what', title: 'Hva vi lagrer', body: [{ list: [
        'Konto: navn, e-postadresse, en hash av passordet ditt (aldri selve passordet), språk, tidssone, om du vil ha påminnelser på e-post, når du registrerte deg og sist logget inn.',
        'Tilkoblede plattformer: hvilke plattformer du har koblet til, tilgangsnøkler eller øktinformasjonskapsler for dem (kryptert), og når de sist ble oppdatert.',
        'Oppgaver fra plattformene: tittel, fag eller emne, frist, status, lenke, beskrivelse, og om du har markert oppgaven som levert.',
        'Samtaler med AI-veilederen: tittel og tema for samtalen og alle meldinger du og AI-veilederen skriver.',
        'Læring: quizer og læringskort laget til deg, svarene dine og om de var riktige, repetisjonene av læringskortene, en aktivitetshistorikk (for eksempel «logget inn», «quiz fullført») og prestasjonene dine.',
        'Påminnelser: hvilke oppgaver vi allerede har sendt deg e-post om.',
      ] },
        'Mens du bekrefter e-postadressen, ligger navnet og passord-hashen i serverens minne i opptil 15 minutter. IP-adressen din brukes bare i minnet for å hindre at det sendes for mange e-poster med registreringskode.',
      ] },
      { id: 'why', title: 'Hvorfor vi trenger det', body: [{ list: [
        'Kontoen lar deg logge inn og viser frister på ditt språk og i din tidssone.',
        'Tilgangen til plattformene lar oss hente oppgavene dine hvert 30. minutt og når du trykker «Oppdater».',
        'Oppgavene vises i én liste og gir AI-veilederen og quizene sammenhengen rundt oppgaven.',
        'Samtalehistorikken sendes med hvert nytt spørsmål, slik at AI-veilederen kan følge samtalen.',
        'Svar, repetisjoner og aktivitetshistorikk brukes til å sjekke svar, planlegge repetisjon og vise fremgangen din.',
        'E-postadressen brukes til registreringskoden og, hvis du vil, påminnelser 24 timer før en frist.',
      ] }] },
      { id: 'share', title: 'Hvem andre som får data', body: [
        'Vi bruker andre tjenester for å drive LekseRadar. De får bare det de trenger:',
        { list: [
          'AI-tjeneste (hos oss Groq, et selskap i USA): teksten i meldingene dine og samtalehistorikken, tittel, frist og tekst for oppgaven du spør om, fag, tema, nivå og språk, temaene du sliter mest med, og quizsvar som skal sjekkes. Ikke navnet eller e-postadressen din.',
          'E-post (Gmail): e-postadressen og navnet ditt, registreringskoden, og i påminnelser titler, frister og lenker til oppgavene dine.',
          'Google: hvis du kobler til Google Classroom, logger du inn hos Google, og vi får lesetilgang til fagene og oppgavene dine. Når du kobler fra, trekker vi tilbake tilgangen hos Google.',
          'Skoleplattformene dine (Canvas, Microsoft Teams, itslearning, Visma InSchool, Google Classroom): serveren vår henter oppgavene dine fra dem med din tilgang.',
          'Drift (serverne som kjører LekseRadars backend og database, og Vercel for nettsiden): dataene lagres på eller går gjennom disse serverne. Driftsleverandører kan lagre tekniske logger, for eksempel IP-adresser.',
          'Google Fonts: når siden laster skrifttypen, ser Google IP-adressen din.',
        ] },
        'Flere av disse selskapene holder til utenfor EU/EØS, de fleste i USA.',
      ] },
      { id: 'browser', title: 'Hva som lagres i nettleseren din', body: [
        'Nettleseren tar vare på innloggingen din, temaet og språket du har valgt (i «local storage»), slik at du forblir innlogget og siden ser ut slik du vil. Vi bruker ikke informasjonskapsler til reklame eller sporing.',
      ] },
      { id: 'keep', title: 'Hvor lenge vi lagrer data', body: [{ list: [
        'Vi lagrer dataene til de slettes. Det finnes ingen automatiske slettefrister ennå.',
        'Når du kobler fra en plattform, slettes tilgangen, alle oppgavene fra den og alt du har laget til disse oppgavene: samtaler, quizer, læringskort, svar og historikk.',
        'Når du sletter en samtale, slettes alle meldingene i den.',
        'Du kan slå av påminnelser på e-post i Innstillinger.',
        'Du kan ikke slette kontoen på nettsiden ennå; det er planlagt. Til da kan du spørre oss, se Kontakt.',
      ] }] },
      { id: 'children', title: 'Barn og unge', body: [
        'LekseRadar er laget for elever. I Norge kan du selv samtykke til at en nettjeneste bruker personopplysningene dine fra du er 13 år. Er du yngre, må en forelder eller foresatt samtykke.',
        'Hvis skolen din bruker LekseRadar, må skolen ta hensyn til dette og informere elever og foreldre.',
      ] },
      { id: 'rights', title: 'Rettighetene dine', body: [
        'Du har rett til å se dataene vi har om deg, rette dem, få dem slettet og protestere mot hvordan de brukes. Navn, språk og passord kan du endre selv i profilen og innstillingene. For alt annet, kontakt oss.',
        'Du kan også klage til [Datatilsynet](https://www.datatilsynet.no).',
      ] },
      { id: 'security', title: 'Sikkerhet', body: [
        'Tilgangen til plattformene er kryptert, passord lagres bare som hash, og det er grenser for innloggingsforsøk og AI-forespørsler. LekseRadar har ennå ikke hatt en uavhengig sikkerhetsrevisjon. Har du funnet et sikkerhetsproblem? [Meld det fra privat på GitHub](' + SECURITY_URL + ').',
      ] },
      { id: 'changes', title: 'Endringer', body: [
        'Hvis vi endrer erklæringen, oppdaterer vi datoen øverst på siden.',
      ] },
      { id: 'contact', title: 'Kontakt', body: [
        'Spørsmål, eller vil du få dataene dine slettet? [Opprett en sak på GitHub](' + ISSUES_URL + ') – og ikke skriv navn, e-postadresse, skole eller andre personopplysninger i den.',
      ] },
    ],
  },

  terms: {
    eyebrow: 'BRUKERVILKÅR',
    title: 'Reglene for å bruke LekseRadar',
    intro: 'Når du oppretter en konto eller bruker LekseRadar, godtar du disse vilkårene. Les dem sammen med [personvernerklæringen](/privacy).',
    sections: [
      { id: 'about', title: 'Hva LekseRadar er', body: [
        'LekseRadar samler leksene fra skoleplattformene dine på ett sted og hjelper deg å lære med en AI-veileder, quizer og læringskort. Det er gratis.',
        'LekseRadar ble startet av et elevteam på hackathonet Oslo Sprint (6.–8. november 2026) og utvikles fortsatt. Funksjoner kan endres, og tjenesten kan settes på pause eller legges ned.',
      ] },
      { id: 'account', title: 'Kontoen din', body: [{ list: [
        'Bruk din egen e-postadresse og hold passordet for deg selv.',
        'Én konto per person. Ikke bruk andres konto.',
        'Du er ansvarlig for det som skjer på kontoen din. Tror du noen andre har brukt den, bytt passord.',
        'Er du under 13 år, må en forelder eller foresatt si ja før du bruker LekseRadar.',
      ] }] },
      { id: 'platforms', title: 'Tilkobling av skoleplattformer', body: [{ list: [
        'Koble bare til dine egne kontoer, og følg reglene til skolen og plattformen.',
        'Tilkoblingene til itslearning og Visma InSchool er uoffisielle. De kan slutte å virke når som helst.',
        'Du kan koble fra en plattform når som helst. Da slettes tilgangen, oppgavene og samtalene, quizene og læringskortene du har laget til dem.',
        'Oppgavene tilhører skolen og lærerne dine. LekseRadar viser deg bare en kopi.',
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
        'Det du skriver (samtaler og svar), er ditt. Vi lagrer og bruker det bare for å drive LekseRadar for deg, slik det står i [personvernerklæringen](/privacy).',
      ] },
      { id: 'warranty', title: 'Ingen garantier', body: [
        'LekseRadar leveres «som den er». Vi gjør vårt beste, men kan ikke love at den virker uten feil eller avbrudd, at påminnelser alltid kommer frem, eller at AI-svar er riktige. Så langt loven tillater det, er teamet ikke ansvarlig for oversittede frister, tapte data eller valg du tar basert på AI-svar.',
      ] },
      { id: 'end', title: 'Å slutte', body: [
        'Du kan slutte å bruke LekseRadar når som helst. Du kan ikke slette kontoen på nettsiden ennå; til da kan du be oss om å slette den (se Kontakt).',
      ] },
      { id: 'changes', title: 'Endringer i vilkårene', body: [
        'Vi kan endre vilkårene. Datoen øverst viser den nyeste versjonen.',
      ] },
      { id: 'contact', title: 'Kontakt', body: [
        'Spørsmål: [opprett en sak på GitHub](' + ISSUES_URL + ') uten personopplysninger. Sikkerhetsproblemer: [meld dem fra privat](' + SECURITY_URL + ').',
      ] },
    ],
  },
};
