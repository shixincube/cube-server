<script setup lang="ts">
/**
 * 主机实时性能（动态信息）。
 *
 * 参考 Windows 任务管理器「性能」页的表达方式：**左侧大号当前值 + 右侧滚动曲线**，
 * 曲线上方一行小字给出各序列的当前值，左侧补一行窗口内的峰值 / 谷值。
 * 按需求**不使用 Tab**：CPU、内存、网络、磁盘、JVM 堆、线程与进程 依次纵向排列，
 * 一次扫读即可定位压力来源；曲线数据由本组件按 `metrics` 到达节奏自行累积（保留 5 分钟）。
 *
 * 排版约束：主机卡片只有半屏宽，曲线区实际仅剩 300px 上下，因此图例**必须单行不换行**，
 * 峰值 / 谷值放到左侧窄栏（那栏本来就有富余高度），否则随数值长度随机折行会让各行动态跳动。
 *
 * 网络 / 磁盘是**双序列指标**（发送-接收、读取-写入），左侧值区按两列并列展示（各占一半宽度，
 * 与曲线同色圆点对应），因此左栏宽度需按比例放宽、曲线相应收窄——用 `clamp()` 让左右占比
 * 随卡片宽度自适应（窄屏保住文字不折行，宽屏把余量让给曲线）。
 */
