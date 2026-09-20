<script setup lang="ts">
/**
 * 服务单元管理。
 *
 * 列表展示部署信息、运行状态与负载；详情展示 Cellet 与实际加载的 JAR；
 * 配置弹窗以只读方式呈现访问点、存储与缓存配置（写入能力后端尚未实现）。
 */
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadBar from '@/components/LoadBar.vue'
import PageHeader from '@/components/PageHeader.vue'
import ServiceMonitor from '@/components/ServiceMonitor.vue'
import StatusPill from '@/components/StatusPill.vue'
import {
  fetchServiceDeployInfo,
  fetchServiceStatus,
  startService,
  stopService
} from '@/api/console'
import { calcServiceLoad, fetchLastPerformance, type LoadRate } from '@/api/monitor'
import type { CacheConfig, SeriesCacheConfig, ServiceServer } from '@/api/types'
import { usePolling } from '@/composables/usePolling'
import { useServersStore } from '@/stores/servers'
import { useToastStore } from '@/stores/toast'
import {
  convBytesToMB,
  convMillisToHours,
  formatFullTime,
  formatSize,
  formatTimeMDHMS
} from '@/utils/format'
import { md5 } from '@/utils/md5'

const servers = useServersStore()
const toast = useToastStore()

/** 启停操作的状态轮询次数与间隔 */
const TOGGLE_POLL_TIMES = 6
const TOGGLE_POLL_INTERVAL = 5000

/** 存储配置展示顺序 */
const STORAGE_KEYS = ['Auth', 'Contact', 'Messaging', 'FileStorage'] as const

/** 存储类型的中文说明 */
const STORAGE_LABELS: Record<string, string> = {
  Auth: 'Auth 帐号存储',
  Contact: 'Contact 联系人存储',
  Messaging: 'Messaging 消息存储',
  FileStorage: 'FileStorage 文件存储'
}

/* ------------------------------------------------------------------ */
/* 列表                                                                */
/* ------------------------------------------------------------------ */

const perfMap = ref<Record<string, { load: LoadRate; startTime: number }>>({})
const loading = ref(false)

const rows = computed(() =>
  servers.services.map((server) => ({ server, perf: perfMap.value[server.name] ?? null }))
)

/** 拉取所有运行中服务单元的最近一份性能报告 */
async function loadPerformance(): Promise<void> {
  const targets = servers.services.filter((item) => item.running)
  const results = await Promise.all(
    targets.map(async (server) => {
      try {
        const report = await fetchLastPerformance(server.name)
        if (!report) {
          return null
        }
        return [
          server.name,
          { load: calcServiceLoad(report), startTime: report.systemStartTime }
        ] as const
      } catch {
        return null
      }
    })
  )

  const next: Record<string, { load: LoadRate; startTime: number }> = {}
  for (const item of results) {
    if (item) {
      next[item[0]] = item[1]
    }
  }
  perfMap.value = next
}

async function load(): Promise<void> {
  loading.value = true
  try {
    await servers.loadServices()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '加载服务单元列表失败')
  } finally {
    loading.value = false
  }
  await loadPerformance()
}

async function refresh(): Promise<void> {
  await load()
  toast.success('已刷新服务单元数据')
}

/** 新增部署：后端尚未提供实现，与旧版保持一致 */
function onNewDeploy(): void {
  toast.warning('新增部署功能尚未接入，请在部署文件中登记服务单元')
}

/* ------------------------------------------------------------------ */
/* 详情                                                                */
/* ------------------------------------------------------------------ */

const detailsOpen = ref(false)
const currentService = ref<ServiceServer | null>(null)

function openDetails(server: ServiceServer): void {
  currentService.value = server
  detailsOpen.value = true
}

/* ------------------------------------------------------------------ */
/* 配置（只读）                                                        */
/* ------------------------------------------------------------------ */

const configOpen = ref(false)
const configTab = ref<'storage' | 'cache'>('storage')
const configService = ref<ServiceServer | null>(null)

/** 存储配置卡片 */
const storageCards = computed(() => {
  const storage = configService.value?.storage ?? {}
  return STORAGE_KEYS.map((key) => ({
    key,
    label: STORAGE_LABELS[key] ?? key,
    config: storage[key]
  })).filter((item) => Boolean(item.config))
})

