<script setup lang="ts">
/**
 * 应用外壳：侧边栏导航 + 顶栏 + 内容区。
 *
 * 替代旧版在每个 HTML 页面里整段复制的 AdminLTE 布局。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import BaseModal from './BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useServersStore } from '@/stores/servers'

interface NavItem {
  path: string
  label: string
  icon: string
  badge?: 'dispatcher' | 'service'
}

interface NavGroup {
  key: string
  label: string
  icon: string
  children: NavItem[]
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const servers = useServersStore()

/** 侧边栏在窄屏下的抽屉状态 */
const drawerOpen = ref(false)
/** 仪表板分组展开状态 */
const dashboardOpen = ref(true)
/** 用户详情弹窗 */
const profileOpen = ref(false)
/** 退出登录中 */
const signingOut = ref(false)

const groups: NavGroup[] = [
  {
    key: 'dashboard',
    label: '仪表板',
    icon: 'gauge',
    children: [
      { path: '/dashboard', label: '服务器概览', icon: 'activity' },
      { path: '/ai-units', label: 'AI单元概览', icon: 'cube' },
      { path: '/overview', label: '联系人概览', icon: 'users' }
    ]
  }
]

const standalone: NavItem[] = [
  { path: '/dispatcher', label: '调度机', icon: 'sitemap', badge: 'dispatcher' },
  { path: '/service', label: '服务单元', icon: 'server', badge: 'service' }
]

const badgeValue = (kind: NavItem['badge']): number => {
  if (kind === 'dispatcher') {
    return servers.dispatcherTotal
  }
  if (kind === 'service') {
    return servers.serviceTotal
  }
  return 0
}

const isActive = (path: string): boolean => route.path === path

const dashboardActive = computed(() =>
  groups[0].children.some((child) => isActive(child.path))
)

const navigate = (path: string): void => {
  drawerOpen.value = false
  if (route.path !== path) {
    void router.push(path)
  }
}

const onSignOut = async (): Promise<void> => {
  signingOut.value = true
  try {
    await auth.logout()
    profileOpen.value = false
    await router.replace('/login')
  } finally {
    signingOut.value = false
  }
}

onMounted(() => {
  // 外壳渲染后补一次清单，保证侧边栏徽标与后端一致
  void servers.loadAll().catch(() => undefined)
})
</script>

