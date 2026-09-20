<script setup lang="ts">
/**
 * 服务单元监视器。
 *
 * 展示任务平均应答时间趋势与 JVM 内存占用。
 */
import { computed, onMounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import EmptyState from './EmptyState.vue'
import { queryJVMReports } from '@/api/console'
import {
  arrangeAvgResponseTime,
  fetchLastPerformance,
  fetchPerformanceAt,
  toJVMSeries,
  type AvgResponseRankItem,
  type JVMSeries
} from '@/api/monitor'
import { useChart } from '@/composables/useChart'
import { usePolling } from '@/composables/usePolling'
import type { PerformanceReport, ServiceServer } from '@/api/types'
import type { EChartsCoreOption } from '@/charts/echarts'
import { formatFullTime, formatTimeHHMMSS } from '@/utils/format'

const props = defineProps<{
  servers: ServiceServer[]
}>()

/** 自动刷新间隔（毫秒） */
const AUTO_REFRESH_INTERVAL = 30000
/** 趋势图回溯的采样点数（每分钟一个点） */
const SAMPLE_POINTS = 10
/** 平均应答时间展示的曲线数量 */
const SERIES_LIMIT = 5

const LINE_COLORS = ['#6366f1', '#f97316', '#10b981', '#eab308', '#ef4444']

const selectedName = ref('')
const autoRefresh = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const perf = ref<PerformanceReport | null>(null)
const perfCache = ref<PerformanceReport[]>([])
const jvmSeries = ref<JVMSeries>({ labels: [], used: [], free: [] })
const respChartEl = ref<HTMLElement | null>(null)
const jvmChartEl = ref<HTMLElement | null>(null)

const selected = computed(
  () => props.servers.find((item) => item.name === selectedName.value) ?? null
)

const latestTimestamp = computed(() => perf.value?.timestamp ?? 0)

/** 最新一份报告里耗时最高的若干动作，作为曲线维度 */
const topActions = computed<AvgResponseRankItem[]>(() =>
  perf.value ? arrangeAvgResponseTime(perf.value).slice(0, SERIES_LIMIT) : []
)

const hasResponseData = computed(
  () => perfCache.value.length > 0 && topActions.value.length > 0
)

const hasJvmData = computed(() => jvmSeries.value.labels.length > 0)

/** 任务平均应答时间折线图 */
const responseOption = computed<EChartsCoreOption>(() => {
  const labels = perfCache.value.map((item) => formatTimeHHMMSS(item.timestamp))
  const series = topActions.value.map((action, index) => ({
    name: action.action,
    type: 'line' as const,
    smooth: true,
    symbolSize: 5,
    lineStyle: { width: 2 },
    itemStyle: { color: LINE_COLORS[index % LINE_COLORS.length] },
    data: perfCache.value.map((item) => {
      const value = item.benchmark?.avgResponseTimeMap?.[action.cellet]?.[action.action]
      return value ? value.value : null
    })
  }))

  return {
    grid: { top: 34, right: 16, bottom: 28, left: 52 },
    tooltip: { trigger: 'axis', valueFormatter: (value: unknown) => `${value} ms` },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#64748b', fontSize: 11 }
    },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: 'ms',
      nameTextStyle: { color: '#94a3b8', fontSize: 10 },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series
  }
})

/** JVM 内存柱状图 */
const jvmOption = computed<EChartsCoreOption>(() => ({
  grid: { top: 34, right: 16, bottom: 28, left: 52 },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    valueFormatter: (value: unknown) => `${value} MB`
  },
  legend: {
    top: 0,
    right: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#64748b', fontSize: 11 }
  },
  xAxis: {
    type: 'category',
    data: jvmSeries.value.labels,
    axisLine: { lineStyle: { color: '#e2e8f0' } },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 10 }
  },
  yAxis: {
    type: 'value',
    name: 'MB',
    nameTextStyle: { color: '#94a3b8', fontSize: 10 },
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 10 },
    splitLine: { lineStyle: { color: '#f1f5f9' } }
  },
  series: [
    {
      name: '已用内存',
      type: 'bar',
      stack: 'memory',
      barMaxWidth: 18,
      itemStyle: { color: '#6366f1' },
      data: jvmSeries.value.used
    },
    {
      name: '空闲内存',
      type: 'bar',
      stack: 'memory',
      barMaxWidth: 18,
      itemStyle: { color: '#cbd5e1', borderRadius: [3, 3, 0, 0] },
      data: jvmSeries.value.free
    }
  ]
}))