/** 缓存配置卡片 */
const cacheCards = computed(() => {
  const service = configService.value
  if (!service) {
    return []
  }
  const list: { key: string; label: string; config: CacheConfig | SeriesCacheConfig }[] = [
    { key: 'token', label: '令牌缓存池', config: service.tokenPool },
    { key: 'general', label: '通用缓存器', config: service.generalCache },
    { key: 'contact', label: '联系人缓存器', config: service.contactCache },
    { key: 'filelabel', label: '文件标签缓存器', config: service.fileLabelCache },
    { key: 'messaging', label: '消息时序缓存', config: service.messagingSeries }
  ]
  return list.filter((item) => Boolean(item.config))
})

/** 序列缓存器比通用缓存器多出分片与阈值字段 */
function isSeriesConfig(config: CacheConfig | SeriesCacheConfig): config is SeriesCacheConfig {
  return 'segmentNum' in config
}

function openConfig(server: ServiceServer): void {
  configService.value = server
  configTab.value = 'storage'
  configOpen.value = true
}

/* ------------------------------------------------------------------ */
/* 启停操作                                                            */
/* ------------------------------------------------------------------ */

const toggleOpen = ref(false)
const toggleBusy = ref(false)
const toggleTarget = ref<ServiceServer | null>(null)
const togglePassword = ref('')
const showPassword = ref(false)
const toggleError = ref('')

function openToggle(server: ServiceServer): void {
  toggleTarget.value = server
  togglePassword.value = ''
  toggleError.value = ''
  showPassword.value = false
  toggleOpen.value = true
}

const sleep = (ms: number): Promise<void> =>
  new Promise((resolve) => {
    window.setTimeout(resolve, ms)
  })

async function waitForState(
  tag: string,
  deployPath: string,
  expectedRunning: boolean
): Promise<ServiceServer | null> {
  for (let i = 0; i < TOGGLE_POLL_TIMES; i++) {
    await sleep(TOGGLE_POLL_INTERVAL)
    try {
      const status = await fetchServiceStatus(tag, deployPath)
      if (status.running === expectedRunning) {
        return status
      }
    } catch {
      // 单次查询失败不中断轮询
    }
  }
  return null
}

async function confirmToggle(): Promise<void> {
  const server = toggleTarget.value
  if (!server) {
    return
  }

  if (togglePassword.value.length === 0) {
    toggleError.value = '请输入管理密码'
    return
  }
  if (togglePassword.value.length < 6) {
    toggleError.value = '管理密码至少 6 位'
    return
  }

  toggleBusy.value = true
  toggleError.value = ''
  const password = md5(togglePassword.value)
  const willRun = !server.running

  try {
    const result = willRun
      ? await startService(server.tag, server.deployPath, password)
      : await stopService(server.tag, server.deployPath, password)

    if (result.running === willRun) {
      servers.patchService(result)
      toast.success(willRun ? '启动服务器成功' : '关停服务器成功')
      toggleOpen.value = false
      return
    }

    const status = await waitForState(server.tag, server.deployPath, willRun)
    if (status) {
      servers.patchService(status)
      toast.success(willRun ? '启动服务器成功' : '关停服务器成功')
    } else {
      toast.error('未能更新到服务器状态')
    }
    toggleOpen.value = false
  } catch (error) {
    toggleError.value =
      error instanceof Error && error.message.includes('401')
        ? '操作失败，请检查管理密码是否正确'
        : '操作失败，请稍后重试'
  } finally {
    toggleBusy.value = false
  }
}

/* ------------------------------------------------------------------ */
/* 生命周期                                                            */
/* ------------------------------------------------------------------ */

onMounted(async () => {
  try {
    const info = await fetchServiceDeployInfo()
    if (!info.deployPath) {
      toast.warning('控制台没有找到部署文件，请参考快速开始文档进行配置')
    }
  } catch {
    // 部署信息缺失不阻断列表加载
  }
  await load()
})

usePolling(loadPerformance, 60000, { immediate: false })
</script>

