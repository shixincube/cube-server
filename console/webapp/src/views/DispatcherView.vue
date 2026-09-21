<script setup lang="ts">
/**
 * 调度机管理。
 *
 * 列表展示部署信息、运行状态与负载；详情弹窗支持编辑容器/业务配置与服务单元路由；
 * 启停操作需要二次校验管理密码。
 */
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import DispatcherMonitor from '@/components/DispatcherMonitor.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadBar from '@/components/LoadBar.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import {
  fetchDispatcherDeployInfo,
  fetchDispatcherStatus,
  startDispatcher,
  stopDispatcher,
  updateDispatcherConfig
} from '@/api/console'
import { calcDispatcherLoad, fetchLastPerformance, type LoadRate } from '@/api/monitor'
import type { DispatcherConfigPayload, DispatcherServer } from '@/api/types'
import { usePolling } from '@/composables/usePolling'
import { useServersStore } from '@/stores/servers'
import { useToastStore } from '@/stores/toast'
import { formatTimeMDHMS } from '@/utils/format'
import { md5 } from '@/utils/md5'
import { isHostAddress, isPort, isUnsigned } from '@/utils/validate'

const servers = useServersStore()
const toast = useToastStore()

/** 启停操作的状态轮询次数与间隔 */
const TOGGLE_POLL_TIMES = 5
const TOGGLE_POLL_INTERVAL = 2000

const LOG_LEVELS = ['DEBUG', 'INFO', 'WARNING', 'ERROR']
/** 不允许在界面上移除的内置服务单元 */
const RESERVED_CELLETS = ['Auth', 'Contact']

interface AccessPointDraft {
  host: string
  port: string
  maxConnection: string
}

interface PlainAccessPointDraft {
  host: string
  port: string
}

interface DirectorDraft {
  address: string
  port: string
  weight: string
  cellets: string[]
}

interface ConfigDraft {
  server: AccessPointDraft
  wsServer: AccessPointDraft
  wssServer: AccessPointDraft
  http: PlainAccessPointDraft
  https: PlainAccessPointDraft
  ssl: { keystore: string; storePassword: string; managerPassword: string }
  logLevel: string
  cellets: string[]
  directors: DirectorDraft[]
}

/* ------------------------------------------------------------------ */
/* 列表                                                                */
/* ------------------------------------------------------------------ */

/** 表格里额外展示的运行时指标 */
const perfMap = ref<Record<string, { load: LoadRate; startTime: number }>>({})
const loading = ref(false)

const rows = computed(() =>
  servers.dispatchers.map((server) => ({ server, perf: perfMap.value[server.name] ?? null }))
)

