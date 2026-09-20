<script setup lang="ts">
/**
 * 调度机监视器。
 *
 * 展示任务执行计数、各访问点网络负载与平均应答时间排行。
 */
import { computed, onMounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import EmptyState from './EmptyState.vue'
import LoadBar from './LoadBar.vue'
import { fetchLastPerformance, toMonitorSnapshot } from '@/api/monitor'
import { useChart } from '@/composables/useChart'
import { usePolling } from '@/composables/usePolling'
import type { DispatcherServer, PerformanceReport } from '@/api/types'
import type { EChartsCoreOption } from '@/charts/echarts'
import { formatFullTime } from '@/utils/format'

const props = defineProps<{
  servers: DispatcherServer[]
}>()

/** 自动刷新间隔（毫秒） */
const AUTO_REFRESH_INTERVAL = 30000
/** 平均应答时间展示条数 */
const RANK_SIZE = 6

const selectedName = ref('')
const autoRefresh = ref(false)
const perf = ref<PerformanceReport | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const chartEl = ref<HTMLElement | null>(null)

const selected = computed(
  () => props.servers.find((item) => item.name === selectedName.value) ?? null
)

const snapshot = computed(() => (perf.value ? toMonitorSnapshot(perf.value) : null))

/** 各访问点负载，按 SHM / WS / WSS 端口匹配 */
const accessPointLoads = computed(() => {
  const server = selected.value
  const loads = snapshot.value?.accessPointLoads ?? []
  const pick = (port?: number) => loads.find((item) => item.port === port)
  return {
    shm: pick(server?.server.port),
    ws: pick(server?.wsServer.port),
    wss: pick(server?.wssServer.port)
  }
})

/** 网络负载展示行 */
const loadRows = computed(() => [
  { key: 'shm', label: 'SHM 访问点负载', value: accessPointLoads.value.shm },
  { key: 'ws', label: 'WS 访问点负载', value: accessPointLoads.value.ws },
  { key: 'wss', label: 'WSS 访问点负载', value: accessPointLoads.value.wss }
])

const rankSlots = computed(() => {
  const rank = snapshot.value?.avgResponseRank ?? []
  return Array.from({ length: RANK_SIZE }, (_, index) => rank[index] ?? null)
})

const chartOption = computed<EChartsCoreOption>(() => {
  const counters = snapshot.value?.counters ?? []
  return {
    grid: { top: 16, right: 16, bottom: 46, left: 48 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: counters.map((item) => item.name),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10, interval: 0, rotate: counters.length > 8 ? 30 : 0 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 26,
        itemStyle: { color: '#6366f1', borderRadius: [3, 3, 0, 0] },
        data: counters.map((item) => item.value)
      }
    ]
  }
})

const { update: renderChart } = useChart(chartEl, chartOption)

/** 拉取最近一份性能报告 */
async function refresh(): Promise<void> {
  const server = selected.value
  if (!server) {
    return
  }
  if (!server.running) {
    perf.value = null
    errorMessage.value = '服务器没有启动'
    return
  }

  loading.value = true
  try {
    perf.value = await fetchLastPerformance(server.name)
    errorMessage.value = perf.value ? '' : '服务端暂无该服务器的性能报告'
    renderChart()
  } catch (error) {
    perf.value = null
    errorMessage.value = error instanceof Error ? error.message : '拉取性能数据失败'
  } finally {
    loading.value = false
  }
}

/** 默认选中第一台运行中的调度机 */
function selectDefault(): void {
  if (selectedName.value && props.servers.some((item) => item.name === selectedName.value)) {
    return
  }
  const first = props.servers.find((item) => item.running) ?? props.servers[0]
  selectedName.value = first?.name ?? ''
}

watch(() => props.servers, selectDefault, { immediate: true, deep: false })

watch(selectedName, () => {
  perf.value = null
  void refresh()
})

watch(autoRefresh, (enabled) => {
  if (enabled) {
    void refresh()
  }
})

usePolling(
  () => {
    if (autoRefresh.value) {
      return refresh()
    }
    return undefined
  },
  AUTO_REFRESH_INTERVAL,
  { immediate: false }
)

onMounted(() => {
  void refresh()
})
</script>

