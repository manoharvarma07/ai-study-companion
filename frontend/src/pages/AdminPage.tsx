import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity, AlertCircle, BarChart3, Bot, BrainCircuit, CheckCircle2, ChevronLeft,
  ChevronRight, Clock, Cpu, Database, Layers, Search, ShieldCheck, Users, Wallet, X, Zap,
} from 'lucide-react';
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from 'recharts';
import { adminApi, type AiUsageQuery } from '../api/services';
import type {
  AdminOverview, AdminUser, AdminUserDetail, AiUsageByFeature, AiUsageByModel,
  AiUsageByUser, AiUsageEvent, AiUsageFilterOptions, AiUsageSummary, AiUsageTimelinePoint,
} from '../api/types';
import { apiErrorMessage } from '../api/client';
import { Badge, Button, Card, Empty, Input, PageHeader, Skeleton } from '../components/ui';
import { cn } from '../lib/utils';

// ---------- formatting helpers (primitives only, never objects) ----------
function formatInt(n: number | null | undefined): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '0';
  return Math.round(n).toLocaleString('en-US');
}

function formatTokens(n: number | null | undefined): string {
  if (n === null || n === undefined) return '0';
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return `${Math.round(n)}`;
}

function formatCost(n: number | null | undefined): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '$0.00';
  if (n === 0) return '$0.00';
  if (n < 0.01) return `$${n.toFixed(4)}`;
  return `$${n.toFixed(2)}`;
}

function formatLatency(ms: number | null | undefined): string {
  if (ms === null || ms === undefined || Number.isNaN(ms)) return 'N/A';
  if (ms < 1000) return `${Math.round(ms)}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function timeAgo(iso: string | null | undefined): string {
  if (!iso) return 'Never';
  const d = new Date(iso).getTime();
  if (Number.isNaN(d)) return 'Never';
  const diff = Date.now() - d;
  if (diff < 0) return 'Just now';
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return 'Yesterday';
  if (days < 30) return `${days}d ago`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months}mo ago`;
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function prettyFeature(f: string): string {
  return f
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(' ');
}

const FEATURE_COLORS = ['#6a9a7b', '#a8b89a', '#d9c9a3', '#8fb3a8', '#c4a484', '#93a5b8', '#b8a9c4'];
const PROVIDER_COLORS: Record<string, string> = {
  openrouter: '#6a9a7b',
  offline: '#a8b89a',
  unknown: '#93a5b8',
};

type Tab = 'users' | 'ai';

// ---------- small primitives ----------
function KpiCard({ icon, label, value, sub }: { icon: React.ReactNode; label: string; value: string; sub?: string }) {
  return (
    <Card className="p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-stone-500 dark:text-stone-400">{label}</p>
        <span className="text-forest-600 dark:text-forest-400">{icon}</span>
      </div>
      <p className="mt-1.5 truncate text-[1.45rem] font-semibold tracking-tight text-stone-900 dark:text-stone-50">{value}</p>
      {sub && <p className="mt-0.5 truncate text-xs text-stone-500 dark:text-stone-400">{sub}</p>}
    </Card>
  );
}

function SectionTitle({ icon, title, hint }: { icon: React.ReactNode; title: string; hint?: string }) {
  return (
    <div className="mb-3 flex items-baseline justify-between gap-3">
      <h3 className="flex items-center gap-2 text-[14px] font-semibold text-stone-900 dark:text-stone-100">
        <span className="text-forest-600 dark:text-forest-400">{icon}</span>{title}
      </h3>
      {hint && <p className="text-xs text-stone-500 dark:text-stone-400">{hint}</p>}
    </div>
  );
}

function ChartTooltip({ active, payload, label, money }: { active?: boolean; payload?: { name: string; value: number; color?: string }[]; label?: string; money?: boolean }) {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div className="rounded-lg border border-stone-200 bg-white px-3 py-2 text-xs shadow-lg dark:border-white/10 dark:bg-stone-900">
      {label && <p className="mb-1 font-semibold text-stone-800 dark:text-stone-100">{label}</p>}
      {payload.map((p) => (
        <p key={p.name} className="flex items-center gap-2 text-stone-600 dark:text-stone-300">
          <span className="inline-block h-2 w-2 rounded-full" style={{ background: p.color ?? '#6a9a7b' }} />
          {p.name}: <span className="font-semibold text-stone-900 dark:text-stone-50">{money ? formatCost(p.value) : formatInt(p.value)}</span>
        </p>
      ))}
    </div>
  );
}

