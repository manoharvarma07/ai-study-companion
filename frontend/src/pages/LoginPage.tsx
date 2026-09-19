import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GraduationCap } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { apiErrorMessage, mocksEnabled } from '../api/client';
import { mockDemoEmail, mockDemoPassword } from '../api/mock';
import { Button, Card, Input } from '../components/ui';

async function loginAction() { return null; }

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err, 'Login failed. Check your credentials.'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-center gap-2.5">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-forest-700 text-white shadow-soft dark:bg-forest-600"><GraduationCap size={21} /></span>
          <p className="text-lg font-semibold tracking-tight">Study Companion</p>
        </div>
        <Card className="p-8">
          <h1 className="text-xl font-semibold tracking-tight">Welcome back</h1>
          <p className="mb-6 mt-1 text-sm text-stone-500 dark:text-stone-400">Sign in to continue learning.</p>
          <form onSubmit={onSubmit} className="space-y-4">
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Email</label>
              <Input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
            </div>
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Password</label>
              <Input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
            </div>
            {error && <p className="rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700 dark:bg-red-950/50 dark:text-red-300">{error}</p>}
            <Button type="submit" disabled={busy} className="w-full py-2.5">{busy ? 'Signing in…' : 'Login'}</Button>
          </form>
          {mocksEnabled() && (
            <p className="mt-4 rounded-lg bg-cream px-3 py-2.5 text-center text-xs text-stone-500 dark:bg-white/[0.04] dark:text-stone-400">
              Demo mode (backend offline): use <span className="font-medium text-stone-700 dark:text-stone-300">{mockDemoEmail}</span> / <span className="font-medium text-stone-700 dark:text-stone-300">{mockDemoPassword}</span>
            </p>
          )}
          <p className="mt-5 text-center text-sm text-stone-500 dark:text-stone-400">
            No account? <Link to="/register" className="font-medium text-forest-700 hover:underline dark:text-forest-300">Create account</Link>
          </p>
        </Card>
        <p className="mt-5 text-center text-xs text-stone-400">A calm space for deep learning.</p>
      </div>
    </div>
  );
}

export { loginAction };
