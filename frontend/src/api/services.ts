import { api, getToken, isNetworkError, mocksEnabled } from './client';
import * as mock from './mock';
import type {
  AdminOverview, AdminUserDetail, AdminUsersResponse, AiUsageByFeature, AiUsageByModel,
  AiUsageByUser, AiUsageFilterOptions, AiUsageRecentResponse, AiUsageSummary, AiUsageTimelinePoint,
  AnswerResult, AuthResponse, ChatMessage, ChatResponse, Conversation, Growth,
  LearningContext, Mastery, Material, OverviewAnalytics, Project, ProjectAnalytics,
  QuizDetail, QuizSummary, Recommendation, Space, User,
} from './types';

async function withMock<T>(fn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    // Only fall back to mock data when the backend is unreachable.
    // Real HTTP errors (400/401/403/404/409/500) always propagate so
    // validation, auth, and error states can be verified for real.
    if (isNetworkError(err) && mocksEnabled()) return fallback;
    throw err;
  }
}

// ---------- Auth ----------
// Real backend auth is the source of truth. The demo fallback below only
// activates when VITE_USE_MOCKS=true AND the backend is unreachable.
export const authApi = {
  async login(email: string, password: string): Promise<AuthResponse> {
    try {
      const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password });
      return data;
    } catch (err) {
      if (
        isNetworkError(err) &&
        mocksEnabled() &&
        email.trim().toLowerCase() === mock.mockDemoEmail &&
        password === mock.mockDemoPassword
      ) {
        return { token: mock.mockDemoToken, user: mock.mockUser };
      }
      throw err;
    }
  },
  async register(name: string, email: string, password: string): Promise<AuthResponse> {
    try {
      const { data } = await api.post<AuthResponse>('/api/auth/register', { name, email, password });
      return data;
    } catch (err) {
      if (isNetworkError(err) && mocksEnabled()) {
        return { token: mock.mockDemoToken, user: { ...mock.mockUser, name: name.trim(), email: email.trim() } };
      }
      throw err;
    }
  },
  async me(): Promise<User> {
    try {
      const { data } = await api.get<User>('/api/auth/me');
      return data;
    } catch (err) {
      if (isNetworkError(err) && mocksEnabled() && getToken() === mock.mockDemoToken) return mock.mockUser;
      throw err;
    }
  },
};

// ---------- Spaces ----------
export const spacesApi = {
  list: () => withMock(async () => (await api.get<Space[]>('/api/spaces')).data, mock.mockSpaces),
  create: (name: string, description?: string) =>
    withMock(async () => (await api.post<Space>('/api/spaces', { name, description })).data,
      { id: `s-${Date.now()}`, name, description }),
  get: (id: string) =>
    withMock(async () => (await api.get<Space>(`/api/spaces/${id}`)).data,
      mock.mockSpaces.find((s) => s.id === id) ?? mock.mockSpaces[0]),
  update: (id: string, name: string, description?: string) =>
    withMock(async () => (await api.put<Space>(`/api/spaces/${id}`, { name, description })).data,
      { id, name, description }),
  remove: (id: string) =>
    withMock(async () => { await api.delete(`/api/spaces/${id}`); }, undefined as void),
};

// ---------- Projects ----------
export const projectsApi = {
  list: (spaceId: string) =>
    withMock(async () => (await api.get<Project[]>(`/api/spaces/${spaceId}/projects`)).data,
      mock.mockProjects.filter((p) => p.spaceId === spaceId)),
  create: (spaceId: string, payload: { name: string; description?: string; goal?: string }) =>
    withMock(async () => (await api.post<Project>(`/api/spaces/${spaceId}/projects`, payload)).data,
      { id: `p-${Date.now()}`, spaceId, ...payload }),
  get: (projectId: string) =>
    withMock(async () => (await api.get<Project>(`/api/projects/${projectId}`)).data,
      mock.mockProjects.find((p) => p.id === projectId) ?? mock.mockProjects[0]),
  update: (projectId: string, payload: { name: string; description?: string; goal?: string }) =>
    withMock(async () => (await api.put<Project>(`/api/projects/${projectId}`, payload)).data,
      { ...(mock.mockProjects.find((p) => p.id === projectId) ?? mock.mockProjects[0]), ...payload }),
  remove: (projectId: string) =>
    withMock(async () => { await api.delete(`/api/projects/${projectId}`); }, undefined as void),
};

