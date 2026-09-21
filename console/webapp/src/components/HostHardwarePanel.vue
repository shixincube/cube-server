<script setup lang="ts">
/**
 * 主机硬件配置（静态信息）。
 *
 * 只渲染键值行，不包含卡片外壳与标题——由 `DashboardView` 统一提供，
 * 以便与「实时性能」区在同一张卡片内形成「静态 / 动态」的清晰分层。
 */
import { computed } from 'vue'
import type { HostStaticResponse } from '@/api/types'
import { formatBytes, formatFrequency } from '@/utils/format'

const props = defineProps<{
  info: HostStaticResponse | null
  loading?: boolean
}>()

/** 组装键值行；值为空时统一显示 `--` 并保持行存在，避免布局跳动 */
const rows = computed<{ key: string; value: string }[]>(() => {
  const info = props.info
  if (!info) {
    return []
  }

  const cpu = info.cpu ?? {}
  const memory = info.memory ?? {}
  const system = info.system ?? {}
  const host = info.host ?? {}
  const runtime = info.runtime ?? {}
  const baseboard = system.baseboard ?? {}
  const firmware = system.firmware ?? {}

  // 整机型号 / 主板 / 固件统一按「厂商 + 型号」拼接，厂商为空时只显示型号
  const join = (...parts: (string | undefined)[]): string =>
    parts.filter((part) => part && part.trim().length > 0).join(' ') || '--'

  // 缓存按层级聚合，同层多个缓存求和
  const cacheByLevel = new Map<number, number>()
  for (const cache of cpu.caches ?? []) {
    cacheByLevel.set(cache.level, (cacheByLevel.get(cache.level) ?? 0) + cache.sizeBytes)
  }
  const cacheText =
    cacheByLevel.size === 0
      ? '--'
      : [...cacheByLevel.entries()]
          .sort((left, right) => left[0] - right[0])
          .map(([level, size]) => `L${level} ${formatBytes(size)}`)
          .join(' · ')

  // 核心规格：物理 / 逻辑（多路时才补路数）
  const coreParts: string[] = []
  if (cpu.physicalCores) {
    coreParts.push(`${cpu.physicalCores} 物理核`)
  }
  if (cpu.logicalCores) {
    coreParts.push(`${cpu.logicalCores} 逻辑核`)
  }
  if ((cpu.physicalPackages ?? 0) > 1) {
    coreParts.push(`${cpu.physicalPackages} 路`)
  }
  const coreText = coreParts.length > 0 ? coreParts.join(' / ') : '--'

  // 内存：总容量 + 内存条类型与频率
  const modules = memory.modules ?? []
  const moduleTypes = [...new Set(modules.map((item) => item.memoryType).filter(Boolean))]
  const moduleSpeeds = [
    ...new Set(modules.map((item) => item.clockSpeedHz).filter((speed) => speed > 0))
  ]
  const memoryParts = [formatBytes(memory.totalBytes)]
  if (modules.length > 0) {
    memoryParts.push(`${modules.length} 条`)
  }
  if (moduleTypes.length > 0) {
    memoryParts.push(moduleTypes.join('/'))
  }
  if (moduleSpeeds.length > 0) {
    memoryParts.push(formatFrequency(Math.max(...moduleSpeeds)))
  }

  // 存储：逐块磁盘「型号 容量」
  const drives = info.diskDrives ?? []
  const driveText =
    drives.length === 0
      ? '--'
      : drives
          .map((drive) => `${drive.model || drive.name} ${formatBytes(drive.sizeBytes)}`)
          .join('；')

  // 网卡：只列已启用的物理网卡
  const nics = (info.nicList ?? []).filter((nic) => nic.up)
  const nicText =
    nics.length === 0
      ? '--'
      : nics
          .map((nic) => {
            const addresses = nic.addresses.filter((item) => item.indexOf(':') < 0).join(', ')
            return [nic.displayName || nic.name, addresses, nic.mac].filter(Boolean).join(' · ')
          })
          .join('；')

  // 操作系统：家族 + 版本 + 架构 + 位数
  const osParts = [host.osFamily || host.osName]
  const osVersion = host.osVersionInfo || host.osVersion
  if (osVersion) {
    osParts.push(osVersion)
  }
  if (host.osBuildNumber) {
    osParts.push(`build ${host.osBuildNumber}`)
  }
  if (host.osArch) {
    osParts.push(host.osArch)
  }
  if (host.bitness) {
    osParts.push(`${host.bitness} 位`)
  }

  // 运行时：Java 版本 + JVM + 进程号
  const runtimeParts = [runtime.jvmName, `Java ${runtime.javaVersion}`].filter(Boolean)
  if (runtime.pid) {
    runtimeParts.push(`PID ${runtime.pid}`)
  }

  return [
    { key: '主机名', value: host.hostName || '--' },
    { key: '整机型号', value: join(system.manufacturer, system.model) },
    { key: 'CPU', value: cpu.name || '--' },
    {
      key: '核心 / 主频',
      value: `${coreText}${cpu.maxFrequencyHz ? ` · ${formatFrequency(cpu.maxFrequencyHz)}` : ''}`
    },
    { key: '处理器缓存', value: cacheText },
    { key: '内存容量', value: memoryParts.join(' · ') },
    { key: '存储设备', value: driveText },
    { key: '主 板', value: join(baseboard.manufacturer, baseboard.model) },
    { key: '固件 / BIOS', value: join(firmware.name, firmware.version) },
    { key: '网卡', value: nicText },
    { key: '操作系统', value: osParts.filter(Boolean).join(' · ') || '--' },
    { key: '控制台运行时', value: runtimeParts.join(' · ') || '--' }
  ]
})

const placeholders = computed(() => Array.from({ length: 12 }, (_, index) => index))
</script>

<template>
  <div>
    <!-- 加载骨架：首次拉取静态信息期间占位，避免区块塌陷 -->
    <div v-if="loading && !info" class="grid grid-cols-1 gap-x-6 sm:grid-cols-2">
      <div v-for="item in placeholders" :key="item" class="kv-row">
        <span class="kv-key">加载中</span>
        <span class="kv-val animate-pulse text-slate-300">--</span>
      </div>
    </div>

    <div v-else-if="rows.length > 0" class="grid grid-cols-1 gap-x-6 sm:grid-cols-2">
      <div v-for="row in rows" :key="row.key" class="kv-row">
        <span class="kv-key">{{ row.key }}</span>
        <span class="kv-val" :class="{ 'text-slate-400': row.value === '--' }">
          {{ row.value }}
        </span>
      </div>
    </div>

    <p v-else class="text-xs text-slate-400">未能读取主机硬件信息。</p>
  </div>
</template>
