/** 题型/难度/状态等枚举的展示映射与时间格式化工具 */

/** Element Plus el-tag 可用的 type 值 */
export type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

export const TYPE_TEXT: Record<string, string> = {
  SINGLE: '单选题',
  MULTI: '多选题',
  JUDGE: '判断题',
  FILL: '填空题'
}

export const TYPE_TAG: Record<string, TagType> = {
  SINGLE: 'primary',
  MULTI: 'warning',
  JUDGE: 'success',
  FILL: 'info'
}

export const DIFFICULTY_TEXT: Record<number, string> = {
  1: '简单',
  2: '中等',
  3: '困难'
}

export const SOURCE_TEXT: Record<string, string> = {
  MANUAL: '人工录入',
  AI: 'AI生成'
}

export function typeText(type?: string): string {
  return type ? TYPE_TEXT[type] || type : '-'
}

export function typeTag(type?: string): TagType {
  return type ? TYPE_TAG[type] || 'info' : 'info'
}

export function difficultyText(d?: number): string {
  return d != null ? DIFFICULTY_TEXT[d] || String(d) : '-'
}

export function sourceText(s?: string): string {
  return s ? SOURCE_TEXT[s] || s : '-'
}

/** 审核状态:0待审核/1通过 */
export function reviewText(s?: number): string {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  return '-'
}

/** 考试状态:0未发布/1已发布/2已结束 */
export function examStatusText(s?: number): string {
  if (s === 0) return '未发布'
  if (s === 1) return '已发布'
  if (s === 2) return '已结束'
  return '-'
}

/** 考试记录状态:0进行中/1已交卷 */
export function recordStatusText(s?: number): string {
  if (s === 0) return '进行中'
  if (s === 1) return '已交卷'
  return '-'
}

/** 判断题答案展示 */
export function answerText(type?: string, answer?: string): string {
  if (answer == null || answer === '') return '-'
  if (type === 'JUDGE') {
    return answer.toLowerCase() === 'true' ? '正确' : '错误'
  }
  return answer
}

/** 解析后端 yyyy-MM-dd HH:mm:ss 字符串为时间戳(本地时区),无效返回 NaN */
export function parseServerTime(s?: string | null): number {
  if (!s) return NaN
  return new Date(s.replace(/-/g, '/')).getTime()
}

/** 秒数格式化为 HH:mm:ss */
export function formatDuration(totalSeconds: number): string {
  const sec = Math.max(0, Math.floor(totalSeconds))
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(h)}:${pad(m)}:${pad(s)}`
}

/** 多选题答案排序合并为 "A,B" */
export function sortMultiAnswer(arr: string[]): string {
  return [...arr].sort().join(',')
}
