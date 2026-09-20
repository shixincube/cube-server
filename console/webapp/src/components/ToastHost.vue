<script setup lang="ts">
/** 全局通知宿主，渲染 `toast` store 中的队列。 */
import { computed } from 'vue'
import { useToastStore, type ToastKind } from '@/stores/toast'
import AppIcon from './AppIcon.vue'

const toast = useToastStore()

const ICON: Record<ToastKind, string> = {
  success: 'success',
  error: 'error',
  warning: 'warning',
  info: 'info'
}

const TONE: Record<ToastKind, string> = {
  success: 'text-emerald-600 bg-emerald-50 ring-emerald-200',
  error: 'text-rose-600 bg-rose-50 ring-rose-200',
  warning: 'text-amber-600 bg-amber-50 ring-amber-200',
  info: 'text-brand-600 bg-brand-50 ring-brand-200'
}

const items = computed(() => toast.items)
</script>

<template>
  <Teleport to="body">
    <div class="pointer-events-none fixed top-4 right-4 z-[60] flex w-80 flex-col gap-2">
      <TransitionGroup name="fade">
        <div
          v-for="item in items"
          :key="item.id"
          class="pointer-events-auto flex items-start gap-2.5 rounded-xl border border-slate-200 bg-white px-3.5 py-3 shadow-lg"
        >
          <span
            class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full ring-1 ring-inset"
            :class="TONE[item.kind]"
          >
            <AppIcon :name="ICON[item.kind]" :size="14" />
          </span>
          <p class="flex-1 text-[13px] leading-5 text-slate-700">{{ item.message }}</p>
          <button
            type="button"
            class="mt-0.5 rounded p-0.5 text-slate-300 transition hover:text-slate-500"
            aria-label="关闭"
            @click="toast.dismiss(item.id)"
          >
            <AppIcon name="close" :size="13" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
