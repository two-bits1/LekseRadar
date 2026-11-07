// Шар AI-veileder: стеклянная оболочка, «северное сияние» внутри и луч радара.
// Наведение — переливается; клик — распадается на частицы и открывает чат; «думает», пока ИИ отвечает.
import { useRef, useState, type CSSProperties, type MouseEvent } from 'react';
import { NavLink } from 'react-router-dom';
import { useOrbThinking } from './orbState';

const COLORS = ['#5fd3c0', '#7a9cff', '#ffad84', '#b69cff', '#dff6ff'];
const PARTICLES = 170;
const BURST_SIZE = 280;  // px, холст вокруг шара
const POP_MS = 1000;

const reducedMotion = () => typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;

function OrbBody({ size }: { size?: number }) {
  const style = size ? ({ '--orb-size': `${size}px` } as CSSProperties) : undefined;
  return <span className="orb" style={style}><span className="orb-in"><i /><i /><i /><i /><span className="orb-sweep" /></span></span>;
}

// Декоративный шар (без действия) — для главной, чата, входа.
export function Orb({ size }: { size?: number }) {
  return <span aria-hidden="true" className="orb-deco"><OrbBody size={size} /></span>;
}

// Частицы тех же цветов разлетаются из шара, от него расходится кольцо, как от радара.
function burst(canvas: HTMLCanvasElement, radius: number, dark: boolean) {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  const dpr = window.devicePixelRatio || 1;
  canvas.width = BURST_SIZE * dpr; canvas.height = BURST_SIZE * dpr;
  const c = BURST_SIZE / 2;
  let parts = Array.from({ length: PARTICLES }, (_, n) => {
    const a = Math.random() * Math.PI * 2, d = Math.sqrt(Math.random()) * radius * 0.95, sp = 1.1 + Math.random() * 3.1;
    return { x: c + Math.cos(a) * d, y: c + Math.sin(a) * d, vx: Math.cos(a) * sp - Math.sin(a) * sp * 0.35, vy: Math.sin(a) * sp + Math.cos(a) * sp * 0.35,
      r: 1 + Math.random() * 2.3, color: COLORS[n % COLORS.length], life: 0, max: 45 + Math.random() * 35 };
  });
  let ring: { r: number; a: number } | null = { r: radius * 1.08, a: 0.75 };
  const tick = () => {
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, BURST_SIZE, BURST_SIZE);
    ctx.globalCompositeOperation = dark ? 'lighter' : 'source-over';
    if (ring) {
      ctx.globalAlpha = ring.a; ctx.beginPath(); ctx.arc(c, c, ring.r, 0, Math.PI * 2);
      ctx.strokeStyle = dark ? '#7fb3a9' : '#3f6e67'; ctx.lineWidth = 1.5; ctx.stroke();
      ring.r += 2.4; ring.a *= 0.94; if (ring.a < 0.02) ring = null;
    }
    parts = parts.filter(p => p.life < p.max);
    for (const p of parts) {
      p.life++; p.x += p.vx; p.y += p.vy; p.vx *= 0.965; p.vy *= 0.965;
      const t = 1 - p.life / p.max;
      ctx.globalAlpha = Math.max(t, 0); ctx.beginPath(); ctx.arc(p.x, p.y, p.r * (0.6 + t * 0.6), 0, Math.PI * 2);
      ctx.fillStyle = p.color; ctx.fill();
    }
    ctx.globalAlpha = 1;
    if (parts.length || ring) requestAnimationFrame(tick); else ctx.clearRect(0, 0, BURST_SIZE, BURST_SIZE);
  };
  requestAnimationFrame(tick);
}

// Шар в шапке — ссылка на чат (открывается и в новой вкладке, на /chat помечен активным).
export function OrbLink({ label, to }: { label: string; to: string }) {
  const thinking = useOrbThinking();
  const [popping, setPopping] = useState(false);
  const busy = useRef(false);
  const holder = useRef<HTMLSpanElement>(null);
  const canvas = useRef<HTMLCanvasElement>(null);

  const onClick = (e: MouseEvent<HTMLAnchorElement>) => {
    if (busy.current) { e.preventDefault(); return; }  // двойной клик: одна анимация и один переход
    if (reducedMotion() || !holder.current || !canvas.current) return;
    busy.current = true;
    const orb = holder.current;
    canvas.current.style.left = `${orb.offsetLeft + orb.offsetWidth / 2}px`;
    canvas.current.style.top = `${orb.offsetTop + orb.offsetHeight / 2}px`;
    const dark = orb.closest('[data-theme]')?.getAttribute('data-theme') === 'dark';
    setPopping(true);
    burst(canvas.current, orb.offsetWidth / 2, dark);
    setTimeout(() => { setPopping(false); busy.current = false; }, POP_MS);
  };

  return <NavLink to={to} onClick={onClick} aria-label={label}
    className={({ isActive }) => ['orb-link', isActive && 'active', thinking && 'thinking', popping && 'popping'].filter(Boolean).join(' ')}>
    <span ref={holder} className="orb-holder"><OrbBody /></span>
    <span>{label}</span>
    <canvas ref={canvas} className="orb-burst" aria-hidden="true" />
  </NavLink>;
}
