<script setup lang="ts">
/** 概览指标卡。 */
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    /** 主数值后缀，如「台」 */
    unit?: string
    /** 次级说明文案 */
    hint?: string
    icon: string
    /** 强调色 */
    tone?: 'brand' | 'emerald' | 'amber' | 'rose' | 'slate'
  }>(),
  {
    unit: '',
    hint: '',
    tone: 'brand'
  }
)

const TONE: Record<string, { icon: string; value: string }> = {
  brand: { icon: 'bg-brand-50 text-brand-600', value: 'text-slate-900' },
  emerald: { icon: 'bg-emerald-50 text-emerald-600', value: 'text-emerald-600' },
  amber: { icon: 'bg-amber-50 text-amber-600', value: 'text-amber-600' },
  rose: { icon: 'bg-rose-50 text-rose-600', value: 'text-rose-600' },
  slate: { icon: 'bg-slate-100 text-slate-500', value: 'text-slate-900' }
}
</script>

<template>
  <div class="card px-5 py-4">
    <div class="flex items-start justify-between gap-3">
      <span class="text-xs font-medium text-slate-500">{{ label }}</span>
      <span
        class="flex h-8 w-8 items-center justify-center rounded-lg"
        :class="TONE[tone].icon"
      >
        <AppIcon :name="icon" :size="16" />
      </span>
    </div>
    <div class="mt-2.5 flex items-baseline gap-1">
      <span class="tabular text-2xl font-semibold" :class="TONE[tone].value">{{ value }}</span>
      <span v-if="unit" class="text-xs text-slate-400">{{ unit }}</span>
    </div>
    <p v-if="hint" class="mt-1 text-xs text-slate-500">{{ hint }}</p>
    <slot />
  </div>
</template>
