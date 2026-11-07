// Типы ответов и запросов бэкенда — как в API.md («Типы»).
export type Locale = 'nb' | 'nn' | 'en' | 'ru' | 'uk';
export type Level = 'beginner' | 'intermediate' | 'advanced';

export type User = {
  id: number;
  email: string;
  displayName: string;
  locale: Locale;          // язык интерфейса
  timezone: string;        // "Europe/Oslo"
  createdAt: string;       // ISO-8601, UTC
  lastLoginAt: string | null;
};

export type AuthResponse = { token: string; user: User };

export type Chat = {
  id: number;
  title: string;                  // "Ny samtale", "Ny samtale: тема" или "Quiz: тема"
  topic: string | null;
  mode: 'tutor' | 'quiz';         // не меняется после создания
  subject: string | null;
  language: string | null;        // язык ответов ИИ; null — тьютор сначала спросит язык
  level: Level;
  assignmentId: number | null;
  quizSetId: number | null;       // только у чата quiz
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Message = {
  id: number;
  role: 'student' | 'tutor';
  content: string;         // у tutor — Markdown (может быть LaTeX в $...$)
  model: string | null;
  createdAt: string;
};

export type ChatReply = { chat: Chat; userMessage: Message; assistantMessage: Message };

export type Subject = { id: number; name: string };

export type Question = {
  position: number;              // с 1
  exerciseId: number;
  type: 'single_choice' | 'multiple_choice' | 'text';
  question: string;              // может содержать LaTeX в $...$
  options: string[] | null;      // null у "text"
};

export type Quiz = {
  id: number;
  quizSetId: number;
  title: string;
  subject: string | null;
  topic: string;
  level: Level;
  status: 'in_progress' | 'completed' | 'abandoned';
  currentPosition: number;
  questions: Question[];         // без правильных ответов
};

export type QuizSummary = {
  id: number; quizSetId: number; chatId: number | null; assignmentId: number | null;
  title: string; subject: string | null; topic: string; level: Level;
  status: Quiz['status']; score: number | null; currentPosition: number; questionCount: number;
  startedAt: string; completedAt: string | null;
};

export type QuizProgress = {
  subject: string | null; topic: string; quizzesCompleted: number; quizzesInProgress: number;
  bestScore: number | null; lastScore: number | null; lastActivity: string;
};

export type AnswerResult = {
  verdict: 'correct' | 'partial' | 'incorrect';
  explanation: string | null;
  correctAnswer: string;         // у multiple_choice — JSON-массив строкой
};

export type Flashcard = {
  exerciseId: number; subject: string | null; topic: string;
  front: string; back: string; explanation: string | null; nextReviewAt: string;
};

export type FlashcardStats = { cards: number; dueNow: number; learned: number; reviews: number };
export type ReviewResult = { nextReviewAt: string; intervalDays: number };
export type Deck = { quizSetId: number; chatId: number | null; generated: boolean; added: number; cards: Flashcard[] };

export type ApiError = {
  status: number;
  message: string;                        // текст бэкенда (по-русски) — интерфейс показывает свой по коду
  fields: Record<string, string> | null;  // ошибки по полям формы (только 400)
  timestamp: string;
};

export type Health = { status: 'UP' | 'DOWN' };

// Тела запросов
export type RegisterRequest = { email: string; password: string; displayName: string };
export type RegisterStarted = { email: string; expiresInSeconds: number };  // 202: код ушёл на почту, токена нет
export type VerifyEmailRequest = { email: string; code: string };
export type LoginRequest = { email: string; password: string };
export type UpdateUserRequest = Partial<Pick<User, 'displayName' | 'locale' | 'timezone'>>;
export type ChangePasswordRequest = { currentPassword: string; newPassword: string };
export type CreateChatRequest = { title?: string; topic?: string; subject?: string; mode?: Chat['mode']; level?: Level; assignmentId?: number | null };
export type UpdateChatRequest = { title?: string; topic?: string; archived?: boolean };
export type StartQuizRequest = { subject: string; topic: string; level?: Level; count?: number; assignmentId?: number | null; fresh?: boolean };
export type AnswerRequest = { position: number; answer: string };

// 🕓 Платформы (ARCHITECTURE.md §5): поля — как ждёт homework-collector (README «credentials»).
export type PlatformKey = 'canvas' | 'itslearning' | 'inschool' | 'teams' | 'ics';
export type PlatformCredentials = Record<string, unknown>;
export type PlatformConnection = { platform: PlatformKey; connected: boolean; updatedAt?: string | null };
// ДЗ (GET /homework, GET/PATCH /homework/{id})
export type Platform = 'canvas' | 'teams' | 'itslearning' | 'inschool';
export type AssignmentStatus = 'todo' | 'overdue' | 'submitted' | 'graded';
export type Assignment = {
  id: number;
  source: Platform;
  title: string;
  course: string;
  dueAt: string | null;
  status: AssignmentStatus;       // todo с прошедшим дедлайном приходит как overdue
  url: string | null;
  kind: string;                   // assignment | exam | quiz | task …
  description: string;            // в списке обрезан до 300 символов, полный — в GET /homework/{id}
};
export type UpdateAssignmentRequest = { status: 'todo' | 'submitted' };

// Дашборд и история (GET /dashboard, GET /history)
export type TopicProgress = { topic: string; attempts: number; masteryPct: number | null };
export type DayActivity = { day: string; events: number; exercises: number; quizzes: number };
export type Dashboard = {
  homeworkTodo: number; homeworkOverdue: number; homeworkDone: number;
  quizzesCompleted: number; avgQuizScore: number | null; exercisesAnswered: number;
  questionsAsked: number; activeDays30: number;
  flashcards: FlashcardStats; topics: TopicProgress[]; days: DayActivity[];
};
export type HistoryEvent = {
  id: number;
  type: string;                   // login | chat_started | quiz_started | exercise_answered | homework_done …
  refTable: string | null;
  refId: number | null;
  payload: Record<string, unknown>;
  createdAt: string;
};
