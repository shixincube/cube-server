/**
 * ECharts 生命周期封装。
 *
 * 负责实例创建、配置响应式更新、容器尺寸变化重绘与销毁，业务组件只需提供配置。
 */

import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'
import { init, type ECharts, type EChartsCoreOption } from '@/charts/echarts'

export function useChart(
  container: Ref<HTMLElement | null>,
  option: Ref<EChartsCoreOption> | (() => EChartsCoreOption)
) {
  let chart: ECharts | null = null
  let observer: ResizeObserver | null = null

  const resolveOption = (): EChartsCoreOption =>
    typeof option === 'function' ? option() : option.value

  /** 创建实例（幂等） */
  const render = (): void => {
    if (!container.value) {
      return
    }
    if (!chart) {
      chart = init(container.value, undefined, { renderer: 'canvas' })
    }
    chart.setOption(resolveOption(), true)
    chart.resize()
  }

  onMounted(() => {
    render()
    if (container.value) {
      observer = new ResizeObserver(() => chart?.resize())
      observer.observe(container.value)
    }
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
    chart?.dispose()
    chart = null
  })

  if (typeof option !== 'function') {
    watch(option, render, { deep: true })
  }

  return {
    /** 手动重绘，用于数据变化后主动刷新 */
    update: render,
    /** 获取底层实例，供特殊交互使用 */
    getInstance: () => chart
  }
}
