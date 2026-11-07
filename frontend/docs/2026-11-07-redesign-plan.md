# Редизайн «Северное сияние» — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** весь фронтенд LekseRadar в стиле «Северное сияние» с живым шаром AI-veileder и языком интерфейса nb / en / ru из настроек.

**Architecture:** стиль меняется через токены в `src/styles.css` (стекло, сияние, Inter), поэтому все страницы обновляются сразу. Шар — отдельный компонент с общим состоянием «думает» (маленький стор). Перевод — свои типизированные словари: `useT()` возвращает объект строк текущего языка; `nb` — эталон, `en` и `ru` обязаны иметь ту же форму (проверяет TypeScript).

**Tech Stack:** React 19, React Router 7, Vite 8, Tailwind 4 (только reset), vitest 5 (node), Inter с Google Fonts.

**Spec:** `frontend/docs/2026-11-07-redesign-design.md`

## Global Constraints

- Палитра — токены из `COLORS.md`; ярче можно только шар и акценты. Текст — WCAG AA (≥ 4.5:1), без чистых `#000` / `#FFF` для фона и текста.
- `prefers-reduced-motion: reduce` → никакой анимации (уже есть глобальное правило в конце `styles.css`), клик по шару просто открывает `/chat`.
- Языки: `nb` (по умолчанию), `en`, `ru`; выбор только в настройках; ключ `localStorage` — `lekseradar-lang`.
- Шапка: Kalender, Læringskort | шар | Profil, Innstillinger; справа только «Logg inn» до входа, после входа пусто.
- Адреса страниц и URL API не меняются (контракт с `docs/API.md`, тесты `paths.test.ts`, `api.test.ts`).
- Каждая задача — отдельный коммит в `Frontend-setup`; после неё проходят `pnpm test` и `pnpm build`.

## Review Focus

- Смена языка без входа не должна вызывать `PATCH /users/me` (иначе 401 → выброс на `/login`) — тест в задаче 3.
- Профиль с `locale: 'nn'` (бэкенд это допускает) → интерфейс на `nb`, без ошибок — тест в задаче 3.
- Мусор в `localStorage` (`lekseradar-lang = 'xx'`) → `nb` — тест в задаче 3.
- Двойной клик по шару → одна анимация и один переход (флаг занятости) — проверка в браузере, задача 2.
- Телефон 375 px: шапка с 4 пунктами и шаром без горизонтальной прокрутки — проверка в задаче 5.

---

### Task 1: Общий стиль — стекло, сияние, Inter

**Files:**
- Modify: `src/styles.css` (импорт шрифта, токены `.app` / `.app[data-theme=dark]`, шапка, карточки)
- Modify: `index.html` (без изменений шрифта — шрифт грузится из CSS)

**Interfaces:**
- Produces: CSS-токены `--glass`, `--glass-strong`, `--glass-border`, `--aurora-1`, `--aurora-2`, `--orb-core` в обеих темах; класс-модификатор не нужен — стекло применяется к существующим `.header`, `.lesson-card`, `.assignment`, `.progress-card`, `.calendar-panel`, `.flip-card`, `.msg.tutor`.

- [ ] **Step 1:** заменить импорт DM Sans + Manrope на Inter (400–700); все `font-family` ('DM Sans', Manrope) → `Inter, system-ui, sans-serif`.
- [ ] **Step 2:** добавить токены: светлая тема `--glass: rgba(255,253,248,.78)`, тёмная `rgba(38,42,46,.62)`; `--glass-border` светлая `rgba(43,45,51,.1)`, тёмная `rgba(255,255,255,.08)`; `--aurora-1` бирюза `#5fd3c0`, `--aurora-2` синий `#7a9cff`.
- [ ] **Step 3:** сияние: `.app::before` — фиксированный слой вверху (высота ~420 px), два радиальных пятна `--aurora-1/2`, прозрачность .22 в тёмной и .12 в светлой, `pointer-events:none`, `z-index:0`; контент над ним.
- [ ] **Step 4:** стекло: перечисленные блоки получают `background: var(--glass)`, `border: 1px solid var(--glass-border)`, `backdrop-filter: blur(16px) saturate(1.2)`; в `@supports not (backdrop-filter: blur(1px))` — `background: var(--surface)`.
- [ ] **Step 5:** появление карточек: `@keyframes rise` (opacity 0→1, translateY 8px→0, .45s) на `.lesson-card, .assignment, .progress-card, .msg`.
- [ ] **Step 6:** `pnpm test && pnpm build` → PASS; глазами в браузере светлая и тёмная тема главной и чата; контраст `--text` на `--glass` поверх фона ≥ 4.5:1 (расчёт по худшему случаю — непрозрачная подложка `--bg`).
- [ ] **Step 7:** commit `frontend: стиль «Северное сияние» — стекло, сияние, Inter`.

