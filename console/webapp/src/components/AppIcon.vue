<script setup lang="ts">
/**
 * 内联图标集。
 *
 * 旧版依赖 FontAwesome + Ionicons 两个插件包（合计 >10MB 字体资源），
 * 这里改为按需内联 SVG，仅保留控制台真正用到的图标。
 *
 * 所有图标均为 24x24 描边风格，颜色继承 `currentColor`。
 */
import { computed } from 'vue'

const ICONS: Record<string, string> = {
  menu: '<path d="M3 6h18M3 12h18M3 18h18"/>',
  gauge:
    '<path d="M12 14l4.5-4.5"/><path d="M3.5 17.5a9 9 0 1 1 17 0"/><circle cx="12" cy="17" r="1.4"/>',
  users:
    '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  sitemap:
    '<rect x="9" y="2" width="6" height="6" rx="1.5"/><rect x="2" y="16" width="6" height="6" rx="1.5"/><rect x="16" y="16" width="6" height="6" rx="1.5"/><path d="M12 8v4M5 16v-2h14v2"/>',
  server:
    '<rect x="2.5" y="3" width="19" height="7.5" rx="2"/><rect x="2.5" y="13.5" width="19" height="7.5" rx="2"/><path d="M6.5 6.75h.01M6.5 17.25h.01"/>',
  logout:
    '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/>',
  close: '<path d="M18 6L6 18M6 6l12 12"/>',
  chevronDown: '<path d="M6 9l6 6 6-6"/>',
  chevronRight: '<path d="M9 6l6 6-6 6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  pencil:
    '<path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
  trash:
    '<path d="M3 6h18"/><path d="M8 6V4.5A1.5 1.5 0 0 1 9.5 3h5A1.5 1.5 0 0 1 16 4.5V6"/><path d="M18.5 6l-.9 13a2 2 0 0 1-2 1.9H8.4a2 2 0 0 1-2-1.9L5.5 6"/><path d="M10 11v6M14 11v6"/>',
  play: '<path d="M6 4.5l13 7.5-13 7.5Z"/>',
  stop: '<rect x="5.5" y="5.5" width="13" height="13" rx="2"/>',
  refresh:
    '<path d="M21 3.5v5.5h-5.5"/><path d="M3 20.5V15h5.5"/><path d="M20.3 9A8.5 8.5 0 0 0 5.7 6.2L3 9"/><path d="M3.7 15a8.5 8.5 0 0 0 14.6 2.8L21 15"/>',
  eye: '<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/>',
  eyeOff:
    '<path d="M10.6 5.2A9.9 9.9 0 0 1 12 5c6.4 0 10 7 10 7a17.6 17.6 0 0 1-3.2 4.2"/><path d="M6.6 6.6A17.4 17.4 0 0 0 2 12s3.6 7 10 7a9.9 9.9 0 0 0 4.4-1"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/><path d="M2 2l20 20"/>',
  search: '<circle cx="11" cy="11" r="7.5"/><path d="M20.5 20.5l-4.2-4.2"/>',
  external:
    '<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><path d="M15 3h6v6"/><path d="M10.5 13.5L21 3"/>',
  warning: '<path d="M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4.5M12 17.2h.01"/>',
  success: '<path d="M21.5 11.1V12a9.5 9.5 0 1 1-5.6-8.7"/><path d="M21.5 4.5L12 14l-2.9-2.9"/>',
  error: '<circle cx="12" cy="12" r="9.5"/><path d="M15 9l-6 6M9 9l6 6"/>',
  info: '<circle cx="12" cy="12" r="9.5"/><path d="M12 16.5v-5M12 8h.01"/>',
  settings:
    '<circle cx="12" cy="12" r="3"/><path d="M19.1 14.6a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1.04 1.56V21a2 2 0 1 1-4 0v-.09a1.7 1.7 0 0 0-1.1-1.56 1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.7 1.7 0 0 0 .34-1.87 1.7 1.7 0 0 0-1.56-1.04H3a2 2 0 1 1 0-4h.09a1.7 1.7 0 0 0 1.56-1.1 1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.7 1.7 0 0 0 1.87.34H9a1.7 1.7 0 0 0 1.04-1.56V3a2 2 0 1 1 4 0v.09a1.7 1.7 0 0 0 1.04 1.56 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.7 1.7 0 0 0-.34 1.87V9a1.7 1.7 0 0 0 1.56 1.04H21a2 2 0 1 1 0 4h-.09a1.7 1.7 0 0 0-1.56 1.04Z"/>',
  bolt: '<path d="M13 2L3.5 13.5H11l-1 8.5 9.5-11.5H12l1-8.5Z"/>',
  list: '<path d="M8 6h13M8 12h13M8 18h13"/><path d="M3.5 6h.01M3.5 12h.01M3.5 18h.01"/>',
  terminal: '<path d="M4.5 17l6-5-6-5"/><path d="M12 19h7.5"/>',
  activity: '<path d="M22 12h-4.5l-3 8.5L9.5 3.5l-3 8.5H2"/>',
  cpu: '<rect x="4.5" y="4.5" width="15" height="15" rx="2"/><rect x="9.5" y="9.5" width="5" height="5"/><path d="M9.5 1.5v3M14.5 1.5v3M9.5 19.5v3M14.5 19.5v3M19.5 9.5h3M19.5 14.5h3M1.5 9.5h3M1.5 14.5h3"/>',
  database:
    '<ellipse cx="12" cy="5.5" rx="8.5" ry="3"/><path d="M20.5 12c0 1.66-3.8 3-8.5 3s-8.5-1.34-8.5-3"/><path d="M3.5 5.5v13c0 1.66 3.8 3 8.5 3s8.5-1.34 8.5-3v-13"/>',
  drive:
    '<path d="M22 12H2"/><path d="M5.4 5.1L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.4-6.9A2 2 0 0 0 16.8 4H7.2a2 2 0 0 0-1.8 1.1Z"/><path d="M6.5 16h.01M10.5 16h.01"/>',
  lock: '<rect x="3.5" y="10.5" width="17" height="10.5" rx="2"/><path d="M7.5 10.5V7a4.5 4.5 0 0 1 9 0v3.5"/>',
  user: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  shield: '<path d="M12 22s8-4 8-10V5.5L12 2.5 4 5.5V12c0 6 8 10 8 10Z"/>',
  inbox:
    '<path d="M21 12h-6l-2 3h-2l-2-3H3"/><path d="M5.4 5.1L3 12v6a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-6l-2.4-6.9A2 2 0 0 0 16.8 4H7.2a2 2 0 0 0-1.8 1.1Z"/>',
  loader:
    '<path d="M12 2v4M12 18v4M4.9 4.9l2.9 2.9M16.2 16.2l2.9 2.9M2 12h4M18 12h4M4.9 19.1l2.9-2.9M16.2 7.8l2.9-2.9"/>',
  cube: '<path d="M12 2.5l9 5v9l-9 5-9-5v-9Z"/><path d="M3 7.5l9 5 9-5M12 12.5V22.5"/>'
}

const props = withDefaults(
  defineProps<{
    name: string
    size?: number | string
    strokeWidth?: number
  }>(),
  {
    size: 16,
    strokeWidth: 1.7
  }
)

const markup = computed(() => ICONS[props.name] ?? '')
const pixels = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))
</script>

<template>
  <svg
    :width="pixels"
    :height="pixels"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    class="shrink-0"
    v-html="markup"
  />
</template>
