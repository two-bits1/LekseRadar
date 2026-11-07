// Markdown + формулы LaTeX ($...$, $$...$$) — ответы тьютора, вопросы и пояснения квизов, карточки.
// Сырой HTML не рендерится (по умолчанию react-markdown), ссылки открываются в новой вкладке.
import Markdown from 'react-markdown';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';

export function Md({ children, inline = false }: { children: string; inline?: boolean }) {
  return <Markdown
    remarkPlugins={[remarkMath]}
    rehypePlugins={[rehypeKatex]}
    components={{
      a: ({ node, ...props }) => <a {...props} target="_blank" rel="noopener noreferrer" />,
      ...(inline ? { p: ({ node, ...props }) => <span {...props} /> } : {}),
    }}>{children}</Markdown>;
}
