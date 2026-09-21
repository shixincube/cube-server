<script setup lang="ts">
/**
 * 服务器概览。
 *
 * 汇总调度机/服务单元数量与运行状态，并按服务器维度展示 JVM 内存趋势与实时日志。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import HostHardwarePanel from '@/components/HostHardwarePanel.vue'
import HostPerformancePanel from '@/components/HostPerformancePanel.vue'
import JvmMemoryChart from '@/components/JvmMemoryChart.vue'
import LoadBar from '@/components/LoadBar.vue'
import LogPanel from '@/components/LogPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { queryConsoleLogs, queryJVMReports, queryServerLogs } from '@/api/console'
import { fetchHostMetrics, fetchHostStatic } from '@/api/host'
import { toJVMSeries, type JVMSeries } from '@/api/monitor'
import { usePolling } from '@/composables/usePolling'
import { useServersStore } from '@/stores/servers'
import { useToastStore } from '@/stores/toast'
import type { HostMetricsResponse, HostStaticResponse } from '@/api/types'
import { formatFullTime, formatUptimeSeconds } from '@/utils/format'

const servers = useServersStore()
const toast = useToastStore()

type TabKey = string

/** JVM 内存面板当前选中的服务器 */
const activeChartTab = ref<TabKey>('')
/** 日志面板当前选中的来源 */
const activeLogTab = ref<TabKey>('console')
/** 各服务器的 JVM 内存序列 */
const memorySeries = ref<Record<string, JVMSeries>>({})
/** 最近一次 JVM 数据刷新时间 */
const lastReportAt = ref(0)
const loading = ref(false)

/** 主机静态配置（控制台所在服务器），只需拉取一次 */
const hostStatic = ref<HostStaticResponse | null>(null)
/** 主机动态性能，按秒级轮询 */
const hostMetrics = ref<HostMetricsResponse | null>(null)
const hostLoading = ref(false)
const hostError = ref('')

const EMPTY_SERIES: JVMSeries = { labels: [], used: [], free: [] }

/** 图表面板的服务器清单：只列出正在运行的实例 */
const monitoredServers = computed(() => servers.allServers.filter((item) => item.server.running))

const logSources = computed(() => [
  { key: 'console', label: '控制台' },
  ...servers.allServers.map(({ kind, server }) => ({
    key: server.name,
    label: `${kind === 'dispatcher' ? '调度机' : '服务单元'}#${server.server.port}`
  }))
])

const runningTotal = computed(() => servers.dispatcherRunning + servers.serviceRunning)

const totalServers = computed(() => servers.dispatcherTotal + servers.serviceTotal)

const stoppedTotal = computed(() => totalServers.value - runningTotal.value)

const runningPercent = computed(() =>
  totalServers.value === 0 ? 0 : Math.round((runningTotal.value / totalServers.value) * 100)
)

const currentSeries = computed<JVMSeries>(
  () => memorySeries.value[activeChartTab.value] ?? EMPTY_SERIES
)

/** 拉取全部运行中服务器的 JVM 内存报告 */
async function loadMemoryReports(): Promise<void> {
  const list = monitoredServers.value
  if (list.length === 0) {
    memorySeries.value = {}
    return
  }

  const results = await Promise.all(
    list.map(async ({ server }) => {
      try {
        const reports = await queryJVMReports(server.name, 8)
        return [server.name, toJVMSeries(reports)] as const
      } catch {
        return [server.name, EMPTY_SERIES] as const
      }
    })
  )

  memorySeries.value = Object.fromEntries(results)
  lastReportAt.value = Date.now()
}

/** 主机静态配置：失败后不再重试，页面上给出提示即可 */
async function loadHostStatic(): Promise<void> {
  if (hostStatic.value) {
    return
  }
  try {
    hostStatic.value = await fetchHostStatic()
  } catch (error) {
    hostError.value = error instanceof Error ? error.message : '读取主机硬件信息失败'
  }
}

/**
 * 主机动态性能。
 *
 * CPU 使用率与网络 / 磁盘吞吐都要靠两次采样求差，控制台首次收到请求时还没有基准，
 * 因此这里静默失败，等下一次轮询再刷新即可。
 */
async function loadHostMetrics(): Promise<void> {
  try {
    hostMetrics.value = await fetchHostMetrics()
    hostError.value = ''
  } catch (error) {
    hostError.value = error instanceof Error ? error.message : '读取主机性能数据失败'
  }
}

/** 主机信息整体加载：静态一次，动态先拉一次再补一次以尽快拿到速率 */
async function loadHostInfo(): Promise<void> {
  hostLoading.value = true
  try {
    await Promise.all([loadHostStatic(), loadHostMetrics()])
  } finally {
    hostLoading.value = false
  }
}

/** 首次加载：清单 + 报告 + 主机信息 */
async function bootstrap(): Promise<void> {
  loading.value = true
  try {
    await servers.loadAll()
    if (!activeChartTab.value && monitoredServers.value.length > 0) {
      activeChartTab.value = monitoredServers.value[0].server.name
    }
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '加载服务器列表失败')
  } finally {
    loading.value = false
  }
  await loadMemoryReports()
}

