/**
 * Cube Console 后端数据模型定义。
 *
 * 字段名与 `console/src/cube/console/**` 下各 `toJSON()` 实现严格对齐，
 * 修改后端模型时必须同步此处。
 */

/** 通用端点 */
export interface Endpoint {
  host: string
  port: number
}

/** 访问点，比普通端点多出最大连接数 */
export interface AccessPoint extends Endpoint {
  maxConnection: number
}

/** SSL 配置 */
export interface SslConfig {
  keystore: string
  storePassword: string
  managerPassword: string
}

/** 调度机的路由目标服务单元 */
export interface Director {
  address: string
  port: number
  weight: number
  cellets: string[]
}

/** 服务单元里某个 Cellet 对应的 JAR 文件信息 */
export interface CelletJar {
  path: string
  name?: string
  size?: number
  lastModified?: number
}

/** 服务单元的 Cellet 配置 */
export interface CelletConfig {
  ports: number[]
  jar?: CelletJar
  classes: string[]
}

/** 缓存集群节点 */
export interface ClusterNode {
  host: string
  port: number
}

/** 通用缓存器配置（对应 token-pool / general-cache / contact-cache / filelabel-cache） */
export interface CacheConfig {
  host: string
  port: number
  capacity: number
  /** 毫秒 */
  expiry: number
  /** 字节 */
  threshold: number
  blocking: number
  storage: string
  routetable: string
  clusterNodes: ClusterNode[]
  pedestal?: ClusterNode
  backupPedestal?: ClusterNode
}

/** 消息时序缓存器配置（对应 messaging-series-memory） */
export interface SeriesCacheConfig {
  host: string
  port: number
  capacity: number
  segmentNum: number
  segmentSize: number
  /** 毫秒 */
  expiry: number
  /** 字节 */
  indexThreshold: number
  /** 字节 */
  dataThreshold: number
  timeout: number
  storage: string
  clusterNodes: ClusterNode[]
  pedestal?: ClusterNode
  backupPedestal?: ClusterNode
}

/** 服务单元里单类存储的配置 */
export interface StorageConfig {
  type: string
  file?: string
  host?: string
  port?: number
  schema?: string
  user?: string
  password?: string
}

/** 调度机服务器 */
export interface DispatcherServer {
  tag: string
  deployPath: string
  name: string
  cellConfigFile: string
  propertiesFile: string
  running: boolean
  server: AccessPoint
  wsServer: AccessPoint
  wssServer: AccessPoint
  http: AccessPoint
  https: AccessPoint
  ssl?: SslConfig
  logLevel: string
  cellets: string[]
  directors: Director[]
}

/** 服务单元服务器 */
export interface ServiceServer {
  tag: string
  deployPath: string
  configPath: string
  celletsPath: string
  name: string
  running: boolean
  server: AccessPoint
  logLevel: string
  cellets: CelletConfig[]
  adapter: Endpoint
  storage: Record<string, StorageConfig>
  tokenPool: CacheConfig
  generalCache: CacheConfig
  contactCache: CacheConfig
  fileLabelCache: CacheConfig
  messagingSeries: SeriesCacheConfig
}

/** 控制台用户 */
export interface ConsoleUser {
  name: string
  avatar: string
  displayName: string
  role: number
  group: string
}

/** 登录令牌 */
export interface UserToken {
  token: string
  creation: number
  expire: number
  user: ConsoleUser
}

/** 列表接口的通用响应外壳 */
export interface ListResponse<T> {
  tag?: string
  list: T[]
}

/** 默认部署信息（调度机） */
export interface DispatcherDeployInfo {
  tag?: string
  deployPath?: string
  cellConfigFile?: string
  propertiesFile?: string
}

/** 默认部署信息（服务单元） */
export interface ServiceDeployInfo {
  tag?: string
  deployPath?: string
  configPath?: string
  celletsPath?: string
}

/** 日志行 */
export interface LogLine {
  time: number
  /** 1=Debug 2=Info 3=Warning 4=Error */
  level: number
  tag: string
  text: string
}

