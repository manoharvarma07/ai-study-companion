// Central typed models. Mirror backend DTOs — frontend never computes these.

export interface User {
  id: string;
  name: string;
  email: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Space {
  id: string;
  name: string;
  description?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface Project {
  id: string;
  spaceId: string;
  name: string;
  description?: string | null;
  goal?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export type MaterialStatus = 'QUEUED' | 'PROCESSING' | 'READY' | 'FAILED';

export interface Material {
  id: string;
  filename: string;
  contentType: string;
  fileSize: number;
  pageCount?: number | null;
  status: MaterialStatus | string;
  processingAttempts: number;
  errorMessage?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface Conversation {
  id: string;
  title: string;
  updatedAt?: string;
}

export interface Citation {
  material?: string;
  page?: number | null;
}

export interface ChatMessage {
  id: string;
  role: 'USER' | 'ASSISTANT' | string;
  content: string;
  grounded?: boolean | null;
  citations?: Citation[];
  createdAt?: string;
}

export interface ChatResponse {
  conversationId: string;
  answer: string;
  grounded: boolean;
  citations: Citation[];
}

export interface QuizQuestion {
  id: string;
  type: string;
  prompt: string;
  options?: string[] | null;
  conceptName?: string | null;
  difficulty?: string | null;
  position: number;
}

export interface QuizDetail {
  id: string;
  title?: string | null;
  status: string;
  score?: number | null;
  questions: QuizQuestion[];
  createdAt?: string;
  completedAt?: string | null;
}

export interface QuizSummary {
  id: string;
  title?: string | null;
  status: string;
  score?: number | null;
  questionCount: number;
  createdAt?: string;
}

export interface AnswerResult {
  answerId: string;
  score: number;
  feedback?: string | null;
  understood?: string[];
  missing?: string[];
}

export interface Mastery {
  conceptId: string;
  conceptName: string;
  masteryScore: number;
  confidence: number;
  updatedAt?: string;
}

export interface Growth {
  conceptId: string;
  conceptName: string;
  masteryScore: number;
  trend: string;
  detail?: string | null;
}

export interface Recommendation {
  id: string;
  text: string;
  reason?: string | null;
  createdAt?: string;
}

export interface LearningContext {
  goals?: string | null;
  strengths?: string | null;
  weaknesses?: string | null;
  repeatedMistakes?: string | null;
  assessmentSummary?: string | null;
}

export interface ProjectAnalytics {
  materials: number;
  materialsReady: number;
  chunks: number;
  concepts: number;
  tutorMessages: number;
  quizzes: number;
  quizAttempts: number;
  averageScore: number;
  masteryDistribution: { strong: number; developing: number; weak: number };
  recentEvents: { type: string; at: string; metadata: string }[];
}

export interface OverviewAnalytics {
  events: { type: string; at: string; projectId: string }[];
  aiUsage: { feature: string; model: string; success: boolean; at: string }[];
}

// ---------- Admin ----------
export interface AdminOverview {
  totalUsers: number;
  activeUsers: number;
  totalSpaces: number;
  totalProjects: number;
  totalMaterials: number;
}

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: string;
  createdAt: string | null;
  spaces: number;
  projects: number;
  materials: number;
  quizzes: number;
  tutorMessages: number;
  lastActivity: string | null;
  status: string;
}

export interface AdminUsersResponse {
  content: AdminUser[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AdminUserDetail extends AdminUser {
  spaceList: { id: string; name: string; createdAt: string | null }[];
  projectList: { id: string; name: string; spaceId: string | null; createdAt: string | null }[];
  recentActivity: { type: string; projectId: string | null; at: string | null }[];
  recentAiUsage: AiUsageEvent[];
  quizCount: number;
  conversationCount: number;
  tutorMessageCount: number;
}

export interface AiUsageSummary {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  successRate: number;
  totalTokens: number;
  inputTokens: number;
  outputTokens: number;
  estimatedCost: number;
  avgLatencyMs: number;
}

export interface AiUsageTimelinePoint {
  date: string;
  requests: number;
  tokens: number;
  cost: number;
}

export interface AiUsageByFeature {
  feature: string;
  requests: number;
  tokens: number;
  cost: number;
  avgLatencyMs: number;
}

export interface AiUsageByModel {
  provider: string;
  model: string;
  requests: number;
  tokens: number;
  successRate: number;
  avgLatencyMs: number;
  cost: number;
  estimatedCost: number;
}

export interface AiUsageByUser {
  userId: string;
  name: string;
  email: string | null;
  requests: number;
  tokens: number;
  cost: number;
  estimatedCost: number;
  lastUsed: string | null;
}

export interface AiUsageEvent {
  id: string;
  time: string | null;
  at: string | null;
  userId: string | null;
  userName: string | null;
  userEmail: string | null;
  projectId: string | null;
  feature: string;
  provider: string;
  model: string;
  latencyMs: number;
  inputTokens: number;
  outputTokens: number;
  tokens: number;
  totalTokens: number;
  cost: number;
  estimatedCost: number;
  success: boolean;
  status: string;
}

export interface AiUsageRecentResponse {
  content: AiUsageEvent[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AiUsageFilterOptions {
  features: string[];
  models: string[];
  providers: string[];
}

export interface AiUsageFilters {
  days: string;
  feature: string;
  provider: string;
  model: string;
  userId: string;
  status: string;
}
