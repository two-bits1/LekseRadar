// Публичная главная «/» для гостя (композиция как у Raycast): шапка-капсула, крупный заголовок по центру,
// регистрация по центру, на фоне — миллиметровка с осями и тонкой параболой. После входа «/» показывает главную.
import { Link } from 'react-router-dom';
import { ArrowDown, Inbox, Layers, ListChecks, Sparkles } from 'lucide-react';
import type { ReactNode } from 'react';
import { PATHS } from '../paths';
import { AuthForm } from './Account';
import { useT } from '../i18n';

// Миллиметровка: мелкая клетка 24px, крупная 120px, оси с делениями и парабола y = a(x − 720)² + c.
function GraphPaper() {
  const ticks = Array.from({ length: 11 }, (_, i) => 120 + i * 120);
  return <svg className="lp-graph" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
    <defs>
      <pattern id="lp-minor" width="24" height="24" patternUnits="userSpaceOnUse"><path d="M24 0V24M0 24H24" className="lp-minor" /></pattern>
      <pattern id="lp-major" width="120" height="120" patternUnits="userSpaceOnUse"><path d="M120 0V120M0 120H120" className="lp-major" /></pattern>
      <linearGradient id="lp-curve" x1="0" x2="1" y1="0" y2="0">
        <stop offset="0" stopColor="#5fd3c0" stopOpacity=".15" /><stop offset=".3" stopColor="#5fd3c0" />
        <stop offset=".55" stopColor="#7a9cff" /><stop offset=".8" stopColor="#a98bff" /><stop offset="1" stopColor="#a98bff" stopOpacity=".15" />
      </linearGradient>
      <filter id="lp-glow" x="-10%" y="-10%" width="120%" height="120%"><feGaussianBlur stdDeviation="6" /></filter>
    </defs>
    <rect width="1440" height="900" fill="url(#lp-minor)" />
    <rect width="1440" height="900" fill="url(#lp-major)" />
    <g className="lp-axes">
      <path d="M0 600H1440M720 0V900" />
      {ticks.map(x => <path key={`x${x}`} d={`M${x} 594V606`} />)}
      {[120, 240, 360, 480, 720, 840].map(y => <path key={`y${y}`} d={`M714 ${y}H726`} />)}
    </g>
    <path className="lp-curve lp-curve-glow" d="M0 60Q720 1260 1440 60" filter="url(#lp-glow)" />
    <path className="lp-curve" d="M0 60Q720 1260 1440 60" />
    <circle className="lp-vertex" cx="720" cy="660" r="4" />
  </svg>;
}

function Feature({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return <article className="lp-feature"><span className="lp-feature-icon">{icon}</span><h3>{title}</h3><p>{text}</p></article>;
}

export function Landing() {
  const t = useT(); const l = t.landing;
  return <section className="landing">
    <div className="lp-hero">
      <div className="lp-bg"><GraphPaper /></div>
      <div className="lp-hero-inner">
        <h1 className="lp-title">{l.title1}<br />{l.title2}</h1>
        <p className="lp-intro">{l.intro}</p>
        <AuthForm register />
        <p className="lp-links">{l.haveAccount} <Link to={PATHS.login}>{t.auth.login}</Link><span aria-hidden="true">·</span><a href="#features">{l.how} <ArrowDown size={14} /></a></p>
      </div>
    </div>
    <section id="features" className="lp-features">
      <h2>{l.featuresTitle}</h2>
      <div className="lp-grid">
        <Feature icon={<Inbox size={18} />} {...l.features.homework} />
        <Feature icon={<Sparkles size={18} />} {...l.features.tutor} />
        <Feature icon={<ListChecks size={18} />} {...l.features.quiz} />
        <Feature icon={<Layers size={18} />} {...l.features.cards} />
      </div>
    </section>
  </section>;
}
