/**
 * 媒体查询封装。
 *
 * 把 CSS 断点变成响应式布尔值，供模板做逻辑判断（纯 CSS 无法在 JS 里读取断点）。
 * 侧边栏收窄只在桌面端生效：窄屏下侧边栏是抽屉，必须始终渲染完整菜单。
 */

import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

const canMatch = typeof window !== 'undefined' && typeof window.matchMedia === 'function'

export function useMediaQuery(query: string): Ref<boolean> {
  // 首帧即取值，避免带着持久化状态刷新时先渲染宽菜单再跳变成窄菜单
  const matches = ref(canMatch ? window.matchMedia(query).matches : false)

  let mql: MediaQueryList | null = null

  const update = (event: MediaQueryList | MediaQueryListEvent): void => {
    matches.value = event.matches
  }

  onMounted(() => {
    if (!canMatch) {
      return
    }
    mql = window.matchMedia(query)
    update(mql)
    mql.addEventListener('change', update)
  })

  onBeforeUnmount(() => {
    mql?.removeEventListener('change', update)
    mql = null
  })

  return matches
}