/** 拉取所有运行中调度机的最近一份性能报告 */
async function loadPerformance(): Promise<void> {
  const targets = servers.dispatchers.filter((item) => item.running)
  const results = await Promise.all(
    targets.map(async (server) => {
      try {
        const report = await fetchLastPerformance(server.name)
        if (!report) {
          return null
        }
        return [
          server.name,
          { load: calcDispatcherLoad(report), startTime: report.systemStartTime }
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
    await servers.loadDispatchers()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '加载调度机列表失败')
  } finally {
    loading.value = false
  }
  await loadPerformance()
}

async function refresh(): Promise<void> {
  await load()
  toast.success('已刷新调度机数据')
}

/** 新增部署：后端尚未提供实现，保持与旧版一致的提示 */
function onNewDeploy(): void {
  toast.warning('新增部署功能尚未接入，请在部署文件中登记调度机')
}

/* ------------------------------------------------------------------ */
/* 详情与配置编辑                                                       */
/* ------------------------------------------------------------------ */

const detailsOpen = ref(false)
const editing = ref(false)
const submitting = ref(false)
const currentServer = ref<DispatcherServer | null>(null)
const draft = ref<ConfigDraft | null>(null)
const selectedDirectorIndex = ref(0)
const newCellet = ref('')
const newDirectorCellet = ref('')
const errors = ref<Record<string, string>>({})

const selectedDirector = computed<DirectorDraft | null>(
  () => draft.value?.directors[selectedDirectorIndex.value] ?? null
)

const isReservedCellet = (name: string): boolean => RESERVED_CELLETS.includes(name)

const randomInt = (min: number, max: number): number =>
  Math.floor(Math.random() * (max - min)) + min

/** 将服务器数据转为可编辑草稿 */
function createDraft(server: DispatcherServer): ConfigDraft {
  return {
    server: {
      host: server.server.host,
      port: String(server.server.port),
      maxConnection: String(server.server.maxConnection)
    },
    wsServer: {
      host: server.wsServer.host,
      port: String(server.wsServer.port),
      maxConnection: String(server.wsServer.maxConnection)
    },
    wssServer: {
      host: server.wssServer.host,
      port: String(server.wssServer.port),
      maxConnection: String(server.wssServer.maxConnection)
    },
    http: { host: server.http.host, port: String(server.http.port) },
    https: { host: server.https.host, port: String(server.https.port) },
    ssl: {
      keystore: server.ssl?.keystore ?? '',
      storePassword: server.ssl?.storePassword ?? '',
      managerPassword: server.ssl?.managerPassword ?? ''
    },
    logLevel: server.logLevel,
    cellets: [...server.cellets],
    directors: server.directors.map((item) => ({
      address: item.address,
      port: String(item.port),
      weight: String(item.weight),
      cellets: [...item.cellets]
    }))
  }
}

function openDetails(server: DispatcherServer): void {
  currentServer.value = server
  draft.value = createDraft(server)
  selectedDirectorIndex.value = 0
  editing.value = false
  errors.value = {}
  newCellet.value = ''
  newDirectorCellet.value = ''
  detailsOpen.value = true
}

function closeDetails(): void {
  detailsOpen.value = false
  editing.value = false
  submitting.value = false
  errors.value = {}
}

/** 放弃修改，回到进入编辑前的状态 */
function cancelEditing(): void {
  if (currentServer.value) {
    draft.value = createDraft(currentServer.value)
  }
  editing.value = false
  errors.value = {}
  selectedDirectorIndex.value = 0
  newCellet.value = ''
  newDirectorCellet.value = ''
}

function addCellet(): void {
  const name = newCellet.value.trim()
  if (!draft.value || !name || draft.value.cellets.includes(name)) {
    newCellet.value = ''
    return
  }
  draft.value.cellets.push(name)
  newCellet.value = ''
}

function removeCellet(name: string): void {
  if (!draft.value) {
    return
  }
  draft.value.cellets = draft.value.cellets.filter((item) => item !== name)
}

function addDirector(): void {
  const value = draft.value
  if (!value) {
    return
  }
  const base = selectedDirector.value
  value.directors.push({
    address: `192.168.${randomInt(1, 254)}.${randomInt(1, 254)}`,
    port: '6000',
    weight: String(base?.weight ?? 1),
    cellets: base ? [...base.cellets] : []
  })
  selectedDirectorIndex.value = value.directors.length - 1
}

function removeDirector(): void {
  const value = draft.value
  if (!value) {
    return
  }
  if (value.directors.length <= 1) {
    toast.warning('操作无效，当前仅有一条服务单元记录')
    return
  }
  value.directors.splice(selectedDirectorIndex.value, 1)
  selectedDirectorIndex.value = Math.max(0, selectedDirectorIndex.value - 1)
}

function addDirectorCellet(): void {
  const target = selectedDirector.value
  const name = newDirectorCellet.value.trim()
  if (!target || !name || target.cellets.includes(name)) {
    newDirectorCellet.value = ''
    return
  }
  target.cellets.push(name)
  newDirectorCellet.value = ''
}

function removeDirectorCellet(name: string): void {
  const target = selectedDirector.value
  if (!target) {
    return
  }
  target.cellets = target.cellets.filter((item) => item !== name)
}

/** 校验草稿，返回以字段路径为键的错误字典 */
function validate(value: ConfigDraft): Record<string, string> {
  const result: Record<string, string> = {}

  const checkAccessPoint = (key: string, ap: AccessPointDraft): void => {
    if (!isHostAddress(ap.host.trim())) {
      result[`${key}.host`] = '主机地址不正确'
    }
    if (!isPort(ap.port.trim())) {
      result[`${key}.port`] = '端口需为 1-65535 之间的整数'
    }
    if (!isUnsigned(ap.maxConnection.trim()) || Number(ap.maxConnection) > 65535) {
      result[`${key}.maxConnection`] = '最大连接数需为 1-65535 之间的整数'
    }
  }

  checkAccessPoint('wsServer', value.wsServer)
  checkAccessPoint('wssServer', value.wssServer)
  if (!isHostAddress(value.server.host.trim())) {
    result['server.host'] = '主机地址不正确'
  }
  if (!isPort(value.server.port.trim())) {
    result['server.port'] = '端口需为 1-65535 之间的整数'
  }
  if (!isUnsigned(value.server.maxConnection.trim()) || Number(value.server.maxConnection) > 65535) {
    result['server.maxConnection'] = '最大连接数需为 1-65535 之间的整数'
  }

  const checkPlain = (key: string, ap: PlainAccessPointDraft, label: string): void => {
    if (!isHostAddress(ap.host.trim())) {
      result[`${key}.host`] = `${label} 地址不正确`
    }
    if (!isPort(ap.port.trim())) {
      result[`${key}.port`] = `${label} 端口不正确`
    }
  }

  checkPlain('http', value.http, 'HTTP')
  checkPlain('https', value.https, 'HTTPS')

  if (value.ssl.keystore.trim().length <= 3) {
    result['ssl.keystore'] = 'SSL Keystore 路径不正确'
  }
  if (value.ssl.storePassword.trim().length <= 3) {
    result['ssl.storePassword'] = 'SSL 存储密码不正确'
  }
  if (value.ssl.managerPassword.trim().length <= 3) {
    result['ssl.managerPassword'] = 'SSL 管理密码不正确'
  }

  value.directors.forEach((director, index) => {
    if (!isHostAddress(director.address.trim())) {
      result[`director.${index}.address`] = '服务单元地址不正确'
    }
    if (!isPort(director.port.trim())) {
      result[`director.${index}.port`] = '服务单元端口不正确'
    }
    if (!isUnsigned(director.weight.trim()) || Number(director.weight) > 10) {
      result[`director.${index}.weight`] = '路由权重需为 1-10 之间的整数'
    }
  })

  return result
}

/** 当前选中服务单元的错误信息 */
const directorError = computed(() => {
  const index = selectedDirectorIndex.value
  return (
    errors.value[`director.${index}.address`] ??
    errors.value[`director.${index}.port`] ??
    errors.value[`director.${index}.weight`] ??
    ''
  )
})

async function submitConfig(): Promise<void> {
  const server = currentServer.value
  const value = draft.value
  if (!server || !value) {
    return
  }

  const invalid = validate(value)
  errors.value = invalid
  const firstError = Object.values(invalid)[0]
  if (firstError) {
    toast.error(firstError)
    return
  }

  const payload: DispatcherConfigPayload = {
    tag: server.tag,
    deployPath: server.deployPath,
    server: {
      host: value.server.host.trim(),
      port: Number(value.server.port),
      maxConnection: Number(value.server.maxConnection)
    },
    wsServer: {
      host: value.wsServer.host.trim(),
      port: Number(value.wsServer.port),
      maxConnection: Number(value.wsServer.maxConnection)
    },
    wssServer: {
      host: value.wssServer.host.trim(),
      port: Number(value.wssServer.port),
      maxConnection: Number(value.wssServer.maxConnection)
    },
    http: { host: value.http.host.trim(), port: Number(value.http.port) },
    https: { host: value.https.host.trim(), port: Number(value.https.port) },
    ssl: {
      keystore: value.ssl.keystore.trim(),
      storePassword: value.ssl.storePassword.trim(),
      managerPassword: value.ssl.managerPassword.trim()
    },
    logLevel: value.logLevel,
    cellets: [...value.cellets],
    directors: value.directors.map((item) => ({
      address: item.address.trim(),
      port: Number(item.port),
      weight: Number(item.weight),
      cellets: [...item.cellets]
    }))
  }

  submitting.value = true
  try {
    const updated = await updateDispatcherConfig(payload)
    servers.patchDispatcher(updated)
    currentServer.value = updated
    draft.value = createDraft(updated)
    editing.value = false
    toast.success('调度机配置已更新')
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '修改服务器配置失败')
  } finally {
    submitting.value = false
  }
}

/* ------------------------------------------------------------------ */
/* 启停操作                                                            */
/* ------------------------------------------------------------------ */

const toggleOpen = ref(false)
const toggleBusy = ref(false)
const toggleTarget = ref<DispatcherServer | null>(null)
const togglePassword = ref('')
const showPassword = ref(false)
const toggleError = ref('')

function openToggle(server: DispatcherServer): void {
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

/** 轮询服务器状态，直到运行状态翻转或超出重试次数 */
async function waitForState(
  tag: string,
  deployPath: string,
  expectedRunning: boolean
): Promise<DispatcherServer | null> {
  for (let i = 0; i < TOGGLE_POLL_TIMES; i++) {
    await sleep(TOGGLE_POLL_INTERVAL)
    try {
      const status = await fetchDispatcherStatus(tag, deployPath)
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
      ? await startDispatcher(server.tag, server.deployPath, password)
      : await stopDispatcher(server.tag, server.deployPath, password)

    if (result.running === willRun) {
      servers.patchDispatcher(result)
      toast.success(willRun ? '启动服务器成功' : '关停服务器成功')
      toggleOpen.value = false
      return
    }

    // 后端返回的瞬时状态尚未翻转，轮询确认
    const status = await waitForState(server.tag, server.deployPath, willRun)
    if (status) {
      servers.patchDispatcher(status)
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
    const info = await fetchDispatcherDeployInfo()
    if (!info.deployPath) {
      toast.warning('控制台没有找到部署文件，请参考快速开始文档进行配置')
    }
  } catch {
    // 部署信息缺失不阻断列表加载
  }
  await load()
})

/** 表格负载与启动时间每分钟刷新 */
usePolling(loadPerformance, 60000, { immediate: false })
</script>

<template>
  <div>
    <PageHeader title="调度机" description="调度机的部署信息、运行状态与网络负载">
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
          <AppIcon name="sitemap" :size="12" />
          {{ servers.dispatcherTotal }} 台 · {{ servers.dispatcherRunning }} 运行中
        </span>
      </div>

      <div class="overflow-x-auto">
        <!--
          `table-fixed` + 明确列宽：默认 table-layout:auto 下 th 的宽度只是"偏好"，浏览器会把余量
          按内容宽度分配，出现"部署路径被挤成逐字换行、时间戳折成两行"的问题。
          这里按最窄目标 1280（内容盒 990px）反推各列宽度：7 列显式宽度合计 832px，
          余量全部留给唯一的自动列「部署路径」（1280 下 158px，1920 下约 800px），路径不会长期贴边。
          `min-w-[60rem]` 是下限兜底：容器再窄就横向滚动，而不是继续压缩各列。
        -->
        <table class="data-table table-fixed min-w-[60rem]">
          <thead>
            <tr>
              <th class="w-[40px]">#</th>
              <th class="w-[176px]">标签</th>
              <th class="w-[78px]">版本号</th>
              <th>部署路径</th>
              <th class="w-[100px]">状态</th>
              <th class="w-[142px]">负载</th>
              <th class="w-[132px]">启动时间</th>
              <th class="w-[172px] text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="row.server.name">
              <td class="tabular text-slate-400">{{ index + 1 }}</td>
              <td>
                <span class="font-medium text-slate-800">{{ row.server.tag }}</span>
                <p class="tabular text-[11px] text-slate-400">{{ row.server.name }}</p>
              </td>
              <td class="tabular text-xs whitespace-nowrap text-slate-600">
                {{ row.server.version || '--' }}
              </td>
              <td>
                <span class="tabular text-xs break-words text-slate-600" :title="row.server.deployPath">
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
              <td class="tabular text-xs whitespace-nowrap text-slate-600">
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
                    class="btn btn-primary btn-sm"
                    @click="openDetails(row.server)"
                  >
                    <AppIcon name="settings" :size="12" />
                    详情
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="rows.length === 0">
              <td colspan="8">
                <EmptyState
                  icon="sitemap"
                  title="暂无已部署的调度机"
                  description="请在控制台的部署配置中登记调度机后刷新"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 监视器 -->
    <div class="mt-5">
      <DispatcherMonitor :servers="servers.dispatchers" />
    </div>

    <!-- 详情 -->
    <BaseModal
      v-model="detailsOpen"
      :title="editing ? '修改调度机配置' : '调度机详情'"
      :subtitle="currentServer?.name"
      :busy="submitting"
      width="xl"
    >
      <template #header-actions>
        <button
          v-if="!editing"
          type="button"
          class="btn btn-primary btn-sm"
          @click="editing = true"
        >
          <AppIcon name="pencil" :size="12" />
          修改配置
        </button>
        <template v-else>
          <button type="button" class="btn btn-ghost btn-sm" @click="cancelEditing">取消修改</button>
          <button type="button" class="btn btn-primary btn-sm" @click="submitConfig">
            <AppIcon name="success" :size="12" />
            确认修改
          </button>
        </template>
      </template>

      <div v-if="draft" class="space-y-5">
        <!-- 基础信息 -->
        <div class="rounded-xl bg-slate-50 px-4 py-2">
          <div class="grid grid-cols-1 gap-x-6 sm:grid-cols-2">
            <div class="kv-row">
              <span class="kv-key">控制台标签</span>
              <span class="kv-val">{{ currentServer?.tag }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-key">部署路径</span>
              <span class="kv-val tabular" :title="currentServer?.deployPath">
                {{ currentServer?.deployPath }}
              </span>
            </div>
            <div class="kv-row">
              <span class="kv-key">容器配置</span>
              <span class="kv-val tabular">{{ currentServer?.cellConfigFile }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-key">业务配置</span>
              <span class="kv-val tabular">{{ currentServer?.propertiesFile }}</span>
            </div>
          </div>
        </div>

        <!-- 访问点 -->
        <div>
          <h3 class="mb-2.5 text-xs font-semibold tracking-wide text-slate-500">访问点</h3>
          <div class="grid grid-cols-1 gap-4 lg:grid-cols-3">
            <!-- SHM -->
            <div class="rounded-xl border border-slate-200 px-4 py-3">
              <p class="mb-2 text-xs font-medium text-slate-600">SHM 访问点</p>
              <template v-if="editing">
                <label class="label">地址</label>
                <input
                  v-model="draft.server.host"
                  class="input input-mono"
                  :class="errors['server.host'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['server.host']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['server.host'] }}
                </p>
                <label class="label mt-2.5">端口</label>
                <input
                  v-model="draft.server.port"
                  class="input input-mono"
                  :class="errors['server.port'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['server.port']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['server.port'] }}
                </p>
                <label class="label mt-2.5">最大连接数</label>
                <input
                  v-model="draft.server.maxConnection"
                  class="input input-mono"
                  :class="errors['server.maxConnection'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['server.maxConnection']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['server.maxConnection'] }}
                </p>
              </template>
              <div v-else class="space-y-1.5">
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">地址</span>
                  <span class="tabular text-slate-800">{{ draft.server.host }}</span>
                </p>
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">端口</span>
                  <span class="tabular text-slate-800">{{ draft.server.port }}</span>
                </p>
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">最大连接数</span>
                  <span class="tabular text-slate-800">{{ draft.server.maxConnection }}</span>
                </p>
              </div>
            </div>

            <!-- WS -->
            <div class="rounded-xl border border-slate-200 px-4 py-3">
              <p class="mb-2 text-xs font-medium text-slate-600">WS 访问点</p>
              <template v-if="editing">
                <label class="label">地址</label>
                <input
                  v-model="draft.wsServer.host"
                  class="input input-mono"
                  :class="errors['wsServer.host'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['wsServer.host']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['wsServer.host'] }}
                </p>
                <label class="label mt-2.5">端口</label>
                <input
                  v-model="draft.wsServer.port"
                  class="input input-mono"
                  :class="errors['wsServer.port'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['wsServer.port']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['wsServer.port'] }}
                </p>
                <label class="label mt-2.5">最大连接数</label>
                <input
                  v-model="draft.wsServer.maxConnection"
                  class="input input-mono"
                  :class="errors['wsServer.maxConnection'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['wsServer.maxConnection']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['wsServer.maxConnection'] }}
                </p>
              </template>
              <div v-else class="space-y-1.5">
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">地址</span>
                  <span class="tabular text-slate-800">{{ draft.wsServer.host }}</span>
                </p>
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">端口</span>
                  <span class="tabular text-slate-800">{{ draft.wsServer.port }}</span>
                </p>
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">最大连接数</span>
                  <span class="tabular text-slate-800">{{ draft.wsServer.maxConnection }}</span>
                </p>
              </div>
            </div>

            <!-- WSS -->
            <div class="rounded-xl border border-slate-200 px-4 py-3">
              <p class="mb-2 text-xs font-medium text-slate-600">WSS 访问点</p>
              <template v-if="editing">
                <label class="label">地址</label>
                <input
                  v-model="draft.wssServer.host"
                  class="input input-mono"
                  :class="errors['wssServer.host'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['wssServer.host']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['wssServer.host'] }}
                </p>
                <label class="label mt-2.5">端口</label>
                <input
                  v-model="draft.wssServer.port"
                  class="input input-mono"
                  :class="errors['wssServer.port'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['wssServer.port']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['wssServer.port'] }}
                </p>
                <label class="label mt-2.5">最大连接数</label>
                <input
                  v-model="draft.wssServer.maxConnection"
                  class="input input-mono"
                  :class="errors['wssServer.maxConnection'] ? 'input-invalid' : ''"
                />
                <p v-if="errors['wssServer.maxConnection']" class="mt-1 text-[11px] text-rose-600">
                  {{ errors['wssServer.maxConnection'] }}
                </p>
              </template>
              <div v-else class="space-y-1.5">
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">地址</span>
                  <span class="tabular text-slate-800">{{ draft.wssServer.host }}</span>
                </p>
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">端口</span>
                  <span class="tabular text-slate-800">{{ draft.wssServer.port }}</span>
                </p>
                <p class="flex justify-between text-xs">
                  <span class="text-slate-500">最大连接数</span>
                  <span class="tabular text-slate-800">{{ draft.wssServer.maxConnection }}</span>
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- HTTP / HTTPS / SSL / 日志 -->
        <div>
          <h3 class="mb-2.5 text-xs font-semibold tracking-wide text-slate-500">
            HTTP · SSL · 日志
          </h3>
          <div class="rounded-xl border border-slate-200 px-4 py-1.5">
            <!-- HTTP -->
            <div class="kv-row">
              <span class="kv-key">HTTP AP</span>
              <span class="kv-val">
                <template v-if="editing">
                  <div class="flex gap-2">
                    <input
                      v-model="draft.http.host"
                      class="input input-mono flex-1"
                      :class="errors['http.host'] ? 'input-invalid' : ''"
                    />
                    <input
                      v-model="draft.http.port"
                      class="input input-mono w-24"
                      :class="errors['http.port'] ? 'input-invalid' : ''"
                    />
                  </div>
                  <p v-if="errors['http.host'] || errors['http.port']" class="mt-1 text-[11px] text-rose-600">
                    {{ errors['http.host'] || errors['http.port'] }}
                  </p>
                </template>
                <span v-else class="tabular">
                  {{ draft.http.host }}:<span class="text-slate-400">{{ draft.http.port }}</span>
                </span>
              </span>
            </div>

            <!-- HTTPS -->
            <div class="kv-row">
              <span class="kv-key">HTTPS AP</span>
              <span class="kv-val">
                <template v-if="editing">
                  <div class="flex gap-2">
                    <input
                      v-model="draft.https.host"
                      class="input input-mono flex-1"
                      :class="errors['https.host'] ? 'input-invalid' : ''"
                    />
                    <input
                      v-model="draft.https.port"
                      class="input input-mono w-24"
                      :class="errors['https.port'] ? 'input-invalid' : ''"
                    />
                  </div>
                  <p
                    v-if="errors['https.host'] || errors['https.port']"
                    class="mt-1 text-[11px] text-rose-600"
                  >
                    {{ errors['https.host'] || errors['https.port'] }}
                  </p>
                </template>
                <span v-else class="tabular">
                  {{ draft.https.host }}:<span class="text-slate-400">{{ draft.https.port }}</span>
                </span>
              </span>
            </div>

            <!-- SSL Keystore -->
            <div class="kv-row">
              <span class="kv-key">SSL 证书</span>
              <span class="kv-val">
                <input
                  v-if="editing"
                  v-model="draft.ssl.keystore"
                  class="input input-mono"
                  :class="errors['ssl.keystore'] ? 'input-invalid' : ''"
                />
                <span v-else class="tabular">{{ draft.ssl.keystore || '--' }}</span>
              </span>
            </div>

            <!-- 存储密码 -->
            <div class="kv-row">
              <span class="kv-key">存储密码</span>
              <span class="kv-val">
                <input
                  v-if="editing"
                  v-model="draft.ssl.storePassword"
                  class="input input-mono"
                  :class="errors['ssl.storePassword'] ? 'input-invalid' : ''"
                />
                <span v-else class="tabular">{{ draft.ssl.storePassword || '--' }}</span>
              </span>
            </div>

            <!-- 管理密码 -->
            <div class="kv-row">
              <span class="kv-key">管理密码</span>
              <span class="kv-val">
                <input
                  v-if="editing"
                  v-model="draft.ssl.managerPassword"
                  class="input input-mono"
                  :class="errors['ssl.managerPassword'] ? 'input-invalid' : ''"
                />
                <span v-else class="tabular">{{ draft.ssl.managerPassword || '--' }}</span>
              </span>
            </div>

            <!-- 日志等级 -->
            <div class="kv-row">
              <span class="kv-key">日志等级</span>
              <span class="kv-val">
                <select v-if="editing" v-model="draft.logLevel" class="select w-40">
                  <option v-for="level in LOG_LEVELS" :key="level" :value="level">{{ level }}</option>
                </select>
                <span v-else class="pill pill-neutral">{{ draft.logLevel }}</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 服务列表 -->
        <div>
          <h3 class="mb-2.5 text-xs font-semibold tracking-wide text-slate-500">服务列表</h3>
          <div class="rounded-xl border border-slate-200 px-4 py-3">
            <div class="flex flex-wrap gap-2">
              <span
                v-for="name in draft.cellets"
                :key="name"
                class="pill"
                :class="isReservedCellet(name) ? 'pill-neutral' : 'pill-info'"
              >
                {{ name }}
                <button
                  v-if="editing && !isReservedCellet(name)"
                  type="button"
                  class="text-brand-400 transition hover:text-rose-600"
                  :aria-label="`移除 ${name}`"
                  @click="removeCellet(name)"
                >
                  <AppIcon name="close" :size="11" />
                </button>
              </span>
              <span v-if="draft.cellets.length === 0" class="text-xs text-slate-400">暂无</span>
            </div>

            <div v-if="editing" class="mt-3 flex gap-2">
              <input
                v-model="newCellet"
                class="input flex-1"
                placeholder="填写新增的服务单元名后回车"
                @keyup.enter="addCellet"
              />
              <button type="button" class="btn btn-primary" @click="addCellet">
                <AppIcon name="plus" :size="13" />
                添加
              </button>
            </div>
          </div>
        </div>

        <!-- 服务单元路由 -->
        <div>
          <div class="mb-2.5 flex items-center justify-between">
            <h3 class="text-xs font-semibold tracking-wide text-slate-500">服务单元</h3>
            <div v-if="editing" class="flex gap-1.5">
              <button type="button" class="btn btn-ghost btn-sm" @click="addDirector">
                <AppIcon name="plus" :size="12" />
                新增
              </button>
              <button type="button" class="btn btn-danger btn-sm" @click="removeDirector">
                <AppIcon name="trash" :size="12" />
                删除
              </button>
            </div>
          </div>

          <div class="rounded-xl border border-slate-200 px-4 py-3">
            <div class="mb-3 flex flex-wrap gap-1.5">
              <button
                v-for="(director, index) in draft.directors"
                :key="`${director.address}:${director.port}-${index}`"
                type="button"
                class="tabular rounded-lg px-2.5 py-1 text-xs font-medium transition"
                :class="
                  selectedDirectorIndex === index
                    ? 'bg-brand-50 text-brand-700 ring-1 ring-brand-200 ring-inset'
                    : 'text-slate-500 hover:bg-slate-100'
                "
                @click="selectedDirectorIndex = index"
              >
                #{{ index + 1 }} {{ director.address }}:{{ director.port }}
              </button>
            </div>

            <div v-if="selectedDirector" class="space-y-3">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div>
                  <label class="label">地址</label>
                  <input
                    v-if="editing"
                    v-model="selectedDirector.address"
                    class="input input-mono"
                    :class="directorError ? 'input-invalid' : ''"
                  />
                  <p v-else class="tabular text-[13px] text-slate-800">
                    {{ selectedDirector.address }}
                  </p>
                </div>
                <div>
                  <label class="label">端口</label>
                  <input
                    v-if="editing"
                    v-model="selectedDirector.port"
                    class="input input-mono"
                    :class="directorError ? 'input-invalid' : ''"
                  />
                  <p v-else class="tabular text-[13px] text-slate-800">
                    {{ selectedDirector.port }}
                  </p>
                </div>
                <div>
                  <label class="label">权重</label>
                  <input
                    v-if="editing"
                    v-model="selectedDirector.weight"
                    class="input input-mono"
                    :class="directorError ? 'input-invalid' : ''"
                  />
                  <p v-else class="tabular text-[13px] text-slate-800">
                    {{ selectedDirector.weight }}
                  </p>
                </div>
              </div>

              <p v-if="editing && directorError" class="text-[11px] text-rose-600">
                {{ directorError }}
              </p>

              <div>
                <label class="label">该服务单元承载的服务</label>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="name in selectedDirector.cellets"
                    :key="name"
                    class="pill"
                    :class="isReservedCellet(name) ? 'pill-neutral' : 'pill-info'"
                  >
                    {{ name }}
                    <button
                      v-if="editing && !isReservedCellet(name)"
                      type="button"
                      class="text-brand-400 transition hover:text-rose-600"
                      :aria-label="`移除 ${name}`"
                      @click="removeDirectorCellet(name)"
                    >
                      <AppIcon name="close" :size="11" />
                    </button>
                  </span>
                  <span v-if="selectedDirector.cellets.length === 0" class="text-xs text-slate-400">
                    暂无
                  </span>
                </div>

                <div v-if="editing" class="mt-2.5 flex gap-2">
                  <input
                    v-model="newDirectorCellet"
                    class="input flex-1"
                    placeholder="填写新增的服务单元名后回车"
                    @keyup.enter="addDirectorCellet"
                  />
                  <button type="button" class="btn btn-primary" @click="addDirectorCellet">
                    <AppIcon name="plus" :size="13" />
                    添加
                  </button>
                </div>
              </div>
            </div>
            <p v-else class="text-xs text-slate-400">暂无服务单元配置</p>
          </div>
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn btn-ghost" @click="closeDetails">关闭</button>
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
        调度机服务器吗？
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
