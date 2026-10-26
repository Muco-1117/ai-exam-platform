import request from '@/api/request'
import type { LoginResult, UserInfo } from '@/api/types'

export function login(data: { username: string; password: string }): Promise<LoginResult> {
  return request.post<LoginResult>('/auth/login', data)
}

export function register(data: {
  username: string
  password: string
  nickname: string
}): Promise<LoginResult> {
  return request.post<LoginResult>('/auth/register', data)
}

export function getMe(): Promise<UserInfo> {
  return request.get<UserInfo>('/user/me')
}
