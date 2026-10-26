import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMe, login as loginApi } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import type { UserInfo } from '@/api/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => {
    return !!userInfo.value?.roleCodes?.includes('ADMIN')
  })

  async function login(username: string, password: string): Promise<void> {
    const res = await loginApi({ username, password })
    token.value = res.token
    setToken(res.token)
    userInfo.value = res.user || null
    // roleCodes 可能为空,登录后拉取一次完整信息
    if (!userInfo.value?.roleCodes) {
      await fetchMe().catch(() => undefined)
    }
  }

  async function fetchMe(): Promise<UserInfo | null> {
    if (!token.value) return null
    const me = await getMe()
    userInfo.value = me
    return me
  }

  function logout(): void {
    token.value = ''
    userInfo.value = null
    removeToken()
  }

  return { token, userInfo, isLoggedIn, isAdmin, login, fetchMe, logout }
})
