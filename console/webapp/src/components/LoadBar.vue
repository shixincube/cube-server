<script setup lang="ts">
/**
 * 负载进度条。
 *
 * 阈值配色：<60% 正常、60%-85% 关注、>85% 告警。
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    percent: number
    /** 是否显示右侧百分比数字 */
    showValue?: boolean
    size?: 'sm' | 'md'
  }>(),
  {
    showValue: true,
    size: 'sm'
  }
)

const clamped = computed(() => Math.max(0, Math.min(100, Math.round(props.percent || 0))))

const barCls = computed(() => {
  if (clamped.value >= 85) {
    return 'bg-rose-500'
  }
  if (clamped.value >= 60) {
    return 'bg-amber-500'
  }
  return 'bg-emerald-500'
})

const textCls = computed(() => {
  if (clamped.value >= 85) {
    return 'text-rose-600'
  }
  if (clamped.value >= 60) {
    return 'text-amber-600'
  }
  return 'text-slate-500'
})

const heightCls = computed(() => (props.size === 'sm' ? 'h-1.5' : 'h-2'))
</script>

<template>
  <div class="flex items-center gap-2.5">
    <div
      class="w-full min-w-16 overflow-hidden rounded-full bg-slate-200"
      :class="heightCls"
      role="progressbar"
      :aria-valuenow="clamped"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <div
        class="h-full rounded-full transition-[width] duration-500"
        :class="barCls"
        :style="{ width: `${clamped}%` }"
      />
    </div>
    <span v-if="showValue" class="tabular w-9 shrink-0 text-right text-xs" :class="textCls">
      {{ clamped }}%
    </span>
  </div>
</template>