/** 日志查询响应 */
export interface LogResponse {
  name?: string
  lines: LogLine[]
  last: number
}

/** JVM 报告 */
export interface JVMReport {
  name: string
  timestamp: number
  reporter: string
  maxMemory: number
  totalMemory: number
  freeMemory: number
  systemStartTime: number
  systemDuration: number
}

/** 连接数统计 */
export interface ConnNum {
  port: number
  realtime: number
  max: number
}

/** 平均应答时间的统计值 */
export interface AvgResponseValue {
  value: number
  delta: number
}

/** 业务单元运行时指标 */
export interface PerfItem {
  onlineNum: number
  maxNum: number
  [key: string]: number
}

/** 性能报告（benchmark 的详细结构由后端动态生成，此处只声明用到的部分） */
export interface PerformanceReport {
  name: string
  timestamp: number
  reporter: string
  systemStartTime: number
  systemDuration: number
  connNums: ConnNum[]
  items: Record<string, PerfItem>
  benchmark: {
    avgResponseTimeMap: Record<string, Record<string, AvgResponseValue>>
    counterMap: Record<string, number>
  }
}

/** 服务器报告查询响应 */
export interface JVMReportResponse {
  name: string
  list: JVMReport[]
}

export interface PerformanceReportResponse {
  name: string
  report?: PerformanceReport
}

/** 用户统计的时段切片 */
export interface StatisticSlice {
  beginning: number
  ending: number
  numContacts: number
}

/**
 * 联系人统计数据。
 *
 * 统计口径：只统计 ID 十进制位数大于等于 8 位的联系人。
 */
export interface Statistic {
  TNU: number
  DAU: number
  /** 平均在线时长，单位：小时 */
  AOT: number
  TD: StatisticSlice[]
  /** 日新增用户，仅在 /statistic/recent 里由控制台计算后下发 */
  DNU?: number
}

/** 统计接口响应 */
export interface StatisticResponse {
  tag: string
  statistic: Statistic
  year: number
  month: number
  date: number
}

/** AI 单元的工作状态 */
export type UnitState = 'online' | 'offline' | 'inactive'

/**
 * AI 单元能力（后端 `AICapability`）。
 *
 * 能力挂在 AIGC 单元上，而单元以 Contact 为物理实体：一个物理实体上可以注册多个单元，
 * 因此台账的一行（一个物理实体）会对应一个能力数组。能力只存在于节点内存，由节点周期上报。
 */
export interface UnitCapability {
  /** 能力名，例如 TextGeneration */
  name: string
  /** 能力所属任务类型，例如 NaturalLanguageProcessing */
  task: string
  version?: string
  /** 单一子任务时后端给字符串 */
  subtask?: string
  /** 多个子任务时后端给数组 */
  subtasks?: string[]
  description: string
}

/**
 * AI 单元（ID 十进制位数大于等于 6 且小于 8 位的联系人）。
 *
 * 注意：这里的「一行」是一个 Contact 物理实体，不是单个 AIGC 单元实例——
 * 同一个实体上可能注册了多个单元，能力数组即由这些单元汇总而来。
 */
export interface UnitInfo {
  id: number
  name: string
  /** 最近上报的设备名 */
  device: string
  /** 最近上报的设备平台 */
  platform: string
  state: UnitState
  /** 统计日内的活动次数 */
  activeCount: number
  /** 统计日内的在线时长，单位：小时 */
  duration: number
  /** 最近一次活动时间戳，0 表示回溯窗口内没有活动 */
  lastActiveTime: number
  /** 统计日首次活动时间戳，0 表示当日没有活动 */
  firstActiveTime: number
  /** 回溯窗口内的活跃天数 */
  activeDays: number
  /**
   * 该物理实体当前承载的能力。
   *
   * 来自节点上报（约每分钟一次），节点未上报或上报已过期时为空数组。
   * 字段可选是为了兼容尚未升级的后端。
   */
  capabilities?: UnitCapability[]
}

