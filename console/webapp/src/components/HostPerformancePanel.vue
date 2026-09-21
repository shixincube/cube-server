<script setup lang="ts">
/**
 * 主机实时性能（动态信息）。
 *
 * 与「硬件配置」区在同一张卡片内以分隔线区分：这里全部是随采样变化的指标，
 * 统一按「指标块 → CPU 构成 → 明细 → 分区」四层由上到下排列，不使用 Tab，
 * 目的是一次扫读即可看清主机当前的压力来源。
 */
import { computed } from 'vue'
import type { HostMetricsResponse } from '@/api/types'
import {
  formatBytes,
  formatPercent,
  formatRate,
  formatUptimeSeconds
} from '@/utils/format'
import LoadBar from './LoadBar.vue'

const props = defineProps<{
  metrics: HostMetricsResponse | null
  loading?: boolean
}>()

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

const cpu = computed(() => props.metrics?.cpu)
const memory = computed(() => props.metrics?.memory)
const network = computed(() => props.metrics?.network)
const disk = computed(() => props.metrics?.disk)
const jvm = computed(() => props.metrics?.jvm)
const system = computed(() => props.metrics?.system)

/** 系统 CPU 使用率，首次采样无基准时为空 */
const cpuUsage = computed(() => cpu.value?.usage)

/** 非空闲占比，用于 CPU 构成条的图例顺序 */
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

/** 主机运行时长（秒） */
const uptimeText = computed(() => formatUptimeSeconds(system.value?.uptimeSeconds))

/** 打开文件描述符，取不到时返回 null 以便整行隐藏 */
const fileDescriptors = computed(() => {
  const open = system.value?.openFileDescriptors
  const max = system.value?.maxFileDescriptors
  if (open === undefined || open < 0) {
    return null
  }
  return { open, max: max !== undefined && max > 0 ? max : null }
})

const hasAnyData = computed(
  () =>
    undefined !== cpuUsage.value ||
    undefined !== memory.value ||
    undefined !== network.value ||
    undefined !== disk.value
)
</script>

<template>
  <div>
    <div v-if="loading && !hasAnyData" class="py-6 text-center text-xs text-slate-400">
      正在采样实时性能…
    </div>

    <template v-else-if="hasAnyData">
      <!-- 第一层：四块核心指标 -->
      <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div class="rounded-lg bg-slate-50 px-3 py-2.5">
          <p class="text-xs text-slate-500">CPU 使用率</p>
          <p class="tabular mt-1 text-xl font-semibold text-slate-900">
            {{ cpuUsage === undefined ? '--' : formatPercent(cpuUsage) }}
          </p>
          <div class="mt-1.5">
            <LoadBar :percent="(cpuUsage ?? 0) * 100" :show-value="false" size="sm" />
          </div>
          <p class="mt-1 text-[11px] text-slate-400">
            控制台进程 {{ formatPercent(cpu?.processUsage ?? 0) }}
          </p>
        </div>

        <div class="rounded-lg bg-slate-50 px-3 py-2.5">
          <p class="text-xs text-slate-500">内存使用率</p>
          <p class="tabular mt-1 text-xl font-semibold text-slate-900">
            {{ formatPercent(memory?.usage ?? 0) }}
          </p>
          <div class="mt-1.5">
            <LoadBar :percent="(memory?.usage ?? 0) * 100" :show-value="false" size="sm" />
          </div>
          <p class="mt-1 text-[11px] text-slate-400">
            {{ formatBytes(memory?.usedBytes) }} / {{ formatBytes(memory?.totalBytes) }}
          </p>
        </div>

        <div class="rounded-lg bg-slate-50 px-3 py-2.5">
          <p class="text-xs text-slate-500">网络吞吐</p>
          <p class="tabular mt-1 text-base font-semibold text-slate-900">
            ↓ {{ formatRate(network?.rxBytesPerSec) }}
          </p>
          <p class="tabular text-base font-semibold text-slate-900">
            ↑ {{ formatRate(network?.txBytesPerSec) }}
          </p>
          <p class="mt-1 text-[11px] text-slate-400">
            累计 ↓ {{ formatBytes(network?.rxBytesTotal) }} · ↑ {{ formatBytes(network?.txBytesTotal) }}
          </p>
        </div>

        <div class="rounded-lg bg-slate-50 px-3 py-2.5">
          <p class="text-xs text-slate-500">磁盘吞吐</p>
          <p class="tabular mt-1 text-base font-semibold text-slate-900">
            读 {{ formatRate(disk?.readBytesPerSec) }}
          </p>
          <p class="tabular text-base font-semibold text-slate-900">
            写 {{ formatRate(disk?.writeBytesPerSec) }}
          </p>
          <p class="mt-1 text-[11px] text-slate-400">
            累计读 {{ formatBytes(disk?.readBytesTotal) }} · 写
            {{ formatBytes(disk?.writeBytesTotal) }}
          </p>
        </div>
      </div>

      <!-- 第二层：CPU 时间构成 -->
      <div class="mt-4">
        <div class="flex items-center justify-between">
          <p class="text-xs font-medium text-slate-500">CPU 时间构成</p>
          <p class="text-[11px] text-slate-400">
            逻辑线程 {{ cpu?.logicalCores ?? '--' }} 条
          </p>
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

      <!-- 第三层：主机与 JVM 明细 -->
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
          <dt>进程 / 线程</dt>
          <dd class="tabular text-slate-700">
            {{ system?.processCount ?? '--' }} / {{ system?.threadCount ?? '--' }}
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
          <dt>控制台 JVM 堆</dt>
          <dd class="tabular text-slate-700">
            {{ formatBytes(jvm?.heapUsedBytes) }} / {{ formatBytes(jvm?.heapMaxBytes) }}
            ({{ formatPercent(jvm?.heapUsage ?? 0) }})
          </dd>
        </div>
        <div class="flex items-center gap-1">
          <dt>JVM 线程</dt>
          <dd class="tabular text-slate-700">
            {{ jvm?.threadCount ?? '--' }} / 峰值 {{ jvm?.peakThreadCount ?? '--' }}
          </dd>
        </div>
      </dl>

      <!-- 第四层：分区占用 -->
      <div v-if="partitions.length > 0" class="mt-3 border-t border-dashed border-slate-200 pt-3">
        <p class="text-xs font-medium text-slate-500">分区占用</p>
        <div class="mt-2 space-y-2">
          <div v-for="item in partitions" :key="`${item.mount}-${item.name}`" class="flex items-center gap-3">
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

      <!-- 第五层：活跃网卡明细 -->
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
            <span class="tabular text-slate-700">↓ {{ formatRate(nic.rxBytesPerSec) }}</span>
            <span class="tabular text-slate-700">↑ {{ formatRate(nic.txBytesPerSec) }}</span>
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
