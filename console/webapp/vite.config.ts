import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

/**
 * 控制台前端的构建配置。
 *
 * 构建产物直接输出到上级的 `web` 目录，该目录正是 Jetty `ResourceHandler`
 * 的 resourceBase，因此后端无需为静态资源做任何改动。
 */
export default defineConfig({
  plugins: [
    // 图片一律放在 public/ 下并以绝对路径引用，无需 Vite 做 asset 转换；
    // 关闭后可避免为每个绝对路径生成冗余的中转模块。
    vue({ template: { transformAssetUrls: false } }),
    tailwindcss()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    outDir: '../web',
    emptyOutDir: true,
    // 内网部署环境，禁用压缩产物里的 sourcemap 注释，减少体积
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts'],
          vendor: ['vue', 'vue-router', 'pinia']
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 开发态将接口请求代理到本地运行的 Cube Console（默认 7080 端口）
      '/signin': 'http://127.0.0.1:7080',
      '/signout': 'http://127.0.0.1:7080',
      '/servers': 'http://127.0.0.1:7080',
      '/deploy': 'http://127.0.0.1:7080',
      '/dispatcher': 'http://127.0.0.1:7080',
      '/service': 'http://127.0.0.1:7080',
      '/auth': 'http://127.0.0.1:7080',
      '/statistic': 'http://127.0.0.1:7080',
      '/log': 'http://127.0.0.1:7080',
      '/server-report': 'http://127.0.0.1:7080'
    }
  }
})