/** 手动刷新清单与报告 */
async function refreshAll(): Promise<void> {
  await bootstrap()
  await loadHostInfo()
  toast.success('已刷新服务器数据')
}

/** 主机运行时长（秒），优先取系统接口，缺省时用启动时间推算 */
const hostUptimeText = computed(() => {
  const metrics = hostMetrics.value
  if (!metrics) {
    return '--'
  }
  const seconds = metrics.system?.uptimeSeconds
  if (seconds && seconds > 0) {
    return formatUptimeSeconds(seconds)
  }
  const bootTime = metrics.system?.bootTime
  if (bootTime && bootTime > 0) {
    return formatUptimeSeconds(Math.floor((Date.now() - bootTime) / 1000))
  }
  return '--'
})

/** 主机名，用于卡片右上角标识当前展示的是哪台机器 */
const hostName = computed(() => hostStatic.value?.host?.hostName ?? '')

/** 清单每 30 秒同步一次，运行状态变化后能及时反映到卡片与图表选项卡 */
usePolling(async () => {
  try {
    await servers.loadAll()
    if (!activeChartTab.value && monitoredServers.value.length > 0) {
      activeChartTab.value = monitoredServers.value[0].server.name
    }
  } catch {
    // 轮询失败静默处理，避免打断查看
  }
}, 30000, { immediate: false })

/** JVM 报告每分钟刷新 */
usePolling(loadMemoryReports, 60000, { immediate: false })

/** 主机动态性能每 5 秒刷新（后端最小采样间隔 1 秒，5 秒能拿到稳定的瞬时速率） */
usePolling(loadHostMetrics, 5000, { immediate: false })

/** 首次采样没有基准，1.5 秒后补一次即可让使用率与速率立刻可见 */
let warmUpTimer = 0

onMounted(async () => {
  await bootstrap()
  await loadHostInfo()
  warmUpTimer = window.setTimeout(() => {
    void loadHostMetrics()
  }, 1500)
})

onUnmounted(() => {
  window.clearTimeout(warmUpTimer)
})
</script>

