import request from '@/api/request'
import type { PageResult, Question, QuestionOption } from '@/api/types'

export interface QuestionPageParams {
  categoryId?: number
  type?: string
  difficulty?: number
  keyword?: string
  reviewStatus?: number
  page?: number
  size?: number
}

export interface QuestionSavePayload {
  categoryId: number
  type: string
  content: string
  options: QuestionOption[] | null
  answer: string
  analysis?: string
  difficulty: number
  score: number
}

export function getQuestionPage(params: QuestionPageParams): Promise<PageResult<Question>> {
  return request.get<PageResult<Question>>('/question/page', params)
}

export function getQuestionDetail(id: number): Promise<Question> {
  return request.get<Question>(`/question/${id}`)
}

export function addQuestion(data: QuestionSavePayload): Promise<boolean> {
  return request.post<boolean>('/question', data)
}

export function updateQuestion(id: number, data: QuestionSavePayload): Promise<boolean> {
  return request.put<boolean>(`/question/${id}`, data)
}

export function deleteQuestion(id: number): Promise<boolean> {
  return request.delete<boolean>(`/question/${id}`)
}

export function reviewQuestion(id: number, status: number): Promise<boolean> {
  return request.post<boolean>(`/question/${id}/review`, { status })
}