// ---------- Materials ----------
export const materialsApi = {
  list: (projectId: string) =>
    withMock(async () => (await api.get<Material[]>(`/api/projects/${projectId}/materials`)).data, mock.mockMaterials),
  upload: async (projectId: string, file: File, onProgress?: (pct: number) => void): Promise<Material> => {
    const form = new FormData();
    form.append('file', file);
    try {
      const { data } = await api.post<Material>(`/api/projects/${projectId}/materials`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
          if (e.total && onProgress) onProgress(Math.round((e.loaded / e.total) * 100));
        },
      });
      return data;
    } catch (err) {
      if (isNetworkError(err) && mocksEnabled()) {
        return { id: `m-${Date.now()}`, filename: file.name, contentType: file.type || 'application/pdf', fileSize: file.size, status: 'PROCESSING', processingAttempts: 0, createdAt: new Date().toISOString() };
      }
      throw err;
    }
  },
  retry: (projectId: string, materialId: string) =>
    withMock(async () => (await api.post<Material>(`/api/projects/${projectId}/materials/${materialId}/retry`)).data,
      mock.mockMaterials[0]),
};

// ---------- Tutor ----------
export const tutorApi = {
  chat: (projectId: string, message: string, conversationId?: string | null): Promise<ChatResponse> =>
    withMock(
      async () => (await api.post<ChatResponse>(`/api/projects/${projectId}/tutor/chat`, { conversationId: conversationId ?? null, message })).data,
      { conversationId: conversationId ?? 'c1', answer: '[dev-mock] Backend unreachable — showing placeholder. Start the Spring Boot API for grounded AI answers.', grounded: false, citations: [] },
    ),
  conversations: (projectId: string) =>
    withMock(async () => (await api.get<Conversation[]>(`/api/projects/${projectId}/tutor/conversations`)).data, mock.mockConversations),
  messages: (conversationId: string) =>
    withMock(async () => (await api.get<ChatMessage[]>(`/api/conversations/${conversationId}/messages`)).data, mock.mockMessages),
};

// ---------- Quiz ----------
export const quizApi = {
  list: (projectId: string) =>
    withMock(async () => (await api.get<QuizSummary[]>(`/api/projects/${projectId}/quizzes`)).data, mock.mockQuizzes),
  create: (projectId: string, opts?: { title?: string | null; mcqCount: number; openCount: number }) =>
    withMock(async () => (await api.post<QuizDetail>(`/api/projects/${projectId}/quizzes`, opts ?? { title: null, mcqCount: 3, openCount: 1 })).data, mock.mockQuizDetail),
  get: (quizId: string) =>
    withMock(async () => (await api.get<QuizDetail>(`/api/quizzes/${quizId}`)).data, mock.mockQuizDetail),
  answer: (quizId: string, questionId: string, payload: { answerText?: string | null; selectedOption?: string | null }) =>
    withMock(async () => (await api.post<AnswerResult>(`/api/quizzes/${quizId}/questions/${questionId}/answers`, payload)).data,
      { answerId: 'a1', score: 80, feedback: '[dev-mock] Placeholder feedback.', understood: [], missing: [] }),
  complete: (quizId: string) =>
    withMock(async () => (await api.post(`/api/quizzes/${quizId}/complete`)).data, { quizId, status: 'COMPLETED', score: 78, answered: 2 }),
};

// ---------- Learning ----------
export const learningApi = {
  mastery: (projectId: string) =>
    withMock(async () => (await api.get<Mastery[]>(`/api/projects/${projectId}/mastery`)).data, mock.mockMastery),
  growth: (projectId: string) =>
    withMock(async () => (await api.get<Growth[]>(`/api/projects/${projectId}/growth`)).data, mock.mockGrowth),
  recommendations: (projectId: string) =>
    withMock(async () => (await api.get<Recommendation[]>(`/api/projects/${projectId}/recommendations`)).data, mock.mockRecs),
  generateRecommendation: (projectId: string) =>
    withMock(async () => (await api.post<Recommendation>(`/api/projects/${projectId}/recommendations`)).data, mock.mockRecs[0]),
  context: (projectId: string) =>
    withMock(async () => (await api.get<LearningContext>(`/api/projects/${projectId}/context`)).data,
      { goals: 'Master ML', strengths: 'Linear models', weaknesses: 'Regularization', repeatedMistakes: '', assessmentSummary: '' }),
};

