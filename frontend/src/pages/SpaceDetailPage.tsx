import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, FolderOpen, Plus } from 'lucide-react';
import { projectsApi, spacesApi } from '../api/services';
import { apiErrorMessage } from '../api/client';
import { Button, Card, Empty, Input, PageHeader, Skeleton, Textarea } from '../components/ui';
import type { Project, Space } from '../api/types';

export function SpaceDetailPage() {
  const { spaceId = '' } = useParams();
  const [space, setSpace] = useState<Space | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [goal, setGoal] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const [s, p] = await Promise.all([spacesApi.get(spaceId), projectsApi.list(spaceId)]);
        setSpace(s);
        setProjects(p);
      } catch (err) {
        setError(apiErrorMessage(err));
      } finally {
        setLoading(false);
      }
    })();
  }, [spaceId]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    try {
      const p = await projectsApi.create(spaceId, { name: name.trim(), goal: goal.trim() || undefined });
      setProjects((prev) => [p, ...prev]);
      setName('');
      setGoal('');
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create project.'));
    }
  }

  return (
    <div>
      <Link to="/spaces" className="mb-4 inline-flex items-center gap-1.5 text-sm text-stone-500 hover:text-forest-700 dark:text-stone-400 dark:hover:text-forest-300">
        <ArrowLeft size={15} /> Spaces
      </Link>
      <PageHeader
        eyebrow="Space"
        title={space?.name ?? 'Space'}
        subtitle={space?.description ?? 'Projects inside this space'}
      />
      {error && <p className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/50 dark:text-red-300">{error}</p>}
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        <Card className="h-fit p-6">
          <h2 className="mb-1 flex items-center gap-2 text-[15px] font-semibold"><Plus size={16} className="text-forest-600 dark:text-forest-400" /> New project</h2>
          <p className="mb-4 text-[13px] text-stone-500 dark:text-stone-400">A focused learning workspace.</p>
          <form onSubmit={create} className="space-y-3">
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Name</label>
              <Input placeholder="e.g. ML Fundamentals" value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Learning goal</label>
              <Textarea rows={3} placeholder="What do you want to learn? (optional)" value={goal} onChange={(e) => setGoal(e.target.value)} />
            </div>
            <Button className="w-full">Create project</Button>
          </form>
        </Card>
        <div className="space-y-3 lg:col-span-2">
          {loading ? (<><Skeleton className="h-28" /><Skeleton className="h-28" /></>) : projects.length === 0 ? (
            <Empty title="No projects yet" hint="Create a project, then add learning materials to it." />
          ) : projects.map((p) => (
            <Card key={p.id} className="p-5 transition-shadow hover:shadow-lift sm:p-6">
              <div className="flex items-start gap-3.5">
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">
                  <FolderOpen size={19} />
                </span>
                <div className="min-w-0 flex-1">
                  <Link to={`/projects/${p.id}`} className="text-[15px] font-semibold tracking-tight hover:text-forest-700 hover:underline dark:hover:text-forest-300">{p.name}</Link>
                  {p.goal && <p className="mt-0.5 text-sm text-stone-500 dark:text-stone-400">Goal: {p.goal}</p>}
                </div>
                <Link to={`/projects/${p.id}`} className="shrink-0">
                  <Button variant="outline" className="px-3.5">Open <ArrowRight size={14} /></Button>
                </Link>
              </div>
              <div className="mt-4 flex flex-wrap gap-1.5 border-t border-stone-100 pt-3.5 dark:border-white/[0.06]">
                {[
                  { to: 'materials', label: 'Materials' },
                  { to: 'tutor', label: 'Tutor' },
                  { to: 'quiz', label: 'Quiz' },
                  { to: 'mastery', label: 'Mastery' },
                  { to: 'growth', label: 'Growth' },
                  { to: 'analytics', label: 'Analytics' },
                ].map((tab) => (
                  <Link key={tab.to} to={`/projects/${p.id}/${tab.to}`} className="rounded-full border border-stone-900/[0.09] px-3 py-1 text-xs font-semibold text-stone-600 transition-colors hover:border-forest-500 hover:bg-forest-50 hover:text-forest-800 dark:border-white/10 dark:text-stone-300 dark:hover:border-forest-600 dark:hover:bg-forest-900/30 dark:hover:text-forest-200">
                    {tab.label}
                  </Link>
                ))}
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
