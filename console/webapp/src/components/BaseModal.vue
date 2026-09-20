<script setup lang="ts">
/**
 * 通用弹窗。
 *
 * 替代旧版 Bootstrap Modal，支持宽度预设、ESC 关闭、点击遮罩关闭与忙碌态遮罩。
 */
import { computed, onBeforeUnmount, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    subtitle?: string
    /** 弹窗宽度预设 */
    width?: 'sm' | 'md' | 'lg' | 'xl'
    /** 忙碌态：显示遮罩并禁止关闭 */
    busy?: boolean
    /** 内容区是否使用无内边距布局（表格、日志用） */
    flush?: boolean
  }>(),
  {
    subtitle: '',
    width: 'md',
    busy: false,
    flush: false
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const widthCls = computed(() => {
  switch (props.width) {
    case 'sm':
      return 'max-w-md'
    case 'lg':
      return 'max-w-3xl'
    case 'xl':
      return 'max-w-5xl'
    default:
      return 'max-w-xl'
  }
})

const close = (): void => {
  if (props.busy) {
    return
  }
  emit('update:modelValue', false)
}

const onKeydown = (event: KeyboardEvent): void => {
  if (event.key === 'Escape') {
    close()
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      document.addEventListener('keydown', onKeydown)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', onKeydown)
      document.body.style.overflow = ''
    }
  }
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/45 px-4 py-10 backdrop-blur-[2px]"
        @click.self="close"
      >
        <div
          class="relative w-full overflow-hidden rounded-2xl bg-white shadow-2xl ring-1 ring-slate-900/5"
          :class="widthCls"
          role="dialog"
          aria-modal="true"
        >
          <header class="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-4">
            <div class="min-w-0">
              <h2 class="truncate text-[15px] font-semibold text-slate-900">{{ title }}</h2>
              <p v-if="subtitle" class="tabular mt-0.5 truncate text-xs text-slate-500">
                {{ subtitle }}
              </p>
            </div>
            <div class="flex shrink-0 items-center gap-1.5">
              <slot name="header-actions" />
              <button
                type="button"
                class="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                aria-label="关闭"
                @click="close"
              >
                <AppIcon name="close" :size="16" />
              </button>
            </div>
          </header>

          <div :class="flush ? '' : 'px-5 py-4'" class="max-h-[70vh] overflow-y-auto">
            <slot />
          </div>

          <footer
            v-if="$slots.footer"
            class="flex items-center justify-end gap-2 border-t border-slate-200 bg-slate-50/70 px-5 py-3"
          >
            <slot name="footer" />
          </footer>

          <div
            v-if="busy"
            class="absolute inset-0 flex items-center justify-center gap-2 bg-white/70 backdrop-blur-[1px]"
          >
            <AppIcon name="loader" :size="18" class="animate-spin text-brand-600" />
            <span class="text-sm text-slate-600">处理中…</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