<template>
  <div class="flex h-full w-full bg-slate-100">
    <!-- 窄屏遮罩 -->
    <div
      v-if="drawerOpen"
      class="fixed inset-0 z-30 bg-slate-900/40 backdrop-blur-[1px] lg:hidden"
      @click="drawerOpen = false"
    />

    <!-- 侧边栏 -->
    <aside
      class="fixed inset-y-0 left-0 z-40 flex w-60 shrink-0 flex-col bg-slate-900 transition-transform duration-200 lg:static lg:translate-x-0"
      :class="drawerOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <RouterLink
        to="/dashboard"
        class="flex items-center gap-2.5 border-b border-white/10 px-4 py-4"
        @click="drawerOpen = false"
      >
        <img
          src="/assets/img/cube_256.png"
          alt="Cube"
          class="h-8 w-8 rounded-lg ring-1 ring-white/15"
        />
        <span class="flex flex-col leading-tight">
          <span class="text-[13px] font-semibold text-white">Cube</span>
          <span class="text-[11px] tracking-wide text-slate-400">Cube Console 3.0</span>
        </span>
      </RouterLink>

      <nav class="flex-1 overflow-y-auto px-2.5 py-3">
        <!-- 仪表板分组 -->
        <button
          type="button"
          class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] font-medium transition"
          :class="dashboardActive ? 'text-white' : 'text-slate-300 hover:bg-white/5'"
          @click="dashboardOpen = !dashboardOpen"
        >
          <AppIcon name="gauge" :size="16" />
          <span class="flex-1 text-left">仪表板</span>
          <AppIcon
            name="chevronDown"
            :size="14"
            class="text-slate-500 transition-transform"
            :class="dashboardOpen ? '' : '-rotate-90'"
          />
        </button>

        <div v-show="dashboardOpen" class="mt-0.5 mb-1 ml-4 space-y-0.5 border-l border-white/10 pl-3">
          <button
            v-for="item in groups[0].children"
            :key="item.path"
            type="button"
            class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-1.5 text-[13px] transition"
            :class="
              isActive(item.path)
                ? 'bg-brand-600 font-medium text-white'
                : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'
            "
            @click="navigate(item.path)"
          >
            <AppIcon :name="item.icon" :size="14" />
            <span>{{ item.label }}</span>
          </button>
        </div>

        <!-- 顶级导航 -->
        <div class="mt-1 space-y-0.5">
          <button
            v-for="item in standalone"
            :key="item.path"
            type="button"
            class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] transition"
            :class="
              isActive(item.path)
                ? 'bg-brand-600 font-medium text-white'
                : 'text-slate-300 hover:bg-white/5 hover:text-white'
            "
            @click="navigate(item.path)"
          >
            <AppIcon :name="item.icon" :size="16" />
            <span class="flex-1 text-left">{{ item.label }}</span>
            <span
              class="tabular rounded-full px-1.5 py-0.5 text-[11px]"
              :class="isActive(item.path) ? 'bg-white/20 text-white' : 'bg-white/10 text-slate-300'"
            >
              {{ badgeValue(item.badge) }}
            </span>
          </button>
        </div>
      </nav>

      <div class="border-t border-white/10 px-2.5 py-3">
        <a
          href="https://www.aimindecho.com"
          target="_blank"
          rel="noreferrer"
          class="flex items-center gap-2 rounded-lg px-2.5 py-2 text-xs text-slate-400 transition hover:bg-white/5 hover:text-slate-200"
        >
          <AppIcon name="external" :size="14" />
          <span>aimindecho.com</span>
        </a>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="flex min-w-0 flex-1 flex-col">
      <header
        class="sticky top-0 z-20 flex h-14 shrink-0 items-center gap-3 border-b border-slate-200 bg-white/85 px-4 backdrop-blur-md lg:px-6"
      >
        <button
          type="button"
          class="rounded-lg p-1.5 text-slate-500 transition hover:bg-slate-100 lg:hidden"
          aria-label="打开导航"
          @click="drawerOpen = true"
        >
          <AppIcon name="menu" :size="18" />
        </button>

        <div class="flex items-center gap-2 text-xs text-slate-500">
          <span class="hidden sm:inline">{{ route.meta.title ?? '控制台' }}</span>
        </div>

        <div class="flex-1" />

        <div class="flex items-center gap-3">
          <span class="hidden items-center gap-1.5 text-xs text-slate-500 sm:flex">
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-500" />
            {{ servers.dispatcherRunning + servers.serviceRunning }} 台运行中
          </span>

          <button
            type="button"
            class="flex items-center gap-2 rounded-lg px-2 py-1.5 transition hover:bg-slate-100"
            @click="profileOpen = true"
          >
            <img
              :src="auth.user?.avatar || '/assets/img/avatar.png'"
              alt="用户头像"
              class="h-7 w-7 rounded-full ring-1 ring-slate-200"
            />
            <span class="hidden text-[13px] font-medium text-slate-700 sm:inline">
              {{ auth.displayName }}
            </span>
          </button>
        </div>
      </header>

      <main class="min-h-0 flex-1 overflow-y-auto px-4 py-5 lg:px-6 lg:py-6">
        <RouterView v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>

    <!-- 用户详情 -->
    <BaseModal v-model="profileOpen" title="用户详情" width="sm">
      <div class="flex items-start gap-4">
        <img
          :src="auth.user?.avatar || '/assets/img/avatar.png'"
          alt="用户头像"
          class="h-16 w-16 rounded-full ring-2 ring-slate-100"
        />
        <div class="min-w-0 flex-1 space-y-2">
          <p class="text-[15px] font-semibold text-slate-900">{{ auth.displayName }}</p>
          <div class="kv-row">
            <span class="kv-key">账号</span>
            <span class="kv-val">{{ auth.user?.name ?? '--' }}</span>
          </div>
          <div class="kv-row">
            <span class="kv-key">角色</span>
            <span class="kv-val">
              <span class="pill" :class="auth.isSuperAdmin ? 'pill-info' : 'pill-neutral'">
                <AppIcon name="shield" :size="12" />
                {{ auth.roleLabel }}
              </span>
            </span>
          </div>
          <div class="kv-row">
            <span class="kv-key">用户组</span>
            <span class="kv-val">{{ auth.user?.group || '--' }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <button
          type="button"
          class="btn btn-ghost"
          :disabled="signingOut"
          @click="onSignOut"
        >
          <AppIcon name="logout" :size="14" />
          退出登录
        </button>
      </template>
    </BaseModal>
  </div>
</template>
