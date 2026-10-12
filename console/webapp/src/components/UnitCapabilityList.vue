<script setup lang="ts">
/**
 * AI 单元能力徽标组。
 *
 * 一个物理实体上可注册多个 AIGC 单元（每个单元一个能力），能力数可达十来个，
 * 台账的「能力」列只有两百多像素宽。早期实现用单行 truncate 截断，
 * 能力名与子任务都会被压成「A…」「S…」——信息等于没展示。
 *
 * 这里的取法：
 * <ul>
 *   <li><b>按任务类型分组</b>：类型（自然语言 / 多模态 / 音频…）是能力的稳定属性，
 *       同一实体的能力天然聚成几类，分组后每组一个色标与图标，比逐条罗列更容易扫读；</li>
 *   <li><b>能力名不截断</b>：改用换行（break-words），宁可行高变高也不丢字；</li>
 *   <li><b>子任务折叠</b>：能力名下方的子任务默认只显示首个，其余折成「+N」，
 *       点击展开。子任务是能力的细粒度参数，扫读时不需要全部平铺；</li>
 *   <li><b>超量能力折叠</b>：能力数超过阈值时先只展示前若干个，
 *       末尾给「共 N 项」的展开入口，避免行高失控。</li>
 * </ul>
 */
import { computed, ref, watch } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import type { UnitCapability } from '@/api/types'

const props = withDefaults(
  defineProps<{
    capabilities: UnitCapability[]
    /** 超过该数量时折叠，默认 6 */
    collapseAfter?: number
  }>(),
  { collapseAfter: 6 }
)

/** 任务类型的展示元数据：中文名、色板、图标 */
interface TaskMeta {
  label: string
  /** 色标：底色 + 文字色 + 左侧竖条 */
  chip: string
  /** 组标题的图标 */
  icon: string
}

const TASK_META: Record<string, TaskMeta> = {
  NaturalLanguageProcessing: {
    label: '自然语言',
    chip: 'bg-sky-50 text-sky-700 ring-sky-200',
    icon: 'chat'
  },
  Multimodal: {
    label: '多模态',
    chip: 'bg-violet-50 text-violet-700 ring-violet-200',
    icon: 'layers'
  },
  ComputerVision: {
    label: '计算机视觉',
    chip: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    icon: 'eye'
  },
  AudioProcessing: {
    label: '音频处理',
    chip: 'bg-amber-50 text-amber-700 ring-amber-200',
    icon: 'mic'
  },
  DataProcessing: {
    label: '数据处理',
    chip: 'bg-teal-50 text-teal-700 ring-teal-200',
    icon: 'database'
  },
  OtherProcessing: {
    label: '其它',
    chip: 'bg-slate-100 text-slate-700 ring-slate-200',
    icon: 'cube'
  }
}

/** 未收录的任务类型回退为通用样式，标签保留原始值便于排查 */
const metaOf = (task: string): TaskMeta =>
  TASK_META[task] ?? { label: task || '--', chip: TASK_META.OtherProcessing.chip, icon: 'cube' }

/** 子任务列表：后端单子任务给字符串，多子任务给数组 */
function subtasksOf(capability: UnitCapability): string[] {
  if (capability.subtask) {
    return [capability.subtask]
  }
  return capability.subtasks ?? []
}

/**
 * 子任务里与能力同名的那条不重复展示
 * （多数能力的主子任务与能力同名，如 TextGeneration / TextGeneration）。
 */
function distinctSubtasks(capability: UnitCapability): string[] {
  const name = (capability.name ?? '').toLowerCase()
  return subtasksOf(capability).filter((item) => item.toLowerCase() !== name)
}

/**
 * 按任务类型分组，组内与组间均按能力名排序。
 *
 * 排序而非沿用后端序，是为了让同一实体每次渲染的分组与顺序一致，
 * 节点上报顺序变化时表格不会跳动。
 */
interface CapabilityGroup {
  task: string
  meta: TaskMeta
  items: UnitCapability[]
}

const groups = computed<CapabilityGroup[]>(() => {
  const buckets = new Map<string, UnitCapability[]>()

  for (const capability of props.capabilities) {
    const task = capability.task || 'OtherProcessing'
    const list = buckets.get(task)
    if (list) {
      list.push(capability)
    } else {
      buckets.set(task, [capability])
    }
  }

  return [...buckets.entries()]
    .map(([task, items]) => ({
      task,
      meta: metaOf(task),
      items: [...items].sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''))
    }))
    .sort((a, b) => a.meta.label.localeCompare(b.meta.label, 'zh-Hans-CN'))
})

const total = computed(() => props.capabilities.length)
const collapsed = computed(() => total.value > props.collapseAfter)

/** 整组展开 / 收起 */
const expandedAll = ref(false)

