import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react';
import { cn } from '../lib/utils';

/**
 * Calm Learning AI design primitives.
 * Warm ivory surfaces, forest-green identity, layered elevation.
 * Same component API as before — pages keep their props.
 */
export function Button({ className, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'outline' | 'ghost' | 'danger' | 'soft' }) {
  const { variant = 'primary', ...rest } = props as { variant?: string } & ButtonHTMLAttributes<HTMLButtonElement>;
  return (
    <button
      {...rest}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-forest-500 focus-visible:ring-offset-2 focus-visible:ring-offset-cream disabled:cursor-not-allowed disabled:opacity-50 dark:focus-visible:ring-offset-bark',
        variant === 'primary' && 'bg-forest-700 text-white shadow-[0_6px_18px_-8px_rgba(47,79,48,0.7)] hover:bg-forest-800 active:bg-forest-900 dark:bg-forest-600 dark:hover:bg-forest-500',
        variant === 'soft' && 'bg-forest-100 text-forest-800 hover:bg-forest-200 dark:bg-forest-900/50 dark:text-forest-200 dark:hover:bg-forest-900/70',
        variant === 'outline' && 'border border-stone-300/90 bg-cream-card text-stone-700 shadow-[0_1px_2px_rgba(62,53,40,0.06)] hover:border-forest-400 hover:bg-moss-50 hover:text-forest-800 dark:border-white/10 dark:bg-white/[0.04] dark:text-stone-200 dark:hover:border-forest-600 dark:hover:bg-forest-900/40 dark:hover:text-forest-200',
        variant === 'ghost' && 'text-stone-600 hover:bg-stone-900/[0.05] hover:text-stone-900 dark:text-stone-300 dark:hover:bg-white/[0.06] dark:hover:text-stone-100',
        variant === 'danger' && 'bg-red-700/90 text-white hover:bg-red-800 dark:bg-red-900 dark:hover:bg-red-800',
        className,
      )}
    />
  );
}

export function Card({ className, children, elevated }: { className?: string; children: ReactNode; elevated?: boolean }) {
  return (
    <div className={cn(
      'rounded-2xl border border-stone-900/[0.07] bg-cream-card dark:border-white/[0.07] dark:bg-bark-card',
      elevated ? 'shadow-lift' : 'shadow-card',
      className,
    )}>
      {children}
    </div>
  );
}

const fieldClasses =
  'w-full rounded-xl border border-stone-900/[0.12] bg-white px-3.5 py-2.5 text-sm text-ink shadow-[inset_0_1px_2px_rgba(62,53,40,0.05)] placeholder:text-stone-400 transition-colors focus:border-forest-500 focus:outline-none focus:ring-2 focus:ring-forest-500/20 dark:border-white/10 dark:bg-stone-900 dark:text-stone-100 dark:placeholder:text-stone-500';

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn(fieldClasses, className)} />;
}

export function Textarea({ className, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={cn(fieldClasses, 'leading-relaxed', className)} />;
}

export function Badge({ children, tone = 'slate' }: { children: ReactNode; tone?: 'slate' | 'green' | 'amber' | 'red' | 'blue' | 'teal' | 'violet' }) {
  const tones: Record<string, string> = {
    slate: 'bg-stone-900/[0.06] text-stone-600 ring-stone-900/10 dark:bg-white/[0.06] dark:text-stone-300 dark:ring-white/10',
    green: 'bg-forest-100 text-forest-800 ring-forest-600/25 dark:bg-forest-900/50 dark:text-forest-200 dark:ring-forest-700/60',
    amber: 'bg-amber-100/90 text-amber-800 ring-amber-600/25 dark:bg-amber-950/60 dark:text-amber-200 dark:ring-amber-800/50',
    red: 'bg-red-100/80 text-red-800 ring-red-600/20 dark:bg-red-950/50 dark:text-red-200 dark:ring-red-900/60',
    blue: 'bg-sky-100/90 text-sky-800 ring-sky-600/20 dark:bg-sky-950/50 dark:text-sky-200 dark:ring-sky-900/60',
    teal: 'bg-teal-50 text-teal-800 ring-teal-600/20 dark:bg-teal-950/60 dark:text-teal-200 dark:ring-teal-800/50',
    violet: 'bg-violet-100/80 text-violet-800 ring-violet-600/20 dark:bg-violet-950/50 dark:text-violet-200 dark:ring-violet-900/60',
  };
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset', tones[tone])}>
      {children}
    </span>
  );
}

