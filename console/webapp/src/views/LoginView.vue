<script setup lang="ts">
/**
 * 登录页。
 *
 * 口令以 MD5 摘要提交，与后端 `UserManager#signIn` 的比对方式一致。
 * 已登录用户会被路由守卫直接送回首页。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const remember = ref(true)
const showPassword = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

/** 后端在登录页跳转时携带的错误码 */
const ERROR_CODES: Record<string, string> = {
  '9': '登录状态已失效，请重新登录',
  '10': '账号或口令不正确'
}

const canSubmit = computed(
  () => username.value.trim().length >= 3 && password.value.length >= 6 && !submitting.value
)

async function onSubmit(): Promise<void> {
  errorMessage.value = ''

  if (username.value.trim().length < 3) {
    errorMessage.value = '请输入正确的登录账号（至少 3 位）'
    return
  }
  if (password.value.length < 6) {
    errorMessage.value = '请输入至少 6 位口令'
    return
  }

  submitting.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    if (!remember.value) {
      // 后端 Cookie 的生命周期由其自行决定，这里仅在用户要求时提示
      // 「记住我」关闭时不额外持久化任何前端状态
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value =
      error instanceof Error && error.message.includes('HTTP')
        ? '账号或口令不正确'
        : '登录失败，请检查账号与口令'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const code = typeof route.query.e === 'string' ? route.query.e : ''
  if (code && ERROR_CODES[code]) {
    errorMessage.value = ERROR_CODES[code]
  }
})
</script>

<template>
  <div class="flex min-h-full items-center justify-center bg-slate-100 px-4 py-10">
    <div class="grid w-full max-w-4xl overflow-hidden rounded-2xl bg-white shadow-xl lg:grid-cols-2">
      <!-- 品牌区 -->
      <div
        class="relative hidden flex-col justify-between bg-slate-900 p-8 text-white lg:flex"
      >
        <div
          class="pointer-events-none absolute inset-0 opacity-[0.18]"
          style="
            background-image:
              radial-gradient(circle at 15% 20%, #6366f1 0, transparent 45%),
              radial-gradient(circle at 85% 75%, #0ea5e9 0, transparent 45%);
          "
        />
        <div class="relative">
          <div class="flex items-center gap-2.5">
            <img
              src="/assets/img/cube_256.png"
              alt="Cube"
              class="h-9 w-9 rounded-lg ring-1 ring-white/20"
            />
            <span class="text-sm font-semibold">Cube</span>
          </div>

          <h1 class="mt-10 text-2xl leading-snug font-semibold">
            服务器运行状态
            <br />
            一站式监控控制台
          </h1>
          <p class="mt-3 max-w-xs text-[13px] leading-6 text-slate-300">
            集中查看调度机与服务单元的部署、启停、负载与实时日志，快速定位线上问题。
          </p>
        </div>

        <ul class="relative mt-10 space-y-2.5 text-[13px] text-slate-300">
          <li class="flex items-center gap-2">
            <AppIcon name="sitemap" :size="15" class="text-brand-400" />
            调度机集群拓扑与路由权重
          </li>
          <li class="flex items-center gap-2">
            <AppIcon name="cpu" :size="15" class="text-brand-400" />
            JVM 内存与任务应答耗时趋势
          </li>
          <li class="flex items-center gap-2">
            <AppIcon name="terminal" :size="15" class="text-brand-400" />
            服务器日志实时滚动
          </li>
        </ul>
      </div>

      <!-- 表单区 -->
      <div class="p-8 sm:p-10">
        <div class="mb-7 flex items-center gap-2.5 lg:hidden">
          <img src="/assets/img/cube_256.png" alt="Cube" class="h-8 w-8 rounded-lg" />
          <span class="text-sm font-semibold text-slate-800">时信魔方控制台</span>
        </div>

        <h2 class="text-xl font-semibold text-slate-900">登录控制台</h2>
        <p class="mt-1 text-[13px] text-slate-500">请使用控制台管理员账号登录</p>

        <form class="mt-7 space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="label" for="username">账号</label>
            <div class="relative">
              <span
                class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-slate-400"
              >
                <AppIcon name="user" :size="15" />
              </span>
              <input
                id="username"
                v-model="username"
                class="input pl-9"
                type="text"
                autocomplete="username"
                placeholder="请输入账号"
                @input="errorMessage = ''"
              />
            </div>
          </div>

          <div>
            <label class="label" for="password">口令</label>
            <div class="relative">
              <span
                class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-slate-400"
              >
                <AppIcon name="lock" :size="15" />
              </span>
              <input
                id="password"
                v-model="password"
                class="input pr-10 pl-9"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="请输入口令"
                @input="errorMessage = ''"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-2 flex items-center rounded px-1.5 text-slate-400 transition hover:text-slate-600"
                :aria-label="showPassword ? '隐藏口令' : '显示口令'"
                @click="showPassword = !showPassword"
              >
                <AppIcon :name="showPassword ? 'eyeOff' : 'eye'" :size="15" />
              </button>
            </div>
          </div>

          <label class="flex cursor-pointer items-center gap-2 text-[13px] text-slate-600">
            <input
              v-model="remember"
              type="checkbox"
              class="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500/30"
            />
            记住我
          </label>

          <Transition name="fade">
            <p
              v-if="errorMessage"
              class="flex items-start gap-2 rounded-lg bg-rose-50 px-3 py-2.5 text-[13px] text-rose-700 ring-1 ring-rose-200 ring-inset"
            >
              <AppIcon name="warning" :size="15" class="mt-0.5" />
              {{ errorMessage }}
            </p>
          </Transition>

          <button type="submit" class="btn btn-primary w-full py-2.5" :disabled="!canSubmit">
            <AppIcon v-if="submitting" name="loader" :size="15" class="animate-spin" />
            {{ submitting ? '登录中…' : '登 录' }}
          </button>
        </form>

        <p class="mt-8 text-center text-xs text-slate-400">
          版权所有 © 2024-2026
          <a
            href="https://www.aimindecho.com"
            target="_blank"
            rel="noreferrer"
            class="text-slate-500 transition hover:text-brand-600"
          >
            Cube Team
          </a>
        </p>
      </div>
    </div>
  </div>
</template>
