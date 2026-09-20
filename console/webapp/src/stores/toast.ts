/**
 * 全局通知队列。
 *
 * 替代旧版散落各处的 SweetAlert toast，统一由 `<ToastHost>` 渲染。
 */

import { defineStore } from 'pinia'

export type ToastKind = 'success' | 'error' | 'warning' | 'info'

export interface ToastItem {
  id: number
  kind: ToastKind
  message: string
}

let seed = 0

export const useToastStore = defineStore('toast', {
  state: () => ({
    items: [] as ToastItem[]
  }),

  actions: {
    /** 弹出一条通知，默认 3.5 秒后自动消失 */
    push(kind: ToastKind, message: string, duration = 3500): void {
      const id = ++seed
      this.items.push({ id, kind, message })
      window.setTimeout(() => this.dismiss(id), duration)
    },

    success(message: string): void {
      this.push('success', message)
    },

    error(message: string): void {
      this.push('error', message)
    },

    warning(message: string): void {
      this.push('warning', message)
    },

    info(message: string): void {
      this.push('info', message)
    },

    /** 关闭指定通知 */
    dismiss(id: number): void {
      this.items = this.items.filter((item) => item.id !== id)
    }
  }
})