<template>
  <div>
    <PageHeader title="服务单元" description="服务单元的部署信息、运行状态与业务指标">
      <template #actions>
        <button type="button" class="btn btn-ghost" :disabled="loading" @click="refresh">
          <AppIcon name="refresh" :size="14" :class="loading ? 'animate-spin' : ''" />
          刷新
        </button>
        <button type="button" class="btn btn-primary" @click="onNewDeploy">
          <AppIcon name="plus" :size="14" />
          新增部署
        </button>
      </template>
    </PageHeader>

    <!-- 列表 -->
    <section class="card overflow-hidden">
      <div class="card-header">
        <h2 class="card-title">已部署列表</h2>
        <span class="pill pill-neutral">
          <AppIcon name="server" :size="12" />
          {{ servers.serviceTotal }} 台 · {{ servers.serviceRunning }} 运行中
        </span>
      </div>

      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th class="w-14">#</th>
              <th class="w-44">标签</th>
              <th>部署路径</th>
              <th class="w-28">状态</th>
              <th class="w-52">负载</th>
              <th class="w-40">启动时间</th>
              <th class="w-72 text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="row.server.name">
              <td class="tabular text-slate-400">{{ index + 1 }}</td>
              <td>
                <span class="font-medium text-slate-800">{{ row.server.tag }}</span>
                <p class="tabular text-[11px] text-slate-400">{{ row.server.name }}</p>
              </td>
              <td>
                <span
                  class="tabular text-xs break-all text-slate-600"
                  :title="row.server.deployPath"
                >
                  {{ row.server.deployPath }}
                </span>
              </td>
              <td><StatusPill :running="row.server.running" /></td>
              <td>
                <LoadBar v-if="row.perf" :percent="row.perf.load.percent" />
                <span v-else class="text-xs text-slate-400">
                  {{ row.server.running ? '采集中的…' : '--' }}
                </span>
              </td>
              <td class="tabular text-xs text-slate-600">
                {{ row.perf ? formatTimeMDHMS(row.perf.startTime) : '--' }}
              </td>
              <td>
                <div class="flex items-center justify-end gap-1.5">
                  <button
                    type="button"
                    class="btn btn-sm"
                    :class="row.server.running ? 'btn-danger' : 'btn-success'"
                    @click="openToggle(row.server)"
                  >
                    <AppIcon :name="row.server.running ? 'stop' : 'play'" :size="12" />
                    {{ row.server.running ? '停止' : '启动' }}
                  </button>
                  <button
                    type="button"
                    class="btn btn-ghost btn-sm"
                    @click="openDetails(row.server)"
                  >
                    <AppIcon name="list" :size="12" />
                    详情
                  </button>
                  <button
                    type="button"
                    class="btn btn-primary btn-sm"
                    @click="openConfig(row.server)"
                  >
                    <AppIcon name="settings" :size="12" />
                    配置
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="rows.length === 0">
              <td colspan="7">
                <EmptyState
                  icon="server"
                  title="暂无已部署的服务单元"
                  description="请在控制台的部署配置中登记服务单元后刷新"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 监视器 -->
    <div class="mt-5">
      <ServiceMonitor :servers="servers.services" />
    </div>

    <!-- 详情 -->
    <BaseModal
      v-model="detailsOpen"
      title="服务单元详情"
      :subtitle="currentService?.name"
      width="lg"
    >
      <div v-if="currentService" class="space-y-5">
        <div class="rounded-xl bg-slate-50 px-4 py-2">
          <div class="grid grid-cols-1 gap-x-6 sm:grid-cols-2">
            <div class="kv-row">
              <span class="kv-key">控制台标签</span>
              <span class="kv-val">{{ currentService.tag }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-key">部署路径</span>
              <span class="kv-val tabular" :title="currentService.deployPath">
                {{ currentService.deployPath }}
              </span>
            </div>
            <div class="kv-row">
              <span class="kv-key">配置路径</span>
              <span class="kv-val tabular" :title="currentService.configPath">
                {{ currentService.configPath }}
              </span>
            </div>
            <div class="kv-row">
              <span class="kv-key">服务单元路径</span>
              <span class="kv-val tabular" :title="currentService.celletsPath">
                {{ currentService.celletsPath }}
              </span>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div class="rounded-xl border border-slate-200 px-4 py-3">
            <p class="mb-2 text-xs font-medium text-slate-600">SHM 访问点</p>
            <p class="flex justify-between text-xs">
              <span class="text-slate-500">地址</span>
              <span class="tabular text-slate-800">{{ currentService.server.host }}</span>
            </p>
            <p class="mt-1.5 flex justify-between text-xs">
              <span class="text-slate-500">端口</span>
              <span class="tabular text-slate-800">{{ currentService.server.port }}</span>
            </p>
            <p class="mt-1.5 flex justify-between text-xs">
              <span class="text-slate-500">最大连接数</span>
              <span class="tabular text-slate-800">
                {{ currentService.server.maxConnection }}
              </span>
            </p>
          </div>

          <div class="rounded-xl border border-slate-200 px-4 py-3">
            <p class="mb-2 text-xs font-medium text-slate-600">数据适配器</p>
            <p class="flex justify-between text-xs">
              <span class="text-slate-500">地址</span>
              <span class="tabular text-slate-800">{{ currentService.adapter.host }}</span>
            </p>
            <p class="mt-1.5 flex justify-between text-xs">
              <span class="text-slate-500">端口</span>
              <span class="tabular text-slate-800">{{ currentService.adapter.port }}</span>
            </p>
            <p class="mt-1.5 flex justify-between text-xs">
              <span class="text-slate-500">日志等级</span>
              <span class="pill pill-neutral">{{ currentService.logLevel }}</span>
            </p>
          </div>
        </div>

        <div>
          <h3 class="mb-2.5 text-xs font-semibold tracking-wide text-slate-500">
            服务单元（{{ currentService.cellets.length }}）
          </h3>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div
              v-for="(cellet, index) in currentService.cellets"
              :key="index"
              class="rounded-xl border border-slate-200 px-4 py-3"
            >
              <div class="flex items-start gap-2.5">
                <span
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-600"
                >
                  <AppIcon name="cube" :size="15" />
                </span>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-[13px] font-medium text-slate-800">
                    {{ cellet.classes?.[0] ?? '未知服务单元' }}
                  </p>
                  <template v-if="cellet.jar">
                    <p class="tabular truncate text-[11px] text-slate-500" :title="cellet.jar.path">
                      {{ cellet.jar.name ?? cellet.jar.path }}
                    </p>
                    <p class="tabular text-[11px] text-slate-400">
                      <template v-if="cellet.jar.size">
                        {{ formatSize(cellet.jar.size) }} ·
                        {{ formatFullTime(cellet.jar.lastModified ?? 0) }}
                      </template>
                      <template v-else>未找到对应的 JAR 文件</template>
                    </p>
                  </template>
                  <p v-else class="text-[11px] text-slate-400">未配置文件</p>
                  <p v-if="cellet.ports?.length" class="tabular mt-1 text-[11px] text-slate-400">
                    端口 {{ cellet.ports.join(' / ') }}
                  </p>
                </div>
              </div>
            </div>
            <p v-if="currentService.cellets.length === 0" class="text-xs text-slate-400">
              暂无服务单元
            </p>
          </div>
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn btn-ghost" @click="detailsOpen = false">关闭</button>
      </template>
    </BaseModal>

    <!-- 配置（只读） -->
    <BaseModal
      v-model="configOpen"
      title="服务单元配置"
      :subtitle="configService ? `${configService.name} · 由服务端配置文件提供，控制台暂不支持写入` : ''"
      width="xl"
    >
      <div v-if="configService" class="space-y-5">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div class="rounded-xl border border-slate-200 px-4 py-3">
            <p class="mb-2 text-xs font-medium text-slate-600">SHM 访问点</p>
            <p class="tabular text-[13px] text-slate-800">
              {{ configService.server.host }}:{{ configService.server.port }}
            </p>
            <p class="tabular mt-1 text-[11px] text-slate-500">
              最大连接数 {{ configService.server.maxConnection }}
            </p>
          </div>
          <div class="rounded-xl border border-slate-200 px-4 py-3">
            <p class="mb-2 text-xs font-medium text-slate-600">数据适配器</p>
            <p class="tabular text-[13px] text-slate-800">
              {{ configService.adapter.host }}:{{ configService.adapter.port }}
            </p>
          </div>
          <div class="rounded-xl border border-slate-200 px-4 py-3">
            <p class="mb-2 text-xs font-medium text-slate-600">日志等级</p>
            <span class="pill pill-neutral">{{ configService.logLevel }}</span>
          </div>
        </div>

        <div class="seg">
          <button
            type="button"
            class="seg-item"
            :class="configTab === 'storage' ? 'seg-item-active' : ''"
            @click="configTab = 'storage'"
          >
            存储配置
          </button>
          <button
            type="button"
            class="seg-item"
            :class="configTab === 'cache' ? 'seg-item-active' : ''"
            @click="configTab = 'cache'"
          >
            缓存配置
          </button>
        </div>

        <!-- 存储 -->
        <div v-if="configTab === 'storage'" class="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div
            v-for="card in storageCards"
            :key="card.key"
            class="rounded-xl border border-slate-200 px-4 py-3"
          >
            <div class="mb-2 flex items-center justify-between">
              <p class="text-[13px] font-medium text-slate-800">{{ card.label }}</p>
              <span
                class="pill"
                :class="card.config?.type === 'MySQL' ? 'pill-info' : 'pill-neutral'"
              >
                <AppIcon name="database" :size="11" />
                {{ card.config?.type ?? '--' }}
              </span>
            </div>

            <template v-if="card.config?.type === 'MySQL'">
              <div class="kv-row">
                <span class="kv-key">访问地址</span>
                <span class="kv-val tabular">
                  {{ card.config.host }}:{{ card.config.port }}
                </span>
              </div>
              <div class="kv-row">
                <span class="kv-key">Schema</span>
                <span class="kv-val tabular">{{ card.config.schema }}</span>
              </div>
              <div class="kv-row">
                <span class="kv-key">访问用户</span>
                <span class="kv-val tabular">{{ card.config.user }}</span>
              </div>
              <div class="kv-row">
                <span class="kv-key">访问密码</span>
                <span class="kv-val tabular">••••••</span>
              </div>
            </template>
            <template v-else>
              <div class="kv-row">
                <span class="kv-key">数据库文件</span>
                <span class="kv-val tabular">{{ card.config?.file ?? '--' }}</span>
              </div>
            </template>
          </div>
          <EmptyState v-if="storageCards.length === 0" icon="database" title="暂无存储配置" />
        </div>

        <!-- 缓存 -->
        <div v-else class="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div
            v-for="card in cacheCards"
            :key="card.key"
            class="rounded-xl border border-slate-200 px-4 py-3"
          >
            <p class="mb-2 text-[13px] font-medium text-slate-800">{{ card.label }}</p>

            <div class="kv-row">
              <span class="kv-key">地址</span>
              <span class="kv-val tabular">{{ card.config.host }}:{{ card.config.port }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-key">容量</span>
              <span class="kv-val tabular">{{ card.config.capacity }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-key">数据有效期</span>
              <span class="kv-val tabular">
                {{ convMillisToHours(card.config.expiry).toFixed(1) }} 小时
              </span>
            </div>

            <template v-if="isSeriesConfig(card.config)">
              <div class="kv-row">
                <span class="kv-key">分片数</span>
                <span class="kv-val tabular">{{ card.config.segmentNum }}</span>
              </div>
              <div class="kv-row">
                <span class="kv-key">分片大小</span>
                <span class="kv-val tabular">{{ card.config.segmentSize }}</span>
              </div>
              <div class="kv-row">
                <span class="kv-key">索引阈值</span>
                <span class="kv-val tabular">
                  {{ convBytesToMB(card.config.indexThreshold).toFixed(1) }} MB
                </span>
              </div>
              <div class="kv-row">
                <span class="kv-key">数据阈值</span>
                <span class="kv-val tabular">
                  {{ convBytesToMB(card.config.dataThreshold).toFixed(1) }} MB
                </span>
              </div>
              <div class="kv-row">
                <span class="kv-key">应答超时</span>
                <span class="kv-val tabular">{{ card.config.timeout }}</span>
              </div>
            </template>
            <template v-else>
              <div class="kv-row">
                <span class="kv-key">内存上限</span>
                <span class="kv-val tabular">
                  {{ convBytesToMB(card.config.threshold).toFixed(1) }} MB
                </span>
              </div>
              <div class="kv-row">
                <span class="kv-key">阻塞模式</span>
                <span class="kv-val tabular">{{ card.config.blocking }}</span>
              </div>
              <div class="kv-row">
                <span class="kv-key">路由表名</span>
                <span class="kv-val tabular">{{ card.config.routetable || '--' }}</span>
              </div>
            </template>

            <div class="kv-row">
              <span class="kv-key">集群节点</span>
              <span class="kv-val">
                <span v-if="card.config.clusterNodes?.length" class="flex flex-wrap gap-1.5">
                  <span
                    v-for="node in card.config.clusterNodes"
                    :key="`${node.host}:${node.port}`"
                    class="tabular pill pill-neutral"
                  >
                    {{ node.host }}:{{ node.port }}
                  </span>
                </span>
                <span v-else class="text-slate-400">--</span>
              </span>
            </div>

            <div class="kv-row">
              <span class="kv-key">存储桩</span>
              <span class="kv-val tabular">
                {{ card.config.pedestal ? `${card.config.pedestal.host}:${card.config.pedestal.port}` : '--' }}
              </span>
            </div>
            <div class="kv-row">
              <span class="kv-key">备份存储桩</span>
              <span class="kv-val tabular">
                {{
                  card.config.backupPedestal
                    ? `${card.config.backupPedestal.host}:${card.config.backupPedestal.port}`
                    : '--'
                }}
              </span>
            </div>
          </div>
          <EmptyState v-if="cacheCards.length === 0" icon="drive" title="暂无缓存配置" />
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn btn-ghost" @click="configOpen = false">关闭</button>
      </template>
    </BaseModal>

    <!-- 启停确认 -->
    <BaseModal
      v-model="toggleOpen"
      title="操作验证"
      width="sm"
      :busy="toggleBusy"
      :subtitle="toggleTarget?.name"
    >
      <p class="text-[13px] text-slate-700">
        您确定要
        <b :class="toggleTarget?.running ? 'text-rose-600' : 'text-emerald-600'">
          {{ toggleTarget?.running ? '关停' : '启动' }}
        </b>
        服务单元服务器吗？
      </p>

      <div class="mt-4 space-y-3">
        <div>
          <label class="label">标签</label>
          <input class="input" :value="toggleTarget?.tag" disabled />
        </div>
        <div>
          <label class="label">部署路径</label>
          <input class="input input-mono" :value="toggleTarget?.deployPath" disabled />
        </div>
        <div>
          <label class="label">配置路径</label>
          <input class="input input-mono" :value="toggleTarget?.configPath" disabled />
        </div>
        <div>
          <label class="label">管理密码</label>
          <div class="relative">
            <input
              v-model="togglePassword"
              class="input pr-10"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入管理密码"
              @input="toggleError = ''"
              @keyup.enter="confirmToggle"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-2 flex items-center rounded px-1.5 text-slate-400 transition hover:text-slate-600"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <AppIcon :name="showPassword ? 'eyeOff' : 'eye'" :size="15" />
            </button>
          </div>
        </div>

        <p
          v-if="toggleError"
          class="flex items-start gap-2 rounded-lg bg-rose-50 px-3 py-2 text-[13px] text-rose-700 ring-1 ring-rose-200 ring-inset"
        >
          <AppIcon name="warning" :size="14" class="mt-0.5" />
          {{ toggleError }}
        </p>
      </div>

      <template #footer>
        <button
          type="button"
          class="btn btn-ghost"
          :disabled="toggleBusy"
          @click="toggleOpen = false"
        >
          取消
        </button>
        <button
          type="button"
          class="btn"
          :class="toggleTarget?.running ? 'btn-danger' : 'btn-success'"
          :disabled="toggleBusy"
          @click="confirmToggle"
        >
          <AppIcon :name="toggleTarget?.running ? 'stop' : 'play'" :size="13" />
          确认{{ toggleTarget?.running ? '关停' : '启动' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>
