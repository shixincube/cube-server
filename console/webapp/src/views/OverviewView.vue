<script setup lang="ts">
/**
 * 联系人概览。
 *
 * 数据来源为统计库 `/statistic/recent`，展示所选域昨日的联系人指标与时段分布。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { fetchDomains, fetchRecentStatistic, pickList } from '@/api/console'
import type { StatisticResponse } from '@/api/types'
import { useChart } from '@/composables/useChart'
import { useToastStore } from '@/stores/toast'
import type { EChartsCoreOption } from '@/charts/echarts'

const toast = useToastStore()

const domains = ref<string[]>([])
const domain = ref('')
const statistic = ref<StatisticResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const chartEl = ref<HTMLElement | null>(null)

/** 峰值时段：取 contacts 数最大的切片 */
const peakSlice = computed(() => {
  const slices = statistic.value?.statistic.TD ?? []
  if (slices.length === 0) {
    return null
  }
  return slices.reduce((max, item) => (item.numContacts > max.numContacts ? item : max), slices[0])
})

const peakText = computed(() => {
  const slice = peakSlice.value
  if (!slice) {
    return '--:-- - --:-- # --'
  }
  const begin = new Date(slice.beginning)
  const end = new Date(slice.ending)
  const fmt = (date: Date) => `${String(date.getHours()).padStart(2, '0')}:00`
  return `${fmt(begin)} - ${fmt(end)} # ${slice.numContacts}`
})

/** 平均在线时长（小时），保留两位小数 */
const aotText = computed(() => {
  const value = statistic.value?.statistic.AOT
  return undefined === value || null === value ? '--' : Number(value).toFixed(2)
})

/** 时段分布图：按小时聚合联系人数量 */
const chartOption = computed<EChartsCoreOption>(() => {
  const slices = statistic.value?.statistic.TD ?? []
  const sorted = [...slices].sort((a, b) => a.beginning - b.beginning)
  const labels = sorted.map((item) => {
    const date = new Date(item.beginning)
    return `${String(date.getHours()).padStart(2, '0')}:00`
  })
  const values = sorted.map((item) => item.numContacts)
  const peak = peakSlice.value?.beginning

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
      name: '联系人',
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
            color: sorted[index].beginning === peak ? '#f59e0b' : '#818cf8',
            borderRadius: [3, 3, 0, 0]
          }
        }))
      }
    ]
  }
})

/** 只有拿到时段分布数据后才渲染图表容器 */
const hasTimelineData = computed(() => (statistic.value?.statistic.TD?.length ?? 0) > 0)

const { update: renderChart } = useChart(chartEl, chartOption)

watch(hasTimelineData, async (ready) => {
  if (!ready) {
    return
  }
  // 容器由 v-if 控制，需等 DOM 就绪后再初始化图表
  await nextTick()
  renderChart()
})

/** 切换域后重新拉取统计 */
async function load(): Promise<void> {
  if (!domain.value) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    statistic.value = await fetchRecentStatistic(domain.value)
  } catch (error) {
    statistic.value = null
    errorMessage.value =
      error instanceof Error ? error.message : `加载域「${domain.value}」统计数据失败`
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
      title="联系人概览"
      description="按域查看昨日联系人规模、活跃度与在线时段分布"
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
      v-if="statistic"
      class="mb-4 flex flex-wrap items-center gap-x-5 gap-y-1 text-xs text-slate-500"
    >
      <span class="tabular">
        统计日期：
        {{ statistic.year }}-{{ String(statistic.month).padStart(2, '0') }}-{{
          String(statistic.date).padStart(2, '0')
        }}
      </span>
      <span>数据域：{{ domain }}</span>
      <span class="text-slate-400">联系人</span>
    </div>

    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        label="昨日新增 / 联系人总数"
        :value="statistic ? `${statistic.statistic.DNU ?? 0} / ${statistic.statistic.TNU}` : '--'"
        icon="users"
        tone="brand"
        hint="DNU 为与前一日总数之差"
      />
      <StatCard
        label="昨日活跃联系人数"
        :value="statistic?.statistic.DAU ?? '--'"
        icon="activity"
        tone="emerald"
        hint="DAU"
      />
      <StatCard
        label="昨日平均在线时长"
        :value="aotText"
        unit="小时"
        icon="gauge"
        tone="amber"
        hint="昨日所有登录联系人的平均在线时长"
      />
      <StatCard
        label="昨日峰值时段"
        :value="peakText"
        icon="bolt"
        tone="rose"
        hint="时段内联系人数量最高的切片"
      />
    </div>

    <section class="card mt-5 overflow-hidden">
      <div class="card-header">
        <div>
          <h2 class="card-title">在线时段分布</h2>
          <p class="mt-0.5 text-xs text-slate-400">按小时聚合的联系人数量，橙色柱为峰值时段</p>
        </div>
        <span v-if="peakSlice" class="pill pill-info">
          <AppIcon name="bolt" :size="12" />
          峰值 {{ peakSlice.numContacts }}
        </span>
      </div>
      <div class="card-body">
        <div v-if="hasTimelineData" ref="chartEl" class="h-64 w-full" />
        <EmptyState
          v-else
          icon="activity"
          :title="errorMessage || '暂无时段分布数据'"
          :description="errorMessage ? '' : '统计服务上报数据后，这里会显示各时段的活跃情况'"
        />
      </div>
    </section>
  </div>
</template>