### Task 2: Шапка и новый шар

**Files:**
- Create: `src/components/Orb.tsx`, `src/components/orbState.ts`, `src/components/orbState.test.ts`
- Modify: `src/main.tsx` (шапка, удалить старый `Orb`), `src/pages/Dashboard.tsx`, `src/pages/Chats.tsx` (импорт `Orb`, «думает»), `src/styles.css` (стили шара, удалить старые `.orb*`)

**Interfaces:**
- Produces:
  - `orbState.ts`: `setOrbThinking(on: boolean): void`, `useOrbThinking(): boolean` (на `useSyncExternalStore`), `subscribe`/`get` для теста.
  - `Orb.tsx`: `Orb({ size?: number }): JSX.Element` — декоративный шар (aria-hidden); `OrbButton({ label: string, onActivate: () => void }): JSX.Element` — кнопка в шапке с наведением, распадом на частицы (canvas поверх, ~170 частиц цветов `#5fd3c0 #7a9cff #ffad84 #b69cff #dff6ff`, кольцо-пинг), флагом занятости ~1 с и состоянием «думает» из `useOrbThinking()`.

- [ ] **Step 1:** тест `orbState.test.ts`: по умолчанию `false`; `setOrbThinking(true)` → `get() === true` и подписчик вызван один раз; повторный `true` подписчика не вызывает.
- [ ] **Step 2:** `pnpm test` → FAIL (нет модуля).
- [ ] **Step 3:** реализовать `orbState.ts`.
- [ ] **Step 4:** `pnpm test` → PASS.
- [ ] **Step 5:** `Orb.tsx` по таблице состояний из спецификации (покой: дыхание 5 с; наведение/фокус: скорость ×2.5, `saturate(1.5) brightness(1.2)`; клик: `scale(1.1)`, частицы, кольцо, сразу `navigate('/chat')`; думает: быстрее + пульс 1.3 с; reduced motion: без анимации и частиц). Слои: 4 пятна + сектор радара (`conic-gradient`) + стеклянная оболочка.
- [ ] **Step 6:** шапка в `main.tsx`: `OrbButton` по центру меню; справа `HeaderUser` → «Logg inn» без токена, `null` с токеном; логотип — иконка радара цвета `--primary` + «LekseRadar.» Inter 650.
- [ ] **Step 7:** `ChatView` вызывает `setOrbThinking(true)` на время ожидания ответа и `false` в `finally` и при размонтировании.
- [ ] **Step 8:** проверка в браузере: наведение, клик (один переход при двойном клике), «думает» во время ответа (фейковый бэкенд), обе темы; `pnpm build` → PASS.
- [ ] **Step 9:** commit `frontend: шапка и живой шар AI-veileder`.

### Task 3: Перевод nb / en / ru

**Files:**
- Create: `src/i18n/nb.ts`, `src/i18n/en.ts`, `src/i18n/ru.ts`, `src/i18n/index.tsx`, `src/i18n/i18n.test.ts`
- Modify: `src/main.tsx` (провайдер, тексты шапки/подвала/плашки), `src/pages/Other.tsx` (выбор языка в настройках), `src/pages/Account.tsx` (убрать «Språk» из формы профиля), `src/forms.tsx` (`LOCALES` → только для профиля не нужен; удалить, если не используется)