<template>
  <div>
    <PageHeader title="服务器概览" description="主机硬件配置、实时性能、内存趋势与实时日志">
      <template #actions>
        <span v-if="lastReportAt" class="hidden text-xs text-slate-400 sm:inline">
          JVM 数据更新于 {{ formatFullTime(lastReportAt) }}
        </span>
        <button type="button" class="btn btn-ghost" :disabled="loading" @click="refreshAll">
          <AppIcon name="refresh" :size="14" :class="loading ? 'animate-spin' : ''" />
          刷新
        </button>
      </template>
    </PageHeader>

    <!-- 指标区 -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        label="调度机"
        :value="servers.dispatcherTotal"
        unit="台"
        icon="sitemap"
        tone="brand"
      >
        <p class="mt-2 flex items-center gap-1.5 text-xs text-slate-500">
          <span class="pill pill-on">
            <span class="pill-dot bg-emerald-500" />
            {{ servers.dispatcherRunning }} 运行中
          </span>
          <span v-if="servers.dispatcherTotal - servers.dispatcherRunning > 0" class="pill pill-off">
            {{ servers.dispatcherTotal - servers.dispatcherRunning }} 已关闭
          </span>
        </p>
      </StatCard>

      <StatCard
        label="服务单元"
        :value="servers.serviceTotal"
        unit="台"
        icon="server"
        tone="emerald"
      >
        <p class="mt-2 flex items-center gap-1.5 text-xs text-slate-500">
          <span class="pill pill-on">
            <span class="pill-dot bg-emerald-500" />
            {{ servers.serviceRunning }} 运行中
          </span>
          <span v-if="servers.serviceTotal - servers.serviceRunning > 0" class="pill pill-off">
            {{ servers.serviceTotal - servers.serviceRunning }} 已关闭
          </span>
        </p>
      </StatCard>

      <StatCard
        label="整体运行率"
        :value="runningPercent"
        unit="%"
        :hint="`${runningTotal} / ${totalServers} 台实例处于运行中`"
        icon="activity"
        :tone="runningPercent >= 85 ? 'emerald' : runningPercent >= 60 ? 'amber' : 'rose'"
      >
        <div class="mt-3">
          <LoadBar :percent="runningPercent" :show-value="false" />
        </div>
      </StatCard>

      <StatCard
        label="已关闭实例"
        :value="stoppedTotal"
        unit="台"
        :hint="stoppedTotal === 0 ? '全部实例运行正常' : '存在未启动的实例，请到对应页面检查'"
        icon="warning"
        :tone="stoppedTotal === 0 ? 'emerald' : 'rose'"
      >
        <p class="mt-3">
          <span class="pill" :class="stoppedTotal === 0 ? 'pill-on' : 'pill-off'">
            <span class="pill-dot" :class="stoppedTotal === 0 ? 'bg-emerald-500' : 'bg-rose-500'" />
            {{ stoppedTotal === 0 ? '健康' : '需关注' }}
          </span>
        </p>
      </StatCard>
    </div>

    <div class="mt-5 grid grid-cols-1 gap-5 xl:grid-cols-2">
      <!-- 左列：主机硬件与性能在上，JVM 内存在下；右列日志自动等高 -->
      <div class="flex flex-col gap-5">
        <!-- 主机硬件与性能 -->
        <section class="card flex flex-col">
          <div class="card-header">
            <div>
              <h2 class="card-title">主机硬件与性能</h2>
              <p class="mt-0.5 text-xs text-slate-400">
                控制台所在服务器的硬件配置与实时运行指标
              </p>
            </div>
            <span class="pill pill-neutral">
              <AppIcon name="server" :size="12" />
              {{ hostName || '本机' }}
            </span>
          </div>

          <div class="card-body">
            <p v-if="hostError" class="mb-3 flex items-center gap-1.5 text-xs text-amber-600">
              <AppIcon name="warning" :size="13" />
              {{ hostError }}
            </p>

            <!-- 静态信息 -->
            <div class="mb-3 flex items-center justify-between">
              <h3 class="text-xs font-semibold tracking-wide text-slate-600">硬件配置</h3>
              <span class="text-[11px] text-slate-400">静态 · 5 分钟缓存</span>
            </div>
            <HostHardwarePanel :info="hostStatic" :loading="hostLoading" />

            <div class="my-4 border-t border-slate-200" />

            <!-- 动态信息 -->
            <div class="mb-3 flex items-center justify-between">
              <h3 class="text-xs font-semibold tracking-wide text-slate-600">实时性能</h3>
              <span class="flex items-center gap-1.5 text-[11px] text-slate-400">
                <span class="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                每 5 秒采样 · 主机已运行 {{ hostUptimeText }}
              </span>
            </div>
            <HostPerformancePanel :metrics="hostMetrics" :loading="hostLoading" />
          </div>
        </section>

        <!-- JVM 内存 -->
        <section class="card flex flex-col">
          <div class="card-header">
            <div>
              <h2 class="card-title">JVM 内存</h2>
              <p class="mt-0.5 text-xs text-slate-400">每分钟采样一次，显示最近 8 个采样点</p>
            </div>
            <span class="pill pill-neutral">
              <AppIcon name="cpu" :size="12" />
              {{ monitoredServers.length }} 台在监控
            </span>
          </div>

          <div v-if="monitoredServers.length > 0" class="flex flex-wrap gap-1.5 px-5 pt-3">
            <button
              v-for="item in monitoredServers"
              :key="item.server.name"
              type="button"
              class="rounded-lg px-2.5 py-1 text-xs font-medium transition"
              :class="
                activeChartTab === item.server.name
                  ? 'bg-brand-50 text-brand-700 ring-1 ring-brand-200 ring-inset'
                  : 'text-slate-500 hover:bg-slate-100'
              "
              @click="activeChartTab = item.server.name"
            >
              {{ item.kind === 'dispatcher' ? '调度机' : '服务单元' }}#{{ item.server.server.port }}
            </button>
          </div>

          <div class="card-body">
            <JvmMemoryChart v-if="monitoredServers.length > 0" :series="currentSeries" />
            <EmptyState
              v-else
              icon="cpu"
              title="暂无可监控的服务器"
              description="服务器启动后，这里会显示 JVM 堆内存的使用趋势"
            />
          </div>
        </section>
      </div>

      <!-- 实时日志 -->
      <section class="card flex flex-col overflow-hidden">
        <div class="card-header">
          <div>
            <h2 class="card-title">实时日志</h2>
            <p class="mt-0.5 text-xs text-slate-400">每 10 秒增量拉取，每个视图保留最近 80 行</p>
          </div>
        </div>

        <div class="flex flex-wrap gap-1.5 px-5 pt-3 pb-1">
          <button
            v-for="source in logSources"
            :key="source.key"
            type="button"
            class="rounded-lg px-2.5 py-1 text-xs font-medium transition"
            :class="
              activeLogTab === source.key
                ? 'bg-brand-50 text-brand-700 ring-1 ring-brand-200 ring-inset'
                : 'text-slate-500 hover:bg-slate-100'
            "
            @click="activeLogTab = source.key"
          >
            {{ source.label }}
          </button>
        </div>

        <div class="h-80 min-h-0 flex-1 border-t border-slate-100">
          <LogPanel
            v-if="activeLogTab === 'console'"
            key="console"
            source-key="console"
            :fetcher="queryConsoleLogs"
          />
          <template v-else>
            <LogPanel
              :key="activeLogTab"
              :source-key="activeLogTab"
              :fetcher="(start: number) => queryServerLogs(activeLogTab, start)"
            />
          </template>
        </div>
      </section>
    </div>
  </div>
</template>