/** 展开时逐组展示全部能力；收起时按能力总数截断（而非按组截断，避免整组消失） */
const visibleGroups = computed<CapabilityGroup[]>(() => {
  if (!collapsed.value || expandedAll.value) {
    return groups.value
  }

  const result: CapabilityGroup[] = []
  let budget = props.collapseAfter

  for (const group of groups.value) {
    if (budget <= 0) {
      break
    }
    result.push({ ...group, items: group.items.slice(0, budget) })
    budget -= group.items.length
  }

  return result
})

const hiddenCount = computed(() => total.value - visibleGroups.value.reduce((n, g) => n + g.items.length, 0))

/**
 * 单个能力是否展开子任务，用能力名+子任务做键。
 *
 * 键必须含子任务：同一能力名下可能有多个单元实例（主子任务不同），
 * 只用能力名会让展开状态互相串扰。
 */
const expandedItems = ref<Record<string, boolean>>({})

function itemKey(capability: UnitCapability, index: number): string {
  return `${capability.name}/${subtasksOf(capability).join(',')}/${index}`
}

function toggleItem(key: string): void {
  expandedItems.value = { ...expandedItems.value, [key]: !expandedItems.value[key] }
}

// 数据整体换掉（切换单元、刷新）时收起展开态，避免残留上一行的展开痕迹
watch(
  () => props.capabilities,
  () => {
    expandedAll.value = false
    expandedItems.value = {}
  }
)

/** 悬浮提示：完整的任务类型、版本与描述，表格内则按分组收敛展示 */
function titleOf(capability: UnitCapability): string {
  const lines = [`能力：${capability.name}`, `任务类型：${metaOf(capability.task).label}`]

  const subtasks = subtasksOf(capability)
  if (subtasks.length > 0) {
    lines.push(`子任务：${subtasks.join(' / ')}`)
  }
  if (capability.version) {
    lines.push(`版本：${capability.version}`)
  }
  if (capability.description) {
    lines.push(capability.description)
  }

  return lines.join('\n')
}
</script>

<template>
  <div v-if="capabilities.length > 0" class="space-y-1.5">
    <div v-for="group in visibleGroups" :key="group.task" class="space-y-1">
      <!-- 分组头：类型色标与图标承担「区分」职责，同一类型的 capability 归为一组 -->
      <div class="flex items-center gap-1">
        <AppIcon :name="group.meta.icon" :size="11" class="text-slate-400" />
        <span class="text-[10px] font-medium tracking-wide text-slate-500">
          {{ group.meta.label }}
        </span>
        <span class="tabular text-[10px] text-slate-300">{{ group.items.length }}</span>
      </div>

      <ul class="space-y-0.5">
        <li
          v-for="(capability, index) in group.items"
          :key="itemKey(capability, index)"
          :title="titleOf(capability)"
          class="rounded-md px-1.5 py-1 ring-1 ring-inset"
          :class="group.meta.chip"
        >
          <!-- 能力名换行而非截断：宁可行高变高，也不能让用户看到「A…」 -->
          <div class="flex items-start gap-1.5">
            <span class="min-w-0 flex-1 break-words text-[11px] font-medium leading-4">
              {{ capability.name }}
            </span>
            <span
              v-if="capability.version"
              class="tabular shrink-0 text-[10px] opacity-60"
            >
              v{{ capability.version }}
            </span>
          </div>

          <!-- 子任务：默认只露首个，其余折进「+N」，点击本行展开 -->
          <div v-if="distinctSubtasks(capability).length > 0" class="mt-0.5">
            <p class="break-words text-[10px] leading-4 opacity-70">
              {{ expandedItems[itemKey(capability, index)] ? distinctSubtasks(capability).join(' · ') : distinctSubtasks(capability)[0] }}
              <button
                v-if="distinctSubtasks(capability).length > 1"
                type="button"
                class="ml-0.5 underline decoration-dotted underline-offset-2 opacity-70 transition hover:opacity-100"
                @click.stop="toggleItem(itemKey(capability, index))"
              >
                {{
                  expandedItems[itemKey(capability, index)]
                    ? '收起'
                    : `+${distinctSubtasks(capability).length - 1}`
                }}
              </button>
            </p>
          </div>
        </li>
      </ul>
    </div>

    <!-- 超量能力：给出确切总数与展开入口，而不是静默丢弃 -->
    <button
      v-if="collapsed && (expandedAll || hiddenCount > 0)"
      type="button"
      class="inline-flex items-center gap-1 text-[10px] text-slate-400 transition hover:text-slate-600"
      @click="expandedAll = !expandedAll"
    >
      <AppIcon
        name="chevronDown"
        :size="10"
        class="transition-transform"
        :class="expandedAll ? 'rotate-180' : ''"
      />
      {{ expandedAll ? '收起' : `展开其余 ${hiddenCount} 项（共 ${total} 项）` }}
    </button>
  </div>

  <span v-else class="text-xs text-slate-400" title="节点尚未上报该实体的能力，节点每约 1 分钟上报一次">
    --
  </span>
</template>
