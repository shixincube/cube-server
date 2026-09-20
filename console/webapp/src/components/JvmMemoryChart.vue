<script setup lang="ts">
/** JVM 内存趋势图（堆总量 / 空闲量，单位 MB）。 */
import { computed, ref } from 'vue'
import { useChart } from '@/composables/useChart'
import type { JVMSeries } from '@/api/monitor'
import type { EChartsCoreOption } from '@/charts/echarts'

const props = withDefaults(
  defineProps<{
    series: JVMSeries
    height?: number
  }>(),
  {
    height: 210
  }
)

const el = ref<HTMLElement | null>(null)

const option = computed<EChartsCoreOption>(() => ({
  grid: { top: 32, right: 12, bottom: 24, left: 46 },
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
    data: props.series.labels,
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
      barMaxWidth: 14,
      itemStyle: { color: '#6366f1', borderRadius: [0, 0, 0, 0] },
      data: props.series.used
    },
    {
      name: '空闲内存',
      type: 'bar',
      stack: 'memory',
      barMaxWidth: 14,
      itemStyle: { color: '#cbd5e1', borderRadius: [3, 3, 0, 0] },
      data: props.series.free
    }
  ]
}))

useChart(el, option)
</script>

<template>
  <div ref="el" :style="{ height: `${height}px` }" class="w-full" />
</template>
