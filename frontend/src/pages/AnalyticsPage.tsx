import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Activity, Award, FileText, Layers, MessagesSquare, Target } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { analyticsApi, learningApi } from '../api/services';
import { Card, PageHeader, Skeleton } from '../components/ui';
import { timeAgo } from '../lib/utils';
import type { Mastery, ProjectAnalytics } from '../api/types';

const SOFT_BARS = ['#7AA37E', '#6FA3A0', '#7FA8C9', '#A8B5D1', '#C4B5A5', '#8FC1B5'];

export function AnalyticsPage() {
  const { projectId = '' } = useParams();
  const [data, setData] = useState<ProjectAnalytics | null>(null);
  const [mastery, setMastery] = useState<Mastery[]>([]);

  useEffect(() => {
    analyticsApi.project(projectId).then(setData).catch(() => null);
    learningApi.mastery(projectId).then(setMastery).catch(() => null);
  }, [projectId]);

  const cards = data ? [
    { label: 'Materials', value: String(data.materials), icon: FileText },
    { label: 'Ready', value: String(data.materialsReady), icon: Layers },
    { label: 'Concepts', value: String(data.concepts), icon: Award },
    { label: 'Tutor messages', value: String(data.tutorMessages), icon: MessagesSquare },
    { label: 'Quizzes', value: String(data.quizzes), icon: Target },
    { label: 'Avg score', value: `${Math.round(data.averageScore)}%`, icon: Activity },
  ] : [];

  return (
    <div>
      <PageHeader eyebrow="Insights" title="Project Analytics" subtitle="What has happened in this project so far." />
      {!data ? <Skeleton className="h-40" /> : (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
            {cards.map((c) => (
              <Card key={c.label} className="p-4">
                <c.icon size={16} className="text-forest-600 dark:text-forest-400" />
                <p className="mt-2 text-[1.4rem] font-semibold tracking-tight">{c.value}</p>
                <p className="text-xs text-stone-500 dark:text-stone-400">{c.label}</p>
              </Card>
            ))}
          </div>
          <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-5">
            <Card className="p-6 lg:col-span-3">
              <h3 className="text-[15px] font-semibold">Mastery by concept</h3>
              <p className="mb-3 mt-0.5 text-[13px] text-stone-500 dark:text-stone-400">Current estimated score per concept.</p>
              {mastery.length === 0 ? (
                <p className="py-8 text-center text-sm text-stone-500 dark:text-stone-400">No mastery data yet.</p>
              ) : (
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={mastery} margin={{ top: 4, right: 8, bottom: 0, left: -12 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="currentColor" opacity={0.12} vertical={false} />
                      <XAxis dataKey="conceptName" tick={{ fontSize: 11 }} interval={0} angle={-18} height={62} tickLine={false} axisLine={false} />
                      <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} tickLine={false} axisLine={false} width={36} />
                      <Tooltip cursor={{ fill: 'rgba(120,113,100,0.08)' }} formatter={(v) => [`${Math.round(Number(v))}%`, 'Mastery']} />
                      <Bar dataKey="masteryScore" radius={[5, 5, 0, 0]} barSize={22}>
                        {mastery.map((_, i) => (
                          <Cell key={i} fill={SOFT_BARS[i % SOFT_BARS.length]} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>
            <Card className="p-6 lg:col-span-2">
              <h3 className="text-[15px] font-semibold">Recent activity</h3>
              <p className="mb-3 mt-0.5 text-[13px] text-stone-500 dark:text-stone-400">Latest events in this project.</p>
              <ul className="max-h-64 space-y-0 overflow-y-auto text-sm">
                {(data.recentEvents ?? []).map((e, i) => (
                  <li key={i} className="flex items-center justify-between gap-2 border-b border-stone-100 py-2.5 last:border-0 dark:border-white/[0.06]">
                    <span className="font-medium">{e.type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())}</span>
                    <span className="shrink-0 text-xs text-stone-400">{timeAgo(e.at)}</span>
                  </li>
                ))}
                {(data.recentEvents ?? []).length === 0 && <li className="py-4 text-center text-stone-500 dark:text-stone-400">No activity yet.</li>}
              </ul>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
