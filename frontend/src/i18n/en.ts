import type { Dict } from './nb';

export const en: Dict = {
  nav: { main: 'Main menu', calendar: 'Calendar', flashcards: 'Flashcards', tutor: 'AI tutor', profile: 'Profile', settings: 'Settings' },
  header: { home: 'LekseRadar — overview', login: 'Log in', skip: 'Skip to content' },
  demoBar: { text: 'A small taste of an easier school day.', link: 'Explore the demo' },
  footer: { tagline: 'Learn a little. Understand more.', status: 'Prototype · See development status' },
  settings: {
    eyebrow: 'YOUR OWN LITTLE CORNER', title: 'Just the way you like it.', intro: 'Make LekseRadar a little more you.',
    appearance: 'Appearance and language', theme: 'Theme',
    themeHint: (from: number, to: string) => `Auto: dark from ${from}:00 to ${to}:00, or when your device uses dark mode`,
    auto: 'Auto', light: 'Light', dark: 'Dark',
    language: 'Language', languageHint: 'Applies to the whole site and the AI tutor',
    reminders: 'Reminders', remindersHint: 'Notifications aren’t available yet', soon: 'Coming',
    platformsTitle: 'Everything in one place. Eventually.',
    platformsIntro: 'School integrations are in development. Don’t paste real access tokens into the demo.',
    seeSetup: 'See setup', notYet: 'Not available yet',
    preview: 'This is a preview. No information is sent or stored.',
    calendarUrl: 'Calendar URL', schoolUrl: 'School URL', connectLater: 'Connect — coming later',
    account: 'Account', loggedInAs: (email: string) => `Logged in as ${email}.`, logout: 'Log out',
  },
};