<template>
  <section class="card">
    <div class="card-header flex-wrap">
      <div>
        <h2 class="card-title">监视器</h2>
        <p class="mt-0.5 text-xs text-slate-400">
          任务执行计数、网络负载与平均应答时间
          <template v-if="snapshot"> · 采集于 {{ formatFullTime(snapshot.timestamp) }}</template>
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <select v-model="selectedName" class="select w-56">
          <option value="" disabled>请选择需要监视的服务器</option>
          <option v-for="item in servers" :key="item.name" :value="item.name">
            {{ item.name }}{{ item.running ? '' : '（已关闭）' }}
          </option>
        </select>

        <label class="flex cursor-pointer items-center gap-2 text-xs text-slate-600">
          <input
            v-model="autoRefresh"
            type="checkbox"
            class="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500/30"
          />
          自动刷新
        </label>

        <button type="button" class="btn btn-ghost" :disabled="loading || !selected" @click="refresh">
          <AppIcon name="refresh" :size="14" :class="loading ? 'animate-spin' : ''" />
          刷新
        </button>
      </div>
    </div>

    <div class="card-body">
      <div class="grid grid-cols-1 gap-5 lg:grid-cols-5">
        <!-- 任务执行统计 -->
        <div class="lg:col-span-3">
          <p class="mb-2 text-xs font-medium text-slate-500">任务执行统计</p>
          <div class="relative">
            <div ref="chartEl" class="h-56 w-full" />
            <div
              v-if="!snapshot"
              class="absolute inset-0 flex items-center justify-center rounded-lg bg-white/70 backdrop-blur-[1px]"
            >
              <EmptyState
                icon="activity"
                :title="errorMessage || '暂无监视数据'"
                description="选择一台运行中的调度机后开始采集"
              />
            </div>
          </div>
        </div>

        <!-- 网络负载 -->
        <div class="lg:col-span-2">
          <p class="mb-2 text-xs font-medium text-slate-500">网络负载</p>
          <div class="space-y-3">
            <div v-for="row in loadRows" :key="row.key">
              <div class="mb-1 flex items-baseline justify-between text-xs">
                <span class="text-slate-600">{{ row.label }}</span>
                <span class="tabular text-slate-400">
                  <b class="text-sm text-slate-700">{{ row.value?.realtime ?? '--' }}</b>
                  / {{ row.value?.max ?? '--' }}
                </span>
              </div>
              <LoadBar :percent="row.value?.percent ?? 0" :show-value="false" />
            </div>
          </div>

          <div class="mt-4 border-t border-dashed border-slate-200 pt-3">
            <div class="mb-1 flex items-baseline justify-between text-xs">
              <span class="text-slate-600">综合负载</span>
              <span class="tabular text-base font-semibold text-slate-800">
                {{ snapshot ? `${snapshot.load.percent}%` : '--' }}
              </span>
            </div>
            <LoadBar :percent="snapshot?.load.percent ?? 0" :show-value="false" size="md" />
          </div>
        </div>
      </div>

      <!-- 平均应答时间 -->
      <div class="mt-5 border-t border-slate-100 pt-4">
        <p class="mb-2.5 text-xs font-medium text-slate-500">平均应答时间（Top {{ RANK_SIZE }}）</p>
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
          <div
            v-for="(item, index) in rankSlots"
            :key="index"
            class="rounded-lg border border-slate-200 px-3 py-2.5"
          >
            <p class="tabular text-sm font-semibold text-slate-800">
              {{ item ? `${item.value} ms` : '-- ms' }}
            </p>
            <p class="truncate text-xs text-slate-500" :title="item ? `${item.cellet}.${item.action}` : ''">
              {{ item ? item.action : '--' }}
            </p>
            <p class="tabular mt-0.5 text-[11px]">
              <template v-if="item && item.delta < 0">
                <span class="text-emerald-600">↓ {{ Math.abs(item.delta) }}</span>
              </template>
              <template v-else-if="item && item.delta > 0">
                <span class="text-rose-600">↑ {{ item.delta }}</span>
              </template>
              <template v-else>
                <span class="text-slate-400">— 0</span>
              </template>
            </p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
