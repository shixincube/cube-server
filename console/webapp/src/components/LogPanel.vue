<script setup lang="ts">
/**
 * 实时日志面板。
 *
 * 以游标方式增量拉取日志，保留最近 `maxLines` 行；用户向上滚动时暂停自动吸底，
 * 避免打断查阅历史日志。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import EmptyState from './EmptyState.vue'
import { usePolling } from '@/composables/usePolling'
import type { LogLine, LogResponse } from '@/api/types'
import { formatLogTime } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    /** 日志来源，用于切换时重置视图 */
    sourceKey: string
    /** 拉取函数，`start` 为上次返回的游标 */
    fetcher: (start: number) => Promise<LogResponse>
    /** 初始游标，默认取 5 分钟前 */
    initialStart?: number
    /** 保留的最大行数 */
    maxLines?: number
    /** 轮询间隔（毫秒） */
    interval?: number
  }>(),
  {
    initialStart: () => Date.now() - 300000,
    maxLines: 80,
    interval: 10000
  }
)

const LEVEL_LABEL: Record<number, string> = {
  1: 'DEBUG',
  2: 'INFO',
  3: 'WARN',
  4: 'ERROR'
}

const LEVEL_CLASS: Record<number, string> = {
  1: 'log-debug',
  2: 'log-info',
  3: 'log-warn',
  4: 'log-error'
}

/** 日志级别过滤：0 表示全部 */
const levelFilter = ref(0)
const lines = ref<LogLine[]>([])
const cursor = ref(props.initialStart)
const viewport = ref<HTMLElement | null>(null)
const stickToBottom = ref(true)
const loadError = ref('')

const FILTERS = [
  { level: 0, label: '全部' },
  { level: 2, label: 'INFO+' },
  { level: 3, label: 'WARN+' },
  { level: 4, label: 'ERROR' }
]

const visibleLines = computed(() =>
  levelFilter.value === 0 ? lines.value : lines.value.filter((line) => line.level >= levelFilter.value)
)

/** 按当前滚动位置判断是否继续自动吸底 */
function onScroll(): void {
  const el = viewport.value
  if (!el) {
    return
  }
  stickToBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 24
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  const el = viewport.value
  if (el && stickToBottom.value) {
    el.scrollTop = el.scrollHeight
  }
}

async function poll(): Promise<void> {
  try {
    const response = await props.fetcher(cursor.value)
    loadError.value = ''
    if (response.last) {
      cursor.value = response.last
    }
    const incoming = response.lines ?? []
    if (incoming.length > 0) {
      const merged = [...lines.value, ...incoming]
      lines.value = merged.slice(Math.max(0, merged.length - props.maxLines))
      await scrollToBottom()
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '日志拉取失败'
  }
}

/** 切换日志来源时清空重来 */
function reset(): void {
  lines.value = []
  cursor.value = Date.now() - 300000
  stickToBottom.value = true
  void poll()
}

watch(() => props.sourceKey, reset)

onMounted(() => {
  void scrollToBottom()
})

usePolling(poll, props.interval, { immediate: true })

defineExpose({ reset })
</script>

<template>
  <div class="flex h-full flex-col">
    <div class="flex items-center gap-2 border-b border-slate-200 px-3 py-2">
      <div class="seg">
        <button
          v-for="item in FILTERS"
          :key="item.level"
          type="button"
          class="seg-item"
          :class="levelFilter === item.level ? 'seg-item-active' : ''"
          @click="levelFilter = item.level"
        >
          {{ item.label }}
        </button>
      </div>

      <span class="tabular text-xs text-slate-400">{{ visibleLines.length }} / {{ maxLines }} 行</span>

      <div class="flex-1" />

      <button
        type="button"
        class="btn btn-ghost btn-sm"
        :class="stickToBottom ? '' : 'text-brand-600'"
        @click="
          stickToBottom = true;
          scrollToBottom()
        "
      >
        <AppIcon name="chevronDown" :size="13" />
        {{ stickToBottom ? '跟随最新' : '跳到最新' }}
      </button>
    </div>

    <div ref="viewport" class="log-view min-h-0 flex-1" @scroll="onScroll">
      <EmptyState
        v-if="visibleLines.length === 0"
        icon="terminal"
        :title="loadError ? '日志拉取失败' : '暂无日志'"
        :description="loadError || '服务器启动后日志会实时滚动到这里'"
      />
      <p
        v-for="(line, index) in visibleLines"
        :key="`${line.time}-${index}`"
        class="log-line"
        :class="LEVEL_CLASS[line.level] ?? 'log-debug'"
      >
        <span class="text-slate-400">{{ formatLogTime(line.time) }}</span>
        <span class="ml-1 font-semibold">[{{ LEVEL_LABEL[line.level] ?? 'DEBUG' }}]</span>
        <span class="ml-1 text-slate-500">{{ line.tag }}</span>
        <span class="text-slate-400"> - </span>
        <span>{{ line.text }}</span>
      </p>
    </div>
  </div>
</template>
