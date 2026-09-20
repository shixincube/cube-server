/**
 * 登录态。
 *
 * 凭据保存在后端下发的 `CubeConsoleToken` Cookie 中（HttpOnly 由后端决定，前端不读取），
 * 前端只缓存用户档案用于渲染侧边栏与用户详情。
 */

import { defineStore } from 'pinia'
import { signIn, signInWithToken, signOut } from '@/api/console'
import type { ConsoleUser } from '@/api/types'
import { md5 } from '@/utils/md5'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as ConsoleUser | null,
    /** 是否已完成首次登录态探测 */
    ready: false
  }),

  getters: {
    isAuthenticated: (state) => state.user !== null,

    /** 是否拥有超级管理员权限（role === 1） */
    isSuperAdmin: (state) => state.user?.role === 1,

    roleLabel: (state) => (state.user?.role === 1 ? '超级管理员' : '管理员'),

    displayName: (state) => state.user?.displayName ?? state.user?.name ?? ''
  },

  actions: {
    /** 应用启动时用 Cookie 探测登录态 */
    async bootstrap(): Promise<boolean> {
      try {
        const token = await signInWithToken()
        this.user = token.user
        return true
      } catch {
        this.user = null
        return false
      } finally {
        this.ready = true
      }
    },

    /**
     * 账号口令登录。
     *
     * 口令在提交前做 MD5，与后端 `UserManager#signIn` 的比对方式一致。
     */
    async login(username: string, password: string): Promise<void> {
      const token = await signIn(username, md5(password))
      this.user = token.user
    },

    /** 退出登录，无论后端结果如何都清空本地状态 */
    async logout(): Promise<void> {
      try {
        await signOut()
      } catch {
        // 忽略退出失败，本地状态仍然需要清空
      } finally {
        this.user = null
      }
    },

    /** 仅清空本地状态（鉴权失效时调用） */
    reset(): void {
      this.user = null
    }
  }
})
