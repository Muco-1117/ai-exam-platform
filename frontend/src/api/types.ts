/** 与后端统一响应体、分页结构及业务实体对应的类型定义 */

/** 统一响应体 { code, message, data } */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** MyBatis-Plus Page 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

/** 用户对象(脱敏) */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string | null
  email?: string | null
  phone?: string | null
  status?: number
  createTime?: string
  roleCodes?: string[]
  permissions?: string[]
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export interface Category {
  id: number
  name: string
  description?: string | null
}

export interface QuestionOption {
  key: string
  text: string
}

/** 题目(管理端详情含 answer/analysis) */
export interface Question {
  id: number
  categoryId: number
  categoryName?: string
  type: string
  content: string
  options?: QuestionOption[] | null
  answer?: string
  analysis?: string | null
  difficulty: number
  score: number
  source?: string
  reviewStatus?: number
  createTime?: string
}

/** 题目答题卡格式(做题时使用,不含 answer/analysis) */
export interface QuestionCard {
  id: number
  type: string
  content: string
  options?: QuestionOption[] | null
  difficulty: number
  score: number
  categoryId: number
  categoryName?: string
  savedAnswer?: string | null
}

/** 练习判分结果 */
export interface JudgeResult {
  isCorrect: boolean
  correctAnswer: string
  analysis?: string | null
  score: number
}

/** 练习记录(记录含题目简要信息,字段名后端序列化,取可选) */
export interface PracticeRecord {
  id: number
  questionId?: number
  questionContent?: string
  userAnswer?: string
  isCorrect?: boolean | number
  score?: number
  createTime?: string
  [key: string]: unknown
}

export interface PracticeTypeStat {
  type: string
  total: number
  correct: number
}

export interface PracticeStats {
  totalCount: number
  correctCount: number
  correctRate: number
  byType: PracticeTypeStat[]
}

/** 错题本条目(含题目详情与 wrongCount) */
export interface WrongItem {
  id?: number
  questionId: number
  wrongCount?: number
  mastered?: number
  question?: Question | null
  createTime?: string
  [key: string]: unknown
}

export interface ReviewItem {
  questionId: number
  wrongCount?: number
  lastWrongAt?: string | null
  nextReviewAt?: string | null
  question: QuestionCard
}

export interface ReviewSet {
  items: ReviewItem[]
  dueCount: number
  activeCount: number
  limit: number
}

/** 收藏条目 */
export interface FavorItem {
  id?: number
  questionId: number
  question?: Question | null
  createTime?: string
  [key: string]: unknown
}

export interface ExamItem {
  id: number
  recordId?: number | null
  recordStatus?: number | null
  name: string
  categoryId?: number
  categoryName?: string
  duration?: number
  passScore?: number
  totalScore?: number
  questionCount?: number
  status: number
  createTime?: string
  [key: string]: unknown
}

export interface ExamQuestionRef {
  questionId: number
  score: number
}

export interface ExamStartResult {
  recordId: number
  examName: string
  duration: number
  deadline: string
  questions: QuestionCard[]
  totalScore: number
  passScore: number
}

export interface ExamSubmitDetail {
  questionId?: number
  isCorrect?: boolean | number
  score?: number
  gotScore?: number
  userAnswer?: string | null
  correctAnswer?: string
  /** 后端 recordDetail 返回内嵌题目对象 */
  question?: QuestionCard & { answer?: string }
  analysis?: string | null
  content?: string
  [key: string]: unknown
}

export interface ExamSubmitResult {
  totalScore: number
  correctCount: number
  detail: ExamSubmitDetail[]
}

/** 考试记录默认查询本人，管理员可显式查询全部考生。 */
export interface ExamRecordItem {
  id: number
  examId?: number
  examName?: string
  userId?: number
  username?: string
  totalScore?: number
  status?: number
  startTime?: string
  submitTime?: string
  deadline?: string
  [key: string]: unknown
}

/** 考试详情(答卷+判分明细),字段以后端序列化为准,取可选 */
export interface ExamRecordDetail {
  examName?: string
  totalScore?: number
  correctCount?: number
  record?: ExamRecordItem
  questions?: (QuestionCard & { userAnswer?: string })[]
  detail?: ExamSubmitDetail[]
  [key: string]: unknown
}

export interface RankItem {
  userId: number
  nickname: string
  totalScore: number
  submitTime?: string
  [key: string]: unknown
}

/** AI 出题日志 */
export interface AiLog {
  id: number
  status?: number
  errorMsg?: string | null
  categoryId?: number
  categoryName?: string
  type?: string
  difficulty?: number
  count?: number
  knowledge?: string
  createTime?: string
  [key: string]: unknown
}

/** 管理端用户条目(角色以 roleId 1/2 表示) */
export interface UserAdminItem {
  id: number
  username: string
  nickname: string
  avatar?: string | null
  email?: string | null
  phone?: string | null
  status?: number
  roleId?: number
  roleName?: string
  roles?: { id: number; name?: string; code?: string }[]
  createTime?: string
  [key: string]: unknown
}
