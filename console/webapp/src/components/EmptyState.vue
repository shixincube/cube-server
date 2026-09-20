<script setup lang="ts">
/** 空数据占位。 */
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    icon?: string
    title: string
    description?: string
  }>(),
  {
    icon: 'inbox',
    description: ''
  }
)
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-2 px-6 py-14 text-center">
    <div class="flex h-11 w-11 items-center justify-center rounded-full bg-slate-100 text-slate-400">
      <AppIcon :name="icon" :size="20" />
    </div>
    <p class="text-sm font-medium text-slate-600">{{ title }}</p>
    <p v-if="description" class="max-w-md text-xs leading-5 text-slate-400">{{ description }}</p>
    <slot />
  </div>
</template>
