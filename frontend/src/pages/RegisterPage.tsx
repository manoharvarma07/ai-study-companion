import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GraduationCap } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { apiErrorMessage } from '../api/client';
import { Button, Card, Input } from '../components/ui';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (password !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    setError(null);
    setBusy(true);
    try {
      await register(name.trim(), email.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      const msg = apiErrorMessage(err, 'Registration failed.');
      // If backend registers but doesn't return a token, redirect to login.
      if (/login|token/i.test(msg)) navigate('/login', { replace: true });
      else setError(msg);
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
          <h1 className="text-xl font-semibold tracking-tight">Create account</h1>
          <p className="mb-6 mt-1 text-sm text-stone-500 dark:text-stone-400">Start your learning workspace in seconds.</p>
          <form onSubmit={onSubmit} className="space-y-4">
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Name</label>
              <Input required minLength={2} value={name} onChange={(e) => setName(e.target.value)} placeholder="Ada Learner" />
            </div>
            <div>
              <label className="mb-1.5 block text-[13px] font-medium">Email</label>
              <Input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label className="mb-1.5 block text-[13px] font-medium">Password</label>
                <Input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
              </div>
              <div>
                <label className="mb-1.5 block text-[13px] font-medium">Confirm</label>
                <Input type="password" required value={confirm} onChange={(e) => setConfirm(e.target.value)} placeholder="••••••••" />
              </div>
            </div>
            {error && <p className="rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700 dark:bg-red-950/50 dark:text-red-300">{error}</p>}
            <Button type="submit" disabled={busy} className="w-full py-2.5">{busy ? 'Creating…' : 'Create account'}</Button>
          </form>
          <p className="mt-5 text-center text-sm text-stone-500 dark:text-stone-400">
            Have an account? <Link to="/login" className="font-medium text-forest-700 hover:underline dark:text-forest-300">Login</Link>
          </p>
        </Card>
      </div>
    </div>
  );
}
