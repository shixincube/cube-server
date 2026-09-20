/**
 * 路由表。
 *
 * 采用 history 模式，后端 `SpaFallbackHandler` 会把非静态、非接口路径
 * 统一回退到 `index.html`。
 */

import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/AppShell.vue'),
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '服务器概览' }
      },
      {
        path: 'ai-units',
        name: 'ai-units',
        component: () => import('@/views/UnitOverviewView.vue'),
        meta: { title: 'AI单元概览' }
      },
      {
        path: 'overview',
        name: 'overview',
        component: () => import('@/views/OverviewView.vue'),
        meta: { title: '联系人概览' }
      },
      {
        path: 'dispatcher',
        name: 'dispatcher',
        component: () => import('@/views/DispatcherView.vue'),
        meta: { title: '调度机' }
      },
      {
        path: 'service',
        name: 'service',
        component: () => import('@/views/ServiceView.vue'),
        meta: { title: '服务单元' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // 首次进入时用 Cookie 探测登录态
  if (!auth.ready) {
    await auth.bootstrap()
  }

  if (to.meta.public) {
    // 已登录用户访问登录页直接回到首页
    if (to.name === 'login' && auth.isAuthenticated) {
      return { path: '/dashboard' }
    }
    return true
  }

  if (!auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})
