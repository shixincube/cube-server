<script setup lang="ts">
/**
 * AI单元概览。
 *
 * 展示 AI 单元的台账与工作状态。
 *
 * 统计口径：ID 十进制位数大于等于 6 且小于 8 位的联系人是 AI 单元节点，
 * 位数大于等于 8 位的是联系人（见「联系人概览」）；位数小于 6 位的不是统计对象。
 *
 * 数据来源为统计库 `/statistic/units`：聚合统计按所选日（默认昨日）计算，
 * 工作状态按最近一次上报活动时间判定。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { fetchDomains, fetchUnitOverview, pickList } from '@/api/console'
import type { UnitInfo, UnitOverviewResponse, UnitState } from '@/api/types'
import { useChart } from '@/composables/useChart'
import { useToastStore } from '@/stores/toast'
import { formatTimeMDHMS } from '@/utils/format'
import type { EChartsCoreOption } from '@/charts/echarts'

const toast = useToastStore()

const domains = ref<string[]>([])
const domain = ref('')
const overview = ref<UnitOverviewResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const chartEl = ref<HTMLElement | null>(null)

/** 工作状态的展示样式与文案 */
const STATE_META: Record<UnitState, { label: string; pill: string; dot: string }> = {
  online: {
    label: '在线',
    pill: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 ring-inset',
    dot: 'bg-emerald-500'
  },
  offline: {
    label: '离线',
    pill: 'bg-amber-50 text-amber-700 ring-1 ring-amber-200 ring-inset',
    dot: 'bg-amber-500'
  },
  inactive: {
    label: '无活动',
    pill: 'bg-slate-100 text-slate-600 ring-1 ring-slate-200 ring-inset',
    dot: 'bg-slate-400'
  }
}

const stateMeta = (state: UnitState) => STATE_META[state] ?? STATE_META.inactive

const units = computed<UnitInfo[]>(() => overview.value?.units ?? [])

const activeOffset = computed(() => {
  const total = overview.value?.total ?? 0
  if (total === 0) {
    return ''
  }
  return `${overview.value?.activeCount ?? 0} / ${total}`
})

/** 昨日活跃峰值时段 */
const peakText = computed(() => {
  const peak = overview.value?.peak
  if (!peak) {
    return '--:-- - --:-- # --'
  }
  const fmt = (time: number) => `${String(new Date(time).getHours()).padStart(2, '0')}:00`
  return `${fmt(peak.beginning)} - ${fmt(peak.ending)} # ${peak.numUnits}`
})

/** 时段分布图：按小时聚合活跃单元数 */
const chartOption = computed<EChartsCoreOption>(() => {
  const slices = overview.value?.timeline ?? []
  const labels = slices.map((item) => `${String(new Date(item.beginning).getHours()).padStart(2, '0')}:00`)
  const values = slices.map((item) => item.numUnits)
  const peak = overview.value?.peak?.beginning

  return {
    grid: { top: 24, right: 12, bottom: 28, left: 52 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: '单元',
      nameTextStyle: { color: '#94a3b8', fontSize: 10 },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 26,
        data: values.map((value, index) => ({
          value,
          itemStyle: {
            color: slices[index].beginning === peak ? '#f59e0b' : '#34d399',
            borderRadius: [3, 3, 0, 0]
          }
        }))
      }
    ]
  }
})

/** 只有拿到时段分布数据后才渲染图表容器 */
const hasTimelineData = computed(() => (overview.value?.timeline?.length ?? 0) > 0)

const { update: renderChart } = useChart(chartEl, chartOption)

watch(hasTimelineData, async (ready) => {
  if (!ready) {
    return
  }
  // 容器由 v-if 控制，需等 DOM 就绪后再初始化图表
  await nextTick()
  renderChart()
})

const formatHours = (value: number | undefined): string =>
  undefined === value ? '--' : value.toFixed(2)

function formatActiveTime(time: number): string {
  return time > 0 ? formatTimeMDHMS(time) : '--'
}

