/** 应用入口。 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { setUnauthorizedHandler } from './api/client'
import { useAuthStore } from './stores/auth'
import { useToastStore } from './stores/toast'
import './styles/theme.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 任何接口返回 401 时统一清理登录态并跳回登录页
setUnauthorizedHandler(() => {
  const auth = useAuthStore(pinia)
  if (!auth.isAuthenticated) {
    return
  }
  auth.reset()
  useToastStore(pinia).warning('登录状态已失效，请重新登录')
  void router.replace({
    path: '/login',
    query: { redirect: router.currentRoute.value.fullPath }
  })
})

app.mount('#app')
