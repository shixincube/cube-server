<script setup lang="ts">
/** 服务器运行状态徽标。 */
import { computed } from 'vue'

const props = defineProps<{ running: boolean }>()

const cls = computed(() => (props.running ? 'pill pill-on' : 'pill pill-off'))
const dotCls = computed(() =>
  props.running ? 'pill-dot bg-emerald-500 animate-pulse' : 'pill-dot bg-rose-500'
)
</script>

<template>
  <span :class="cls">
    <span :class="dotCls" />
    {{ running ? '运行中' : '已关闭' }}
  </span>
</template>
