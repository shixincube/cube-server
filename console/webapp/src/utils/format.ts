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

/** 字节数转人类可读容量（自适应 B / KB / MB / GB / TB） */
export function formatBytes(bytes: number | undefined | null): string {
  if (bytes === undefined || bytes === null || !Number.isFinite(bytes) || bytes < 0) {
    return '--'
  }
  if (bytes < KB) {
    return `${Math.round(bytes)} B`
  }
  if (bytes < MB) {
    return `${(bytes / KB).toFixed(1)} KB`
  }
  if (bytes < GB) {
    return `${(bytes / MB).toFixed(1)} MB`
  }
  if (bytes < TB) {
    return `${(bytes / GB).toFixed(1)} GB`
  }
  return `${(bytes / TB).toFixed(1)} TB`
}

/** 字节/秒转人类可读速率，如 `1.2 MB/s` */
export function formatRate(bytesPerSec: number | undefined | null): string {
  if (bytesPerSec === undefined || bytesPerSec === null || !Number.isFinite(bytesPerSec)) {
    return '--'
  }
  return `${formatBytes(bytesPerSec)}/s`
}

/** 链路速率（bit/s）转人类可读文案，如 `1000 Mbps` */
export function formatLinkSpeed(bitsPerSec: number | undefined | null): string {
  if (!bitsPerSec || !Number.isFinite(bitsPerSec) || bitsPerSec <= 0) {
    return '--'
  }
  if (bitsPerSec >= 1e9) {
    return `${(bitsPerSec / 1e9).toFixed(1)} Gbps`
  }
  if (bitsPerSec >= 1e6) {
    return `${Math.round(bitsPerSec / 1e6)} Mbps`
  }
  return `${Math.round(bitsPerSec / 1e3)} Kbps`
}

/** 频率（Hz）转人类可读主频，如 `2.70 GHz` */
export function formatFrequency(hz: number | undefined | null): string {
  if (!hz || !Number.isFinite(hz) || hz <= 0) {
    return '--'
  }
  if (hz >= 1e9) {
    return `${(hz / 1e9).toFixed(2)} GHz`
  }
  if (hz >= 1e6) {
    return `${Math.round(hz / 1e6)} MHz`
  }
  return `${Math.round(hz / 1e3)} KHz`
}

/** 0~1 的比值转百分比整数 */
export function formatPercent(ratio: number | undefined | null): string {
  if (ratio === undefined || ratio === null || !Number.isFinite(ratio)) {
    return '--'
  }
  return `${Math.round(Math.max(0, Math.min(1, ratio)) * 100)}%`
}

/** 已运行时长：由持续秒数得到「x 天 x 小时 x 分」 */
export function formatUptimeSeconds(seconds: number | undefined | null): string {
  if (!seconds || !Number.isFinite(seconds) || seconds <= 0) {
    return '--'
  }
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

/** 已运行时长：由毫秒得到「x 天 x 小时 x 分」 */
export function formatUptimeMillis(millis: number | undefined | null): string {
  if (!millis) {
    return '--'
  }
  return formatUptimeSeconds(Math.floor(millis / 1000))
}
