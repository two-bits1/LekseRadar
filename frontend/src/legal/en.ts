import { ISSUES_URL, SECURITY_URL, type LegalTexts } from './types';

export const en: LegalTexts = {
  updated: 'Version of 8 November 2026',
  contents: 'Contents',

  privacy: {
    eyebrow: 'PRIVACY POLICY',
    title: 'How LekseRadar handles your personal data',
    intro: 'This policy explains what personal data LekseRadar processes, why, on which legal basis under the GDPR, who receives it, how long it is kept and what your rights are.',
    sections: [
      { id: 'short', title: 'The short version', body: [{ list: [
        'We process only what LekseRadar needs to work: your account, your connected platforms, your homework, your chats with the AI tutor and your learning progress.',
        'Access to your school platforms is stored encrypted. Passwords are stored only as hashes.',
        'The AI service receives the text of your chats and of the assignment you ask about — not your name or email address.',
        'No advertising, no tracking, no selling of data.',
        'You can download all your data and delete your account yourself in [Settings](/settings).',
        'An account without a login for 24 months is deleted automatically with all its data.',
      ] }] },
      { id: 'who', title: 'Who is responsible', body: [
        'LekseRadar is run by the LekseRadar team, which started the project at the Oslo Sprint hackathon (6–8 November 2026). The team is the data controller for accounts that students create themselves.',
        'If your school gives you LekseRadar, the school is the data controller and the team processes the data on the school’s behalf under a data processing agreement. In that case you can also contact your school.',
      ] },
      { id: 'what', title: 'What we process', body: [{ list: [
        'Account: name, email address, password hash (never the password itself), language, time zone, reminder setting, when you signed up, last logged in, and when you accepted these terms.',
        'Connected platforms: which platforms you connected, the access tokens or session cookies for them (encrypted with AES-256), and the time and result of the last update.',
        'Homework from your platforms: title, subject or course, deadline, status, link and description, and whether you marked it as done.',
        'AI tutor chats: chat title, topic, and every message you and the AI tutor write.',
        'Learning: quizzes and flashcards made for you, your answers and whether they were right, flashcard review progress, an activity history (for example “logged in”, “quiz completed”) and achievements.',
        'Email reminders: which assignments we already reminded you about.',
      ] },
        'While you confirm your email address, your name, password hash and a hash of the code are kept only in the server’s memory, for at most 15 minutes. Your IP address is used only in memory to limit sign-up emails and login attempts; we do not store it.',
      ] },
      { id: 'why', title: 'Why, and on which legal basis', body: [{ list: [
        'To provide LekseRadar as described in the [terms of use](/terms): your account, collecting your homework, the AI tutor, quizzes, flashcards and progress. Legal basis: performance of a contract with you (GDPR Art. 6(1)(b)).',
        'Email reminders 24 hours before a deadline: our legitimate interest in helping you meet deadlines (Art. 6(1)(f)). They are on by default and you can turn them off at any time in Settings.',
        'Security and preventing abuse (limits on login attempts and AI requests, technical logs): legitimate interest in keeping the service and your data safe (Art. 6(1)(f)).',
        'Keeping a record of when you accepted these terms: to be able to show what you agreed to (Art. 7(1)).',
      ] }, 'We do not use your data for advertising, profiling or training AI models.'] },
      { id: 'share', title: 'Who receives data', body: [
        'We use the following services to run LekseRadar. They process data on our behalf and only for this purpose:',
        { list: [
          'AI service (an OpenAI-compatible provider, for example Groq in the USA): your chat messages and chat history; the title, deadline and text of the assignment you ask about; subject, topic, level and language; your weakest topics; and text answers that need checking. Not your name or email address.',
          'Email service (for example Gmail): your email address, name, sign-up code, and in reminders the titles, deadlines and links of your assignments.',
          'Google: if you connect Google Classroom, you sign in at Google and we get read-only access to your courses and coursework. When you disconnect or delete your account, the access is withdrawn at Google.',
          'Your school platforms (Canvas, Microsoft Teams, itslearning, Visma InSchool, Google Classroom): our server reads your assignments with your own access.',
          'Hosting: the servers that run LekseRadar’s backend and database, and Vercel for the website. They may keep technical logs, such as IP addresses, for a limited time.',
        ] },
        'Some of these providers are outside the EU/EEA, mainly in the USA. Such transfers are made under the safeguards in the providers’ data processing terms: the EU standard contractual clauses or the EU–US Data Privacy Framework.',
      ] },
      { id: 'browser', title: 'Cookies and storage in your browser', body: [
        'LekseRadar does not use cookies for tracking or advertising. Your browser stores only what is needed for the site to work: your login token, your theme and your language (in “local storage”). The font is part of the site and is not loaded from Google.',
      ] },
      { id: 'keep', title: 'How long we keep data', body: [{ list: [
        'Your data is kept while you use LekseRadar.',
        'If you do not log in for 24 months, your account is deleted automatically with all its data.',
        'You can delete your account at any time in [Settings](/settings). Everything is deleted at once, and Google Classroom access is withdrawn.',
        'Disconnecting a platform deletes its access, its homework and the chats, quizzes, flashcards and history you made for that homework.',
        'Deleting a chat deletes all its messages.',
        'An unfinished sign-up is forgotten after 15 minutes.',
      ] }] },
      { id: 'rights', title: 'Your rights', body: [{ list: [
        'Access and data portability (Art. 15 and 20): download all your data as a file in [Settings](/settings).',
        'Rectification (Art. 16): change your name, language and password in your profile and settings.',
        'Erasure (Art. 17): delete your account in [Settings](/settings).',
        'Restriction and objection (Art. 18 and 21): turn off email reminders in Settings, or contact us.',
        'Complaint: you can complain to the Norwegian Data Protection Authority, [Datatilsynet](https://www.datatilsynet.no).',
      ] }, 'We answer requests within one month.'] },
      { id: 'ai', title: 'AI and automated decisions', body: [
        'The AI tutor, quizzes and flashcards are generated by an AI service, and text answers in quizzes are checked by it. These results are for practice only. LekseRadar makes no decisions about you that have legal or similarly significant effects (GDPR Art. 22).',
      ] },
      { id: 'children', title: 'Children and young people', body: [
        'LekseRadar is made for school students. To create an account you confirm that you are at least 13 years old, or that a parent or guardian agrees. In Norway, the age limit for giving consent to online services yourself is 13 (Personal Data Act § 5).',
        'If your school uses LekseRadar, the school decides how students and parents are informed.',
      ] },
      { id: 'security', title: 'Security', body: [
        'We protect your data with encrypted connections, encrypted platform access, password hashing, limits on login attempts and AI requests, and restricted access to the servers. If a personal data breach happens, we notify Datatilsynet within 72 hours and you without undue delay when the law requires it. Found a security problem? [Report it privately](' + SECURITY_URL + ').',
      ] },
      { id: 'changes', title: 'Changes to this policy', body: [
        'If we change this policy, we update the version date at the top. If a change affects how we use your data in an important way, we tell you on the site before it applies.',
      ] },
      { id: 'contact', title: 'Contact', body: [
        'Most requests can be done yourself in [Settings](/settings). For other questions, [open an issue on GitHub](' + ISSUES_URL + ') — do not write your name, email address, school or other personal data in it; we will reply there with how to continue privately.',
      ] },
    ],
  },

  terms: {
    eyebrow: 'TERMS OF USE',
    title: 'The rules for using LekseRadar',
    intro: 'By creating an account you agree to these terms. Please read them together with the [privacy policy](/privacy).',
    sections: [
      { id: 'about', title: 'What LekseRadar is', body: [
        'LekseRadar collects your homework from your school platforms in one place and helps you learn with an AI tutor, quizzes and flashcards. It is free.',
        'LekseRadar was started by a student team at the Oslo Sprint hackathon (6–8 November 2026) and is still being developed. Features can change, and the service can be paused or shut down. If we shut it down, we delete all personal data.',
      ] },
      { id: 'account', title: 'Your account', body: [{ list: [
        'You must be at least 13 years old, or have a parent or guardian agree, to create an account.',
        'Use your own email address and keep your password to yourself.',
        'One account per person. Do not use someone else’s account.',
        'You are responsible for what happens in your account. If you think someone else has used it, change your password.',
      ] }] },
      { id: 'platforms', title: 'Connecting school platforms', body: [{ list: [
        'Connect only your own accounts, and follow your school’s rules and the platform’s rules.',
        'The connections to itslearning and Visma InSchool are unofficial. They can stop working at any time.',
        'You can disconnect a platform at any time. This deletes its access, its homework and the chats, quizzes and flashcards you made for it.',
        'Homework belongs to your school and teachers. LekseRadar only shows you a copy.',
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
        'What you write (chats and answers) is yours. We use it only to run LekseRadar for you, as described in the [privacy policy](/privacy), and never to train AI models.',
      ] },
      { id: 'warranty', title: 'No guarantees', body: [
        'LekseRadar is provided “as is”. We do our best, but we cannot promise that it works without errors or interruptions, that reminders always arrive, or that AI answers are correct. As far as the law allows, the team is not responsible for missed deadlines, lost data or decisions you make based on AI answers. Your rights as a consumer under Norwegian law are not affected.',
      ] },
      { id: 'end', title: 'Ending', body: [
        'You can stop at any time and delete your account in [Settings](/settings); all your data is deleted. An account without a login for 24 months is deleted automatically.',
      ] },
      { id: 'changes', title: 'Changes to these terms', body: [
        'We may change these terms. The version date at the top shows the current version. If a change is important, we tell you on the site before it applies.',
      ] },
      { id: 'contact', title: 'Contact', body: [
        'Questions: [open an issue on GitHub](' + ISSUES_URL + ') without personal data. Security problems: [report them privately](' + SECURITY_URL + ').',
      ] },
    ],
  },
};
