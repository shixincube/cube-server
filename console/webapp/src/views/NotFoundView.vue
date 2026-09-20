<script setup lang="ts">
/** 404 页。 */
import AppIcon from '@/components/AppIcon.vue'
</script>

<template>
  <div class="flex min-h-full items-center justify-center bg-slate-100 px-4">
    <div class="card max-w-md px-8 py-10 text-center">
      <div
        class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400"
      >
        <AppIcon name="search" :size="22" />
      </div>
      <p class="tabular mt-4 text-3xl font-semibold text-slate-300">404</p>
      <h1 class="mt-1 text-base font-semibold text-slate-800">页面不存在</h1>
      <p class="mt-1.5 text-[13px] text-slate-500">
        请求的控制台页面未找到，请从左侧导航进入需要的页面。
      </p>
      <RouterLink to="/dashboard" class="btn btn-primary mt-6">
        <AppIcon name="gauge" :size="14" />
        返回服务器概览
      </RouterLink>
    </div>
  </div>
</template>