**Interfaces:**
- Produces:
  - `type Lang = 'nb' | 'en' | 'ru'`; `type Dict = typeof nb` (значения — строки или функции `(...args) => string`).
  - `langFromLocale(locale: string | null | undefined): Lang` — `'en'→'en'`, `'ru'→'ru'`, всё остальное (`nb`, `nn`, мусор) → `'nb'`.
  - `loadLang(): Lang`, `saveLang(l: Lang): void` (ключ `lekseradar-lang`, try/catch).
  - `localeTag(l: Lang): 'nb-NO' | 'en-GB' | 'ru-RU'`.
  - `I18nProvider` (внутри `AuthProvider`): после загрузки `user` язык = `langFromLocale(user.locale)`; `setLang(l)` → сохранить локально, обновить `<html lang>`, и **только если есть токен** — `usersApi.update({ locale: l })` + `updateUser`.
  - `useT(): Dict`, `useLang(): { lang: Lang; setLang(l: Lang): void; tag: string }`.
- Consumes: `useAuth()` (`token`, `user`, `updateUser`), `usersApi.update`.

- [ ] **Step 1:** тесты `i18n.test.ts`: `langFromLocale('nn') === 'nb'`, `('en') === 'en'`, `('ru') === 'ru'`, `(undefined) === 'nb'`; `loadLang()` с `'xx'` в localStorage → `'nb'`; в `en` и `ru` нет пустых строк (рекурсивный обход); набор ключей `en`/`ru` совпадает с `nb`.
- [ ] **Step 2:** `pnpm test` → FAIL.
- [ ] **Step 3:** реализовать модуль и словари (ключи по разделам: `nav`, `header`, `footer`, `common`, `auth`, `profile`, `settings`, `dashboard`, `chat`, `learning`, `calendar`, `status`, `soon`, `notFound`).
- [ ] **Step 4:** тест «смена языка без токена не вызывает PATCH»: вынести решение в чистую функцию `shouldSyncProfile(token: string | null, lang: Lang, user: User | null): boolean` (true только при токене и `langFromLocale(user.locale) !== lang`), тест на три случая.
- [ ] **Step 5:** `pnpm test` → PASS.
- [ ] **Step 6:** настройки: строка «Språk / Language / Язык» с тремя кнопками `aria-pressed` (Norsk, English, Русский — каждая на своём языке); профиль: убрать выбор языка.
- [ ] **Step 7:** commit `frontend: язык интерфейса nb/en/ru в настройках`.

### Task 4: Страницы — перевод и доводка вида

**Files:**
- Modify: `src/pages/Account.tsx`, `src/pages/Dashboard.tsx`, `src/pages/Chats.tsx`, `src/pages/Learning.tsx`, `src/pages/Other.tsx`, `src/main.tsx`, `src/data.ts` (подписи `/status` через словарь), `src/styles.css`

**Interfaces:**
- Consumes: `useT()`, `useLang().tag` (даты в `Intl.DateTimeFormat`), `Orb` из задачи 2.

- [ ] **Step 1:** вход/регистрация: все тексты из `t.auth`, стеклянная карточка по центру, шар-декор над заголовком (`Orb size={56}`).
- [ ] **Step 2:** главная: тексты из `t.dashboard` (дата — `Intl` по `tag`, без хардкода «Mandag 9. november»); демо-задания остаются на норвежском.
- [ ] **Step 3:** чат: тексты из `t.chat`, даты по `tag`.
- [ ] **Step 4:** Kalender, Læringskort/квиз, профиль, настройки, «Kommer snart», 404, `/status`: тексты из словаря; названия месяцев/дней — через `Intl` по `tag`.
- [ ] **Step 5:** `pnpm test && pnpm build` → PASS; в браузере пройти все страницы на трёх языках (нет ключей вместо текста, нет переполнения на 375 px).
- [ ] **Step 6:** коммиты по группам: `frontend: перевод и вид — вход и главная`, `… — чат`, `… — остальные страницы`.

### Task 5: Проверка целиком

- [ ] **Step 1:** `pnpm test && pnpm build` → PASS.
- [ ] **Step 2:** браузер: 375 px и 1280 px; светлая и тёмная тема; эмуляция `prefers-reduced-motion`; три языка; вход/выход; шар (наведение, клик, «думает»).
- [ ] **Step 3:** обновить `COLORS.md` (стекло, сияние, цвета шара) и `README.md` фронтенда (язык, шар); commit `frontend: документация редизайна`.
