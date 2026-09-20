<script setup lang="ts">
/** 页面标题区：标题、说明与右侧操作区。 */
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="mb-5 flex flex-wrap items-start justify-between gap-3">
    <div>
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="description" class="page-desc">{{ description }}</p>
    </div>
    <div class="flex flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>
