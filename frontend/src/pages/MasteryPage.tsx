import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Activity,
  AlertTriangle,
  Award,
  BookOpen,
  Brain,
  Search,
  Target,
  TrendingUp,
} from 'lucide-react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { learningApi } from '../api/services';
import {
  Badge,
  Button,
  Card,
  Empty,
  Input,
  PageHeader,
  Progress,
  Skeleton,
} from '../components/ui';
import { timeAgo } from '../lib/utils';
import type { Growth, Mastery } from '../api/types';

type Level = 'strong' | 'developing' | 'weak';

function levelOf(score: number): Level {
  if (score >= 70) return 'strong';
  if (score >= 40) return 'developing';
  return 'weak';
}

const LEVEL_META: Record<Level, { label: string; tone: 'green' | 'amber' | 'red' }> = {
  strong: { label: 'Strong', tone: 'green' },
  developing: { label: 'Developing', tone: 'amber' },
  weak: { label: 'Needs Practice', tone: 'red' },
};

function trendTone(trend: string): 'green' | 'red' | 'blue' | 'slate' {
  const t = trend.toUpperCase();
  if (t.includes('IMPROV')) return 'green';
  if (t.includes('ATTENTION') || t.includes('DECLIN') || t.includes('REGRESS')) return 'red';
  if (t.includes('STABLE')) return 'blue';
  return 'slate';
}