// ================================================================ MAIN
export function AdminPage() {
  const [tab, setTab] = useState<Tab>('users');
  return (
    <div>
      <PageHeader
        eyebrow="Administration"
        title="Administration"
        subtitle="Platform-wide metrics and system activity."
      />
      <div className="mb-5 inline-flex rounded-xl border border-stone-200 bg-white p-1 dark:border-white/[0.07] dark:bg-bark-soft">
        {(['users', 'ai'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={cn(
              'flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors',
              tab === t
                ? 'bg-forest-700 text-white shadow-soft dark:bg-forest-600'
                : 'text-stone-600 hover:bg-stone-100 hover:text-stone-900 dark:text-stone-400 dark:hover:bg-white/5 dark:hover:text-stone-100',
            )}
          >
            {t === 'users' ? <Users size={15} /> : <Bot size={15} />}
            {t === 'users' ? 'Users' : 'AI Usage'}
          </button>
        ))}
      </div>
      {tab === 'users' ? <UsersTab /> : <AiUsageTab />}
    </div>
  );
}

// ================================================================ USERS TAB
function UsersTab() {
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [role, setRole] = useState('ALL');
  const [sort, setSort] = useState('createdAt');
  const [direction, setDirection] = useState('desc');
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const pageSize = 10;

  useEffect(() => {
    const t = setTimeout(() => { setDebouncedSearch(search.trim()); setPage(0); }, 350);
    return () => clearTimeout(t);
  }, [search]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [ov, res] = await Promise.all([
        adminApi.overview(),
        adminApi.users({ page, size: pageSize, search: debouncedSearch || undefined, role, sort, direction }),
      ]);
      setOverview(ov);
      setUsers(res.content);
      setTotalElements(res.totalElements);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load admin data'));
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch, role, sort, direction]);

  useEffect(() => { void load(); }, [load]);

  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));

  return (
    <div>
      {error && (
        <Card className="mb-4 flex items-center gap-2 border-red-300/60 p-4 text-sm text-red-700 dark:border-red-900/60 dark:text-red-300">
          <AlertCircle size={16} /> {error}
        </Card>
      )}
      {!overview || loading ? (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-24" />)}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <KpiCard icon={<Users size={16} />} label="Total Users" value={formatInt(overview.totalUsers)} />
          <KpiCard icon={<Activity size={16} />} label="Active Users" value={formatInt(overview.activeUsers)} sub="Active in last 30 days" />
          <KpiCard icon={<Layers size={16} />} label="Total Spaces" value={formatInt(overview.totalSpaces)} />
          <KpiCard icon={<Database size={16} />} label="Total Projects" value={formatInt(overview.totalProjects)} />
        </div>
      )}

      <Card className="mt-4 p-4 sm:p-5">
        <div className="flex flex-wrap items-center gap-2">
          <div className="relative min-w-[200px] flex-1">
            <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-stone-400" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search users by name or email…"
              className="pl-9"
            />
          </div>
          <select
            value={role}
            onChange={(e) => { setRole(e.target.value); setPage(0); }}
            className="rounded-lg border border-stone-300 bg-white px-3 py-2.5 text-sm text-stone-700 dark:border-stone-700 dark:bg-stone-900 dark:text-stone-200"
            aria-label="Filter by role"
          >
            <option value="ALL">All roles</option>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <select
            value={`${sort}:${direction}`}
            onChange={(e) => { const [s, d] = e.target.value.split(':'); setSort(s); setDirection(d); setPage(0); }}
            className="rounded-lg border border-stone-300 bg-white px-3 py-2.5 text-sm text-stone-700 dark:border-stone-700 dark:bg-stone-900 dark:text-stone-200"
            aria-label="Sort users"
          >
            <option value="createdAt:desc">Newest first</option>
            <option value="createdAt:asc">Oldest first</option>
            <option value="name:asc">Name A–Z</option>
            <option value="name:desc">Name Z–A</option>
            <option value="projects:desc">Most projects</option>
            <option value="lastActivity:desc">Recently active</option>
          </select>
        </div>

        <div className="mt-3 overflow-x-auto rounded-lg border border-stone-200/70 dark:border-white/[0.06]">
          <table className="w-full min-w-[820px] border-collapse text-left text-sm">
            <thead>
              <tr className="bg-stone-100/70 text-xs uppercase tracking-wide text-stone-500 dark:bg-white/[0.03] dark:text-stone-400">
                {['User', 'Email', 'Role', 'Spaces', 'Projects', 'Materials', 'Last Activity', 'Status'].map((h) => (
                  <th key={h} className="px-4 py-3 font-semibold">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100 dark:divide-white/[0.05]">
              {loading ? (
                [0, 1, 2].map((i) => (
                  <tr key={i}><td colSpan={8} className="px-4 py-3"><Skeleton className="h-8" /></td></tr>
                ))
              ) : users.length === 0 ? (
                <tr><td colSpan={8} className="px-4 py-8 text-center text-stone-500 dark:text-stone-400">No users found.</td></tr>
              ) : users.map((u) => (
                <tr
                  key={u.id}
                  onClick={() => setSelectedId(u.id)}
                  className="cursor-pointer transition-colors hover:bg-forest-50/60 dark:hover:bg-white/[0.03]"
                >
                  <td className="max-w-[180px] truncate px-4 py-3 font-medium text-stone-900 dark:text-stone-100">{u.name}</td>
                  <td className="max-w-[220px] truncate px-4 py-3 text-stone-500 dark:text-stone-400">{u.email}</td>
                  <td className="px-4 py-3"><Badge tone={u.role === 'ADMIN' ? 'blue' : 'slate'}>{u.role}</Badge></td>
                  <td className="px-4 py-3 text-stone-700 dark:text-stone-300">{formatInt(u.spaces)}</td>
                  <td className="px-4 py-3 text-stone-700 dark:text-stone-300">{formatInt(u.projects)}</td>
                  <td className="px-4 py-3 text-stone-700 dark:text-stone-300">{formatInt(u.materials)}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-stone-500 dark:text-stone-400">{timeAgo(u.lastActivity)}</td>
                  <td className="px-4 py-3">
                    <Badge tone={u.status === 'Active' ? 'green' : 'slate'}>{u.status}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-3 flex items-center justify-between text-sm text-stone-500 dark:text-stone-400">
          <p>{formatInt(totalElements)} user{totalElements === 1 ? '' : 's'}</p>
          <div className="flex items-center gap-1">
            <Button variant="ghost" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} aria-label="Previous page">
              <ChevronLeft size={16} />
            </Button>
            <span className="px-2 text-xs font-medium">Page {page + 1} of {totalPages}</span>
            <Button variant="ghost" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)} aria-label="Next page">
              <ChevronRight size={16} />
            </Button>
          </div>
        </div>
      </Card>

      {selectedId && <UserDetailModal userId={selectedId} onClose={() => setSelectedId(null)} />}
    </div>
  );
}

function UserDetailModal({ userId, onClose }: { userId: string; onClose: () => void }) {
  const [detail, setDetail] = useState<AdminUserDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    adminApi.userDetail(userId)
      .then((d) => { if (!cancelled) setDetail(d); })
      .catch((err: unknown) => { if (!cancelled) setError(apiErrorMessage(err, 'Failed to load user detail')); });
    return () => { cancelled = true; };
  }, [userId]);

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 p-0 sm:items-center sm:p-6" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-t-2xl border border-stone-200 bg-white p-5 dark:border-white/10 dark:bg-bark-soft sm:rounded-2xl sm:p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-forest-600 dark:text-forest-400">User detail</p>
            <h3 className="mt-0.5 text-lg font-semibold text-stone-900 dark:text-stone-50">
              {detail ? detail.name : 'Loading…'}
            </h3>
            {detail && <p className="text-sm text-stone-500 dark:text-stone-400">{detail.email} · <span className="font-medium">{detail.role}</span></p>}
          </div>
          <button onClick={onClose} className="rounded-lg p-1.5 text-stone-500 hover:bg-stone-100 dark:text-stone-400 dark:hover:bg-white/5" aria-label="Close">
            <X size={18} />
          </button>
        </div>

        {error && <p className="mt-3 flex items-center gap-2 text-sm text-red-600 dark:text-red-400"><AlertCircle size={15} /> {error}</p>}
        {!detail && !error && <Skeleton className="mt-4 h-48" />}
        {detail && (
          <div className="mt-4 space-y-4">
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
              {[
                { label: 'Spaces', value: formatInt(detail.spaces) },
                { label: 'Projects', value: formatInt(detail.projects) },
                { label: 'Materials', value: formatInt(detail.materials) },
                { label: 'Quizzes', value: formatInt(detail.quizCount) },
              ].map((s) => (
                <div key={s.label} className="rounded-lg bg-stone-100/70 px-3 py-2.5 dark:bg-white/[0.04]">
                  <p className="text-lg font-semibold text-stone-900 dark:text-stone-50">{s.value}</p>
                  <p className="text-xs text-stone-500 dark:text-stone-400">{s.label}</p>
                </div>
              ))}
            </div>
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div className="rounded-lg bg-stone-100/70 px-3 py-2.5 dark:bg-white/[0.04]">
                <p className="font-semibold text-stone-900 dark:text-stone-50">{formatInt(detail.conversationCount)}</p>
                <p className="text-xs text-stone-500 dark:text-stone-400">Tutor conversations</p>
              </div>
              <div className="rounded-lg bg-stone-100/70 px-3 py-2.5 dark:bg-white/[0.04]">
                <p className="font-semibold text-stone-900 dark:text-stone-50">{formatInt(detail.tutorMessageCount)}</p>
                <p className="text-xs text-stone-500 dark:text-stone-400">Tutor messages</p>
              </div>
            </div>
            <div>
              <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-stone-500 dark:text-stone-400">Spaces</p>
              {detail.spaceList.length === 0
                ? <p className="text-sm text-stone-500 dark:text-stone-400">No spaces yet.</p>
                : (
                  <ul className="space-y-1">
                    {detail.spaceList.slice(0, 8).map((s) => (
                      <li key={s.id} className="flex items-center justify-between rounded-lg bg-stone-100/70 px-3 py-2 text-sm dark:bg-white/[0.04]">
                        <span className="truncate font-medium text-stone-800 dark:text-stone-100">{s.name}</span>
                        <span className="text-xs text-stone-500 dark:text-stone-400">{timeAgo(s.createdAt)}</span>
                      </li>
                    ))}
                  </ul>
                )}
            </div>
            <div>
              <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-stone-500 dark:text-stone-400">Projects</p>
              {detail.projectList.length === 0
                ? <p className="text-sm text-stone-500 dark:text-stone-400">No projects yet.</p>
                : (
                  <ul className="space-y-1">
                    {detail.projectList.slice(0, 8).map((p) => (
                      <li key={p.id} className="flex items-center justify-between rounded-lg bg-stone-100/70 px-3 py-2 text-sm dark:bg-white/[0.04]">
                        <span className="truncate font-medium text-stone-800 dark:text-stone-100">{p.name}</span>
                        <span className="text-xs text-stone-500 dark:text-stone-400">{timeAgo(p.createdAt)}</span>
                      </li>
                    ))}
                  </ul>
                )}
            </div>
            <div>
              <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-stone-500 dark:text-stone-400">Recent activity</p>
              {detail.recentActivity.length === 0 && detail.recentAiUsage.length === 0
                ? <p className="text-sm text-stone-500 dark:text-stone-400">No recent activity.</p>
                : (
                  <ul className="space-y-1">
                    {detail.recentActivity.slice(0, 6).map((a, i) => (
                      <li key={`e-${i}`} className="flex items-center justify-between rounded-lg bg-stone-100/70 px-3 py-2 text-sm dark:bg-white/[0.04]">
                        <span className="font-medium text-stone-800 dark:text-stone-100">{a.type}</span>
                        <span className="text-xs text-stone-500 dark:text-stone-400">{timeAgo(a.at)}</span>
                      </li>
                    ))}
                    {detail.recentAiUsage.slice(0, 4).map((a) => (
                      <li key={a.id} className="flex items-center justify-between rounded-lg bg-stone-100/70 px-3 py-2 text-sm dark:bg-white/[0.04]">
                        <span className="font-medium text-stone-800 dark:text-stone-100">
                          {prettyFeature(a.feature)} · {a.model}
                        </span>
                        <Badge tone={a.success ? 'green' : 'red'}>{a.status}</Badge>
                      </li>
                    ))}
                  </ul>
                )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ================================================================ AI USAGE TAB
const DAY_OPTIONS = [
  { value: '7', label: '7 days' },
  { value: '30', label: '30 days' },
  { value: '90', label: '90 days' },
  { value: 'ALL', label: 'All time' },
];

function AiUsageTab() {
  const [filters, setFilters] = useState<AiUsageQuery>({ days: '30', feature: 'ALL', provider: 'ALL', model: 'ALL', userId: 'ALL', status: 'ALL' });
  const [options, setOptions] = useState<AiUsageFilterOptions>({ features: [], models: [], providers: [] });
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [summary, setSummary] = useState<AiUsageSummary | null>(null);
  const [timeline, setTimeline] = useState<AiUsageTimelinePoint[]>([]);
  const [byFeature, setByFeature] = useState<AiUsageByFeature[]>([]);
  const [byModel, setByModel] = useState<AiUsageByModel[]>([]);
  const [byUser, setByUser] = useState<AiUsageByUser[]>([]);
  const [recent, setRecent] = useState<AiUsageEvent[]>([]);
  const [recentTotal, setRecentTotal] = useState(0);
  const [recentPage, setRecentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const recentPageSize = 12;

  useEffect(() => {
    adminApi.aiFilterOptions().then(setOptions).catch(() => null);
    adminApi.users({ page: 0, size: 100, sort: 'name', direction: 'asc' })
      .then((r) => setUsers(r.content))
      .catch(() => null);
  }, []);

  const query: AiUsageQuery = useMemo(() => ({
    days: filters.days === 'ALL' ? undefined : filters.days,
    feature: filters.feature,
    provider: filters.provider,
    model: filters.model,
    userId: filters.userId,
    status: filters.status,
  }), [filters]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [s, t, f, m, u, r] = await Promise.all([
        adminApi.aiSummary(query),
        adminApi.aiTimeline({ ...query, days: filters.days ?? '30' }),
        adminApi.aiByFeature(query),
        adminApi.aiByModel(query),
        adminApi.aiByUser(query),
        adminApi.aiRecent({ ...query, page: recentPage, size: recentPageSize }),
      ]);
      setSummary(s);
      setTimeline(t);
      setByFeature(f);
      setByModel(m);
      setByUser(u);
      setRecent(r.content);
      setRecentTotal(r.totalElements);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load AI usage'));
    } finally {
      setLoading(false);
    }
  }, [query, filters.days, recentPage]);

  useEffect(() => { void load(); }, [load]);

  const set = (k: keyof AiUsageQuery, v: string) => {
    setFilters((f) => ({ ...f, [k]: v }));
    setRecentPage(0);
  };

  const hasRequests = (summary?.totalRequests ?? 0) > 0;
  const timelineHasData = timeline.some((p) => p.requests > 0);
  const maxFeature = Math.max(1, ...byFeature.map((f) => f.requests));

  const selectCls = 'rounded-lg border border-stone-300 bg-white px-2.5 py-2 text-[13px] text-stone-700 dark:border-stone-700 dark:bg-stone-900 dark:text-stone-200';

  return (
    <div>
      {error && (
        <Card className="mb-4 flex items-center gap-2 border-red-300/60 p-4 text-sm text-red-700 dark:border-red-900/60 dark:text-red-300">
          <AlertCircle size={16} /> {error}
        </Card>
      )}

      {/* Filters */}
      <Card className="mb-4 p-4">
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1 rounded-lg bg-stone-100/80 p-1 dark:bg-white/[0.05]">
            {DAY_OPTIONS.map((d) => (
              <button
                key={d.value}
                onClick={() => set('days', d.value)}
                className={cn(
                  'rounded-md px-3 py-1.5 text-[13px] font-medium transition-colors',
                  filters.days === d.value
                    ? 'bg-white text-stone-900 shadow-sm dark:bg-stone-800 dark:text-stone-50'
                    : 'text-stone-500 hover:text-stone-800 dark:text-stone-400 dark:hover:text-stone-100',
                )}
              >
                {d.label}
              </button>
            ))}
          </div>
          <select value={filters.feature} onChange={(e) => set('feature', e.target.value)} className={selectCls} aria-label="Filter by feature">
            <option value="ALL">All features</option>
            {options.features.map((f) => <option key={f} value={f}>{prettyFeature(f)}</option>)}
          </select>
          <select value={filters.provider} onChange={(e) => set('provider', e.target.value)} className={selectCls} aria-label="Filter by provider">
            <option value="ALL">All providers</option>
            {options.providers.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
          <select value={filters.model} onChange={(e) => set('model', e.target.value)} className={selectCls} aria-label="Filter by model">
            <option value="ALL">All models</option>
            {options.models.map((m) => <option key={m} value={m}>{m}</option>)}
          </select>
          <select value={filters.userId} onChange={(e) => set('userId', e.target.value)} className={selectCls} aria-label="Filter by user">
            <option value="ALL">All users</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.name} · {u.email}</option>)}
          </select>
          <select value={filters.status} onChange={(e) => set('status', e.target.value)} className={selectCls} aria-label="Filter by status">
            <option value="ALL">All statuses</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </Card>

      {/* KPI cards */}
      {loading || !summary ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
          {[0, 1, 2, 3, 4, 5].map((i) => <Skeleton key={i} className="h-24" />)}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
          <KpiCard icon={<Zap size={16} />} label="AI Requests" value={formatInt(summary.totalRequests)} />
          <KpiCard icon={<CheckCircle2 size={16} />} label="Successful" value={formatInt(summary.successfulRequests)} sub={`${summary.successRate}% success`} />
          <KpiCard icon={<AlertCircle size={16} />} label="Failed" value={formatInt(summary.failedRequests)} />
          <KpiCard icon={<BrainCircuit size={16} />} label="Tokens" value={summary.totalTokens > 0 ? formatTokens(summary.totalTokens) : '0'} sub={summary.totalTokens > 0 ? `${formatInt(summary.totalTokens)} total` : 'Not tracked'} />
          <KpiCard icon={<Wallet size={16} />} label="Est. Cost" value={formatCost(summary.estimatedCost)} />
          <KpiCard icon={<Clock size={16} />} label="Avg Latency" value={summary.totalRequests > 0 ? formatLatency(summary.avgLatencyMs) : 'N/A'} />
        </div>
      )}

      {/* Timeline */}
      <Card className="mt-4 p-4 sm:p-5">
        <SectionTitle
          icon={<BarChart3 size={16} />}
          title="AI Usage Over Time"
          hint={filters.days === 'ALL' ? 'All time, daily buckets' : `Last ${filters.days} days`}
        />
        {!loading && !timelineHasData ? (
          <Empty title="No data available yet" hint="AI requests will appear here once the tutor, quiz, or assessment features are used." />
        ) : loading ? (
          <Skeleton className="h-56" />
        ) : (
          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={timeline} margin={{ top: 4, right: 4, bottom: 0, left: -8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="currentColor" className="text-stone-200 dark:text-white/[0.06]" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 11 }}
                  tickFormatter={(v: string) => v.slice(5)}
                  tickLine={false}
                  axisLine={false}
                  minTickGap={24}
                  stroke="currentColor"
                  className="text-stone-400"
                />
                <YAxis tick={{ fontSize: 11 }} tickLine={false} axisLine={false} stroke="currentColor" className="text-stone-400" />
                <Tooltip content={<ChartTooltip />} />
                <Area type="monotone" dataKey="requests" name="Requests" stroke="#6a9a7b" fill="#6a9a7b" fillOpacity={0.25} strokeWidth={2} />
                <Area type="monotone" dataKey="tokens" name="Tokens" stroke="#c4a484" fill="#c4a484" fillOpacity={0.15} strokeWidth={1.5} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>

      {/* By feature + provider */}
      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Card className="p-4 sm:p-5">
          <SectionTitle icon={<Cpu size={16} />} title="Usage by Feature" />
          {loading ? <Skeleton className="h-40" /> : byFeature.length === 0 ? (
            <Empty title="No data available yet" hint="Feature breakdown will appear after the first AI request." />
          ) : (
            <div className="space-y-2.5">
              {byFeature.map((f, i) => (
                <div key={f.feature}>
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium text-stone-800 dark:text-stone-100">{prettyFeature(f.feature)}</span>
                    <span className="text-xs text-stone-500 dark:text-stone-400">
                      {formatInt(f.requests)} requests · {formatTokens(f.tokens)} tokens · {formatCost(f.cost)} · {formatLatency(f.avgLatencyMs)}
                    </span>
                  </div>
                  <div className="mt-1 h-2 overflow-hidden rounded-full bg-stone-200/80 dark:bg-white/[0.08]">
                    <div
                      className="h-full rounded-full transition-all"
                      style={{ width: `${Math.round((f.requests / maxFeature) * 100)}%`, background: FEATURE_COLORS[i % FEATURE_COLORS.length] }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="p-4 sm:p-5">
          <SectionTitle icon={<ShieldCheck size={16} />} title="AI Provider & Model" />
          {loading ? <Skeleton className="h-40" /> : byModel.length === 0 ? (
            <Empty title="No data available yet" hint="Provider and model usage will appear after the first AI request." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[420px] text-sm">
                <thead>
                  <tr className="text-left text-xs uppercase tracking-wide text-stone-500 dark:text-stone-400">
                    <th className="py-2 pr-3 font-semibold">Provider / Model</th>
                    <th className="py-2 pr-3 text-right font-semibold">Requests</th>
                    <th className="py-2 pr-3 text-right font-semibold">Tokens</th>
                    <th className="py-2 text-right font-semibold">Success</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-stone-100 dark:divide-white/[0.05]">
                  {byModel.map((m) => (
                    <tr key={m.model}>
                      <td className="py-2.5 pr-3">
                        <span
                          className="mr-2 inline-block h-2 w-2 rounded-full"
                          style={{ background: PROVIDER_COLORS[m.provider] ?? '#93a5b8' }}
                        />
                        <span className="font-medium text-stone-800 dark:text-stone-100">{m.provider}</span>
                        <span className="block truncate text-xs text-stone-500 dark:text-stone-400">{m.model}</span>
                      </td>
                      <td className="py-2.5 pr-3 text-right text-stone-700 dark:text-stone-300">{formatInt(m.requests)}</td>
                      <td className="py-2.5 pr-3 text-right text-stone-700 dark:text-stone-300">{formatTokens(m.tokens)}</td>
                      <td className="py-2.5 text-right text-stone-700 dark:text-stone-300">{m.successRate}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-2 flex h-40 items-center justify-center">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={byModel} dataKey="requests" nameKey="model" innerRadius={45} outerRadius={65} paddingAngle={2}>
                      {byModel.map((m, i) => (
                        <Cell key={m.model} fill={FEATURE_COLORS[i % FEATURE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<ChartTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </Card>
      </div>

      {/* Per-user */}
      <Card className="mt-4 p-4 sm:p-5">
        <SectionTitle icon={<Users size={16} />} title="User AI Usage" />
        {loading ? <Skeleton className="h-32" /> : byUser.length === 0 ? (
          <Empty title="No data available yet" hint="Per-user consumption will appear after the first AI request." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-sm">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wide text-stone-500 dark:text-stone-400">
                  <th className="py-2 pr-3 font-semibold">User</th>
                  <th className="py-2 pr-3 text-right font-semibold">Requests</th>
                  <th className="py-2 pr-3 text-right font-semibold">Tokens</th>
                  <th className="py-2 pr-3 text-right font-semibold">Est. Cost</th>
                  <th className="py-2 text-right font-semibold">Last Used</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100 dark:divide-white/[0.05]">
                {byUser.slice(0, 10).map((u) => (
                  <tr key={u.userId}>
                    <td className="py-2.5 pr-3">
                      <span className="font-medium text-stone-800 dark:text-stone-100">{u.name}</span>
                      {u.email && <span className="block truncate text-xs text-stone-500 dark:text-stone-400">{u.email}</span>}
                    </td>
                    <td className="py-2.5 pr-3 text-right text-stone-700 dark:text-stone-300">{formatInt(u.requests)}</td>
                    <td className="py-2.5 pr-3 text-right text-stone-700 dark:text-stone-300">{formatTokens(u.tokens)}</td>
                    <td className="py-2.5 pr-3 text-right text-stone-700 dark:text-stone-300">{formatCost(u.estimatedCost ?? u.cost)}</td>
                    <td className="py-2.5 text-right text-stone-500 dark:text-stone-400">{timeAgo(u.lastUsed)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Recent */}
      <Card className="mt-4 p-4 sm:p-5">
        <SectionTitle icon={<Activity size={16} />} title="Recent AI Activity" hint={hasRequests ? `${formatInt(recentTotal)} requests` : undefined} />
        {loading ? <Skeleton className="h-40" /> : recent.length === 0 ? (
          <Empty title="No data available yet" hint="Recent AI requests will be listed here with feature, model, latency, and status." />
        ) : (
          <>
            <div className="overflow-x-auto rounded-lg border border-stone-200/70 dark:border-white/[0.06]">
              <table className="w-full min-w-[860px] text-sm">
                <thead>
                  <tr className="bg-stone-100/70 text-left text-xs uppercase tracking-wide text-stone-500 dark:bg-white/[0.03] dark:text-stone-400">
                    <th className="px-4 py-3 font-semibold">Time</th>
                    <th className="px-4 py-3 font-semibold">User</th>
                    <th className="px-4 py-3 font-semibold">Feature</th>
                    <th className="px-4 py-3 font-semibold">Provider</th>
                    <th className="px-4 py-3 font-semibold">Model</th>
                    <th className="px-4 py-3 text-right font-semibold">Latency</th>
                    <th className="px-4 py-3 text-right font-semibold">Tokens</th>
                    <th className="px-4 py-3 text-right font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-stone-100 dark:divide-white/[0.05]">
                  {recent.map((r) => (
                    <tr key={r.id} className="hover:bg-forest-50/50 dark:hover:bg-white/[0.02]">
                      <td className="whitespace-nowrap px-4 py-2.5 text-stone-500 dark:text-stone-400">{formatDateTime(r.time ?? r.at)}</td>
                      <td className="max-w-[160px] truncate px-4 py-2.5 font-medium text-stone-800 dark:text-stone-100">{r.userName ?? '—'}</td>
                      <td className="px-4 py-2.5 text-stone-700 dark:text-stone-300">{prettyFeature(r.feature)}</td>
                      <td className="px-4 py-2.5 text-stone-700 dark:text-stone-300">{r.provider}</td>
                      <td className="max-w-[180px] truncate px-4 py-2.5 text-stone-500 dark:text-stone-400">{r.model}</td>
                      <td className="px-4 py-2.5 text-right text-stone-700 dark:text-stone-300">{formatLatency(r.latencyMs)}</td>
                      <td className="px-4 py-2.5 text-right text-stone-700 dark:text-stone-300">{r.tokens > 0 ? formatInt(r.tokens) : '—'}</td>
                      <td className="px-4 py-2.5 text-right">
                        <Badge tone={r.success ? 'green' : 'red'}>{r.status}</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mt-3 flex items-center justify-between text-sm text-stone-500 dark:text-stone-400">
              <p>{formatInt(recentTotal)} request{recentTotal === 1 ? '' : 's'}</p>
              <div className="flex items-center gap-1">
                <Button variant="ghost" disabled={recentPage === 0} onClick={() => setRecentPage((p) => Math.max(0, p - 1))} aria-label="Previous page">
                  <ChevronLeft size={16} />
                </Button>
                <span className="px-2 text-xs font-medium">Page {recentPage + 1}</span>
                <Button
                  variant="ghost"
                  disabled={(recentPage + 1) * recentPageSize >= recentTotal}
                  onClick={() => setRecentPage((p) => p + 1)}
                  aria-label="Next page"
                >
                  <ChevronRight size={16} />
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}
