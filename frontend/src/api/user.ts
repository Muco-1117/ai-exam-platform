import request from '@/api/request'
import type { PageResult, UserAdminItem } from '@/api/types'

export function getUserPage(params: {
  username?: string
  page?: number
  size?: number
}): Promise<PageResult<UserAdminItem>> {
  return request.get<PageResult<UserAdminItem>>('/user/page', params)
}

export function setUserStatus(id: number, status: number): Promise<boolean> {
  return request.put<boolean>(`/user/${id}/status`, undefined, { status })
}

export function setUserRole(id: number, roleId: number): Promise<boolean> {
  return request.put<boolean>(`/user/${id}/role`, undefined, { roleId })
}
