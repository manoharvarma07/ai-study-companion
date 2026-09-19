import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center text-center">
      <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-forest-100 text-forest-700 dark:bg-forest-900/50 dark:text-forest-300">
        <Compass size={26} />
      </span>
      <p className="mt-4 text-4xl font-semibold tracking-tight">404</p>
      <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">This page doesn&apos;t exist. Let&apos;s get you back on track.</p>
      <Link to="/dashboard" className="mt-4 text-sm font-medium text-forest-700 hover:underline dark:text-forest-300">Go to dashboard</Link>
    </div>
  );
}
