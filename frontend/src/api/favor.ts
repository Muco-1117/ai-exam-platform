import request from '@/api/request'
import type { FavorItem, PageResult } from '@/api/types'

export function addFavor(questionId: number): Promise<boolean> {
  return request.post<boolean>(`/favor/${questionId}`)
}

export function removeFavor(questionId: number): Promise<boolean> {
  return request.delete<boolean>(`/favor/${questionId}`)
}

export function getFavorPage(params: { page?: number; size?: number }): Promise<PageResult<FavorItem>> {
  return request.get<PageResult<FavorItem>>('/favor/page', params)
}

export function addThumb(questionId: number): Promise<boolean> {
  return request.post<boolean>(`/thumb/${questionId}`)
}

export function getThumbCount(questionId: number): Promise<number> {
  return request.get<number>('/thumb/count', { questionId })
}

export function getMyThumbs(): Promise<number[]> {
  return request.get<number[]>('/thumb/mine')
}
