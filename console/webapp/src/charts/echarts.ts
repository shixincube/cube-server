/**
 * ECharts 按需注册。
 *
 * 只引入控制台用到的图表类型与组件，避免把整个 ECharts 打进产物。
 */

import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([
  BarChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer
])

export { init } from 'echarts/core'
export type { ECharts, EChartsCoreOption } from 'echarts/core'
