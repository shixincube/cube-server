/**
 * 服务器运行指标的计算与拉取。
 *
 * 对应旧版 `Console#calcDispatcherLoad` / `#calcServiceLoad` / `#arrangeAvgResponseTime`，
 * 以及服务单元监视器的历史数据回溯逻辑。
 */

import { queryJVMReport, queryPerformanceReport } from '@/api/console'
import type { AvgResponseValue, JVMReport, PerformanceReport } from '@/api/types'
import { formatTimeHHMM } from '@/utils/format'

/** 负载百分比（0-100） */
export interface LoadRate {
  /** 实时连接总数 */
  realtime: number
  /** 最大连接总数 */
  max: number
  /** 百分比，取整 */
  percent: number
}

/** 单个访问点的负载 */
export interface AccessPointLoad {
  port: number
  realtime: number
  max: number
  percent: number
}

/** 平均应答时间排行项 */
export interface AvgResponseRankItem {
  cellet: string
  action: string
  value: number
  delta: number
}

/** JVM 内存图表数据（单位：MB） */
export interface JVMSeries {
  labels: string[]
  /** 堆已用内存 */
  used: number[]
  /** 堆空闲内存 */
  free: number[]
}

/** 监视器聚合数据 */
export interface MonitorSnapshot {
  /** 报告时间戳 */
  timestamp: number
  /** 系统启动时间 */
  systemStartTime: number
  /** 综合负载 */
  load: LoadRate
  /** 各访问点负载，按端口索引 */
  accessPointLoads: AccessPointLoad[]
  /** 平均应答时间排行（前 N 项） */
  avgResponseRank: AvgResponseRankItem[]
  /** 任务执行计数，按名称升序 */
  counters: { name: string; value: number }[]
}

/** 计算调度机综合负载：所有访问点实时连接均值 / 最大连接均值 */
export function calcDispatcherLoad(perf: PerformanceReport): LoadRate {
  const connNums = perf.connNums ?? []
  let sumRealtime = 0
  let sumMax = 0
  for (const item of connNums) {
    sumRealtime += item.realtime
    sumMax += item.max
  }
  if (sumMax === 0 || connNums.length === 0) {
    return { realtime: sumRealtime, max: sumMax, percent: 0 }
  }
  const avgRealtime = sumRealtime / connNums.length
  const avgMax = sumMax / connNums.length
  return {
    realtime: sumRealtime,
    max: sumMax,
    percent: Math.floor((avgRealtime / avgMax) * 100)
  }
}

/** 计算服务单元负载：Contact 单元的在线数占比 */
export function calcServiceLoad(perf: PerformanceReport): LoadRate {
  const contact = perf.items?.Contact
  if (!contact) {
    return { realtime: 0, max: 0, percent: 0 }
  }
  return {
    realtime: contact.onlineNum,
    max: contact.maxNum,
    percent: Math.floor((contact.onlineNum / contact.maxNum) * 100)
  }
}

/** 拉平平均应答时间为排行列表，按耗时从大到小排序 */
export function arrangeAvgResponseTime(perf: PerformanceReport): AvgResponseRankItem[] {
  const map = perf.benchmark?.avgResponseTimeMap ?? {}
  const list: AvgResponseRankItem[] = []
  for (const cellet of Object.keys(map)) {
    const actions = map[cellet]
    for (const action of Object.keys(actions)) {
      const item: AvgResponseValue = actions[action]
      list.push({ cellet, action, value: item.value, delta: item.delta })
    }
  }
  list.sort((a, b) => b.value - a.value)
  return list
}

/** 取出按名称排名的执行计数，用于监视器柱状图 */
export function arrangeCounters(perf: PerformanceReport): { name: string; value: number }[] {
  const map = perf.benchmark?.counterMap ?? {}
  return Object.keys(map)
    .sort()
    .map((name) => ({ name, value: map[name] }))
}

/** 拉取指定服务器的最近一份性能报告 */
export async function fetchLastPerformance(name: string): Promise<PerformanceReport | null> {
  const response = await queryPerformanceReport(name, 0, true)
  return response.report ?? null
}

/** 拉取指定时间点的性能报告 */
export async function fetchPerformanceAt(
  name: string,
  time: number
): Promise<PerformanceReport | null> {
  const response = await queryPerformanceReport(name, time, true)
  return response.report ?? null
}

/**
 * 回溯最近 10 分钟的 JVM 报告，用于渲染内存趋势。
 *
 * 后端按时间点单点查询，因此这里以当前报告时间为基准向前取 10 个一分钟刻度。
 */
export async function fetchJVMHistory(
  name: string,
  baseTime: number,
  points = 10
): Promise<JVMReport[]> {
  const requests: Promise<JVMReport[]>[] = []
  for (let i = points - 1; i >= 0; i--) {
    requests.push(queryJVMReport(name, 1, baseTime - i * 60000).then((r) => r.list ?? []))
  }
  const batches = await Promise.all(requests)
  return batches.map((list) => list[0]).filter((item): item is JVMReport => Boolean(item))
}

/**
 * JVM 报告序列转为图表数据（内存单位：MB，已用 = 堆总量 - 空闲量）。
 *
 * 注意：节点上报的是字节数，控制台在 `Console#appendJVMReport` 里已经按 1048576 换算成 MB 后入库，
 * 因此 `/server-report?report=JVMReport` 返回的数值本身就是 MB，这里不能再除一次。
 */
export function toJVMSeries(reports: JVMReport[]): JVMSeries {
  return {
    labels: reports.map((item) => formatTimeHHMM(item.timestamp)),
    used: reports.map((item) => Math.round(item.totalMemory - item.freeMemory)),
    free: reports.map((item) => Math.round(item.freeMemory))
  }
}

/** 从性能报告构造监视器快照 */
export function toMonitorSnapshot(perf: PerformanceReport): MonitorSnapshot {
  const connNums = perf.connNums ?? []
  return {
    timestamp: perf.timestamp,
    systemStartTime: perf.systemStartTime,
    load: calcDispatcherLoad(perf),
    accessPointLoads: connNums.map((item) => ({
      port: item.port,
      realtime: item.realtime,
      max: item.max,
      percent: item.max > 0 ? Math.floor((item.realtime / item.max) * 100) : 0
    })),
    avgResponseRank: arrangeAvgResponseTime(perf),
    counters: arrangeCounters(perf)
  }
}
