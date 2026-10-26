import request from '@/api/request'
import type { JudgeResult, PageResult, PracticeRecord, PracticeStats } from '@/api/types'

export function submitPractice(data: {
  questionId: number
  userAnswer: string
}): Promise<JudgeResult> {
  return request.post<JudgeResult>('/practice/submit', data)
}

export function getPracticePage(params: { page?: number; size?: number }): Promise<PageResult<PracticeRecord>> {
  return request.get<PageResult<PracticeRecord>>('/practice/page', params)
}

export function getPracticeStats(): Promise<PracticeStats> {
  return request.get<PracticeStats>('/practice/stats')
}
