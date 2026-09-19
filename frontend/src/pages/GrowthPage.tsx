import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Compass, Sprout, TrendingDown, TrendingUp, Minus, Sparkles } from 'lucide-react';
import { learningApi } from '../api/services';
import { Badge, Button, Card, Empty, PageHeader, Skeleton } from '../components/ui';
import type { Growth, LearningContext, Recommendation } from '../api/types';

export function GrowthPage() {
  const { projectId = '' } = useParams();
  const [growth, setGrowth] = useState<Growth[]>([]);
  const [recs, setRecs] = useState<Recommendation[]>([]);
  const [ctx, setCtx] = useState<LearningContext | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const [g, r, c] = await Promise.all([
        learningApi.growth(projectId),
        learningApi.recommendations(projectId),
        learningApi.context(projectId),
      ]);
      setGrowth(g);
      setRecs(r);
      setCtx(c);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void load(); }, [projectId]);

  async function generate() {
    setGenerating(true);
    try {
      const r = await learningApi.generateRecommendation(projectId);
      setRecs((prev) => [r, ...prev]);
    } finally {
      setGenerating(false);
    }
  }

  function trendIcon(t: string) {
    const u = t.toUpperCase();
    if (u.includes('IMPROV') || u.includes('UP')) return <TrendingUp size={16} className="text-forest-600 dark:text-forest-400" />;
    if (u.includes('DECLIN') || u.includes('DOWN') || u.includes('ATTENTION')) return <TrendingDown size={16} className="text-red-400" />;
    return <Minus size={16} className="text-stone-400" />;
  }

  const improving = growth.filter((g) => /IMPROV|UP/i.test(g.trend));
  const attention = growth.filter((g) => /ATTENTION|DECLIN|DOWN/i.test(g.trend));

  return (
    <div>
      <PageHeader
        eyebrow="Journey"
        title="Growth & Recommendations"
        subtitle="How your understanding is developing, and what to focus on next."
        action={<Button onClick={() => void generate()} disabled={generating}><Sparkles size={15} /> {generating ? 'Generating…' : 'Generate recommendation'}</Button>}
      />
      {loading ? <Skeleton className="h-48" /> : growth.length === 0 && recs.length === 0 && !ctx ? (
        <Empty
          title="Your learning history is still building"
          hint="Complete a few quizzes to see your growth over time — trends, improvements and areas needing attention will appear here."
          action={<Button onClick={() => void generate()} disabled={generating}>Generate recommendation</Button>}
        />
      ) : (
        <div className="space-y-4">
          {(improving.length > 0 || attention.length > 0) && (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Card className="flex items-center gap-4 p-5">
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">
                  <TrendingUp size={19} />
                </span>
                <div>
                  <p className="text-2xl font-semibold tracking-tight">{improving.length}</p>
                  <p className="text-[13px] text-stone-500 dark:text-stone-400">Concepts improving</p>
                </div>
              </Card>
              <Card className="flex items-center gap-4 p-5">
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-amber-100/70 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300">
                  <Compass size={19} />
                </span>
                <div>
                  <p className="text-2xl font-semibold tracking-tight">{attention.length}</p>
                  <p className="text-[13px] text-stone-500 dark:text-stone-400">Need attention</p>
                </div>
              </Card>
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div className="space-y-3">
              <h3 className="text-[13px] font-semibold uppercase tracking-[0.1em] text-stone-400 dark:text-stone-500">Concept trends</h3>
              {growth.length === 0 ? (
                <Card className="p-6 text-center">
                  <Sprout size={22} className="mx-auto text-forest-500" />
                  <p className="mt-2 text-sm font-medium">No trends yet</p>
                  <p className="mx-auto mt-1 max-w-xs text-[13px] text-stone-500 dark:text-stone-400">Growth appears after repeated quizzes on the same concepts.</p>
                </Card>
              ) : growth.map((g) => (
                <Card key={g.conceptId} className="flex items-start justify-between gap-3 p-5 transition-shadow hover:shadow-lift">
                  <div className="min-w-0">
                    <p className="flex items-center gap-2 text-[15px] font-medium">{trendIcon(g.trend)} <span className="truncate">{g.conceptName}</span></p>
                    <p className="mt-0.5 text-xs font-medium uppercase tracking-wide text-stone-400">{g.trend.replace(/_/g, ' ')}</p>
                    {g.detail && <p className="mt-1 text-sm leading-relaxed text-stone-500 dark:text-stone-400">{g.detail}</p>}
                  </div>
                  <Badge tone={g.masteryScore >= 70 ? 'green' : g.masteryScore >= 40 ? 'amber' : 'red'}>{Math.round(g.masteryScore)}%</Badge>
                </Card>
              ))}
              {ctx && (ctx.goals || ctx.strengths || ctx.weaknesses) && (
                <Card className="space-y-2 p-5 text-sm leading-relaxed">
                  <p className="text-[15px] font-semibold">Learning context</p>
                  {ctx.goals && <p><span className="font-medium">Goals:</span> <span className="text-stone-600 dark:text-stone-300">{ctx.goals}</span></p>}
                  {ctx.strengths && <p><span className="font-medium">Strengths:</span> <span className="text-stone-600 dark:text-stone-300">{ctx.strengths}</span></p>}
                  {ctx.weaknesses && <p><span className="font-medium">Weaknesses:</span> <span className="text-stone-600 dark:text-stone-300">{ctx.weaknesses}</span></p>}
                </Card>
              )}
            </div>
            <div className="space-y-3">
              <h3 className="text-[13px] font-semibold uppercase tracking-[0.1em] text-stone-400 dark:text-stone-500">Recommended next actions</h3>
              {recs.length === 0 ? <Empty title="No recommendations" hint="Generate one with the button above." /> : recs.map((r) => (
                <Card key={r.id} className="border-forest-200 bg-forest-50/70 p-5 dark:border-forest-800/50 dark:bg-forest-950/30">
                  <p className="flex items-start gap-2 text-sm font-medium leading-relaxed"><Sparkles size={15} className="mt-0.5 shrink-0 text-forest-600 dark:text-forest-400" /> {r.text}</p>
                  {r.reason && <p className="mt-1.5 pl-6 text-[13px] text-stone-500 dark:text-stone-400">{r.reason}</p>}
                </Card>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
