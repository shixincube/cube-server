/** 格式化工具，行为与旧版 `assets/js/util.js` 保持一致。 */

const KB = 1024
const MB = 1024 * KB
const GB = 1024 * MB
const TB = 1024 * GB

function pad(num: number, length: 2 | 3 | 4): string {
  if (length === 2) {
    return num < 10 ? `0${num}` : `${num}`
  }
  if (length === 3) {
    return num < 10 ? `00${num}` : num < 100 ? `0${num}` : `${num}`
  }
  return num < 10 ? `000${num}` : num < 100 ? `00${num}` : num < 1000 ? `0${num}` : `${num}`
}

/** 将换行与制表符转换为可安全渲染的 HTML 片段 */
export function escapeLogText(text: string): string {
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
  return escaped.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;')
}

/** `HH:mm` */
export function formatTimeHHMM(time: number): string {
  const date = new Date(time)
  return `${pad(date.getHours(), 2)}:${pad(date.getMinutes(), 2)}`
}

/** `HH:mm:ss` */
export function formatTimeHHMMSS(time: number): string {
  const date = new Date(time)
  return `${pad(date.getHours(), 2)}:${pad(date.getMinutes(), 2)}:${pad(date.getSeconds(), 2)}`
}

/** `MM-dd HH:mm:ss` */
export function formatTimeMDHMS(time: number): string {
  if (!time) {
    return '--'
  }
  const date = new Date(time)
  return (
    `${pad(date.getMonth() + 1, 2)}-${pad(date.getDate(), 2)} ` +
    `${pad(date.getHours(), 2)}:${pad(date.getMinutes(), 2)}:${pad(date.getSeconds(), 2)}`
  )
}

/** `yyyy-MM-dd HH:mm:ss` */
export function formatFullTime(time: number): string {
  if (!time) {
    return '--'
  }
  const date = new Date(time)
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1, 2)}-${pad(date.getDate(), 2)} ` +
    `${pad(date.getHours(), 2)}:${pad(date.getMinutes(), 2)}:${pad(date.getSeconds(), 2)}`
  )
}

/** 日志时间戳 `MM-dd HH:mm:ss.SSS` */
export function formatLogTime(time: number): string {
  const date = new Date(time)
  return (
    `${pad(date.getMonth() + 1, 2)}-${pad(date.getDate(), 2)} ` +
    `${pad(date.getHours(), 2)}:${pad(date.getMinutes(), 2)}:${pad(date.getSeconds(), 2)}.` +
    `${pad(date.getMilliseconds(), 3)}`
  )
}

/** 字节数转为人类可读单位 */
export function formatSize(size: number): string {
  if (size < KB) {
    return `${size} B`
  }
  if (size < MB) {
    return `${(size / KB).toFixed(2)} KB`
  }
  if (size < GB) {
    return `${(size / MB).toFixed(2)} MB`
  }
  if (size < TB) {
    return `${(size / GB).toFixed(2)} GB`
  }
  return `${size}`
}

/** 毫秒转小时 */
export function convMillisToHours(ms: number): number {
  return ms / 3600000
}

/** 字节转兆字节 */
export function convBytesToMB(bytes: number): number {
  return bytes / MB
}

/** 计算系统运行时长文案 */
export function formatDuration(startTime: number, now = Date.now()): string {
  if (!startTime) {
    return '--'
  }
  const seconds = Math.max(0, Math.floor((now - startTime) / 1000))
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days > 0) {
    return `${days} 天 ${hours} 小时`
  }
  if (hours > 0) {
    return `${hours} 小时 ${minutes} 分`
  }
  return `${minutes} 分`
}