export function MasteryPage() {
  const { projectId = '' } = useParams();
  const [items, setItems] = useState<Mastery[]>([]);
  const [growth, setGrowth] = useState<Growth[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [levelFilter, setLevelFilter] = useState<'all' | Level>('all');
  const [sort, setSort] = useState<'weakest' | 'strongest' | 'name'>('weakest');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      learningApi.mastery(projectId).catch(() => [] as Mastery[]),
      learningApi.growth(projectId).catch(() => [] as Growth[]),
    ])
      .then(([m, g]) => {
        if (cancelled) return;
        setItems(m ?? []);
        setGrowth(g ?? []);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  const trendByConcept = useMemo(() => {
    const map = new Map<string, Growth>();
    for (const g of growth) map.set(g.conceptId, g);
    return map;
  }, [growth]);

  const stats = useMemo(() => {
    const total = items.length;
    const strong = items.filter((m) => m.masteryScore >= 70).length;
    const developing = items.filter((m) => m.masteryScore >= 40 && m.masteryScore < 70).length;
    const weak = items.filter((m) => m.masteryScore < 40).length;
    const avg =
      total === 0 ? 0 : items.reduce((s, m) => s + m.masteryScore, 0) / total;
    return { total, strong, developing, weak, avg };
  }, [items]);

  const dist = useMemo(
    () => [
      { key: 'strong' as Level, name: 'Strong (≥70)', value: stats.strong, color: '#059669' },
      { key: 'developing' as Level, name: 'Developing (40–69)', value: stats.developing, color: '#d97706' },
      { key: 'weak' as Level, name: 'Needs Practice (<40)', value: stats.weak, color: '#dc2626' },
    ],
    [stats],
  );

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    const filtered = items.filter((m) => {
      if (levelFilter !== 'all' && levelOf(m.masteryScore) !== levelFilter) return false;
      if (q && !m.conceptName.toLowerCase().includes(q)) return false;
      return true;
    });
    return [...filtered].sort((a, b) => {
      if (sort === 'strongest') return b.masteryScore - a.masteryScore;
      if (sort === 'name') return a.conceptName.localeCompare(b.conceptName);
      return a.masteryScore - b.masteryScore; // weakest first (default)
    });
  }, [items, query, levelFilter, sort]);

  const overall = levelOf(stats.avg);
  const overallMsg =
    stats.total === 0
      ? ''
      : overall === 'strong'
        ? `Solid grasp across ${stats.total} concept${stats.total === 1 ? '' : 's'}. Keep reinforcing with mixed quizzes.`
        : overall === 'developing'
          ? `${stats.weak > 0 ? `${stats.weak} concept${stats.weak === 1 ? '' : 's'} need${stats.weak === 1 ? 's' : ''} practice. ` : ''}Focus your next quizzes on the weakest concepts below.`
          : `Most concepts need practice. Start with the weakest concept below, then re-quiz to raise your average.`;

  const kpis = [
    {
      label: 'Total Concepts',
      value: String(stats.total),
      desc: 'Tracked from quiz answers',
      icon: BookOpen,
      ring: 'bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-300',
    },
    {
      label: 'Strong',
      value: String(stats.strong),
      desc: 'Mastery ≥ 70%',
      icon: Award,
      ring: 'bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-300',
    },
    {
      label: 'Developing',
      value: String(stats.developing),
      desc: 'Mastery 40–69%',
      icon: Target,
      ring: 'bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-300',
    },
    {
      label: 'Needs Practice',
      value: String(stats.weak),
      desc: 'Mastery below 40%',
      icon: AlertTriangle,
      ring: 'bg-red-50 dark:bg-red-950/60 text-red-600 dark:text-red-300',
    },
    {
      label: 'Overall Mastery',
      value: `${Math.round(stats.avg)}%`,
      desc: `Average of ${stats.total} concept${stats.total === 1 ? '' : 's'}`,
      icon: Brain,
      ring: 'bg-violet-50 dark:bg-violet-950/60 text-violet-600 dark:text-violet-300',
    },
  ];

  return (
    <div>
      <PageHeader
        title="Concept Mastery"
        subtitle="Estimated per-concept mastery calculated by the backend from your quiz performance."
        action={
          <Link to={`/projects/${projectId}/quiz`}>
            <Button variant="outline">Take a quiz</Button>
          </Link>
        }
      />

      {loading ? (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-24" />
            ))}
          </div>
          <Skeleton className="h-64" />
        </div>
      ) : items.length === 0 ? (
        <Empty
          title="No mastery data yet"
          hint="Complete a quiz to generate concept mastery scores."
          action={
            <Link to={`/projects/${projectId}/quiz`}>
              <Button>Go to quizzes</Button>
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {/* KPI cards */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
            {kpis.map((k) => (
              <Card key={k.label} className="p-4 transition-shadow hover:shadow">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-xs font-medium uppercase tracking-wide text-stone-500 dark:text-stone-400">
                    {k.label}
                  </p>
                  <span className={`flex h-7 w-7 items-center justify-center rounded-full ${k.ring}`}>
                    <k.icon size={15} />
                  </span>
                </div>
                <p className="mt-1 text-2xl font-bold text-stone-900 dark:text-stone-100">
                  {k.value}
                </p>
                <p className="mt-0.5 text-xs text-stone-500 dark:text-stone-400">{k.desc}</p>
              </Card>
            ))}
          </div>

          {/* Distribution + overall */}
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-5">
            <Card className="p-5 lg:col-span-2">
              <h3 className="font-medium text-stone-900 dark:text-stone-100">
                Mastery distribution
              </h3>
              <p className="mt-0.5 text-xs text-stone-500 dark:text-stone-400">
                Share of concepts by level
              </p>
              <div className="mx-auto h-52 max-w-xs">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={dist}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={55}
                      outerRadius={85}
                      paddingAngle={3}
                      strokeWidth={0}
                    >
                      {dist.map((d) => (
                        <Cell key={d.name} fill={d.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value: number | string, name: string) => {
                        const pct =
                          stats.total === 0
                            ? 0
                            : Math.round((Number(value) / stats.total) * 100);
                        return [`${value} (${pct}%)`, name];
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <ul className="mt-2 space-y-1.5 text-sm">
                {dist.map((d) => {
                  const pct =
                    stats.total === 0
                      ? 0
                      : Math.round((d.value / stats.total) * 100);
                  return (
                    <li
                      key={d.name}
                      className="flex items-center justify-between gap-2 text-stone-600 dark:text-stone-300"
                    >
                      <span className="inline-flex items-center gap-2">
                        <span
                          className="inline-block h-2.5 w-2.5 rounded-full"
                          style={{ backgroundColor: d.color }}
                        />
                        {d.name}
                      </span>
                      <span className="font-semibold text-stone-900 dark:text-stone-100">
                        {d.value} · {pct}%
                      </span>
                    </li>
                  );
                })}
              </ul>
            </Card>

            <Card className="p-5 lg:col-span-3">
              <div className="flex items-center justify-between gap-2">
                <h3 className="font-medium text-stone-900 dark:text-stone-100">
                  Overall mastery
                </h3>
                <Badge tone={LEVEL_META[overall].tone}>{LEVEL_META[overall].label}</Badge>
              </div>
              <p className="mt-4 text-5xl font-bold text-stone-900 dark:text-stone-100">
                {Math.round(stats.avg)}
                <span className="text-2xl text-stone-400">%</span>
              </p>
              <Progress value={stats.avg} className="mt-3" />
              <div className="mt-1 flex justify-between text-xs text-stone-400 dark:text-stone-500">
                <span>0</span>
                <span>100</span>
              </div>
              <p className="mt-3 text-sm text-stone-600 dark:text-stone-300">{overallMsg}</p>
            </Card>
          </div>

          {/* Trend / history */}
          <Card className="flex flex-col gap-2 p-5 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-600 dark:bg-blue-950 dark:text-blue-300">
                {growth.length > 0 ? <TrendingUp size={17} /> : <Activity size={17} />}
              </span>
              <div>
                <h3 className="font-medium text-stone-900 dark:text-stone-100">
                  Mastery trend
                </h3>
                {growth.length > 0 ? (
                  <p className="mt-0.5 text-sm text-stone-500 dark:text-stone-400">
                    {growth.filter((g) => g.trend.toUpperCase().includes('IMPROV')).length}{' '}
                    improving ·{' '}
                    {growth.filter((g) => g.trend.toUpperCase().includes('STABLE')).length}{' '}
                    stable ·{' '}
                    {
                      growth.filter((g) =>
                        g.trend.toUpperCase().includes('ATTENTION'),
                      ).length
                    }{' '}
                    needing attention — per-concept snapshot from your latest activity.
                  </p>
                ) : (
                  <p className="mt-0.5 text-sm text-stone-500 dark:text-stone-400">
                    Not enough history yet. Complete more quizzes and progress history
                    will appear here.
                  </p>
                )}
              </div>
            </div>
            <Link to={`/projects/${projectId}/growth`} className="shrink-0">
              <Button variant="outline">View growth</Button>
            </Link>
          </Card>

          {/* Concept details */}
          <Card className="p-5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="font-medium text-stone-900 dark:text-stone-100">
                  Concepts
                </h3>
                <p className="mt-0.5 text-xs text-stone-500 dark:text-stone-400">
                  {visible.length} of {items.length} shown · sorted weakest first by default
                </p>
              </div>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3">
              <div className="relative sm:col-span-1">
                <Search
                  size={15}
                  className="pointer-events-none absolute left-3 top-1/2 -transtone-y-1/2 text-stone-400"
                />
                <Input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Search concepts…"
                  className="pl-9"
                />
              </div>
              <select
                value={levelFilter}
                onChange={(e) => setLevelFilter(e.target.value as 'all' | Level)}
                className="w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 focus:border-forest-500 focus:outline-none focus:ring-1 focus:ring-forest-500 dark:border-stone-600 dark:bg-stone-900 dark:text-stone-300"
                aria-label="Filter by level"
              >
                <option value="all">All levels</option>
                <option value="strong">Strong (≥70)</option>
                <option value="developing">Developing (40–69)</option>
                <option value="weak">Needs Practice (&lt;40)</option>
              </select>
              <select
                value={sort}
                onChange={(e) =>
                  setSort(e.target.value as 'weakest' | 'strongest' | 'name')
                }
                className="w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 focus:border-forest-500 focus:outline-none focus:ring-1 focus:ring-forest-500 dark:border-stone-600 dark:bg-stone-900 dark:text-stone-300"
                aria-label="Sort concepts"
              >
                <option value="weakest">Sort: weakest first</option>
                <option value="strongest">Sort: strongest first</option>
                <option value="name">Sort: name A–Z</option>
              </select>
            </div>

            <div className="mt-3 space-y-2">
              {visible.length === 0 ? (
                <p className="rounded-md bg-stone-50 px-3 py-6 text-center text-sm text-stone-500 dark:bg-stone-800 dark:text-stone-400">
                  No concepts match your search/filter.
                </p>
              ) : (
                visible.map((m) => {
                  const level = levelOf(m.masteryScore);
                  const trend = trendByConcept.get(m.conceptId);
                  return (
                    <div
                      key={m.conceptId}
                      className="rounded-lg border border-stone-200 p-4 transition-colors hover:border-stone-300 hover:bg-stone-50/60 dark:border-stone-700 dark:hover:border-stone-600 dark:hover:bg-stone-800/60"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <p className="min-w-0 flex-1 truncate font-medium text-stone-900 dark:text-stone-100">
                          {m.conceptName}
                        </p>
                        <div className="flex shrink-0 items-center gap-2">
                          {trend && (
                            <Badge tone={trendTone(trend.trend)}>{trend.trend}</Badge>
                          )}
                          <Badge tone={LEVEL_META[level].tone}>
                            {LEVEL_META[level].label} · {Math.round(m.masteryScore)}%
                          </Badge>
                        </div>
                      </div>
                      <Progress value={m.masteryScore} className="mt-3" />
                      <p className="mt-1.5 text-xs text-stone-500 dark:text-stone-400">
                        {m.updatedAt ? `Updated ${timeAgo(m.updatedAt)}` : 'Update time unavailable'}
                        {typeof m.confidence === 'number'
                          ? ` · Confidence ${Math.round(m.confidence * 100)}%`
                          : ''}
                      </p>
                    </div>
                  );
                })
              )}
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
