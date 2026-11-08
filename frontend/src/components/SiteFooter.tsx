// Подвал сайта: о проекте, ссылки на статус и GitHub, политика конфиденциальности и условия использования.
// На странице чата скрыт (styles.css: .app:has(.chat-page)>.site-footer).
import { Link } from 'react-router-dom';
import { ArrowUpRight } from 'lucide-react';
import { PATHS } from '../paths';
import { useT } from '../i18n';
import { ISSUES_URL, REPO_URL, SECURITY_URL } from '../legal/types';

function External({ href, children }: { href: string; children: string }) {
  const t = useT();
  return <a href={href} target="_blank" rel="noreferrer">{children}<ArrowUpRight size={12} aria-hidden="true" /><span className="sr-only">{t.footer.external}</span></a>;
}

export function SiteFooter() {
  const t = useT(); const f = t.footer;
  return <footer className="site-footer">
    <div className="footer-main">
      <div className="footer-brand">
        <Link to={PATHS.home} className="footer-logo">LekseRadar.</Link>
        <p className="footer-tagline">{f.tagline}</p>
        <p>{f.about}</p>
      </div>
      <nav className="footer-nav" aria-label={f.nav}>
        <div>
          <h2>{f.legal}</h2>
          <ul>
            <li><Link to={PATHS.privacy}>{f.privacy}</Link></li>
            <li><Link to={PATHS.terms}>{f.terms}</Link></li>
            <li><External href={SECURITY_URL}>{f.security}</External></li>
          </ul>
        </div>
        <div>
          <h2>{f.project}</h2>
          <ul>
            <li><Link to={PATHS.status}>{f.status}</Link></li>
            <li><External href={REPO_URL}>{f.source}</External></li>
            <li><External href={ISSUES_URL}>{f.feedback}</External></li>
          </ul>
        </div>
      </nav>
    </div>
  </footer>;
}
