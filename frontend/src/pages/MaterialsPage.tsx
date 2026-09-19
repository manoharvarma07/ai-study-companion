import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { FileText, RefreshCw, Upload } from 'lucide-react';
import { materialsApi } from '../api/services';
import { apiErrorMessage } from '../api/client';
import { Button, Card, Empty, PageHeader, Progress, Skeleton, StatusBadge } from '../components/ui';
import { timeAgo } from '../lib/utils';
import { cn } from '../lib/utils';
import type { Material } from '../api/types';

function formatSize(bytes: number): string {
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / 1024).toFixed(0)} KB`;
}

const MAX_PDF_BYTES = 100 * 1024 * 1024;

export function MaterialsPage() {
  const { projectId = '' } = useParams();
  const [items, setItems] = useState<Material[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [pct, setPct] = useState(0);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    try {
      setItems(await materialsApi.list(projectId));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void load(); }, [projectId]);

  async function onFile(file: File | undefined) {
    if (!file) return;
    if (file.size > MAX_PDF_BYTES) {
      setError('File too large (max 100MB).');
      return;
    }
    setUploading(true);
    setPct(0);
    setError(null);
    try {
      const m = await materialsApi.upload(projectId, file, setPct);
      setItems((prev) => [m, ...prev]);
    } catch (err) {
      setError(apiErrorMessage(err, 'Upload failed.'));
    } finally {
      setUploading(false);
    }
  }

  async function retry(id: string) {
    try {
      await materialsApi.retry(projectId, id);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Retry failed.'));
    }
  }

  return (
    <div>
      <PageHeader
        eyebrow="Knowledge base"
        title="Learning Materials"
        subtitle="Upload PDFs up to 100MB. Processing (extract → chunk → embed → concepts) runs on the backend."
        action={
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-forest-700 px-4 py-2.5 text-sm font-semibold text-white shadow-[0_6px_18px_-8px_rgba(47,79,48,0.7)] transition-colors hover:bg-forest-800 dark:bg-forest-600 dark:hover:bg-forest-500">
            <Upload size={15} /> {uploading ? `Uploading ${pct}%` : 'Upload PDF'}
            <input type="file" accept="application/pdf,.pdf" className="hidden" disabled={uploading}
              onChange={(e) => void onFile(e.target.files?.[0])} />
          </label>
        }
      />
      {uploading && (
        <Card className="mb-4 p-4">
          <p className="mb-2 text-sm text-stone-600 dark:text-stone-300">Uploading… {pct}%</p>
          <Progress value={pct} />
        </Card>
      )}
      {error && <p className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">{error}</p>}
      {loading ? (
        <div className="space-y-3"><Skeleton className="h-24" /><Skeleton className="h-24" /></div>
      ) : items.length === 0 ? (
        <Empty title="Add a PDF to build your project knowledge" hint="Upload your first PDF and the backend will extract its text, build searchable chunks and discover key concepts for tutor + quiz." />
      ) : (
        <div className="space-y-3">
          {items.map((m) => (
            <Card key={m.id} className="flex flex-wrap items-center gap-4 p-5 transition-shadow hover:shadow-lift">
              <span className={cn('flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl',
                m.status === 'READY' ? 'bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300'
                : m.status === 'FAILED' ? 'bg-red-100/70 text-red-600 dark:bg-red-950/50 dark:text-red-300'
                : 'bg-amber-100/70 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300')}>
                <FileText size={20} />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-[15px] font-medium">{m.filename}</p>
                <p className="mt-0.5 text-[13px] text-stone-500 dark:text-stone-400">
                  {formatSize(m.fileSize)}
                  {m.pageCount ? ` · ${m.pageCount} pages` : ''} · {timeAgo(m.createdAt)}
                  {m.errorMessage ? ` · ${m.errorMessage}` : ''}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <StatusBadge status={m.status} />
                {m.status === 'FAILED' && (
                  <Button variant="outline" onClick={() => void retry(m.id)}>
                    <RefreshCw size={14} /> Retry
                  </Button>
                )}
              </div>
            </Card>
          ))}
          <Button variant="ghost" onClick={() => void load()}>Refresh statuses</Button>
        </div>
      )}
    </div>
  );
}