import { computed, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import LoadBar from './LoadBar.vue'
import RealtimeAreaChart, { type RealtimeSeries } from './RealtimeAreaChart.vue'
import type { HostMetricsResponse } from '@/api/types'
import {
  formatBytes,
  formatPercent,
  formatRate,
  formatTimeHHMMSS,
  formatUptimeSeconds
} from '@/utils/format'

const props = defineProps<{
  metrics: HostMetricsResponse | null
  loading?: boolean
}>()

/** 曲线窗口：60 个采样点 × 5 秒 = 最近 5 分钟 */
const HISTORY = 60

/** 各指标配色（数值、图例、曲线三处保持一致） */
const COLOR = {
  cpu: '#4f46e5',
  memory: '#0284c7',
  rx: '#0ea5e9',
  tx: '#f59e0b',
  read: '#8b5cf6',
  write: '#f97316',
  heap: '#14b8a6',
  hostThreads: '#6366f1',
  jvmThreads: '#f59e0b'
}

/** CPU 时间构成的展示顺序与配色（空闲置灰压轴） */
const CPU_SEGMENTS: { key: string; label: string; color: string }[] = [
  { key: 'user', label: '用户', color: 'bg-brand-500' },
  { key: 'system', label: '系统', color: 'bg-amber-500' },
  { key: 'iowait', label: 'IO 等待', color: 'bg-sky-500' },
  { key: 'irq', label: '中断', color: 'bg-violet-500' },
  { key: 'nice', label: '低优先级', color: 'bg-teal-500' },
  { key: 'steal', label: '被抢占', color: 'bg-rose-500' },
  { key: 'idle', label: '空闲', color: 'bg-slate-300' }
]

/** 单个采样点：百分比统一存 0~100，速率存 bytes/s，堆存 MB，线程存条数 */
interface Sample {
  time: number
  cpu: number | null
  memory: number | null
  rx: number | null
  tx: number | null
  read: number | null
  write: number | null
  heap: number | null
  hostThreads: number | null
  jvmThreads: number | null
}

/** 双值指标的一侧（网络 / 磁盘）：与曲线同色的圆点 + 名称 + 当前值 */
interface MetricPairItem {
  label: string
  color: string
  text: string
  /** 有明确方向时给出的箭头符号（网络：↑ 发送 / ↓ 接收），无方向则用圆点 */
  symbol?: string
}

/** 一行指标：左侧当前值 + 右侧滚动曲线 */
interface MetricRow {
  key: string
  label: string
  icon: string
  /** 单值指标的当前值；与 `pair` 二选一 */
  value?: string
  /** 双值指标（发送-接收、读取-写入）的当前值，存在时替代 `value` 渲染 */
  pair?: MetricPairItem[]
  /**
   * 值的字号 class。
   * 单值指标用 `text-2xl` / `text-xl`；**双值指标只能用 `text-base`**——左栏最窄时
   * （1280 视口、clamp 取下限 208px）每个双值单元格仅 98px，18px 字号下
   * 「373.8 KB/s」就要 108px 会溢出，16px 后最长常见值约 94px 仍有余量。
   */
  valueClass: string
  hint: string
  max: number | null
  /** 数值 / 提示框的格式化方式 */
  format: (value: number) => string
  series: RealtimeSeries[]
  /** `text` 省略时只显示圆点与名称（数值已在左侧值区给出，避免重复） */
  legend: { label: string; color: string; text?: string }[]
  /** 窗口内峰值 / 谷值，圆点用主序列颜色以便对应；`series` 仅用于悬浮提示 */
  peak: { color: string; text: string; series?: string } | null
}

const samples = ref<Sample[]>([])

const cpu = computed(() => props.metrics?.cpu)
const memory = computed(() => props.metrics?.memory)
const network = computed(() => props.metrics?.network)
const disk = computed(() => props.metrics?.disk)
const jvm = computed(() => props.metrics?.jvm)
const system = computed(() => props.metrics?.system)

const toSample = (metrics: HostMetricsResponse): Sample => ({
  time: metrics.timestamp || Date.now(),
  cpu: metrics.cpu?.usage === undefined ? null : metrics.cpu.usage * 100,
  memory: metrics.memory?.usage === undefined ? null : metrics.memory.usage * 100,
  rx: metrics.network?.rxBytesPerSec ?? null,
  tx: metrics.network?.txBytesPerSec ?? null,
  read: metrics.disk?.readBytesPerSec ?? null,
  write: metrics.disk?.writeBytesPerSec ?? null,
  heap: metrics.jvm?.heapUsedBytes === undefined ? null : metrics.jvm.heapUsedBytes / 1048576,
  hostThreads: metrics.system?.threadCount ?? null,
  jvmThreads: metrics.jvm?.threadCount ?? null
})

// 每次轮询到达一个新对象，直接入队；超窗口丢弃最旧的点
watch(
  () => props.metrics,
  (metrics) => {
    if (!metrics) {
      return
    }
    samples.value = [...samples.value, toSample(metrics)].slice(-HISTORY)
  },
  { immediate: true }
)

const labels = computed(() => samples.value.map((item) => formatTimeHHMMSS(item.time)))

/** 取出某个字段的历史序列 */
const pick = (key: keyof Sample): (number | null)[] =>
  samples.value.map((item) => item[key] as number | null)

/**
 * 窗口内的峰值 / 谷值文案（只看主序列，避免多序列指标产生歧义）。
 * 主序列名称不写进文案——左栏仅 220px，加上名称会让长数值折行、行高随机跳动，
 * 归属改由「同色圆点 + 悬浮提示」表达。
 */
const peakOf = (values: (number | null)[], format: (value: number) => string): string | null => {
  const numbers = values.filter((value): value is number => value !== null && Number.isFinite(value))
  if (numbers.length === 0) {
    return null
  }
  return `峰值 ${format(Math.max(...numbers))} · 谷值 ${format(Math.min(...numbers))}`
}

/** CPU 使用率，首次采样无基准时为空 */
const cpuUsage = computed(() => cpu.value?.usage)

/** CPU 非空闲构成，按占比过滤掉 0 值段 */
const cpuSegments = computed(() => {
  const breakdown = cpu.value?.breakdown
  if (!breakdown) {
    return []
  }
  return CPU_SEGMENTS.map((segment) => ({
    ...segment,
    ratio: (breakdown as Record<string, number | undefined>)[segment.key] ?? 0
  })).filter((segment) => segment.ratio > 0)
})

/** 负载均值，取不到时整行隐藏 */
const loadAverage = computed(() => {
  const values = cpu.value?.loadAverage
  if (!values || values.length < 3) {
    return null
  }
  const format = (value: number | null): string =>
    value === null || value === undefined ? '--' : value.toFixed(2)
  return `${format(values[0])} / ${format(values[1])} / ${format(values[2])}`
})

/** 有流量的网卡，最多展示两块 */
const activeNics = computed(() => (network.value?.activeInterfaces ?? []).slice(0, 2))

/** 分区占用，最多展示三条 */
const partitions = computed(() => (disk.value?.partitions ?? []).slice(0, 3))

/** 打开文件描述符，取不到时返回 null 以便整行隐藏 */
const fileDescriptors = computed(() => {
  const open = system.value?.openFileDescriptors
  const max = system.value?.maxFileDescriptors
  if (open === undefined || open < 0) {
    return null
  }
  return { open, max: max !== undefined && max > 0 ? max : null }
})

const percentText = (ratio: number | undefined): string =>
  ratio === undefined ? '--' : formatPercent(ratio)

const percentAxis = (value: number): string => `${value.toFixed(1)}%`
const rateAxis = (value: number): string => formatRate(value)
/** 堆曲线以 MB 存储，展示时换算回容量单位，和左侧的「已用 / 上限」保持同一量纲 */
const heapAxis = (value: number): string => formatBytes(value * 1048576)
const countAxis = (value: number): string => `${Math.round(value)} 条`
const threadText = (value: number | undefined): string =>
  value === undefined ? '--' : `${value} 条`

/** 六项主要性能，顺序固定：CPU → 内存 → 网络 → 磁盘 → JVM 堆 → 线程与进程 */
const metricRows = computed<MetricRow[]>(() => {
  if (!props.metrics) {
    return []
  }

  const cpuHistory = pick('cpu')
  const memoryHistory = pick('memory')
  const rxHistory = pick('rx')
  const txHistory = pick('tx')
  const readHistory = pick('read')
  const writeHistory = pick('write')
  const heapHistory = pick('heap')
  const hostThreadHistory = pick('hostThreads')
  const jvmThreadHistory = pick('jvmThreads')

  const cpuRow: MetricRow = {
    key: 'cpu',
    label: 'CPU 使用率',
    icon: 'cpu',
    value: percentText(cpuUsage.value),
    valueClass: 'text-2xl',
    hint:
      `逻辑线程 ${cpu.value?.logicalCores ?? '--'} 条 · ` +
      `控制台进程 ${percentText(cpu.value?.processUsage)}` +
      (loadAverage.value ? ` · 负载均值 ${loadAverage.value}` : ''),
    max: 100,
    format: percentAxis,
    series: [{ name: '使用率', color: COLOR.cpu, data: cpuHistory }],
    legend: [{ label: '使用率', color: COLOR.cpu, text: percentText(cpuUsage.value) }],
    peak: (() => {
      const text = peakOf(cpuHistory, percentAxis)
      return text ? { color: COLOR.cpu, text } : null
    })()
  }

  const memoryRow: MetricRow = {
    key: 'memory',
    label: '内存使用率',
    icon: 'memory',
    value: percentText(memory.value?.usage),
    valueClass: 'text-2xl',
    hint:
      `已用 ${formatBytes(memory.value?.usedBytes)} / ${formatBytes(memory.value?.totalBytes)} · ` +
      `交换分区 ${percentText(memory.value?.swapUsage)}`,
    max: 100,
    format: percentAxis,
    series: [{ name: '使用率', color: COLOR.memory, data: memoryHistory }],
    legend: [{ label: '使用率', color: COLOR.memory, text: percentText(memory.value?.usage) }],
    peak: (() => {
      const text = peakOf(memoryHistory, percentAxis)
      return text ? { color: COLOR.memory, text } : null
    })()
  }

  const networkRow: MetricRow = {
    key: 'network',
    label: '网络吞吐',
    icon: 'activity',
    pair: [
      {
        label: '发送',
        symbol: '↑',
        color: COLOR.tx,
        text: formatRate(network.value?.txBytesPerSec)
      },
      {
        label: '接收',
        symbol: '↓',
        color: COLOR.rx,
        text: formatRate(network.value?.rxBytesPerSec)
      }
    ],
    valueClass: 'text-base',
    hint:
      `活跃网卡 ${(network.value?.activeInterfaces ?? []).length} 块 · ` +
      `累计 ↑ ${formatBytes(network.value?.txBytesTotal)} · ↓ ${formatBytes(network.value?.rxBytesTotal)}`,
    max: null,
    format: rateAxis,
    series: [
      { name: '发送', color: COLOR.tx, data: txHistory },
      { name: '接收', color: COLOR.rx, data: rxHistory }
    ],
    legend: [
      { label: '发送', color: COLOR.tx },
      { label: '接收', color: COLOR.rx }
    ],
    peak: (() => {
      const text = peakOf(rxHistory, rateAxis)
      return text ? { color: COLOR.rx, text, series: '接收' } : null
    })()
  }

  const diskRow: MetricRow = {
    key: 'disk',
    label: '磁盘吞吐',
    icon: 'drive',
    pair: [
      { label: '读取', color: COLOR.read, text: formatRate(disk.value?.readBytesPerSec) },
      { label: '写入', color: COLOR.write, text: formatRate(disk.value?.writeBytesPerSec) }
    ],
    valueClass: 'text-base',
    hint:
      `分区 ${(disk.value?.partitions ?? []).length} 个 · ` +
      `累计读 ${formatBytes(disk.value?.readBytesTotal)} · 写 ${formatBytes(disk.value?.writeBytesTotal)}`,
    max: null,
    format: rateAxis,
    series: [
      { name: '读取', color: COLOR.read, data: readHistory },
      { name: '写入', color: COLOR.write, data: writeHistory }
    ],
    legend: [
      { label: '读取', color: COLOR.read },
      { label: '写入', color: COLOR.write }
    ],
    peak: (() => {
      const text = peakOf(readHistory, rateAxis)
      return text ? { color: COLOR.read, text, series: '读取' } : null
    })()
  }

  const heapRow: MetricRow = {
    key: 'heap',
    label: 'JVM 堆内存',
    icon: 'cube',
    value: percentText(jvm.value?.heapUsage),
    valueClass: 'text-2xl',
    hint:
      `已用 ${formatBytes(jvm.value?.heapUsedBytes)} / ${formatBytes(jvm.value?.heapMaxBytes)} · ` +
      `非堆 ${formatBytes(jvm.value?.nonHeapUsedBytes)}`,
    max: null,
    format: heapAxis,
    series: [{ name: '堆已用', color: COLOR.heap, data: heapHistory }],
    legend: [{ label: '堆已用', color: COLOR.heap, text: formatBytes(jvm.value?.heapUsedBytes) }],
    peak: (() => {
      const text = peakOf(heapHistory, heapAxis)
      return text ? { color: COLOR.heap, text } : null
    })()
  }

  const threadRow: MetricRow = {
    key: 'threads',
    label: '线程与进程',
    icon: 'list',
    value: threadText(system.value?.threadCount),
    valueClass: 'text-xl',
    hint:
      `主机进程 ${system.value?.processCount ?? '--'} 个 · ` +
      `控制台 JVM ${jvm.value?.threadCount ?? '--'} / 峰值 ${jvm.value?.peakThreadCount ?? '--'}`,
    max: null,
    format: countAxis,
    series: [
      { name: '主机线程', color: COLOR.hostThreads, data: hostThreadHistory },
      { name: '控制台 JVM', color: COLOR.jvmThreads, data: jvmThreadHistory }
    ],
    legend: [
      { label: '主机线程', color: COLOR.hostThreads, text: threadText(system.value?.threadCount) },
      { label: '控制台 JVM', color: COLOR.jvmThreads, text: threadText(jvm.value?.threadCount) }
    ],
    peak: (() => {
      const text = peakOf(hostThreadHistory, countAxis)
      return text ? { color: COLOR.hostThreads, text } : null
    })()
  }

  return [cpuRow, memoryRow, networkRow, diskRow, heapRow, threadRow]
})

const uptimeText = computed(() => formatUptimeSeconds(system.value?.uptimeSeconds))

const hasAnyData = computed(
  () => props.metrics?.cpu !== undefined || props.metrics?.memory !== undefined
)
</script>

<template>
  <div>
    <div v-if="loading && !hasAnyData" class="py-6 text-center text-xs text-slate-400">
      正在采样实时性能…
    </div>

    <template v-else-if="metricRows.length > 0">
      <!-- 六项主要性能：左值右曲线，依次纵向排列 -->
      <div class="space-y-4">
        <div
          v-for="row in metricRows"
          :key="row.key"
          class="flex flex-col gap-2 border-t border-slate-100 pt-4 first:border-0 first:pt-0 sm:flex-row sm:gap-4"
        >
          <!-- 左栏宽度用 clamp：窄屏保住文字不折行（208px 起），宽屏把余量让给曲线（上限 272px） -->
          <div class="sm:w-[clamp(13rem,42%,17rem)] sm:shrink-0">
            <p class="flex items-center gap-1.5 text-xs text-slate-500">
              <AppIcon :name="row.icon" :size="13" />
              {{ row.label }}
            </p>

            <!-- 双值指标：发送 / 接收、读取 / 写入 左右并列 -->
            <div v-if="row.pair" class="mt-1.5 flex items-start gap-x-3">
              <div v-for="item in row.pair" :key="item.label" class="min-w-0 flex-1">
                <p class="flex items-center gap-1 text-[11px] leading-3 whitespace-nowrap text-slate-400">
                  <span
                    v-if="item.symbol"
                    class="text-xs leading-3 font-semibold"
                    :style="{ color: item.color }"
                  >
                    {{ item.symbol }}
                  </span>
                  <span
                    v-else
                    class="h-1.5 w-1.5 shrink-0 rounded-full"
                    :style="{ background: item.color }"
                  />
                  {{ item.label }}
                </p>
                <p
                  class="tabular mt-1 leading-6 font-semibold whitespace-nowrap text-slate-900"
                  :class="row.valueClass"
                >
                  {{ item.text }}
                </p>
              </div>
            </div>

            <p
              v-else
              class="tabular mt-1 font-semibold leading-tight text-slate-900"
              :class="row.valueClass"
            >
              {{ row.value }}
            </p>
            <p class="mt-1 text-[11px] leading-4 text-slate-400">{{ row.hint }}</p>
            <p
              v-if="row.peak"
              class="mt-0.5 flex items-center gap-1 text-[11px] leading-4 text-slate-400"
              :title="
                row.peak.series
                  ? `${row.peak.series} · 最近 5 分钟峰值 / 谷值`
                  : '最近 5 分钟峰值 / 谷值'
              "
            >
              <span class="h-1.5 w-1.5 shrink-0 rounded-full" :style="{ background: row.peak.color }" />
              <span class="tabular">{{ row.peak.text }}</span>
            </p>
          </div>

          <div class="min-w-0 flex-1">
            <!-- 图例固定单行：数值长度变化时不折行，各行高度才能稳定 -->
            <div
              class="flex items-center gap-2.5 overflow-hidden text-[11px] leading-4 whitespace-nowrap text-slate-400"
            >
              <span
                v-for="item in row.legend"
                :key="item.label"
                class="inline-flex items-center gap-1"
              >
                <span class="h-1.5 w-1.5 shrink-0 rounded-full" :style="{ background: item.color }" />
                {{ item.label }}
                <span v-if="item.text" class="tabular text-slate-600">{{ item.text }}</span>
              </span>
            </div>

            <div class="mt-1 overflow-hidden rounded-lg bg-slate-50/70">
              <RealtimeAreaChart
                :labels="labels"
                :series="row.series"
                :max="row.max"
                :format-value="row.format"
                :height="72"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- CPU 时间构成 -->
      <div class="mt-4 border-t border-dashed border-slate-200 pt-3">
        <div class="flex items-center justify-between">
          <p class="text-xs font-medium text-slate-500">CPU 时间构成</p>
          <p class="text-[11px] text-slate-400">逻辑线程 {{ cpu?.logicalCores ?? '--' }} 条</p>
        </div>

        <div
          v-if="cpuSegments.length > 0"
          class="mt-2 flex h-2.5 w-full overflow-hidden rounded-full bg-slate-100"
        >
          <div
            v-for="segment in cpuSegments"
            :key="segment.key"
            :class="segment.color"
            :style="{ width: `${segment.ratio * 100}%` }"
            :title="`${segment.label} ${formatPercent(segment.ratio)}`"
          />
        </div>
        <div v-else class="mt-2 h-2.5 w-full rounded-full bg-slate-100" />

        <div class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
          <span
            v-for="segment in cpuSegments"
            :key="segment.key"
            class="inline-flex items-center gap-1 text-[11px] text-slate-500"
          >
            <span class="h-1.5 w-1.5 rounded-full" :class="segment.color" />
            {{ segment.label }}
            <span class="tabular text-slate-700">{{ formatPercent(segment.ratio) }}</span>
          </span>
          <span v-if="cpuSegments.length === 0" class="text-[11px] text-slate-400">
            首次采样完成后再来的下一次刷新即可看到构成
          </span>
        </div>
      </div>

      <!-- 主机与 JVM 明细 -->
      <dl
        class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-dashed border-slate-200 pt-3 text-[11px] text-slate-500"
      >
        <div v-if="loadAverage" class="flex items-center gap-1">
          <dt>负载均值</dt>
          <dd class="tabular text-slate-700">{{ loadAverage }}</dd>
        </div>
        <div class="flex items-center gap-1">
          <dt>主机运行</dt>
          <dd class="tabular text-slate-700">{{ uptimeText }}</dd>
        </div>
        <div class="flex items-center gap-1">
          <dt>上下文切换 / 中断</dt>
          <dd class="tabular text-slate-700">
            {{ cpu?.contextSwitches ?? '--' }} / {{ cpu?.interrupts ?? '--' }}
          </dd>
        </div>
        <div v-if="fileDescriptors" class="flex items-center gap-1">
          <dt>打开文件描述符</dt>
          <dd class="tabular text-slate-700">
            {{ fileDescriptors.open }}
            {{ fileDescriptors.max === null ? '' : ` / ${fileDescriptors.max}` }}
          </dd>
        </div>
        <div class="flex items-center gap-1">
          <dt>JVM 运行</dt>
          <dd class="tabular text-slate-700">
            {{ formatUptimeSeconds((jvm?.uptimeMillis ?? 0) / 1000) }}
          </dd>
        </div>
      </dl>

      <!-- 分区占用 -->
      <div v-if="partitions.length > 0" class="mt-3 border-t border-dashed border-slate-200 pt-3">
        <p class="text-xs font-medium text-slate-500">分区占用</p>
        <div class="mt-2 space-y-2">
          <div
            v-for="item in partitions"
            :key="`${item.mount}-${item.name}`"
            class="flex items-center gap-3"
          >
            <span class="w-40 shrink-0 truncate text-[11px] text-slate-600" :title="item.mount">
              {{ item.mount }}
            </span>
            <div class="min-w-0 flex-1">
              <LoadBar :percent="item.usage * 100" :show-value="false" size="sm" />
            </div>
            <span class="tabular w-11 shrink-0 text-right text-[11px] text-slate-500">
              {{ formatPercent(item.usage) }}
            </span>
            <span class="tabular w-24 shrink-0 text-right text-[11px] text-slate-400">
              {{ formatBytes(item.totalBytes) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 活跃网卡明细 -->
      <div v-if="activeNics.length > 0" class="mt-3 border-t border-dashed border-slate-200 pt-3">
        <p class="text-xs font-medium text-slate-500">活跃网卡</p>
        <div class="mt-2 space-y-1.5">
          <div
            v-for="nic in activeNics"
            :key="nic.name"
            class="flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-slate-500"
          >
            <span class="font-medium text-slate-700">{{ nic.displayName || nic.name }}</span>
            <span v-if="nic.ipv4.length > 0" class="tabular">{{ nic.ipv4.join(', ') }}</span>
            <span class="tabular text-slate-700">↑ {{ formatRate(nic.txBytesPerSec) }}</span>
            <span class="tabular text-slate-700">↓ {{ formatRate(nic.rxBytesPerSec) }}</span>
            <span v-if="nic.outErrors > 0 || nic.inErrors > 0" class="text-amber-600">
              错包 收 {{ nic.inErrors }} / 发 {{ nic.outErrors }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <p v-else-if="!loading" class="py-4 text-xs text-slate-400">未能读取主机性能数据。</p>
  </div>
</template>
