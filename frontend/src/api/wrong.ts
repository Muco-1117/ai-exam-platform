import request from '@/api/request'
import type { PageResult, WrongItem } from '@/api/types'

export function getWrongPage(params: { page?: number; size?: number }): Promise<PageResult<WrongItem>> {
  return request.get<PageResult<WrongItem>>('/wrong/page', params)
}

export function masterWrong(questionId: number): Promise<boolean> {
  return request.put<boolean>(`/wrong/${questionId}/master`)
}
