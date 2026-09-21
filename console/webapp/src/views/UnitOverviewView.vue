<script setup lang="ts">
/**
 * AI单元概览。
 *
 * 展示 AI 单元的台账与工作状态。
 *
 * 统计口径：ID 十进制位数大于等于 6 且小于 8 位的联系人是 AI 单元节点，
 * 位数大于等于 8 位的是联系人（见「联系人概览」）；位数小于 6 位的不是统计对象。
 *
 * 台账的一行是一个 Contact 物理实体：一个实体上可以注册多个 AIGC 单元（每个单元一个
 * AICapability），因此「能力」列展示的是该实体上所有单元的能力汇总。实体与活动统计来自统计库，
 * 能力来自节点周期上报（约每分钟一次）。
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
import type {
  UnitCapability,
  UnitInfo,
  UnitOverviewResponse,
  UnitState
} from '@/api/types'
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

/** 台账的展示顺序，也是筛选片的固定排列顺序 */
const STATE_ORDER: UnitState[] = ['online', 'offline', 'inactive']

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

/* ------------------------------------------------------------------ */
/* 台账的筛选与排序（纯前端操作，数据已全量取回，无需再请求后端）        */
/* ------------------------------------------------------------------ */

/** 选中的工作状态，空数组表示不筛选 */
const stateFilter = ref<UnitState[]>([])

/** 单元名排序方向，null 表示保持后端默认序（在线优先 + 最近活动降序） */
const nameSort = ref<'asc' | 'desc' | null>(null)

/** 各状态的单元数：基于全部单元统计，不随筛选变化，否则筛完就看不到别的状态有多少个 */
const stateCounts = computed<Record<UnitState, number>>(() => {
  const counts: Record<UnitState, number> = { online: 0, offline: 0, inactive: 0 }
  for (const unit of units.value) {
    counts[unit.state] = (counts[unit.state] ?? 0) + 1
  }
  return counts
})

/** 排序键：未命名单元退化为 ID，保证顺序稳定且可比较 */
const sortKeyOf = (unit: UnitInfo): string => unit.name || String(unit.id)

const visibleUnits = computed<UnitInfo[]>(() => {
  const selected = stateFilter.value
  const list =
    selected.length === 0
      ? units.value
      : units.value.filter((unit) => selected.includes(unit.state))

  const direction = nameSort.value
  if (!direction) {
    return list
  }

  const factor = direction === 'asc' ? 1 : -1
  return [...list].sort(
    (a, b) =>
      factor * sortKeyOf(a).localeCompare(sortKeyOf(b), 'zh-Hans-CN', { numeric: true })
  )
})

/** 是否处于筛选或排序状态，用于显示「清除」入口 */
const viewModified = computed(() => stateFilter.value.length > 0 || nameSort.value !== null)

const unitCountText = computed(() =>
  viewModified.value ? `${visibleUnits.value.length} / ${units.value.length} 个单元` : `${units.value.length} 个单元`
)

function toggleState(state: UnitState): void {
  if (stateFilter.value.includes(state)) {
    stateFilter.value = stateFilter.value.filter((item) => item !== state)
    return
  }
  // 按 STATE_ORDER 归一顺序，避免展示顺序受点击先后影响
  stateFilter.value = STATE_ORDER.filter(
    (item) => item === state || stateFilter.value.includes(item)
  )
}

function toggleNameSort(): void {
  nameSort.value = nameSort.value === 'asc' ? 'desc' : 'asc'
}

/** 清除筛选与排序，回到后端默认序 */
function resetView(): void {
  stateFilter.value = []
  nameSort.value = null
}

/* ------------------------------------------------------------------ */
/* 能力展示                                                            */
/* ------------------------------------------------------------------ */

/** 能力所属任务类型的中文名，未收录时回退为原始值 */
const TASK_LABEL: Record<string, string> = {
  NaturalLanguageProcessing: '自然语言处理',
  Multimodal: '多模态',
  ComputerVision: '计算机视觉',
  AudioProcessing: '音频处理',
  DataProcessing: '数据处理',
  OtherProcessing: '其它'
}

const taskLabel = (task: string): string => TASK_LABEL[task] ?? (task || '--')