/** 单元活跃时段切片 */
export interface UnitTimeSlice {
  slice: number
  beginning: number
  ending: number
  numUnits: number
}

/** AIGC 工作单元概览响应 */
export interface UnitOverviewResponse {
  tag: string
  domain: string
  year: number
  month: number
  date: number
  /** 统计日起始时间戳 */
  beginning: number
  /** 统计日结束时间戳 */
  ending: number
  /** 单元总数 */
  total: number
  /** 统计日有活动的单元数 */
  activeCount: number
  /** 当前在线（最近 24 小时内上报过）的单元数 */
  onlineCount: number
  offlineCount: number
  inactiveCount: number
  /** 统计日所有单元的总在线时长，单位：小时 */
  totalDuration: number
  /** 统计日活跃单元的平均在线时长，单位：小时 */
  avgDuration: number
  /** 统计日活跃峰值时段 */
  peak: UnitTimeSlice | null
  timeline: UnitTimeSlice[]
  units: UnitInfo[]
  /** 最近一次收到单元能力上报的时间戳，0 表示控制台从未收到过（能力列会全空） */
  unitReportTime?: number
}

/** 更新调度机配置时提交的数据体 */
export interface DispatcherConfigPayload {
  tag: string
  deployPath: string
  server: Endpoint & { maxConnection: number }
  wsServer: Endpoint & { maxConnection: number }
  wssServer: Endpoint & { maxConnection: number }
  http: Endpoint
  https: Endpoint
  ssl: SslConfig
  logLevel: string
  cellets: string[]
  directors: Director[]
}

/* ------------------------------------------------------------------ */
/* 主机硬件与性能（GET /host/static、GET /host/metrics）                */
/* ------------------------------------------------------------------ */

/** 主机基本信息 */
export interface HostBasicInfo {
  hostName: string
  osName: string
  osVersion: string
  osArch: string
  timezone: string
  osFamily?: string
  osManufacturer?: string
  bitness?: number
  /** 主机启动时间（毫秒） */
  bootTime?: number
  osVersionInfo?: string
  osBuildNumber?: string
  osCodeName?: string
}

/** 整机 / 主板 / BIOS 信息 */
export interface HostSystemInfo {
  manufacturer?: string
  model?: string
  serialNumber?: string
  uuid?: string
  firmware?: {
    manufacturer?: string
    name?: string
    version?: string
    releaseDate?: string
  }
  baseboard?: {
    manufacturer?: string
    model?: string
    version?: string
    serialNumber?: string
  }
}

/** CPU 规格 */
export interface HostCpuInfo {
  name?: string
  vendor?: string
  family?: string
  model?: string
  stepping?: string
  microarchitecture?: string
  processorId?: string
  identifier?: string
  cpu64bit?: boolean
  physicalCores?: number
  logicalCores?: number
  physicalPackages?: number
  maxFrequencyHz?: number
  caches?: { level: number; type: string; sizeBytes: number }[]
}

/** 内存条 */
export interface HostMemoryModule {
  bankLabel: string
  capacityBytes: number
  clockSpeedHz: number
  manufacturer: string
  memoryType: string
  partNumber: string
}

/** 内存规格 */
export interface HostMemoryInfo {
  totalBytes?: number
  pageSizeBytes?: number
  modules?: HostMemoryModule[]
}

/** 物理磁盘 */
export interface HostDiskDrive {
  name: string
  model: string
  serial: string
  sizeBytes: number
  partitions: {
    identification: string
    name: string
    type: string
    label: string
    sizeBytes: number
    mountPoint: string
  }[]
}

/** 网卡（静态） */
export interface HostNic {
  name: string
  displayName: string
  up: boolean
  mtu: number
  mac: string
  addresses: string[]
}

/** 控制台运行时信息 */
export interface HostRuntimeInfo {
  javaVersion?: string
  javaVendor?: string
  jvmName?: string
  jvmVersion?: string
  availableProcessors?: number
  configuredMaxHeapBytes?: number
  fileEncoding?: string
  userName?: string
  workDir?: string
  pid?: number
  startTime?: number
}

