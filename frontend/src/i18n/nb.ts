// Эталонный словарь (bokmål). en.ts и ru.ts обязаны повторять его форму — проверяет TypeScript и i18n.test.ts.
export const nb = {
  nav: { main: 'Hovedmeny', calendar: 'Kalender', flashcards: 'Læringskort', tutor: 'AI-veileder', profile: 'Profil', settings: 'Innstillinger' },
  header: { home: 'LekseRadar — oversikt', login: 'Logg inn', skip: 'Hopp til innhold' },
  demoBar: { text: 'En liten smakebit av en enklere skolehverdag.', link: 'Utforsk demoen' },
  footer: { tagline: 'Lær litt. Forstå mer.', status: 'Prototype · Se utviklingsstatus' },
  settings: {
    eyebrow: 'DITT EGET LILLE HJØRNE', title: 'Akkurat slik du liker det.', intro: 'Gjør LekseRadar litt mer deg.',
    appearance: 'Utseende og språk', theme: 'Tema',
    themeHint: (from: number, to: string) => `Auto: mørkt fra kl. ${from} til ${to}, eller når enheten bruker mørk modus`,
    auto: 'Auto', light: 'Lyst', dark: 'Mørkt',
    language: 'Språk', languageHint: 'Gjelder hele nettstedet og AI-veilederen',
    reminders: 'Påminnelser', remindersHint: 'Varsling er ikke tilgjengelig ennå', soon: 'Kommer',
    platformsTitle: 'Alt på ett sted. Etter hvert.',
    platformsIntro: 'Skoleintegrasjoner er under utvikling. Ikke lim inn ekte tilgangstokener i demoen.',
    seeSetup: 'Se oppsett', notYet: 'Ikke tilgjengelig ennå',
    preview: 'Dette er en forhåndsvisning. Ingen opplysninger sendes eller lagres.',
    calendarUrl: 'Kalender-URL', schoolUrl: 'Skolens URL', connectLater: 'Koble til — kommer senere',
    account: 'Konto', loggedInAs: (email: string) => `Logget inn som ${email}.`, logout: 'Logg ut',
  },
};

export type Dict = typeof nb;