/** 切换域后重新拉取统计 */
async function load(): Promise<void> {
  if (!domain.value) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    overview.value = await fetchUnitOverview(domain.value)
  } catch (error) {
    overview.value = null
    errorMessage.value =
      error instanceof Error ? error.message : `加载域「${domain.value}」的单元统计失败`
    toast.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

async function bootstrap(): Promise<void> {
  loading.value = true
  try {
    const response = await fetchDomains()
    domains.value = pickList(response)
    if (domains.value.length > 0) {
      domain.value = domains.value[0]
      await load()
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载域列表失败'
  } finally {
    loading.value = false
  }
}

watch(domain, () => {
  void load()
})

onMounted(bootstrap)
</script>

<template>
  <div>
    <PageHeader
      title="AI单元概览"
      description="按域查看 AI 单元的台账与工作状态"
    >
      <template #actions>
        <select v-model="domain" class="select w-52" :disabled="domains.length === 0">
          <option v-if="domains.length === 0" value="">暂无可用域</option>
          <option v-for="item in domains" :key="item" :value="item">{{ item }}</option>
        </select>
        <button type="button" class="btn btn-ghost" :disabled="loading" @click="load">
          <AppIcon name="refresh" :size="14" :class="loading ? 'animate-spin' : ''" />
          刷新
        </button>
      </template>
    </PageHeader>

    <div
      v-if="overview"
      class="mb-4 flex flex-wrap items-center gap-x-5 gap-y-1 text-xs text-slate-500"
    >
      <span class="tabular">
        统计日期：
        {{ overview.year }}-{{ String(overview.month).padStart(2, '0') }}-{{
          String(overview.date).padStart(2, '0')
        }}
      </span>
      <span>数据域：{{ domain }}</span>
      <span class="text-slate-400">AI单元</span>
    </div>

    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        label="AI单元总数"
        :value="overview?.total ?? '--'"
        icon="cube"
        tone="brand"
        hint="AI能力节点数量"
      />
      <StatCard
        label="当前在线单元"
        :value="overview?.onlineCount ?? '--'"
        icon="bolt"
        tone="emerald"
        :hint="`最近 24 小时内有上报活动 · 离线 ${overview?.offlineCount ?? 0} · 无活动 ${overview?.inactiveCount ?? 0}`"
      />
      <StatCard
        label="统计日活跃单元"
        :value="activeOffset || '--'"
        icon="activity"
        tone="rose"
        hint="当日有过上报活动的单元数 / 总数"
      />
      <StatCard
        label="统计日平均在线时长"
        :value="formatHours(overview?.avgDuration)"
        unit="小时"
        icon="gauge"
        tone="amber"
        :hint="`活跃单元合计 ${formatHours(overview?.totalDuration)} 小时`"
      />
    </div>

    <section class="card mt-5 overflow-hidden">
      <div class="card-header">
        <div>
          <h2 class="card-title">单元活跃时段分布</h2>
          <p class="mt-0.5 text-xs text-slate-400">按小时聚合的活跃单元数，橙色柱为峰值时段</p>
        </div>
        <span class="pill" :class="overview?.peak ? 'pill-info' : 'pill-neutral'">
          <AppIcon name="bolt" :size="12" />
          峰值时段 {{ peakText }}
        </span>
      </div>
      <div class="card-body">
        <div v-if="hasTimelineData" ref="chartEl" class="h-64 w-full" />
        <EmptyState
          v-else
          icon="activity"
          :title="errorMessage || '暂无时段分布数据'"
          :description="errorMessage ? '' : '统计日内没有单元上报活动'"
        />
      </div>
    </section>

    <section class="card mt-5 overflow-hidden">
      <div class="card-header">
        <div>
          <h2 class="card-title">AI单元台账</h2>
          <p class="mt-0.5 text-xs text-slate-400">
            「在线」为最近 24 小时内有上报活动；「无活动」为近 30 天内没有上报活动
          </p>
        </div>
        <span class="pill pill-neutral">
          <AppIcon name="cube" :size="12" />
          {{ units.length }} 个单元
        </span>
      </div>

      <div v-if="units.length > 0" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th class="w-14">#</th>
              <th class="w-56">单元</th>
              <th class="w-24">状态</th>
              <th class="w-24">当日活动</th>
              <th class="w-28">当日在线时长</th>
              <th class="w-40">最近活动</th>
              <th class="w-32">近 30 天活跃</th>
              <th>设备</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(unit, index) in units" :key="unit.id">
              <td class="tabular text-slate-400">{{ index + 1 }}</td>
              <td>
                <span class="font-medium text-slate-800">{{ unit.name || '未命名单元' }}</span>
                <p class="tabular text-[11px] text-slate-400">{{ unit.id }}</p>
              </td>
              <td>
                <span class="pill" :class="stateMeta(unit.state).pill">
                  <span class="pill-dot" :class="stateMeta(unit.state).dot" />
                  {{ stateMeta(unit.state).label }}
                </span>
              </td>
              <td class="tabular text-slate-600">
                {{ unit.activeCount > 0 ? `${unit.activeCount} 次` : '--' }}
              </td>
              <td class="tabular text-slate-600">
                {{ unit.activeCount > 0 ? `${unit.duration.toFixed(2)} 小时` : '--' }}
              </td>
              <td class="tabular text-xs text-slate-600">
                {{ formatActiveTime(unit.lastActiveTime) }}
              </td>
              <td class="tabular text-slate-600">{{ unit.activeDays }} 天</td>
              <td>
                <span class="text-xs text-slate-600">{{ unit.device || '--' }}</span>
                <p v-if="unit.platform" class="text-[11px] text-slate-400">{{ unit.platform }}</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <EmptyState
        v-else
        icon="cube"
        :title="errorMessage || '该域下暂无 AI 单元'"
        :description="
          errorMessage ? '' : 'AI 单元显示在这里'
        "
      />
    </section>
  </div>
</template>