/** `GET /host/static` 响应 */
export interface HostStaticResponse {
  sampledAt: number
  host: HostBasicInfo
  system: HostSystemInfo
  cpu: HostCpuInfo
  memory: HostMemoryInfo
  diskDrives: HostDiskDrive[]
  nicList: HostNic[]
  runtime: HostRuntimeInfo
}

/** CPU 时间构成占比（0~1） */
export interface HostCpuBreakdown {
  user?: number
  system?: number
  idle?: number
  iowait?: number
  irq?: number
  nice?: number
  steal?: number
}

/** 动态 CPU 指标 */
export interface HostCpuMetrics {
  /** 系统整体 CPU 使用率（0~1） */
  usage?: number
  breakdown?: HostCpuBreakdown
  /** 1 / 5 / 15 分钟负载均值，取不到时为 null */
  loadAverage?: (number | null)[]
  contextSwitches?: number
  interrupts?: number
  logicalCores?: number
  /** 控制台进程自身 CPU 占用（0~1） */
  processUsage?: number
}

/** 动态内存指标 */
export interface HostMemoryMetrics {
  totalBytes: number
  availableBytes: number
  usedBytes: number
  /** 使用率（0~1） */
  usage: number
  swapTotalBytes: number
  swapUsedBytes: number
  swapUsage: number
}

/** 控制台 JVM 指标 */
export interface HostJvmMetrics {
  heapUsedBytes: number
  heapCommittedBytes: number
  heapMaxBytes: number
  heapUsage: number
  nonHeapUsedBytes: number
  threadCount: number
  peakThreadCount: number
  uptimeMillis: number
  startTime: number
}

/** 有流量的网卡明细 */
export interface HostActiveNic {
  name: string
  displayName: string
  mac: string
  ipv4: string[]
  mtu: number
  linkSpeedBps: number
  rxBytesTotal: number
  txBytesTotal: number
  rxBytesPerSec?: number
  txBytesPerSec?: number
  inErrors: number
  outErrors: number
}

/** 动态网络指标 */
export interface HostNetworkMetrics {
  rxBytesPerSec?: number
  txBytesPerSec?: number
  rxBytesTotal: number
  txBytesTotal: number
  rxPacketsTotal: number
  txPacketsTotal: number
  activeInterfaces: HostActiveNic[]
}

/** 磁盘吞吐 */
export interface HostDiskThroughput {
  name: string
  model: string
  sizeBytes: number
  readBytesTotal: number
  writeBytesTotal: number
  readBytesPerSec?: number
  writeBytesPerSec?: number
  readsTotal: number
  writesTotal: number
  queueLength: number
}

/** 分区占用 */
export interface HostPartitionUsage {
  name: string
  mount: string
  type: string
  local: boolean
  totalBytes: number
  usableBytes: number
  freeBytes: number
  /** 使用率（0~1） */
  usage: number
}

/** 动态磁盘指标 */
export interface HostDiskMetrics {
  readBytesPerSec?: number
  writeBytesPerSec?: number
  readBytesTotal: number
  writeBytesTotal: number
  drives: HostDiskThroughput[]
  partitions: HostPartitionUsage[]
}

/** 动态系统指标 */
export interface HostSystemMetrics {
  processCount?: number
  threadCount?: number
  uptimeSeconds?: number
  /** 主机启动时间（毫秒） */
  bootTime?: number
  openFileDescriptors?: number
  maxFileDescriptors?: number
}

/** `GET /host/metrics` 响应 */
export interface HostMetricsResponse {
  timestamp: number
  sampleIntervalSeconds: number
  cpu?: HostCpuMetrics
  memory?: HostMemoryMetrics
  jvm?: HostJvmMetrics
  network?: HostNetworkMetrics
  disk?: HostDiskMetrics
  system?: HostSystemMetrics
}
