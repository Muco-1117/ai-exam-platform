import { ElNotification } from 'element-plus'

/**
 * WebSocket 长连接封装
 * 登录后连接 /ws/push?token=<JWT>,断线 5 秒自动重连,
 * 收到服务端推送后弹 ElNotification 全局提示。
 */

let ws: WebSocket | null = null
let reconnectTimer: number | null = null
let closedByUser = false

const MESSAGE_TITLES: Record<string, string> = {
  PRACTICE_RESULT: '刷题判分结果',
  EXAM_SCORED: '考试已自动判分',
  EXAM_FORCE_SUBMIT: '考试已强制交卷'
}

function buildWsUrl(token: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/push?token=${encodeURIComponent(token)}`
}

function handleMessage(event: MessageEvent): void {
  let payload: { type?: string; data?: Record<string, unknown> }
  try {
    payload = JSON.parse(event.data as string)
  } catch {
    return
  }
  const type = payload.type || ''
  const title = MESSAGE_TITLES[type]
  if (!title) return
  const data = payload.data || {}
  let message = ''
  if (type === 'PRACTICE_RESULT') {
    const correct = data.isCorrect === true || data.isCorrect === 1
    message = correct ? '本次作答正确,继续加油!' : '本次作答错误,已加入错题本'
  } else {
    const examName = (data.examName as string) || '考试'
    const score = data.totalScore != null ? `,得分:${data.totalScore}` : ''
    message = `${examName}已自动判分${score}`
  }
  ElNotification({ title, message, type: 'success' })
}

function connect(token: string): void {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return
  }
  closedByUser = false
  ws = new WebSocket(buildWsUrl(token))
  ws.onmessage = handleMessage
  ws.onclose = () => {
    ws = null
    if (!closedByUser) {
      scheduleReconnect(token)
    }
  }
  ws.onerror = () => {
    ws?.close()
  }
}

function scheduleReconnect(token: string): void {
  if (reconnectTimer != null) return
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connect(token)
  }, 5000)
}

/** 登录后调用;重复调用不会建立多条连接 */
export function connectWs(token: string): void {
  if (!token) return
  connect(token)
}

/** 退出登录时调用,彻底断开且不再重连 */
export function disconnectWs(): void {
  closedByUser = true
  if (reconnectTimer != null) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
}
