import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, RotateCcw, XCircle } from 'lucide-react';
import { quizApi } from '../api/services';
import type { AnswerResult, QuizDetail, QuizSummary } from '../api/types';
import {
  Badge,
  Button,
  Card,
  Empty,
  PageHeader,
  Progress,
  Skeleton,
  Textarea,
} from '../components/ui';
import { apiErrorMessage } from '../api/client';
import { cn } from '../lib/utils';

export function QuizListPage() {
  const { projectId = '' } = useParams();
  const navigate = useNavigate();

  const [quizzes, setQuizzes] = useState<QuizSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mcq, setMcq] = useState(3);
  const [open, setOpen] = useState(1);

  useEffect(() => {
    quizApi
      .list(projectId)
      .then(setQuizzes)
      .catch((e) => setError(apiErrorMessage(e)))
      .finally(() => setLoading(false));
  }, [projectId]);

  async function create() {
    const mcqCount = Math.min(20, Math.max(1, Math.floor(mcq) || 1));
    const openCount = Math.min(10, Math.max(0, Math.floor(open) || 0));

    setMcq(mcqCount);
    setOpen(openCount);
    setCreating(true);
    setError(null);

    try {
      const q = await quizApi.create(projectId, {
        title: null,
        mcqCount,
        openCount,
      });

      navigate(`/quiz/${q.id}`);
    } catch (err) {
      setError(
        apiErrorMessage(
          err,
          'Could not generate quiz. Ensure materials are READY.'
        )
      );
    } finally {
      setCreating(false);
    }
  }

  return (
    <div>
      <PageHeader
        eyebrow="Practice"
        title="Quizzes"
        subtitle="Adaptive quizzes generated from your materials, focused where you need practice."
      />

      <Card className="mb-5 p-6">
        <h2 className="text-[15px] font-semibold">Create a new quiz</h2>
        <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">
          Questions are generated from your project material and focused on
          concepts that need practice.
        </p>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
          <label className="text-sm">
            <span className="mb-1.5 block text-[13px] font-medium">MCQ questions</span>
            <input
              type="number"
              min={1}
              max={20}
              value={mcq}
              onChange={(e) => setMcq(Number(e.target.value))}
              className="w-full rounded-lg border border-stone-300 bg-white px-3.5 py-2.5 text-sm focus:border-forest-500 focus:outline-none focus:ring-2 focus:ring-forest-500/25 dark:border-stone-700 dark:bg-stone-900"
            />
          </label>

          <label className="text-sm">
            <span className="mb-1.5 block text-[13px] font-medium">Open-ended questions</span>
            <input
              type="number"
              min={0}
              max={10}
              value={open}
              onChange={(e) => setOpen(Number(e.target.value))}
              className="w-full rounded-lg border border-stone-300 bg-white px-3.5 py-2.5 text-sm focus:border-forest-500 focus:outline-none focus:ring-2 focus:ring-forest-500/25 dark:border-stone-700 dark:bg-stone-900"
            />
          </label>
        </div>

        {error && (
          <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/50 dark:text-red-300">
            {error}
          </p>
        )}

        <Button className="mt-4" onClick={() => void create()} disabled={creating}>
          {creating ? 'Generating quiz…' : 'Generate quiz'}
        </Button>
      </Card>

      {loading ? (
        <Skeleton className="h-48" />
      ) : quizzes.length === 0 ? (
        <Empty
          title="No quizzes yet"
          hint="Generate your first adaptive quiz above."
        />
      ) : (
        <div className="space-y-3">
          {quizzes.map((quiz) => (
            <Card
              key={quiz.id}
              className="flex items-center justify-between gap-4 p-5 transition-shadow hover:shadow-lift"
            >
              <div className="min-w-0">
                <p className="truncate text-[15px] font-medium">
                  {quiz.title || 'Untitled quiz'}
                </p>
                <p className="mt-0.5 text-[13px] text-stone-500 dark:text-stone-400">
                  {quiz.questionCount} questions
                </p>
              </div>

              <div className="flex shrink-0 items-center gap-3">
                <Badge tone={quiz.status === 'COMPLETED' ? 'green' : 'blue'}>
                  {quiz.status}
                </Badge>

                {quiz.score != null && (
                  <span className="text-sm font-semibold tabular-nums">
                    {Math.round(quiz.score)}%
                  </span>
                )}

                <Link to={`/quiz/${quiz.id}`}>
                  <Button variant="outline">
                    {quiz.status === 'COMPLETED' ? 'Review' : 'Continue'}
                  </Button>
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

export function QuizTakePage() {
  const { quizId = '' } = useParams();

  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [idx, setIdx] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [text, setText] = useState('');
  const [result, setResult] = useState<AnswerResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);
  const [finalScore, setFinalScore] = useState<number | null>(null);

  useEffect(() => {
    quizApi
      .get(quizId)
      .then(setQuiz)
      .catch((e) => setError(apiErrorMessage(e)));
  }, [quizId]);

  const q = quiz?.questions[idx];
  const isMcq = q?.type === 'MCQ';

  async function submit() {
    if (!q) return;

    setBusy(true);
    setError(null);

    try {
      const answer = await quizApi.answer(
        quizId,
        q.id,
        isMcq
          ? {
              selectedOption: selected,
              answerText: null,
            }
          : {
              selectedOption: null,
              answerText: text,
            }
      );

      setResult(answer);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not submit answer.'));
    } finally {
      setBusy(false);
    }
  }

  async function next() {
    if (quiz && idx < quiz.questions.length - 1) {
      setIdx(idx + 1);
      setSelected(null);
      setText('');
      setResult(null);
      setError(null);
      return;
    }

    try {
      setBusy(true);
      setError(null);

      const completion = await quizApi.complete(quizId);

      setFinalScore(completion.score);
      setDone(true);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not complete quiz.'));
    } finally {
      setBusy(false);
    }
  }

  if (error && !quiz) {
    return (
      <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
        {error}
      </p>
    );
  }

  if (!quiz) {
    return <Skeleton className="h-64" />;
  }

  if (done) {
    const passed = (finalScore ?? 0) >= 70;
    return (
      <Card className="mx-auto max-w-lg p-8 text-center sm:p-10">
        <span className={cn('mx-auto flex h-14 w-14 items-center justify-center rounded-2xl',
          passed ? 'bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300' : 'bg-amber-100/80 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300')}>
          <CheckCircle2 size={28} />
        </span>

        <h1 className="mt-4 text-xl font-semibold tracking-tight">
          Quiz complete
        </h1>

        <p className="mt-3 text-5xl font-semibold tracking-tight tabular-nums">
          {finalScore != null ? `${Math.round(finalScore)}%` : 'Completed'}
        </p>
        <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">
          {quiz.questions.length} question{quiz.questions.length === 1 ? '' : 's'} answered · Mastery updated on the backend
        </p>

        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <Link to="/dashboard">
            <Button>Back to dashboard</Button>
          </Link>
          {quiz && (
            <Link to="/dashboard">
              <Button variant="outline"><RotateCcw size={14} /> Keep studying</Button>
            </Link>
          )}
        </div>
      </Card>
    );
  }

  if (!q) {
    return (
      <Card className="p-6">
        <p className="text-sm text-stone-600 dark:text-stone-300">
          This quiz has no questions.
        </p>
      </Card>
    );
  }

  const correct = result != null && result.score >= 60;

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/dashboard"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-stone-500 hover:text-forest-700 dark:text-stone-400 dark:hover:text-forest-300"
      >
        <ArrowLeft size={14} />
        Exit quiz
      </Link>

      <div className="mb-2 flex items-center justify-between text-sm">
        <span className="font-medium tabular-nums">
          Question {idx + 1} <span className="font-normal text-stone-400">of {quiz.questions.length}</span>
        </span>
        <span className="flex gap-1.5">
          {q.conceptName && <Badge tone="blue">{q.conceptName}</Badge>}
          {q.difficulty && <Badge>{q.difficulty}</Badge>}
        </span>
      </div>
      <Progress value={((idx + (result ? 1 : 0)) / Math.max(1, quiz.questions.length)) * 100} className="mb-4" />

      <Card className="p-6 sm:p-7">
        <p className="text-[17px] font-medium leading-relaxed">{q.prompt}</p>

        {isMcq ? (
          <div className="mt-5 space-y-2.5">
            {(q.options ?? []).map((opt, i) => {
              const letter = String.fromCharCode(65 + i);
              const active = selected === letter;

              return (
                <button
                  key={i}
                  disabled={!!result}
                  onClick={() => setSelected(letter)}
                  className={cn(
                    'flex w-full items-start gap-3 rounded-xl border px-4 py-3 text-left text-[15px] transition-all',
                    active
                      ? 'border-forest-600 bg-forest-50 shadow-card ring-1 ring-forest-500/30 dark:border-forest-500 dark:bg-forest-950/40'
                      : 'border-stone-900/[0.09] hover:border-forest-400 hover:bg-moss-50/60 dark:border-white/10 dark:hover:border-forest-700 dark:hover:bg-forest-950/20',
                    result && 'cursor-default opacity-90'
                  )}
                >
                  <span className={cn(
                    'flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold',
                    active ? 'bg-forest-700 text-white dark:bg-forest-500' : 'bg-stone-900/[0.06] text-stone-500 dark:bg-white/10 dark:text-stone-400'
                  )}>
                    {letter}
                  </span>
                  <span className="leading-relaxed">{opt.replace(/^[A-D]\)\s*/, '')}</span>
                </button>
              );
            })}
          </div>
        ) : (
          <Textarea
            rows={5}
            className="mt-5"
            placeholder="Write your answer…"
            value={text}
            onChange={(e) => setText(e.target.value)}
            disabled={!!result}
          />
        )}

        {result && (
          <div className={cn('mt-5 rounded-xl border p-4 text-sm',
            correct
              ? 'border-forest-600/25 bg-forest-50 dark:border-forest-800/60 dark:bg-forest-950/30'
              : 'border-red-200 bg-red-50/60 dark:border-red-900/40 dark:bg-red-950/30')}>
            <p className="flex items-center gap-2 font-semibold">
              {correct
                ? <CheckCircle2 size={16} className="text-forest-600 dark:text-forest-400" />
                : <XCircle size={16} className="text-red-500" />}
              Score: {result.score}/100
            </p>

            {result.feedback && (
              <p className="mt-1.5 leading-relaxed text-stone-600 dark:text-stone-300">
                {result.feedback}
              </p>
            )}
          </div>
        )}

        {error && (
          <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/50 dark:text-red-300">
            {error}
          </p>
        )}

        <div className="mt-6 flex justify-end gap-2">
          {!result ? (
            <Button
              onClick={() => void submit()}
              disabled={busy || (isMcq ? !selected : !text.trim())}
              className="px-6"
            >
              {busy ? 'Checking…' : 'Submit answer'}
            </Button>
          ) : (
            <Button onClick={() => void next()} disabled={busy} className="px-6">
              {busy ? 'Completing…' : idx < quiz.questions.length - 1 ? 'Next question' : 'Finish quiz'}
            </Button>
          )}
        </div>
      </Card>

      <div className="mt-4 flex items-center justify-center gap-1 text-xs text-stone-400">
        {quiz.questions.map((_, i) => (
          <span key={i} className={cn('h-1.5 rounded-full transition-all', i < idx || (i === idx && result) ? 'w-6 bg-forest-500' : i === idx ? 'w-6 bg-forest-300 dark:bg-forest-700' : 'w-2.5 bg-stone-300 dark:bg-stone-700')} />
        ))}
      </div>
    </div>
  );
}
