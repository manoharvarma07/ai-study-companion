import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, BookOpen, Brain, FileText, LineChart, MessagesSquare, Sparkles, Target } from 'lucide-react';
import { projectsApi } from '../api/services';
import { cn } from '../lib/utils';
import { Card } from '../components/ui';
import type { Project } from '../api/types';

const TABS = [
  { to: '', label: 'Overview', icon: BookOpen, end: true },
  { to: 'materials', label: 'Materials', icon: FileText },
  { to: 'tutor', label: 'Tutor', icon: MessagesSquare },
  { to: 'quiz', label: 'Quiz', icon: Target },
  { to: 'mastery', label: 'Mastery', icon: Brain },
  { to: 'growth', label: 'Growth', icon: LineChart },
  { to: 'analytics', label: 'Analytics', icon: Sparkles },
];

export function ProjectLayout() {
  const { projectId = '' } = useParams();
  const [project, setProject] = useState<Project | null>(null);

  useEffect(() => {
    projectsApi.get(projectId).then(setProject).catch(() => null);
  }, [projectId]);

  return (
    <div>
      <Link to="/spaces" className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-stone-500 transition-colors hover:text-forest-700 dark:text-stone-400 dark:hover:text-forest-300">
        <ArrowLeft size={15} /> Back to spaces
      </Link>
      <div className="mb-5 rounded-2xl border border-forest-900/[0.08] bg-gradient-to-br from-moss-50 via-cream-card to-clay-soft/40 p-6 shadow-card sm:p-7 dark:border-white/[0.07] dark:from-white/[0.04] dark:via-transparent dark:to-transparent">
        <p className="inline-flex items-center gap-1.5 rounded-full bg-forest-100 px-2.5 py-1 text-[11px] font-bold uppercase tracking-[0.12em] text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">Learning workspace</p>
        <h1 className="mt-2 text-page text-ink dark:text-stone-50">{project?.name ?? 'Project'}</h1>
        {project?.goal && <p className="mt-1.5 max-w-2xl text-[15px] leading-relaxed text-stone-500 dark:text-stone-400">Goal: {project.goal}</p>}
      </div>
      <div className="mb-7 flex gap-1 overflow-x-auto border-b border-stone-900/[0.08] dark:border-white/10">
        {TABS.map((t) => (
          <NavLink
            key={t.to || 'overview'}
            to={t.to}
            end={t.end}
            className={({ isActive }) =>
              cn('relative flex shrink-0 items-center gap-1.5 px-3.5 py-2.5 text-sm font-semibold transition-colors',
                isActive ? 'text-forest-800 dark:text-forest-200' : 'text-stone-500 hover:text-ink dark:text-stone-400 dark:hover:text-stone-100')
            }
          >
            {({ isActive }) => (
              <>
                <t.icon size={15} className={isActive ? 'text-forest-700 dark:text-forest-300' : ''} /> {t.label}
                {isActive && <span className="absolute inset-x-2 -bottom-px h-[3px] rounded-full bg-forest-600 dark:bg-forest-400" />}
              </>
            )}
          </NavLink>
        ))}
      </div>
      <Outlet />
    </div>
  );
}

const STEP_TINTS = [
  'bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300',
  'bg-teal-50 text-teal-700 dark:bg-teal-950/60 dark:text-teal-300',
  'bg-sky-100/70 text-sky-700 dark:bg-sky-950/50 dark:text-sky-300',
  'bg-violet-100/70 text-violet-700 dark:bg-violet-950/50 dark:text-violet-300',
  'bg-amber-100/70 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300',
];

export function ProjectOverviewPage() {
  const { projectId = '' } = useParams();
  const [project, setProject] = useState<Project | null>(null);

  useEffect(() => {
    projectsApi.get(projectId).then(setProject).catch(() => null);
  }, [projectId]);

  const steps = [
    { title: 'Add material', desc: 'Upload PDFs. The backend extracts, chunks and embeds them.', link: 'materials', cta: 'Upload', icon: FileText },
    { title: 'Ask the tutor', desc: 'Grounded Q&A over your own documents.', link: 'tutor', cta: 'Chat', icon: MessagesSquare },
    { title: 'Take a quiz', desc: 'Adaptive questions generated from your materials.', link: 'quiz', cta: 'Start quiz', icon: Target },
    { title: 'Review mastery', desc: 'Per-concept scores updated from quiz results.', link: 'mastery', cta: 'View', icon: Brain },
    { title: 'Track growth', desc: 'Trends and AI recommendations for next steps.', link: 'growth', cta: 'Analyze', icon: LineChart },
  ];

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Your learning journey</h2>
        {project?.description
          ? <p className="mt-1 max-w-2xl text-[15px] text-stone-600 dark:text-stone-300">{project.description}</p>
          : <p className="mt-1 text-[15px] text-stone-500 dark:text-stone-400">Five connected steps, one workspace.</p>}
      </div>
      <div className="grid grid-cols-1 gap-3.5 md:grid-cols-2 xl:grid-cols-3">
        {steps.map((s, i) => (
          <Card key={s.title} className="group flex flex-col p-6 transition-shadow hover:shadow-lift">
            <span className={`inline-flex h-10 w-10 items-center justify-center rounded-xl ${STEP_TINTS[i % STEP_TINTS.length]}`}>
              <s.icon size={18} />
            </span>
            <p className="mt-3.5 text-[15px] font-semibold"><span className="mr-1.5 text-stone-400">{i + 1}.</span>{s.title}</p>
            <p className="mt-1 flex-1 text-sm leading-relaxed text-stone-500 dark:text-stone-400">{s.desc}</p>
            <Link to={`/projects/${projectId}/${s.link}`} className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-forest-700 hover:text-forest-800 hover:underline dark:text-forest-300 dark:hover:text-forest-200">
              {s.cta} <ArrowRight size={14} className="transition-transform group-hover:translate-x-0.5" />
            </Link>
          </Card>
        ))}
      </div>
    </div>
  );
}