// ---------- Analytics / Admin ----------
export interface AdminUsersParams {
  page?: number;
  size?: number;
  search?: string;
  role?: string;
  sort?: string;
  direction?: string;
}

export interface AiUsageQuery {
  days?: string;
  feature?: string;
  provider?: string;
  model?: string;
  userId?: string;
  status?: string;
  page?: number;
  size?: number;
}

function aiUsageParams(q: AiUsageQuery): Record<string, string | number> {
  const out: Record<string, string | number> = {};
  if (q.days) out.days = q.days;
  if (q.feature && q.feature !== 'ALL') out.feature = q.feature;
  if (q.provider && q.provider !== 'ALL') out.provider = q.provider;
  if (q.model && q.model !== 'ALL') out.model = q.model;
  if (q.userId && q.userId !== 'ALL') out.userId = q.userId;
  if (q.status && q.status !== 'ALL') out.status = q.status;
  if (q.page !== undefined) out.page = q.page;
  if (q.size !== undefined) out.size = q.size;
  return out;
}

export const adminApi = {
  overview: async (): Promise<AdminOverview> =>
    (await api.get<AdminOverview>('/api/admin/overview')).data,
  users: async (p: AdminUsersParams = {}): Promise<AdminUsersResponse> =>
    (await api.get<AdminUsersResponse>('/api/admin/users', {
      params: {
        page: p.page ?? 0,
        size: p.size ?? 50,
        ...(p.search ? { search: p.search } : {}),
        ...(p.role && p.role !== 'ALL' ? { role: p.role } : {}),
        sort: p.sort ?? 'createdAt',
        direction: p.direction ?? 'desc',
      },
    })).data,
  userDetail: async (id: string): Promise<AdminUserDetail> =>
    (await api.get<AdminUserDetail>(`/api/admin/users/${id}`)).data,
  aiSummary: async (q: AiUsageQuery = {}): Promise<AiUsageSummary> =>
    (await api.get<AiUsageSummary>('/api/admin/ai-usage/summary', { params: aiUsageParams(q) })).data,
  aiTimeline: async (q: AiUsageQuery = {}): Promise<AiUsageTimelinePoint[]> =>
    (await api.get<AiUsageTimelinePoint[]>('/api/admin/ai-usage/timeline', {
      params: { days: q.days ?? '30', ...aiUsageParams(q) },
    })).data,
  aiByFeature: async (q: AiUsageQuery = {}): Promise<AiUsageByFeature[]> =>
    (await api.get<AiUsageByFeature[]>('/api/admin/ai-usage/by-feature', { params: aiUsageParams(q) })).data,
  aiByModel: async (q: AiUsageQuery = {}): Promise<AiUsageByModel[]> =>
    (await api.get<AiUsageByModel[]>('/api/admin/ai-usage/by-model', { params: aiUsageParams(q) })).data,
  aiByUser: async (q: AiUsageQuery = {}): Promise<AiUsageByUser[]> =>
    (await api.get<AiUsageByUser[]>('/api/admin/ai-usage/by-user', { params: aiUsageParams(q) })).data,
  aiRecent: async (q: AiUsageQuery = {}): Promise<AiUsageRecentResponse> =>
    (await api.get<AiUsageRecentResponse>('/api/admin/ai-usage/recent', {
      params: { page: q.page ?? 0, size: q.size ?? 50, ...aiUsageParams(q) },
    })).data,
  aiFilterOptions: async (): Promise<AiUsageFilterOptions> =>
    (await api.get<AiUsageFilterOptions>('/api/admin/ai-usage/filter-options')).data,
};

export const analyticsApi = {
  project: (projectId: string) =>
    withMock(async () => (await api.get<ProjectAnalytics>(`/api/projects/${projectId}/analytics`)).data, mock.mockAnalytics),
  overview: () =>
    withMock(async () => (await api.get<OverviewAnalytics>('/api/analytics/overview')).data, mock.mockOverview),
  adminAnalytics: (): Promise<Record<string, unknown>> =>
    withMock(async () => (await api.get('/api/admin/analytics')).data, { totalUsers: 12, totalSpaces: 20, totalProjects: 34, totalMaterials: 58 }),
  adminUsers: (): Promise<unknown> =>
    withMock(async () => (await api.get('/api/admin/users')).data, { content: [{ id: 'u1', name: 'Student', email: 'student@example.com', role: 'USER' }] }),
};
