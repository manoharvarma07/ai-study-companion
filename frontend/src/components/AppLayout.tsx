import { useState, type ReactNode } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  BarChart3, BookOpen, Brain, ChevronLeft, GraduationCap, LayoutDashboard,
  Library, LineChart, Menu, MessagesSquare, Moon, Search, Settings, Sun, Target, X,
} from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../theme/ThemeContext';
import { cn } from '../lib/utils';

function projectIdFromPath(pathname: string): string | null {
  const m = pathname.match(/^\/projects\/([^/]+)/);
  return m ? m[1] : null;
}

function SidebarContent({ collapsed, projectId, isAdmin, onNavigate }: { collapsed: boolean; projectId: string | null; isAdmin: boolean; onNavigate?: () => void }) {
  const groups: { heading: string; items: { to: string; label: string; icon: typeof LayoutDashboard }[] }[] = [
    {
      heading: 'Workspace',
      items: [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/spaces', label: 'Spaces', icon: Library },
      ],
    },
  ];
  if (projectId) {
    groups.push({
      heading: 'Learning',
      items: [
        { to: `/projects/${projectId}/tutor`, label: 'AI Tutor', icon: MessagesSquare },
        { to: `/projects/${projectId}/quiz`, label: 'Quizzes', icon: Target },
        { to: `/projects/${projectId}/mastery`, label: 'Mastery', icon: Brain },
        { to: `/projects/${projectId}/growth`, label: 'Growth', icon: LineChart },
      ],
    });
    groups.push({
      heading: 'Insights',
      items: [{ to: `/projects/${projectId}/analytics`, label: 'Analytics', icon: BarChart3 }],
    });
  }
  if (isAdmin) {
    groups.push({
      heading: 'System',
      items: [{ to: '/admin', label: 'Admin', icon: Settings }],
    });
  }

  return (
    <div className="flex h-full flex-col">
      <Link to="/dashboard" className="flex items-center gap-3 px-4 pb-5 pt-6" onClick={onNavigate}>
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-forest-500 via-forest-700 to-forest-900 text-white shadow-[0_8px_20px_-6px_rgba(47,79,48,0.6)]">
          <GraduationCap size={20} />
        </span>
        {!collapsed && (
          <span className="leading-tight">
            <span className="block text-[15px] font-bold tracking-tight text-ink dark:text-stone-50">Study Companion</span>
            <span className="mt-0.5 flex items-center gap-1 text-[11px] font-medium text-forest-700 dark:text-forest-300">
              <span className="h-1.5 w-1.5 rounded-full bg-forest-500" /> Calm learning AI
            </span>
          </span>
        )}
      </Link>
      <nav className="flex-1 space-y-6 overflow-y-auto px-3 pb-4">
        {groups.map((g) => (
          <div key={g.heading}>
            {!collapsed && (
              <p className="mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.16em] text-stone-500/90 dark:text-stone-500">
                {g.heading}
              </p>
            )}
            <div className="space-y-1">
              {g.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={onNavigate}
                  title={collapsed ? item.label : undefined}
                  className={({ isActive }) =>
                    cn(
                      'group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all duration-150',
                      isActive
                        ? 'bg-forest-100 text-forest-900 shadow-[inset_0_1px_0_rgba(255,255,255,0.7),0_2px_8px_-4px_rgba(47,79,48,0.4)] dark:bg-forest-900/60 dark:text-forest-100'
                        : 'text-stone-600 hover:bg-stone-900/[0.05] hover:text-ink dark:text-stone-400 dark:hover:bg-white/[0.06] dark:hover:text-stone-100',
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      {isActive && (
                        <span className="absolute left-0 top-1/2 h-6 w-1 -translate-y-1/2 rounded-r-full bg-forest-600 dark:bg-forest-400" />
                      )}
                      <item.icon size={18} strokeWidth={isActive ? 2.4 : 2} className={cn('shrink-0', isActive ? 'text-forest-700 dark:text-forest-300' : 'text-stone-400 group-hover:text-stone-600 dark:text-stone-500')} />
                      {!collapsed && item.label}
                    </>
                  )}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
        {!collapsed && (
          <div className="rounded-2xl border border-forest-900/[0.08] bg-gradient-to-b from-moss-50 to-cream-card p-4 text-xs leading-relaxed text-stone-500 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)] dark:border-white/[0.06] dark:from-white/[0.04] dark:to-transparent dark:text-stone-400">
            <p className="mb-1.5 flex items-center gap-1.5 font-bold text-ink dark:text-stone-200"><BookOpen size={14} className="text-forest-600 dark:text-forest-400" /> Learning loop</p>
            <p>Space → Project → Material → Tutor → Quiz → Mastery → Growth</p>
          </div>
        )}
      </nav>
    </div>
  );
}

export function AppLayout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const projectId = projectIdFromPath(location.pathname);
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="min-h-screen bg-cream text-ink dark:bg-bark dark:text-stone-200">
      {/* Top bar */}
      <header className="sticky top-0 z-30 flex h-16 items-center gap-2 border-b border-stone-900/[0.07] bg-cream-soft/85 px-4 shadow-[0_1px_12px_-6px_rgba(62,53,40,0.25)] backdrop-blur-md sm:px-6 dark:border-white/[0.07] dark:bg-bark/85">
        <button className="rounded-xl p-2 text-stone-600 hover:bg-stone-900/[0.05] lg:hidden dark:text-stone-300 dark:hover:bg-white/[0.06]" onClick={() => setMobileOpen(true)} aria-label="Open navigation">
          <Menu size={19} />
        </button>
        <button className="hidden rounded-xl p-2 text-stone-500 hover:bg-stone-900/[0.05] lg:block dark:text-stone-400 dark:hover:bg-white/[0.06]" onClick={() => setCollapsed((c) => !c)} aria-label="Toggle sidebar">
          {collapsed ? <Menu size={19} /> : <ChevronLeft size={19} />}
        </button>
        <Link to="/dashboard" className="flex items-center gap-2 text-[15px] font-bold tracking-tight lg:hidden">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-gradient-to-br from-forest-500 to-forest-800 text-white">
            <GraduationCap size={15} />
          </span>
          Study Companion
        </Link>
        <div className="mx-auto hidden w-full max-w-md items-center gap-2.5 rounded-2xl border border-stone-900/[0.08] bg-white px-4 py-2 text-sm text-stone-400 shadow-[inset_0_1px_3px_rgba(62,53,40,0.06)] sm:flex dark:border-white/10 dark:bg-white/[0.04] dark:text-stone-500">
          <Search size={15} className="shrink-0" />
          <span className="truncate text-[13px]">Search spaces, projects, concepts…</span>
          <kbd className="ml-auto hidden rounded-md bg-stone-900/[0.06] px-1.5 py-0.5 font-mono text-[11px] text-stone-500 lg:block dark:bg-white/10 dark:text-stone-400">⌘K</kbd>
        </div>
        <div className="ml-auto flex items-center gap-1.5">
          <button className="rounded-xl p-2.5 text-stone-500 transition-colors hover:bg-stone-900/[0.05] hover:text-ink dark:text-stone-400 dark:hover:bg-white/[0.06] dark:hover:text-stone-100" onClick={toggle} aria-label="Toggle color theme">
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>
          <div className="ml-1 flex items-center gap-2.5 rounded-2xl border border-stone-900/[0.08] bg-white py-1 pl-1 pr-3 shadow-[0_2px_10px_-4px_rgba(62,53,40,0.3)] dark:border-white/10 dark:bg-white/[0.04]">
            <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-forest-500 to-forest-800 text-sm font-bold text-white">
              {(user?.name ?? 'S')[0].toUpperCase()}
            </span>
            <div className="hidden sm:block">
              <p className="max-w-28 truncate text-[13px] font-semibold leading-tight">{user?.name}</p>
              <button
                className="text-xs font-medium text-stone-400 transition-colors hover:text-forest-700 dark:hover:text-forest-300"
                onClick={() => { logout(); navigate('/login'); }}
              >
                Sign out
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="flex">
        {/* Desktop sidebar */}
        <aside className={cn('sticky top-16 hidden h-[calc(100vh-4rem)] shrink-0 border-r border-stone-900/[0.07] bg-[#EFE9DB] transition-all duration-200 lg:block dark:border-white/[0.06] dark:bg-[#211C17]', collapsed ? 'w-[4.75rem]' : 'w-64')}>
          <SidebarContent collapsed={collapsed} projectId={projectId} isAdmin={isAdmin} />
        </aside>

        {/* Mobile drawer */}
        {mobileOpen && (
          <div className="fixed inset-0 z-40 lg:hidden">
            <div className="absolute inset-0 bg-stone-950/50 backdrop-blur-[2px]" onClick={() => setMobileOpen(false)} />
            <aside className="absolute left-0 top-0 h-full w-72 overflow-y-auto rounded-r-3xl bg-[#EFE9DB] shadow-lift dark:bg-[#211C17]">
              <div className="flex justify-end p-2">
                <button className="rounded-xl p-2 hover:bg-stone-900/[0.05] dark:hover:bg-white/[0.06]" onClick={() => setMobileOpen(false)} aria-label="Close navigation">
                  <X size={18} />
                </button>
              </div>
              <SidebarContent collapsed={false} projectId={projectId} isAdmin={isAdmin} onNavigate={() => setMobileOpen(false)} />
            </aside>
          </div>
        )}

        <main className="min-w-0 flex-1 px-4 py-7 sm:px-6 lg:px-10">
          <div className="mx-auto w-full max-w-6xl">{children}</div>
        </main>
      </div>
    </div>
  );
}
