// Шапка в духе Raycast: капсула со стеклом, логотип слева, меню по центру с шаром ровно посередине, действия справа.
// Гость: Kalender · Læringskort · шар · Innstillinger, справа «Logg inn» и «Opprett konto».
// После входа: Kalender · Læringskort · шар · Profil · Innstillinger, справа пусто.
import type { ReactNode } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { CalendarDays, Layers, Radar, Settings2, UserRound } from 'lucide-react';
import { PATHS } from '../paths';
import { useAuth } from '../auth';
import { useT } from '../i18n';
import { OrbLink } from './Orb';

export function AppHeader() {
  const { token } = useAuth();
  const t = useT();
  const item = (to: string, icon: ReactNode, label: string) =>
    <NavLink to={to} className="tb-link" aria-label={label}>{icon}<span>{label}</span></NavLink>;

  return <header className="topbar">
    <div className="topbar-inner">
      <Link to={PATHS.home} className="tb-brand" aria-label={t.header.home}>
        <span className="tb-mark"><Radar size={18} /></span><span className="tb-name">LekseRadar</span>
      </Link>
      <nav className="tb-nav" aria-label={t.nav.main}>
        <div className="tb-side tb-left">
          {item(PATHS.homework, <CalendarDays size={18} />, t.nav.calendar)}
          {item(PATHS.flashcards, <Layers size={18} />, t.nav.flashcards)}
        </div>
        <OrbLink to={PATHS.chats} label={t.nav.tutor} />
        <div className="tb-side tb-right">
          {token && item(PATHS.profile, <UserRound size={18} />, t.nav.profile)}
          {item(PATHS.settings, <Settings2 size={18} />, t.nav.settings)}
        </div>
      </nav>
      <div className="tb-actions">
        {!token && <>
          <Link to={PATHS.login} className="tb-login">{t.header.login}</Link>
          <Link to={PATHS.register} className="btn btn-primary btn-sm">{t.auth.register}</Link>
        </>}
      </div>
    </div>
  </header>;
}