/** 子任务原始文案：后端单子任务给字符串，多子任务给数组 */
const rawSubtaskText = (capability: UnitCapability): string =>
  capability.subtask ?? (capability.subtasks ?? []).join(' / ')

/**
 * 芯片上展示的子任务。
 *
 * 多数能力的主子任务与能力同名（如 TextGeneration / TextGeneration），
 * 重复展示只是噪音，这里与能力名相同就不再输出。
 */
const subtaskText = (capability: UnitCapability): string => {
  const text = rawSubtaskText(capability)
  return text.toLowerCase() === (capability.name ?? '').toLowerCase() ? '' : text
}

/** 悬浮提示给出完整信息，便于区分同名能力 */
const capabilityTitle = (capability: UnitCapability): string => {
  const lines = [`能力：${capability.name}`]
  const subtask = rawSubtaskText(capability)
  if (subtask) {
    lines.push(`子任务：${subtask}`)
  }
  lines.push(`任务类型：${taskLabel(capability.task)}`)
  if (capability.version) {
    lines.push(`版本：${capability.version}`)
  }
  if (capability.description) {
    lines.push(capability.description)
  }
  return lines.join('\n')
}

const capabilitiesOf = (unit: UnitInfo): UnitCapability[] => unit.capabilities ?? []

/** 能力数据的新鲜度说明：能力靠节点周期上报，为空时要让用户知道原因 */
const unitReportHint = computed(() => {
  const time = overview.value?.unitReportTime ?? 0
  return time > 0 ? `能力数据更新于 ${formatTimeMDHMS(time)}` : '尚未收到节点上报的单元能力'
})

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
            「能力」为该实体上所有 AIGC 单元的能力汇总 ·
            「在线」为最近 24 小时内有上报活动；「无活动」为近 30 天内没有上报活动
          </p>
        </div>
        <span class="pill pill-neutral">
          <AppIcon name="cube" :size="12" />
          {{ unitCountText }}
        </span>
      </div>

      <!-- 状态筛选 -->
      <div
        v-if="units.length > 0"
        class="flex flex-wrap items-center gap-x-2 gap-y-2 border-b border-slate-100 px-5 py-3"
      >
        <span class="flex items-center gap-1.5 text-xs text-slate-400">
          <AppIcon name="filter" :size="13" />
          状态
        </span>

        <button
          type="button"
          class="flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-medium transition"
          :class="
            stateFilter.length === 0
              ? 'bg-brand-50 text-brand-700 ring-1 ring-brand-200 ring-inset'
              : 'text-slate-500 hover:bg-slate-100'
          "
          @click="stateFilter = []"
        >
          全部
          <span class="tabular text-[11px] text-slate-400">{{ units.length }}</span>
        </button>

        <button
          v-for="state in STATE_ORDER"
          :key="state"
          type="button"
          class="flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-medium transition"
          :class="
            stateFilter.includes(state)
              ? 'bg-brand-50 text-brand-700 ring-1 ring-brand-200 ring-inset'
              : 'text-slate-500 hover:bg-slate-100'
          "
          @click="toggleState(state)"
        >
          <span class="pill-dot" :class="STATE_META[state].dot" />
          {{ STATE_META[state].label }}
          <span class="tabular text-[11px] text-slate-400">{{ stateCounts[state] }}</span>
        </button>

        <span class="ml-auto flex items-center gap-3">
          <span class="text-[11px] text-slate-400">{{ unitReportHint }}</span>
          <button v-if="viewModified" type="button" class="btn btn-ghost !py-1 !text-xs" @click="resetView">
            <AppIcon name="close" :size="12" />
            清除筛选
          </button>
        </span>
      </div>

      <div
        v-if="units.length > 0 && visibleUnits.length === 0"
        class="px-5 py-10 text-center text-xs text-slate-400"
      >
        当前筛选条件下没有单元
        <button type="button" class="ml-1 text-brand-600 hover:underline" @click="resetView">
          清除筛选
        </button>
      </div>

      <div v-else-if="units.length > 0" class="overflow-x-auto">
        <table class="data-table data-table-dense table-fixed min-w-[58rem]">
          <thead>
            <tr>
              <th class="w-10">#</th>
              <th class="w-28">
                <button
                  type="button"
                  class="inline-flex items-center gap-1 transition hover:text-slate-800"
                  :title="
                    nameSort === 'asc'
                      ? '当前按名称正序，点击切换为倒序'
                      : nameSort === 'desc'
                        ? '当前按名称倒序，点击切换为正序'
                        : '点击按名称正序排列'
                  "
                  @click="toggleNameSort"
                >
                  单元
                  <AppIcon
                    name="chevronDown"
                    :size="12"
                    class="transition-transform"
                    :class="
                      nameSort === 'asc' ? 'rotate-180' : nameSort === 'desc' ? '' : 'opacity-30'
                    "
                  />
                </button>
              </th>
              <!-- 表头回显当前筛选状态：筛选条在表外，表头不提示会让人误以为台账只有几行 -->
              <th class="w-[84px]">
                <span class="inline-flex items-center gap-1">
                  状态
                  <span
                    v-if="stateFilter.length > 0"
                    class="tabular rounded-full bg-brand-100 px-1.5 text-[10px] font-semibold text-brand-700"
                  >
                    {{ stateFilter.length }}
                  </span>
                </span>
              </th>
              <th class="w-[76px]">当日活动</th>
              <th class="w-[100px]">当日在线时长</th>
              <th class="w-32">最近活动</th>
              <th class="w-24">近 30 天活跃</th>
              <!-- 「能力」取 222px：这是 1280 视口下（最窄目标）能容纳的宽度。 -->
              <!-- 宽屏剩余空间由 table-fixed 按列宽比例分摊，避免该列独吞空白。 -->
              <th class="w-[222px]">能力</th>
              <th class="w-[132px]">设备</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(unit, index) in visibleUnits" :key="unit.id">
              <td class="tabular text-slate-400">{{ index + 1 }}</td>
              <td>
                <span class="break-words font-medium text-slate-800">
                  {{ unit.name || '未命名单元' }}
                </span>
                <p class="tabular text-[11px] text-slate-400">{{ unit.id }}</p>
              </td>
              <td>
                <span class="pill" :class="stateMeta(unit.state).pill">
                  <span class="pill-dot" :class="stateMeta(unit.state).dot" />
                  {{ stateMeta(unit.state).label }}
                </span>
              </td>
              <td class="tabular whitespace-nowrap text-slate-600">
                {{ unit.activeCount > 0 ? `${unit.activeCount} 次` : '--' }}
              </td>
              <td class="tabular whitespace-nowrap text-slate-600">
                {{ unit.activeCount > 0 ? `${unit.duration.toFixed(2)} 小时` : '--' }}
              </td>
              <!-- 时间戳必须单行显示，折行会让「日期 / 时刻」错位 -->
              <td class="tabular whitespace-nowrap text-xs text-slate-600">
                {{ formatActiveTime(unit.lastActiveTime) }}
              </td>
              <td class="tabular whitespace-nowrap text-slate-600">{{ unit.activeDays }} 天</td>
              <td>
                <div v-if="capabilitiesOf(unit).length > 0" class="flex flex-wrap gap-1">
                  <span
                    v-for="capability in capabilitiesOf(unit)"
                    :key="`${capability.name}/${subtaskText(capability)}`"
                    class="inline-flex max-w-full items-center gap-1 rounded-md bg-slate-100 px-1.5 py-0.5 text-[11px]"
                    :title="capabilityTitle(capability)"
                  >
                    <span class="truncate font-medium text-slate-700">{{ capability.name }}</span>
                    <span v-if="subtaskText(capability)" class="truncate text-slate-400">
                      {{ subtaskText(capability) }}
                    </span>
                  </span>
                </div>
                <span
                  v-else
                  class="text-xs text-slate-400"
                  title="节点尚未上报该实体的能力，节点每约 1 分钟上报一次"
                >
                  --
                </span>
              </td>
              <td>
                <span class="break-words text-xs text-slate-600">{{ unit.device || '--' }}</span>
                <p v-if="unit.platform" class="break-words text-[11px] text-slate-400">
                  {{ unit.platform }}
                </p>
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
