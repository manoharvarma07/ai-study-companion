import { useEffect, useRef } from 'react';
import { ArrowUp, FileText, Sparkles } from 'lucide-react';
import { cn } from '../lib/utils';
import { Badge } from './ui';
import type { Citation } from '../api/types';

/* ---------- tiny rich-text renderer: **bold**, bullets, numbered lists ---------- */

function renderInline(text: string, keyPrefix: string): React.ReactNode[] {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((p, i) => {
    if (p.startsWith('**') && p.endsWith('**') && p.length > 4) {
      return <strong key={`${keyPrefix}-${i}`}>{p.slice(2, -2)}</strong>;
    }
    return <span key={`${keyPrefix}-${i}`}>{p}</span>;
  });
}

export function RichText({ text }: { text: string }) {
  const clean = text.replace(/\\\\/g, '').replace(/\\\*\*/g, '**').replace(/\\-/g, '-');
  const lines = clean.split('\n');
  const blocks: React.ReactNode[] = [];
  let list: { ordered: boolean; items: string[] } | null = null;
  let key = 0;

  const flush = () => {
    if (!list) return;
    if (list.ordered) {
      blocks.push(
        <ol key={`b-${key++}`}>
          {list.items.map((it, i) => <li key={i}>{renderInline(it, `ol-${key}-${i}`)}</li>)}
        </ol>,
      );
    } else {
      blocks.push(
        <ul key={`b-${key++}`}>
          {list.items.map((it, i) => <li key={i}>{renderInline(it, `ul-${key}-${i}`)}</li>)}
        </ul>,
      );
    }
    list = null;
  };

  for (const raw of lines) {
    const line = raw.trim();
    if (line === '') {
      flush();
      continue;
    }
    const bullet = line.match(/^(?:[•\-*]|\d+[.)])\s+(.*)$/);
    const ordered = /^\d+[.)]\s+/.test(line);
    if (bullet) {
      if (!list || list.ordered !== ordered) {
        flush();
        list = { ordered, items: [] };
      }
      list.items.push(bullet[1]);
    } else {
      flush();
      blocks.push(<p key={`b-${key++}`}>{renderInline(line, `p-${key}`)}</p>);
    }
  }
  flush();
  return <div className="ai-prose">{blocks}</div>;
}

/* ---------- messages ---------- */

