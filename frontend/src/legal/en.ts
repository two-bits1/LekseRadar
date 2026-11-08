import { ISSUES_URL, SECURITY_URL, type LegalTexts } from './types';

export const en: LegalTexts = {
  updated: 'Last updated 8 November 2026',
  contents: 'Contents',

  privacy: {
    eyebrow: 'PRIVACY POLICY',
    title: 'How LekseRadar handles your data',
    intro: 'This page explains what we store, why we need it, who else receives it and how you can get it deleted. We have tried to keep it short and plain.',
    sections: [
      { id: 'short', title: 'The short version', body: [{ list: [
        'We store only what LekseRadar needs to work: your account, your connected platforms, your assignments, your chats with the AI tutor and your learning progress.',
        'Access to your school platforms is stored encrypted.',
        'The AI service receives the text of your chats and of the assignment you ask about. It does not receive your name or email address.',
        'We do not sell your data, show ads or use analytics or tracking.',
        'When you disconnect a platform, its access and all its assignments are deleted.',
        'Deleting your whole account is not possible on the site yet. Until it is, you can ask us to delete it.',
      ] }] },
      { id: 'who', title: 'Who is responsible', body: [
        'LekseRadar is a student project made by the LekseRadar team at the Oslo Sprint hackathon (6–8 November 2026).',
        'If your school gives you LekseRadar, the school is normally responsible for your data (the “data controller”). In that case you can also contact your school.',
      ] },
      { id: 'what', title: 'What we store', body: [{ list: [
        'Account: your name, email address, a hash of your password (never the password itself), language, time zone, whether you want email reminders, when you signed up and last logged in.',
        'Connected platforms: which platforms you connected, the access tokens or session cookies for them (encrypted), and when they were last updated.',
        'Assignments from your platforms: title, subject or course, deadline, status, link, description, and whether you marked it as done.',
        'AI tutor chats: the chat title and topic and every message you and the AI tutor write.',
        'Learning: quizzes and flashcards made for you, your answers and whether they were right, your flashcard reviews, an activity history (for example “logged in”, “quiz completed”) and your achievements.',
        'Reminders: which assignments we have already emailed you about.',
      ] },
        'While you confirm your email, your name and password hash are kept in the server’s memory for up to 15 minutes. Your IP address is used only in memory to stop too many sign-up emails.',
      ] },
      { id: 'why', title: 'Why we need it', body: [{ list: [
        'Your account lets you log in and shows deadlines in your language and time zone.',
        'Platform access lets us fetch your assignments every 30 minutes and when you press Refresh.',
        'Assignments are shown in one list and give the AI tutor and quizzes the context of your task.',
        'The chat history is sent along with each new question, so the AI tutor can follow the conversation.',
        'Answers, flashcard reviews and the activity history are used to check answers, plan reviews and show your progress.',
        'Your email address is used for the sign-up code and, if you want them, reminders 24 hours before a deadline.',
      ] }] },
      { id: 'share', title: 'Who else receives data', body: [
        'We use other services to run LekseRadar. They receive only what they need:',
        { list: [
          'AI service (in our setup Groq, a company in the USA): the text of your chat messages and the chat history, the title, deadline and text of the assignment you ask about, subject, topic, level and language, your weakest topics, and quiz answers that need checking. Not your name or email address.',
          'Email (Gmail): your email address and name, the sign-up code, and in reminders the titles, deadlines and links of your assignments.',
          'Google: if you connect Google Classroom, you sign in at Google and we get read-only access to your courses and assignments. When you disconnect, we withdraw that access at Google.',
          'Your school platforms (Canvas, Microsoft Teams, itslearning, Visma InSchool, Google Classroom): our server asks them for your assignments using your access.',
          'Hosting (Railway for the server and database, Vercel for the website): the data is stored on or passes through their servers. They may keep technical logs, such as IP addresses.',
          'Google Fonts: when the page loads its font, Google sees your IP address.',
        ] },
        'Several of these companies are based outside the EU/EEA, mostly in the USA.',
      ] },
      { id: 'browser', title: 'What is stored in your browser', body: [
        'Your browser keeps your login token, your theme and your language (in “local storage”), so you stay logged in and the site looks the way you chose. We do not use advertising or tracking cookies.',
      ] },
      { id: 'keep', title: 'How long we keep data', body: [{ list: [
        'We keep your data until it is deleted. There are no automatic deletion periods yet.',
        'Disconnecting a platform deletes its access and all its assignments. Chats and quizzes about those assignments are kept.',
        'Deleting a chat deletes all its messages.',
        'You can turn email reminders off in Settings.',
        'Deleting your account is not possible on the site yet; it is planned. Until then, ask us as described under Contact.',
      ] }] },
      { id: 'children', title: 'Children and young people', body: [
        'LekseRadar is made for school students. In Norway you can agree to an online service using your personal data yourself from the age of 13. If you are younger, a parent or guardian has to agree.',
        'If your school uses LekseRadar, the school has to take this into account and tell students and parents about it.',
      ] },
      { id: 'rights', title: 'Your rights', body: [
        'You have the right to see the data we have about you, to correct it, to get it deleted and to object to how it is used. You can change your name, language and password in your profile. For anything else, contact us.',
        'You can also complain to the Norwegian Data Protection Authority, [Datatilsynet](https://www.datatilsynet.no).',
      ] },
      { id: 'security', title: 'Security', body: [
        'Platform access is encrypted, passwords are stored only as hashes, and there are limits on login attempts and AI requests. LekseRadar has not yet had an independent security audit. Found a security problem? [Report it privately on GitHub](' + SECURITY_URL + ').',
      ] },
      { id: 'changes', title: 'Changes', body: [
        'If we change this policy, we update the date at the top of the page.',
      ] },
      { id: 'contact', title: 'Contact', body: [
        'Questions, or want your data deleted? [Open an issue on GitHub](' + ISSUES_URL + ') — and do not write your name, email address, school or other personal data in it.',
      ] },
    ],
  },

  terms: {
    eyebrow: 'TERMS OF USE',
    title: 'The rules for using LekseRadar',
    intro: 'By creating an account or using LekseRadar you agree to these terms. Please read them together with the [privacy policy](/privacy).',
    sections: [
      { id: 'about', title: 'What LekseRadar is', body: [
        'LekseRadar collects your homework from your school platforms in one place and helps you learn with an AI tutor, quizzes and flashcards. It is free.',
        'LekseRadar was started by a student team at the Oslo Sprint hackathon (6–8 November 2026) and is still being developed. Features can change, and the service can be paused or shut down.',
      ] },
      { id: 'account', title: 'Your account', body: [{ list: [
        'Use your own email address and keep your password to yourself.',
        'One account per person. Do not use someone else’s account.',
        'You are responsible for what happens in your account. If you think someone else has used it, change your password.',
        'If you are under 13, you need a parent or guardian to agree before you use LekseRadar.',
      ] }] },
      { id: 'platforms', title: 'Connecting school platforms', body: [{ list: [
        'Connect only your own accounts, and follow your school’s rules and the platform’s rules.',
        'The connections to itslearning and Visma InSchool are unofficial. They can stop working at any time.',
        'You can disconnect a platform at any time. This deletes its access and all its assignments from LekseRadar.',
        'Assignments belong to your school and teachers. LekseRadar only shows you a copy.',
      ] }] },
      { id: 'ai', title: 'The AI tutor, quizzes and flashcards', body: [{ list: [
        'Answers, quizzes and flashcards are made by an AI. They can be wrong or incomplete. Check important things with your teacher or your textbook.',
        'The AI tutor is meant to help you understand, not to do your homework for you. Follow your school’s rules about getting help.',
        'Quiz results are for practice. They are not grades.',
        'Do not write sensitive personal information in the chat, such as your address, phone number or health. Chat text is sent to the AI service (see the [privacy policy](/privacy)).',
      ] }] },
      { id: 'rules', title: 'Not allowed', body: [{ list: [
        'Breaking the law, bullying or harassing anyone, or sharing other people’s personal data.',
        'Trying to get into other accounts or to break or get around LekseRadar’s security or limits.',
        'Overloading the service, for example with automated requests.',
        'Trying to make the AI produce harmful content.',
      ] }, 'We may close or limit an account that breaks these rules.'] },
      { id: 'content', title: 'Your content', body: [
        'What you write (chats and answers) is yours. We store and use it only to run LekseRadar for you, as described in the [privacy policy](/privacy).',
      ] },
      { id: 'warranty', title: 'No guarantees', body: [
        'LekseRadar is provided “as is”. We do our best, but we cannot promise that it works without errors or interruptions, that reminders always arrive, or that AI answers are correct. As far as the law allows, the team is not responsible for missed deadlines, lost data or decisions you make based on AI answers.',
      ] },
      { id: 'end', title: 'Stopping', body: [
        'You can stop using LekseRadar at any time. Deleting your account on the site is not possible yet; until it is, ask us to delete it (see Contact).',
      ] },
      { id: 'changes', title: 'Changes to these terms', body: [
        'We may change these terms. The date at the top shows the latest version.',
      ] },
      { id: 'contact', title: 'Contact', body: [
        'Questions: [open an issue on GitHub](' + ISSUES_URL + ') without personal data. Security problems: [report them privately](' + SECURITY_URL + ').',
      ] },
    ],
  },
};
