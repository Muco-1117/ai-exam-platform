import request from '@/api/request'
import type {
  ExamItem,
  ExamRecordDetail,
  ExamRecordItem,
  ExamStartResult,
  ExamSubmitResult,
  PageResult,
  RankItem
} from '@/api/types'

export function getPublishedExamList(): Promise<ExamItem[]> {
  return request.get<ExamItem[]>('/exam/list')
}

export function getExamPage(params: { page?: number; size?: number }): Promise<PageResult<ExamItem>> {
  return request.get<PageResult<ExamItem>>('/exam/page', params)
}

export function createExam(data: {
  name: string
  categoryId: number
  duration: number
  passScore: number
  questions: { questionId: number; score: number }[]
}): Promise<boolean> {
  return request.post<boolean>('/exam', data)
}

export function publishExam(id: number): Promise<boolean> {
  return request.put<boolean>(`/exam/${id}/publish`)
}

export function deleteExam(id: number): Promise<boolean> {
  return request.delete<boolean>(`/exam/${id}`)
}

/** 开始考试;同一考试进行中重复调用返回原记录 */
export function startExam(examId: number): Promise<ExamStartResult> {
  return request.post<ExamStartResult>(`/exam/${examId}/start`)
}

/** 保存单题答案草稿(幂等 upsert) */
export function saveExamAnswer(data: {
  recordId: number
  questionId: number
  userAnswer: string
}): Promise<boolean> {
  return request.post<boolean>('/exam/answer', data)
}

export function submitExam(recordId: number): Promise<ExamSubmitResult> {
  return request.post<ExamSubmitResult>(`/exam/record/${recordId}/submit`)
}

export function getExamRecordPage(params: {
  page?: number
  size?: number
  scope?: 'mine' | 'all'
}): Promise<PageResult<ExamRecordItem>> {
  return request.get<PageResult<ExamRecordItem>>('/exam/record/page', params)
}

export function getExamRecordDetail(recordId: number): Promise<ExamRecordDetail> {
  return request.get<ExamRecordDetail>(`/exam/record/${recordId}/detail`)
}

export function getExamRank(examId: number): Promise<RankItem[]> {
  return request.get<RankItem[]>(`/exam/rank/${examId}`)
}
