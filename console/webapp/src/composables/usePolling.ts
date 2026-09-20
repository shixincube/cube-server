/**
 * 轮询封装。
 *
 * 统一处理定时器、组件卸载清理，以及页面切到后台时暂停请求，
 * 替代旧版散落在各页面的 `setInterval` + 手工 `clearInterval`。
 */

import { onBeforeUnmount, onMounted, ref } from 'vue'

interface PollingOptions {
  /** 挂载后是否立即执行一次，默认 true */
  immediate?: boolean
  /** 页面隐藏时是否暂停，默认 true */
  pauseOnHidden?: boolean
}

export function usePolling(
  task: () => void | Promise<void>,
  intervalMs: number,
  options: PollingOptions = {}
) {
  const { immediate = true, pauseOnHidden = true } = options

  let timer = 0
  const running = ref(false)

  const tick = async (): Promise<void> => {
    if (running.value) {
      // 上一次请求尚未结束，跳过本次，避免请求堆积
      return
    }
    running.value = true
    try {
      await task()
    } finally {
      running.value = false
    }
  }

  const start = (): void => {
    stop()
    timer = window.setInterval(() => {
      void tick()
    }, intervalMs)
  }

  const stop = (): void => {
    if (timer) {
      window.clearInterval(timer)
      timer = 0
    }
  }

  const onVisibilityChange = (): void => {
    if (document.hidden) {
      stop()
    } else {
      start()
      void tick()
    }
  }

  onMounted(() => {
    if (immediate) {
      void tick()
    }
    start()
    if (pauseOnHidden) {
      document.addEventListener('visibilitychange', onVisibilityChange)
    }
  })

  onBeforeUnmount(() => {
    stop()
    if (pauseOnHidden) {
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  })

  return { start, stop, tick, running }
}