export function AIMessage({
  content, grounded, citations,
}: {
  content: string; grounded?: boolean | null; citations?: Citation[];
}) {
  const deduped = (citations ?? []).filter(
    (c, i, arr) => c?.material && arr.findIndex((o) => o?.material === c.material && o?.page === c.page) === i,
  );
  return (
    <div className="flex gap-3">
      <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-forest-600 to-forest-800 text-white shadow-[0_4px_12px_-4px_rgba(47,79,48,0.6)]">
        <Sparkles size={15} />
      </span>
      <div className="min-w-0 flex-1 rounded-2xl rounded-tl-md border border-forest-900/[0.08] bg-gradient-to-b from-moss-50 to-cream-card px-4 py-3.5 shadow-card dark:border-white/[0.06] dark:from-forest-950/40 dark:to-white/[0.03]">
        <p className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase tracking-[0.08em] text-forest-700 dark:text-forest-300">
          AI Tutor
        </p>
        <RichText text={content} />
        <div className="mt-2.5 flex flex-wrap items-center gap-1.5 border-t border-forest-900/[0.07] pt-2.5 dark:border-white/[0.06]">
          {typeof grounded === 'boolean' && (
            <Badge tone={grounded ? 'green' : 'slate'}>
              <span className={cn('h-1.5 w-1.5 rounded-full', grounded ? 'bg-forest-500' : 'bg-stone-400')} />
              {grounded ? 'Grounded in your materials' : 'Not in your materials'}
            </Badge>
          )}
          {deduped.map((c, i) => (
            <span
              key={`${c.material}-${c.page ?? 'np'}-${i}`}
              className="inline-flex items-center gap-1 rounded-md bg-white px-2 py-1 text-xs font-medium text-stone-600 ring-1 ring-inset ring-stone-900/[0.08] dark:bg-white/[0.05] dark:text-stone-300 dark:ring-white/10"
            >
              <FileText size={11} className="text-forest-600 dark:text-forest-400" />
              {c.material}{c.page ? <span className="text-stone-400">· p.{c.page}</span> : null}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

export function UserMessage({ content }: { content: string }) {
  return (
    <div className="flex justify-end">
      <div className="max-w-[80%] rounded-2xl rounded-br-md bg-forest-700 px-4 py-3 text-[15px] leading-relaxed text-white shadow-[0_6px_16px_-8px_rgba(47,79,48,0.7)] dark:bg-forest-600">
        <p className="whitespace-pre-wrap">{content}</p>
      </div>
    </div>
  );
}

/* ---------- sources ---------- */

export interface GroupedSource {
  material: string;
  pages: (number | null)[];
}

export function groupSources(citations: { material?: string; page?: number | null }[]): GroupedSource[] {
  const map = new Map<string, Set<number | null>>();
  for (const c of citations) {
    if (!c?.material) continue;
    if (!map.has(c.material)) map.set(c.material, new Set());
    map.get(c.material)!.add(c.page ?? null);
  }
  return [...map.entries()].map(([material, pages]) => ({
    material,
    pages: [...pages].sort((a, b) => (a ?? 9999) - (b ?? 9999)),
  }));
}

export function SourceCard({ source }: { source: GroupedSource }) {
  const numbered = source.pages.filter((p): p is number => p !== null);
  const pageLabel = numbered.length === 0
    ? 'Referenced'
    : numbered.length === 1
      ? `Page ${numbered[0]}`
      : `Pages ${numbered.join(', ')}`;
  return (
    <div className="group rounded-xl border border-forest-900/[0.07] bg-gradient-to-b from-moss-50/80 to-cream-card px-3.5 py-3 shadow-[0_1px_2px_rgba(62,53,40,0.05)] transition-all hover:border-forest-500/30 hover:shadow-card dark:border-white/[0.06] dark:from-white/[0.03] dark:to-transparent">
      <p className="flex items-start gap-2 text-[13px] font-semibold leading-snug text-ink dark:text-stone-100">
        <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-forest-100 text-forest-700 dark:bg-forest-900/60 dark:text-forest-300">
          <FileText size={13} />
        </span>
        <span className="min-w-0 break-words">{source.material}</span>
      </p>
      <p className="mt-1.5 pl-8 text-xs font-medium text-forest-700 dark:text-forest-300">{pageLabel}</p>
    </div>
  );
}

/* ---------- composer ---------- */

export function ChatComposer({
  value, onChange, onSend, busy, placeholder,
}: {
  value: string; onChange: (v: string) => void; onSend: () => void; busy: boolean; placeholder?: string;
}) {
  const ref = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`;
  }, [value]);

  return (
    <div className="rounded-2xl border border-stone-900/[0.09] bg-white p-2.5 shadow-composer transition-colors focus-within:border-forest-500/60 focus-within:ring-2 focus-within:ring-forest-500/15 dark:border-white/10 dark:bg-stone-900 dark:focus-within:border-forest-500/50">
      <textarea
        ref={ref}
        rows={1}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            onSend();
          }
        }}
        placeholder={placeholder ?? 'Ask the tutor about your material…'}
        disabled={busy}
        className="max-h-40 w-full resize-none bg-transparent px-3 pb-1 pt-2 text-[15px] leading-relaxed text-ink outline-none placeholder:text-stone-400 disabled:opacity-60 dark:text-stone-100 dark:placeholder:text-stone-500"
      />
      <div className="flex items-center justify-between px-1.5 pb-0.5 pt-1">
        <p className="hidden text-xs text-stone-400 sm:block dark:text-stone-500">
          {busy ? 'Tutor is thinking…' : 'Enter to send · Shift+Enter for a new line'}
        </p>
        <button
          type="button"
          onClick={onSend}
          disabled={busy || !value.trim()}
          aria-label="Send message"
          className="ml-auto flex h-9 w-9 items-center justify-center rounded-xl bg-forest-700 text-white shadow-[0_6px_16px_-6px_rgba(47,79,48,0.8)] transition-all hover:bg-forest-800 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 dark:bg-forest-600 dark:hover:bg-forest-500"
        >
          {busy
            ? <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />
            : <ArrowUp size={17} strokeWidth={2.5} />}
        </button>
      </div>
    </div>
  );
}
