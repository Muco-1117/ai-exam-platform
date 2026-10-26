import request from '@/api/request'
import type { Category } from '@/api/types'

export function getCategoryList(): Promise<Category[]> {
  return request.get<Category[]>('/category/list')
}

export function addCategory(data: { name: string; description?: string }): Promise<boolean> {
  return request.post<boolean>('/category', data)
}

export function updateCategory(id: number, data: { name: string; description?: string }): Promise<boolean> {
  return request.put<boolean>(`/category/${id}`, data)
}

export function deleteCategory(id: number): Promise<boolean> {
  return request.delete<boolean>(`/category/${id}`)
}
