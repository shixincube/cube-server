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
 * 联系人（触点）统计数据。
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

/** AI 单元（ID 十进制位数大于等于 6 且小于 8 位的联系人） */
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
