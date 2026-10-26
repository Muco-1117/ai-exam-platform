import request from '@/api/request'
import type { AiLog, PageResult } from '@/api/types'

export function generateQuestions(data: {
  categoryId: number
  type: string
  difficulty: number
  count: number
  knowledge?: string
}): Promise<{ logId: number }> {
  return request.post<{ logId: number }>('/ai/generate', data)
}

export function getAiLogDetail(logId: number): Promise<AiLog> {
  return request.get<AiLog>(`/ai/log/${logId}`)
}

export function getAiLogPage(params: { page?: number; size?: number }): Promise<PageResult<AiLog>> {
  return request.get<PageResult<AiLog>>('/ai/log/page', params)
}

export function confirmAiGenerate(logId: number, confirm: boolean): Promise<boolean> {
  return request.post<boolean>(`/ai/generate/${logId}/confirm`, { confirm })
}