const { update: renderRespChart } = useChart(respChartEl, responseOption)
const { update: renderJvmChart } = useChart(jvmChartEl, jvmOption)

/** 拉取最近一份报告并回溯历史采样点 */
async function refresh(): Promise<void> {
  const server = selected.value
  if (!server) {
    return
  }
  if (!server.running) {
    perf.value = null
    perfCache.value = []
    jvmSeries.value = { labels: [], used: [], free: [] }
    errorMessage.value = '服务器没有启动'
    return
  }

  loading.value = true
  try {
    const latest = await fetchLastPerformance(server.name)
    if (!latest) {
      perf.value = null
      perfCache.value = []
      jvmSeries.value = { labels: [], used: [], free: [] }
      errorMessage.value = '服务端暂无该服务器的性能报告'
      return
    }

    perf.value = latest
    errorMessage.value = ''

    // 回溯 10 个一分钟刻度采样点
    const timestamps = Array.from(
      { length: SAMPLE_POINTS },
      (_, index) => latest.timestamp - (SAMPLE_POINTS - 1 - index) * 60000
    )

    const [perfList, jvmReports] = await Promise.all([
      Promise.all(
        timestamps.map((time) => fetchPerformanceAt(server.name, time).catch(() => null))
      ),
      queryJVMReports(server.name, SAMPLE_POINTS).catch(() => [])
    ])

    perfCache.value = perfList.filter((item): item is PerformanceReport => item !== null)
    jvmSeries.value = toJVMSeries(jvmReports)

    renderRespChart()
    renderJvmChart()
  } catch (error) {
    perf.value = null
    errorMessage.value = error instanceof Error ? error.message : '拉取性能数据失败'
  } finally {
    loading.value = false
  }
}

/** 默认选中第一台运行中的服务单元 */
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
  perfCache.value = []
  jvmSeries.value = { labels: [], used: [], free: [] }
  void refresh()
})

watch(autoRefresh, (enabled) => {
  if (enabled) {
    void refresh()
  }
})

usePolling(
  () => (autoRefresh.value ? refresh() : undefined),
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
          任务平均应答时间与 JVM 内存趋势
          <template v-if="latestTimestamp"> · 采集于 {{ formatFullTime(latestTimestamp) }}</template>
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

        <button
          type="button"
          class="btn btn-ghost"
          :disabled="loading || !selected"
          @click="refresh"
        >
          <AppIcon name="refresh" :size="14" :class="loading ? 'animate-spin' : ''" />
          刷新
        </button>
      </div>
    </div>

    <div class="card-body space-y-5">
      <!-- 平均应答时间 -->
      <div>
        <p class="mb-2 text-xs font-medium text-slate-500">
          任务平均应答时间（Top {{ SERIES_LIMIT }}）
        </p>
        <div class="relative">
          <div ref="respChartEl" class="h-60 w-full" />
          <div
            v-if="!hasResponseData"
            class="absolute inset-0 flex items-center justify-center rounded-lg bg-white/70 backdrop-blur-[1px]"
          >
            <EmptyState
              icon="activity"
              :title="errorMessage || '暂无应答时间数据'"
              description="选择一台运行中的服务单元后开始采集"
            />
          </div>
        </div>
      </div>

      <!-- JVM 内存 -->
      <div class="border-t border-slate-100 pt-4">
        <p class="mb-2 text-xs font-medium text-slate-500">JVM 内存</p>
        <div class="relative">
          <div ref="jvmChartEl" class="h-52 w-full" />
          <div
            v-if="!hasJvmData"
            class="absolute inset-0 flex items-center justify-center rounded-lg bg-white/70 backdrop-blur-[1px]"
          >
            <EmptyState icon="cpu" title="暂无 JVM 数据" description="服务端上报报告后自动展示" />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
