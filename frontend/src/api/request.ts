import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import type { ApiResponse } from '@/api/types'

/**
 * axios 封装:
 * - 请求拦截器自动携带 Authorization: Bearer <token>
 * - 响应拦截器统一处理:code=200 返回 data;其他 code 弹 ElMessage.error;
 *   401 清除 token 并跳转登录页;HTTP/网络错误同样统一提示。
 * - 直接返回后端 data 字段,业务层无需再解包。
 */

const instance = axios.create({
  baseURL: '/api',
  timeout: 20000
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function gotoLogin(): void {
  removeToken()
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

instance.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        return res.data
      }
      if (res.code === 401) {
        ElMessage.error(res.message || '登录已过期,请重新登录')
        gotoLogin()
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response.data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期,请重新登录')
        gotoLogin()
      } else {
        const msg =
          (error.response.data && (error.response.data.message as string)) ||
          `请求失败(${status})`
        ElMessage.error(msg)
      }
    } else {
      ElMessage.error('网络异常,请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

/** 泛型请求方法,直接返回后端 data */
const request = {
  get<T = unknown>(url: string, params?: object): Promise<T> {
    return instance.get(url, { params }) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown): Promise<T> {
    return instance.post(url, data) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, params?: object): Promise<T> {
    return instance.put(url, data, { params }) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, params?: object): Promise<T> {
    return instance.delete(url, { params }) as unknown as Promise<T>
  }
}

export default request
