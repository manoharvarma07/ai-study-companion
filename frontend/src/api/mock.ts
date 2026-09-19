/**
 * ISOLATED DEV FALLBACK DATA.
 * Used only when VITE_USE_MOCKS=true AND the backend is unreachable.
 * The backend is the source of truth — delete this file once integrated.
 * No AI/embedding/mastery logic lives here; these are static placeholders.
 */
import type {
  ChatMessage, Conversation, Growth, Mastery, Material, OverviewAnalytics,
  Project, ProjectAnalytics, QuizDetail, QuizSummary, Recommendation, Space, User,
} from './types';

export const mockUser: User = { id: 'u1', name: 'Student', email: 'student@example.com', role: 'USER' };

// DEV-ONLY demo login. Accepted only when VITE_USE_MOCKS=true AND the backend
// is unreachable. Never used against the real backend. Delete with this file.
export const mockDemoEmail = 'demo@example.com';
export const mockDemoPassword = 'password123';
export const mockDemoToken = 'dev-mock-token';

export const mockSpaces: Space[] = [
  { id: 's1', name: 'Machine Learning', description: 'Core ML theory and practice', createdAt: new Date().toISOString() },
  { id: 's2', name: 'System Design', description: 'Backend interview prep', createdAt: new Date().toISOString() },
];

export const mockProjects: Project[] = [
  { id: 'p1', spaceId: 's1', name: 'ML Fundamentals', description: 'Regression → neural nets', goal: 'Master core ML concepts', createdAt: new Date().toISOString() },
  { id: 'p2', spaceId: 's1', name: 'Deep Learning', description: 'CNNs and transformers', goal: 'Build intuition for DL', createdAt: new Date().toISOString() },
];

export const mockMaterials: Material[] = [
  { id: 'm1', filename: 'ML Notes.pdf', contentType: 'application/pdf', fileSize: 412000, pageCount: 48, status: 'READY', processingAttempts: 1, createdAt: new Date().toISOString() },
  { id: 'm2', filename: 'Linear Regression.pdf', contentType: 'application/pdf', fileSize: 180000, pageCount: 12, status: 'PROCESSING', processingAttempts: 1, createdAt: new Date().toISOString() },
];

export const mockConversations: Conversation[] = [
  { id: 'c1', title: 'Explain gradient descent', updatedAt: new Date().toISOString() },
];

export const mockMessages: ChatMessage[] = [
  { id: 'm1', role: 'USER', content: 'What is gradient descent?', createdAt: new Date().toISOString() },
  { id: 'm2', role: 'ASSISTANT', content: '[dev-mock] Gradient descent iteratively minimizes loss by stepping opposite the gradient. Connect the backend to get grounded AI answers.', grounded: false, citations: [], createdAt: new Date().toISOString() },
];

export const mockQuizzes: QuizSummary[] = [
  { id: 'q1', title: 'ML Basics — 3 MCQ', status: 'COMPLETED', score: 78, questionCount: 3, createdAt: new Date().toISOString() },
];

export const mockQuizDetail: QuizDetail = {
  id: 'q1', title: 'ML Basics — 3 MCQ', status: 'COMPLETED', score: 78,
  questions: [
    { id: 'qq1', type: 'MCQ', prompt: '[dev-mock] What does regularization reduce?', options: ['A) Overfitting', 'B) Data size', 'C) Learning rate', 'D) Epochs'], conceptName: 'Regularization', difficulty: 'MEDIUM', position: 0 },
    { id: 'qq2', type: 'MCQ', prompt: '[dev-mock] Backpropagation computes…', options: ['A) Gradients', 'B) Weights directly', 'C) Labels', 'D) Batches'], conceptName: 'Backpropagation', difficulty: 'MEDIUM', position: 1 },
  ],
  createdAt: new Date().toISOString(),
};

export const mockMastery: Mastery[] = [
  { conceptId: 'c1', conceptName: 'Gradient Descent', masteryScore: 82, confidence: 0.9 },
  { conceptId: 'c2', conceptName: 'Regularization', masteryScore: 41, confidence: 0.5 },
  { conceptId: 'c3', conceptName: 'Backpropagation', masteryScore: 48, confidence: 0.6 },
  { conceptId: 'c4', conceptName: 'Linear Regression', masteryScore: 74, confidence: 0.8 },
];

export const mockGrowth: Growth[] = [
  { conceptId: 'c1', conceptName: 'Gradient Descent', masteryScore: 82, trend: 'IMPROVING', detail: 'Quiz scores trending up' },
  { conceptId: 'c2', conceptName: 'Regularization', masteryScore: 41, trend: 'DECLINING', detail: 'Missed 2 recent questions' },
];

export const mockRecs: Recommendation[] = [
  { id: 'r1', text: 'Review regularization, then attempt a short practice quiz.', reason: 'Lowest mastery concept', createdAt: new Date().toISOString() },
];

export const mockAnalytics: ProjectAnalytics = {
  materials: 4, materialsReady: 3, chunks: 120, concepts: 18, tutorMessages: 27,
  quizzes: 5, quizAttempts: 42, averageScore: 78,
  masteryDistribution: { strong: 8, developing: 6, weak: 4 },
  recentEvents: [
    { type: 'QUIZ_COMPLETED', at: new Date().toISOString(), metadata: '' },
    { type: 'TUTOR_MESSAGE', at: new Date().toISOString(), metadata: '' },
    { type: 'MATERIAL_UPLOADED', at: new Date().toISOString(), metadata: '' },
  ],
};

export const mockOverview: OverviewAnalytics = {
  events: [
    { type: 'QUIZ_COMPLETED', at: new Date().toISOString(), projectId: 'p1' },
    { type: 'TUTOR_MESSAGE', at: new Date().toISOString(), projectId: 'p1' },
  ],
  aiUsage: [],
};
