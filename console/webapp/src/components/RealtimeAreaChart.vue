<script lang="ts">
/** 单条曲线：父组件据此描述名称、配色与历史数据（与 labels 一一对应） */
export interface RealtimeSeries {
  name: string
  color: string
  data: (number | null)[]
}
</script>

<script setup lang="ts">
/**
 * 实时面积图（任务管理器风格）。
 *
 * 与 `JvmMemoryChart` 的定位不同：这里是**窄高的滚动曲线**，看的是「最近几分钟的走势」，
 * 因此刻意隐藏坐标轴文字、只留横向虚线网格与渐变填充，具体数值由父组件在图上方的
 * 图例 / 最高最低文案里给出，避免小尺寸下文字互相挤压。
 *
 * 实时刷新时**关闭动画**：数据每几秒整体重绘一次，带过渡会出现「重新长出来」的抖动。
 */
import { computed, ref } from 'vue'
import { useChart } from '@/composables/useChart'
import type { EChartsCoreOption } from '@/charts/echarts'

const props = withDefaults(
  defineProps<{
    /** 横轴刻度文案，与各序列 data 一一对应 */
    labels: string[]
    series: RealtimeSeries[]
    /** 画布高度（px） */
    height?: number
    /** 固定 y 轴上界；百分比类指标传 100 可让不同时刻的高度可直接对比 */
    max?: number | null
    /** 提示框与刻度里数值的格式化方式 */
    formatValue?: (value: number) => string
  }>(),
  {
    height: 72,
    max: null,
    formatValue: (value: number) => `${Math.round(value)}`
  }
)

const el = ref<HTMLElement | null>(null)

/** 十六进制颜色转 rgba，用于生成面积渐变 */
const rgba = (hex: string, alpha: number): string => {
  const raw = hex.replace('#', '')
  const full =
    raw.length === 3
      ? raw
          .split('')
          .map((char) => char + char)
          .join('')
      : raw
  const value = Number.parseInt(full, 16)
  return `rgba(${(value >> 16) & 255}, ${(value >> 8) & 255}, ${value & 255}, ${alpha})`
}

/** 未指定固定上界时，按历史峰值留 15% 余量，避免曲线频繁贴顶 */
const yMax = computed(() => {
  if (props.max !== null && props.max !== undefined) {
    return props.max
  }
  let peak = 0
  for (const item of props.series) {
    for (const value of item.data) {
      if (value !== null && Number.isFinite(value) && value > peak) {
        peak = value
      }
    }
  }
  return Math.max(1, peak * 1.15)
})

const option = computed<EChartsCoreOption>(() => ({
  animation: false,
  grid: { top: 4, right: 2, bottom: 4, left: 2 },
  tooltip: {
    trigger: 'axis',
    confine: true,
    padding: [4, 8],
    textStyle: { fontSize: 11 },
    valueFormatter: (value: unknown) =>
      typeof value === 'number' ? props.formatValue(value) : '--'
  },
  xAxis: {
    type: 'category',
    data: props.labels,
    boundaryGap: false,
    show: false
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: yMax.value,
    // 只保留虚线网格：任务管理器的曲线区没有纵轴刻度文字
    splitNumber: 3,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { show: false },
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } }
  },
  series: props.series.map((item) => ({
    name: item.name,
    type: 'line',
    smooth: 0.25,
    showSymbol: false,
    lineStyle: { width: 1.6, color: item.color },
    itemStyle: { color: item.color },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: rgba(item.color, 0.34) },
          { offset: 1, color: rgba(item.color, 0.02) }
        ]
      }
    },
    data: item.data
  }))
}))

useChart(el, option)
</script>

<template>
  <div ref="el" :style="{ height: `${height}px` }" class="w-full" />
</template>