/** Meaningful document/material processing status colors. */
export function StatusBadge({ status }: { status: string }) {
  const s = status.toUpperCase();
  const tone = s === 'READY' ? 'green' : s === 'FAILED' ? 'red' : s === 'PROCESSING' ? 'blue' : 'amber';
  const dot = s === 'READY' ? 'bg-forest-500' : s === 'FAILED' ? 'bg-red-500' : s === 'PROCESSING' ? 'bg-sky-500' : 'bg-amber-500';
  return (
    <Badge tone={tone}>
      <span className={cn('h-1.5 w-1.5 rounded-full', dot, s === 'PROCESSING' && 'animate-pulse')} />
      {s}
    </Badge>
  );
}

export function Progress({ value, className, barClassName }: { value: number; className?: string; barClassName?: string }) {
  return (
    <div className={cn('h-2 w-full overflow-hidden rounded-full bg-stone-900/[0.08] dark:bg-white/[0.08]', className)}>
      <div
        className={cn('h-full rounded-full bg-gradient-to-r from-forest-600 to-forest-500 transition-all duration-500 dark:from-forest-500 dark:to-forest-400', barClassName)}
        style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
      />
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse rounded-xl bg-stone-900/[0.07] dark:bg-white/[0.06]', className)} />;
}

export function Empty({ title, hint, action, icon }: { title: string; hint?: string; action?: ReactNode; icon?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-2xl border border-dashed border-stone-900/[0.14] bg-cream-card/70 px-6 py-14 text-center shadow-[inset_0_1px_0_rgba(255,255,255,0.6)] dark:border-white/10 dark:bg-white/[0.02]">
      {icon && (
        <span className="mb-1 flex h-12 w-12 items-center justify-center rounded-2xl bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">
          {icon}
        </span>
      )}
      <p className="text-[15px] font-semibold text-ink dark:text-stone-100">{title}</p>
      {hint && <p className="max-w-sm text-sm leading-relaxed text-stone-500 dark:text-stone-400">{hint}</p>}
      {action && <div className="mt-3">{action}</div>}
    </div>
  );
}

/** Compact KPI card for analytics-style pages. */
export function StatCard({ icon, label, value, sub }: { icon: ReactNode; label: string; value: string; sub?: string }) {
  return (
    <Card className="p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-stone-500 dark:text-stone-400">{label}</p>
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">{icon}</span>
      </div>
      <p className="mt-1.5 truncate text-[1.45rem] font-semibold tracking-tight text-ink dark:text-stone-50">{value}</p>
      {sub && <p className="mt-0.5 truncate text-xs text-stone-500 dark:text-stone-400">{sub}</p>}
    </Card>
  );
}

export function SectionHeader({ title, hint, action }: { title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
      <div>
        <h3 className="text-section text-ink dark:text-stone-50">{title}</h3>
        {hint && <p className="mt-0.5 text-[13px] text-stone-500 dark:text-stone-400">{hint}</p>}
      </div>
      {action}
    </div>
  );
}

export function PageHeader({ title, subtitle, action, eyebrow }: { title: string; subtitle?: string; action?: ReactNode; eyebrow?: string }) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        {eyebrow && (
          <p className="mb-1.5 inline-flex items-center gap-1.5 rounded-full bg-forest-100 px-2.5 py-1 text-[11px] font-bold uppercase tracking-[0.12em] text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">{eyebrow}</p>
        )}
        <h1 className="text-page text-ink dark:text-stone-50">{title}</h1>
        {subtitle && <p className="mt-1.5 max-w-2xl text-[15px] leading-relaxed text-stone-500 dark:text-stone-400">{subtitle}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
