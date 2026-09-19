import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ArrowRight, BookOpen, Brain, FileText, MessagesSquare, Sparkles, Target } from 'lucide-react';
import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useAuth } from '../auth/AuthContext';
import { analyticsApi, learningApi, projectsApi, spacesApi } from '../api/services';
import { greeting, timeAgo } from '../lib/utils';
import { Badge, Button, Card, Empty, Progress, Skeleton } from '../components/ui';
import type { Mastery } from '../api/types';

const SOFT_BARS = ['#7AA37E', '#6FA3A0', '#7FA8C9', '#A8B5D1', '#C4B5A5', '#8FC1B5', '#93B8D4', '#B7C9A8'];

export function DashboardPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ materials: 0, tutor: 0, avg: 0, concepts: 0 });
  const [recent, setRecent] = useState<{ type: string; at: string }[]>([]);
  const [mastery, setMastery] = useState<Mastery[]>([]);
  const [rec, setRec] = useState<string | null>(null);
  const [continueProject, setContinueProject] = useState<{ id: string; name: string } | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const spaces = await spacesApi.list();
        const firstSpace = spaces[0];
        let project = null;
        if (firstSpace) {
          const projs = await projectsApi.list(firstSpace.id);
          project = projs[0] ?? null;
        }
        if (project) {
          setContinueProject({ id: project.id, name: project.name });
          const [a, m, r] = await Promise.all([
            analyticsApi.project(project.id),
            learningApi.mastery(project.id),
            learningApi.recommendations(project.id),
          ]);
          setStats({ materials: a.materials, tutor: a.tutorMessages, avg: a.averageScore, concepts: a.concepts });
          setRecent((a.recentEvents ?? []).slice(0, 5));
          setMastery(m);
          setRec(r[0]?.text ?? null);
        } else {
          const ov = await analyticsApi.overview();
          setStats({ materials: 0, tutor: 0, avg: 0, concepts: 0 });
          void ov;
        }
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const focus = [...mastery].sort((a, b) => a.masteryScore - b.masteryScore).slice(0, 3);
  const avgMastery = mastery.length === 0 ? 0 : mastery.reduce((s, m) => s + m.masteryScore, 0) / mastery.length;

  return (
    <div className="space-y-8">
      <div className="overflow-hidden rounded-3xl border border-forest-900/[0.08] bg-gradient-to-br from-moss-50 via-cream-card to-clay-soft/50 p-6 shadow-card sm:p-8 dark:border-white/[0.07] dark:from-forest-950/40 dark:via-transparent dark:to-transparent">
        <p className="inline-flex items-center gap-1.5 rounded-full bg-forest-100 px-2.5 py-1 text-[11px] font-bold uppercase tracking-[0.12em] text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">
          <Sparkles size={11} /> Calm learning AI
        </p>
        <h1 className="mt-2 text-page text-ink dark:text-stone-50">
          {greeting()}, {user?.name?.split(' ')[0] ?? 'Student'}
        </h1>
        <p className="mt-1.5 text-[15px] text-stone-500 dark:text-stone-400">Continue your learning journey — grounded in your own materials.</p>
      </div>

      {/* Continue learning */}
      <section>
        <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-[0.1em] text-stone-400 dark:text-stone-500">Continue learning</h2>
        {loading ? <Skeleton className="h-36" /> : continueProject ? (
          <Card className="overflow-hidden">
            <div className="flex flex-wrap items-center justify-between gap-5 p-6 sm:p-7">
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-[0.1em] text-forest-600 dark:text-forest-400">Current project</p>
                <p className="mt-1 text-xl font-semibold tracking-tight">{continueProject.name}</p>
                <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">
                  {Math.round(avgMastery)}% average mastery · {stats.materials} materials · {stats.tutor} tutor questions
                </p>
                <Progress value={avgMastery} className="mt-4 w-full sm:w-64" />
              </div>
              <Link to={`/projects/${continueProject.id}`} className="shrink-0">
                <Button className="px-5 py-2.5">Continue studying <ArrowRight size={16} /></Button>
              </Link>
            </div>
          </Card>
        ) : (
          <Empty title="Start your first learning project" hint="Create a space, then a project, then add your first material." action={<Link to="/spaces"><Button>Create a space</Button></Link>} />
        )}
      </section>

      {/* Progress */}
      <section>
        <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-[0.1em] text-stone-400 dark:text-stone-500">Your progress</h2>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {[
            { icon: FileText, label: 'Materials', value: String(stats.materials), tint: 'bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300' },
            { icon: MessagesSquare, label: 'Tutor questions', value: String(stats.tutor), tint: 'bg-teal-50 text-teal-700 dark:bg-teal-950/60 dark:text-teal-300' },
            { icon: Target, label: 'Quiz average', value: `${Math.round(stats.avg)}%`, tint: 'bg-sky-100/80 text-sky-700 dark:bg-sky-950/50 dark:text-sky-300' },
            { icon: Brain, label: 'Concepts', value: String(stats.concepts), tint: 'bg-violet-100/80 text-violet-700 dark:bg-violet-950/50 dark:text-violet-300' },
          ].map((s) => (
            <Card key={s.label} className="p-5 transition-all hover:-translate-y-0.5 hover:shadow-lift">
              <span className={`inline-flex h-10 w-10 items-center justify-center rounded-2xl ${s.tint}`}>
                <s.icon size={17} />
              </span>
              <p className="mt-3 text-[1.65rem] font-semibold tracking-tight">{loading ? '—' : s.value}</p>
              <p className="text-[13px] text-stone-500 dark:text-stone-400">{s.label}</p>
            </Card>
          ))}
        </div>
      </section>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-5">
        {/* Mastery overview */}
        <Card className="p-6 lg:col-span-3">
          <h3 className="text-[15px] font-semibold">Mastery overview</h3>
          <p className="mb-4 mt-0.5 text-[13px] text-stone-500 dark:text-stone-400">Per-concept scores from your quizzes.</p>
          {loading ? <Skeleton className="h-52" /> : mastery.length === 0 ? <p className="py-6 text-center text-sm text-stone-500 dark:text-stone-400">No mastery data yet — complete a quiz.</p> : (
            <div className="h-56">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={mastery.slice(0, 8)} layout="vertical" margin={{ left: 8, right: 16, top: 0, bottom: 0 }}>
                  <XAxis type="number" domain={[0, 100]} hide />
                  <YAxis type="category" dataKey="conceptName" width={118} tick={{ fontSize: 12, fill: 'currentColor' }} tickLine={false} axisLine={false} />
                  <Tooltip cursor={{ fill: 'rgba(120,113,100,0.08)' }} formatter={(v) => [`${Math.round(Number(v))}%`, 'Mastery']} />
                  <Bar dataKey="masteryScore" radius={[0, 6, 6, 0]} barSize={14}>
                    {mastery.slice(0, 8).map((_, i) => (
                      <Cell key={i} fill={SOFT_BARS[i % SOFT_BARS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <div className="space-y-4 lg:col-span-2">
          {/* Focus areas */}
          <Card className="p-6">
            <h3 className="mb-1 text-[15px] font-semibold">Focus areas</h3>
            <p className="mb-4 text-[13px] text-stone-500 dark:text-stone-400">Your weakest concepts first.</p>
            {loading ? <Skeleton className="h-20" /> : focus.length === 0 ? <p className="text-sm text-stone-500 dark:text-stone-400">Nothing to focus on yet.</p> : (
              <ul className="space-y-3.5">
                {focus.map((f) => (
                  <li key={f.conceptId} className="flex items-center justify-between gap-3">
                    <span className="flex min-w-0 items-center gap-2 text-sm"><AlertTriangle size={15} className="shrink-0 text-amber-500" /> <span className="truncate">{f.conceptName}</span></span>
                    <span className="flex shrink-0 items-center gap-2">
                      <Progress value={f.masteryScore} className="w-20 sm:w-24" />
                      <Badge tone={f.masteryScore < 40 ? 'red' : f.masteryScore < 70 ? 'amber' : 'green'}>{Math.round(f.masteryScore)}%</Badge>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          {/* Recommendation */}
          <Card className="border-forest-900/[0.08] bg-gradient-to-b from-moss-50 to-cream-card p-6 dark:border-forest-800/40 dark:from-forest-950/40 dark:to-transparent">
            <h3 className="mb-1 flex items-center gap-2 text-[15px] font-semibold"><Sparkles size={15} className="text-forest-600 dark:text-forest-400" /> Recommended for you</h3>
            <p className="mt-1 text-sm leading-relaxed text-stone-600 dark:text-stone-300">{loading ? 'Loading…' : (rec ?? 'Add materials and attempt a quiz to unlock recommendations.')}</p>
            {continueProject && (
              <Link to={`/projects/${continueProject.id}/quiz`} className="mt-4 inline-block">
                <Button variant="outline"><BookOpen size={15} /> Practice now</Button>
              </Link>
            )}
          </Card>
        </div>
      </div>

      {/* Recent activity */}
      <section>
        <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-[0.1em] text-stone-400 dark:text-stone-500">Recent activity</h2>
        <Card className="p-6">
          {loading ? <Skeleton className="h-16" /> : recent.length === 0 ? (
            <p className="text-sm text-stone-500 dark:text-stone-400">No activity yet. Upload a material or chat with the tutor to get started.</p>
          ) : (
            <ul className="divide-y divide-stone-100 text-sm dark:divide-white/[0.06]">
              {recent.map((e, i) => (
                <li key={i} className="flex items-center justify-between gap-3 py-2.5">
                  <span className="font-medium">{e.type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())}</span>
                  <span className="shrink-0 text-xs text-stone-400">{timeAgo(e.at)}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </section>
    </div>
  );
}
