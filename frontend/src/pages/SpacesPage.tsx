import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Library, Plus, Trash2 } from 'lucide-react';
import { spacesApi } from '../api/services';
import { apiErrorMessage } from '../api/client';
import { Button, Card, Empty, Input, PageHeader, Skeleton, Textarea } from '../components/ui';
import { timeAgo } from '../lib/utils';
import type { Space } from '../api/types';

const TILE_TINTS = [
  'bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300',
  'bg-teal-50 text-teal-700 dark:bg-teal-950/60 dark:text-teal-300',
  'bg-sky-100/70 text-sky-700 dark:bg-sky-950/50 dark:text-sky-300',
  'bg-violet-100/70 text-violet-700 dark:bg-violet-950/50 dark:text-violet-300',
  'bg-amber-100/70 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300',
];

export function SpacesPage() {
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [desc, setDesc] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setSpaces(await spacesApi.list());
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void load(); }, []);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    setCreating(true);
    try {
      const s = await spacesApi.create(name.trim(), desc.trim() || undefined);
      setSpaces((prev) => [s, ...prev]);
      setName('');
      setDesc('');
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create space.'));
    } finally {
      setCreating(false);
    }
  }

  async function remove(id: string) {
    if (!confirm('Delete this space?')) return;
    await spacesApi.remove(id);
    setSpaces((prev) => prev.filter((s) => s.id !== id));
  }

  return (
    <div>
      <PageHeader
        eyebrow="Organize"
        title="Spaces"
        subtitle="Collections of learning projects, grouped by subject or goal."
      />
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        <Card className="h-fit p-6">
          <h2 className="mb-1 flex items-center gap-2 text-[15px] font-semibold"><Plus size={16} className="text-forest-600 dark:text-forest-400" /> New space</h2>
          <p className="mb-4 text-[13px] text-stone-500 dark:text-stone-400">Group related projects together.</p>
          <form onSubmit={create} className="space-y-3">
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Name</label>
              <Input placeholder="e.g. Machine Learning" value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Description</label>
              <Textarea rows={3} placeholder="What is this space about?" value={desc} onChange={(e) => setDesc(e.target.value)} />
            </div>
            {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/50 dark:text-red-300">{error}</p>}
            <Button disabled={creating} className="w-full">{creating ? 'Creating…' : 'Create space'}</Button>
          </form>
        </Card>
        <div className="space-y-3 lg:col-span-2">
          {loading ? (<><Skeleton className="h-28" /><Skeleton className="h-28" /></>) : spaces.length === 0 ? (
            <Empty title="Start your first learning project" hint="Create a space to hold your projects, materials and quizzes." />
          ) : spaces.map((s, i) => (
            <Card key={s.id} className="flex items-center gap-4 p-5 transition-shadow hover:shadow-lift sm:p-6">
              <span className={`hidden h-12 w-12 shrink-0 items-center justify-center rounded-xl sm:flex ${TILE_TINTS[i % TILE_TINTS.length]}`}>
                <Library size={20} />
              </span>
              <div className="min-w-0 flex-1">
                <Link to={`/spaces/${s.id}`} className="text-[15px] font-semibold tracking-tight hover:text-forest-700 hover:underline dark:hover:text-forest-300">{s.name}</Link>
                {s.description && <p className="mt-0.5 truncate text-sm text-stone-500 dark:text-stone-400">{s.description}</p>}
                <p className="mt-1 text-xs text-stone-400">Created {timeAgo(s.createdAt)}</p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <Link to={`/spaces/${s.id}`}><Button variant="outline" className="px-3.5">Open <ArrowRight size={14} /></Button></Link>
                <Button variant="ghost" onClick={() => void remove(s.id)} aria-label={`Delete ${s.name}`}>
                  <Trash2 size={15} />
                </Button>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
