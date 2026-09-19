import { useEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { BookOpenCheck, ChevronDown, History, Plus, Sparkles } from 'lucide-react';
import { tutorApi } from '../api/services';
import { apiErrorMessage } from '../api/client';
import { Card } from '../components/ui';
import { AIMessage, ChatComposer, SourceCard, UserMessage, groupSources } from '../components/ai';
import { cn } from '../lib/utils';
import type { ChatMessage, Conversation } from '../api/types';

const SUGGESTIONS = [
  'Summarize the key topics in my materials',
  'Explain the main concept with an example',
  'What should I focus on first?',
  'Quiz me on one important idea',
];

export function TutorPage() {
  const { projectId = '' } = useParams();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sourcesOpen, setSourcesOpen] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    tutorApi.conversations(projectId).then((c) => {
      setConversations(c);
      if (c[0]) {
        setActiveId(c[0].id);
        tutorApi.messages(c[0].id).then(setMessages).catch(() => null);
      }
    }).catch(() => null);
  }, [projectId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  function select(id: string) {
    setActiveId(id);
    tutorApi.messages(id).then(setMessages).catch(() => setMessages([]));
  }

  function newChat() {
    setActiveId(null);
    setMessages([]);
    setError(null);
  }

  async function send() {
    const text = input.trim();
    if (!text || busy) return;
    setInput('');
    setError(null);
    const optimistic: ChatMessage = { id: `tmp-${Date.now()}`, role: 'USER', content: text };
    setMessages((prev) => [...prev, optimistic]);
    setBusy(true);
    try {
      const res = await tutorApi.chat(projectId, text, activeId);
      if (!activeId || res.conversationId !== activeId) {
        setActiveId(res.conversationId);
        tutorApi.conversations(projectId).then(setConversations).catch(() => null);
      }
      setMessages((prev) => [
        ...prev,
        { id: `a-${Date.now()}`, role: 'ASSISTANT', content: res.answer, grounded: res.grounded, citations: res.citations },
      ]);
    } catch (err) {
      setError(apiErrorMessage(err, 'Tutor request failed.'));
    } finally {
      setBusy(false);
    }
  }

  const allCitations = useMemo(
    () => messages.flatMap((m) => (m.role === 'ASSISTANT' && m.citations ? m.citations : [])),
    [messages],
  );
  const grouped = useMemo(() => groupSources(allCitations), [allCitations]);

  return (
    <div>
      {/* Tutor header */}
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="mb-1.5 inline-flex items-center gap-1.5 rounded-full bg-forest-100 px-2.5 py-1 text-[11px] font-bold uppercase tracking-[0.12em] text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">
            <Sparkles size={11} /> Grounded AI
          </p>
          <h1 className="text-page text-ink dark:text-stone-50">AI Tutor</h1>
          <p className="mt-1.5 max-w-2xl text-[15px] leading-relaxed text-stone-500 dark:text-stone-400">
            Learn from your project materials with grounded AI.
          </p>
          <p className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-forest-700 ring-1 ring-inset ring-forest-600/20 dark:bg-white/[0.04] dark:text-forest-300 dark:ring-forest-700/50">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-forest-500" />
            Project context active
          </p>
        </div>
      </div>

      {/* History strip */}
      <div className="mb-4 flex items-center gap-2 overflow-x-auto pb-1">
        <span className="flex shrink-0 items-center gap-1.5 text-xs font-bold uppercase tracking-[0.1em] text-stone-500 dark:text-stone-400">
          <History size={13} /> Chats
        </span>
        <button
          onClick={newChat}
          className="flex shrink-0 items-center gap-1.5 rounded-xl border border-dashed border-forest-600/40 bg-forest-50 px-3 py-1.5 text-[13px] font-semibold text-forest-700 transition-colors hover:border-forest-600 hover:bg-forest-100 dark:border-forest-700/60 dark:bg-forest-950/40 dark:text-forest-300 dark:hover:bg-forest-900/60"
        >
          <Plus size={14} /> New chat
        </button>
        {conversations.map((c) => (
          <button
            key={c.id}
            onClick={() => select(c.id)}
            className={cn(
              'shrink-0 rounded-xl px-3 py-1.5 text-[13px] font-medium transition-all',
              c.id === activeId
                ? 'bg-forest-700 text-white shadow-[0_4px_12px_-4px_rgba(47,79,48,0.7)] dark:bg-forest-600'
                : 'border border-stone-900/[0.08] bg-white text-stone-600 hover:border-forest-400 hover:text-forest-800 dark:border-white/10 dark:bg-white/[0.04] dark:text-stone-300',
            )}
          >
            {c.title}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-3">
        {/* Conversation — ~70% */}
        <Card elevated className="flex h-[68vh] flex-col overflow-hidden lg:col-span-2">
          <div className="flex-1 space-y-5 overflow-y-auto bg-gradient-to-b from-cream-card to-cream-soft/60 p-5 sm:p-7 dark:from-transparent dark:to-transparent">
            {messages.length === 0 && (
              <div className="flex h-full flex-col items-center justify-center py-8 text-center">
                <span className="flex h-14 w-14 items-center justify-center rounded-3xl bg-gradient-to-br from-forest-500 to-forest-800 text-white shadow-[0_10px_24px_-8px_rgba(47,79,48,0.7)]">
                  <Sparkles size={24} />
                </span>
                <p className="mt-5 text-xl font-semibold tracking-tight text-ink dark:text-stone-50">AI Tutor</p>
                <p className="mt-1.5 max-w-sm text-[15px] leading-relaxed text-stone-500 dark:text-stone-400">
                  Ask anything about your project materials. Answers stay grounded in what you uploaded.
                </p>
                <p className="mb-3 mt-6 text-xs font-bold uppercase tracking-[0.12em] text-stone-400 dark:text-stone-500">Try asking</p>
                <div className="grid w-full max-w-lg grid-cols-1 gap-2 sm:grid-cols-2">
                  {SUGGESTIONS.map((s) => (
                    <button
                      key={s}
                      onClick={() => setInput(s)}
                      className="rounded-2xl border border-stone-900/[0.08] bg-white px-4 py-3 text-left text-[13px] font-medium leading-snug text-stone-700 shadow-[0_1px_2px_rgba(62,53,40,0.05)] transition-all hover:-translate-y-px hover:border-forest-500/40 hover:text-forest-800 hover:shadow-card dark:border-white/10 dark:bg-white/[0.04] dark:text-stone-200 dark:hover:text-forest-200"
                    >
                      “{s}”
                    </button>
                  ))}
                </div>
              </div>
            )}
            {messages.map((m) => (
              m.role === 'USER'
                ? <UserMessage key={m.id} content={m.content} />
                : <AIMessage key={m.id} content={m.content} grounded={m.grounded} citations={m.citations} />
            ))}
            {busy && (
              <div className="flex items-center gap-2.5 text-sm text-stone-500 dark:text-stone-400">
                <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-forest-100 dark:bg-forest-900/50">
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-forest-300 border-t-forest-700 dark:border-forest-700 dark:border-t-forest-300" />
                </span>
                Tutor is thinking…
              </div>
            )}
            <div ref={bottomRef} />
          </div>
          {error && (
            <p className="border-t border-red-600/20 bg-red-50 px-5 py-2.5 text-sm font-medium text-red-700 dark:bg-red-950/40 dark:text-red-300">
              {error}
            </p>
          )}
          <div className="border-t border-stone-900/[0.07] bg-cream-soft/70 p-3.5 dark:border-white/[0.07] dark:bg-white/[0.02]">
            <ChatComposer value={input} onChange={setInput} onSend={send} busy={busy} />
          </div>
        </Card>

        {/* Sources — ~30%, secondary */}
        <Card className="h-fit lg:sticky lg:top-20">
          <button
            className="flex w-full items-center justify-between p-5 pb-3 text-left lg:cursor-default"
            onClick={() => setSourcesOpen((o) => !o)}
          >
            <span>
              <span className="flex items-center gap-2 text-section text-ink dark:text-stone-50">
                <BookOpenCheck size={17} className="text-forest-600 dark:text-forest-400" /> Sources
              </span>
              <span className="mt-0.5 block text-[13px] text-stone-500 dark:text-stone-400">Referenced in this conversation</span>
            </span>
            <ChevronDown size={17} className={cn('shrink-0 text-stone-400 transition-transform lg:hidden', sourcesOpen && 'rotate-180')} />
          </button>
          <div className={cn('px-5 pb-5', !sourcesOpen && 'hidden lg:block')}>
            {grouped.length === 0 ? (
              <p className="rounded-xl bg-stone-900/[0.04] px-3.5 py-3.5 text-[13px] leading-relaxed text-stone-500 dark:bg-white/[0.04] dark:text-stone-400">
                No sources yet. Cited pages will appear here as you chat.
              </p>
            ) : (
              <div className="space-y-2">
                {grouped.map((g) => <SourceCard key={g.material} source={g} />)}
              </div>
            )}
            <div className="mt-4 rounded-2xl border border-forest-900/[0.07] bg-gradient-to-b from-moss-50 to-transparent p-4 text-[13px] leading-relaxed text-stone-600 dark:border-white/[0.06] dark:from-forest-950/40 dark:text-stone-300">
              The tutor answers only from your uploaded PDFs. If the material doesn&apos;t cover a question, it will say so.
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
