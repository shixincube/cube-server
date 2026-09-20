/**
 * Cube Console 接口封装。
 *
 * 每个方法对应 `console/src/cube/console/container/handler/` 下的一个后端句柄。
 */

import { ApiError, request } from './client'
import type {
  DispatcherConfigPayload,
  DispatcherDeployInfo,
  DispatcherServer,
  JVMReport,
  JVMReportResponse,
  ListResponse,
  LogResponse,
  PerformanceReportResponse,
  ServiceDeployInfo,
  ServiceServer,
  StatisticResponse,
  UnitOverviewResponse,
  UserToken
} from './types'

/** JVM 报告的固定名称，后端按名字分派 */
export const JVM_REPORT_NAME = 'JVMReport'

/** 性能报告的固定名称，后端按名字分派 */
export const PERFORMANCE_REPORT_NAME = 'PerfReport'

/** 登录：账号 + 口令（口令需先做 MD5） */
export function signIn(username: string, password: string): Promise<UserToken> {
  return request<UserToken>('/signin', {
    method: 'POST',
    form: { username, password },
    skipUnauthorizedHandler: true
  })
}

/** 凭 Cookie 校验并续期当前登录态 */
export function signInWithToken(): Promise<UserToken> {
  return request<UserToken>('/signin', {
    method: 'POST',
    skipUnauthorizedHandler: true
  })
}

/** 退出登录 */
export function signOut(): Promise<void> {
  return request<void>('/signout', { method: 'POST', skipUnauthorizedHandler: true })
}

/** 调度机列表 */
export function fetchDispatchers(): Promise<ListResponse<DispatcherServer>> {
  return request<ListResponse<DispatcherServer>>('/servers/dispatcher')
}

/** 服务单元列表 */
export function fetchServices(): Promise<ListResponse<ServiceServer>> {
  return request<ListResponse<ServiceServer>>('/servers/service')
}

/** 控制台上配置的调度机默认部署信息 */
export function fetchDispatcherDeployInfo(): Promise<DispatcherDeployInfo> {
  return request<DispatcherDeployInfo>('/deploy/dispatcher')
}

/** 控制台上配置的服务单元默认部署信息 */
export function fetchServiceDeployInfo(): Promise<ServiceDeployInfo> {
  return request<ServiceDeployInfo>('/deploy/service')
}

/** 查询调度机实时状态 */
export function fetchDispatcherStatus(tag: string, path: string): Promise<DispatcherServer> {
  return request<DispatcherServer>('/dispatcher/status', {
    method: 'POST',
    form: { tag, path }
  })
}

/** 启动调度机，`password` 需为 MD5 摘要 */
export function startDispatcher(
  tag: string,
  path: string,
  password: string
): Promise<DispatcherServer> {
  return request<DispatcherServer>('/dispatcher/start', {
    method: 'POST',
    form: { tag, path, pwd: password },
    timeout: 60000
  })
}

/** 关停调度机，`password` 需为 MD5 摘要 */
export function stopDispatcher(
  tag: string,
  path: string,
  password: string
): Promise<DispatcherServer> {
  return request<DispatcherServer>('/dispatcher/stop', {
    method: 'POST',
    form: { tag, path, pwd: password },
    timeout: 60000
  })
}

/** 提交调度机配置 */
export function updateDispatcherConfig(
  config: DispatcherConfigPayload
): Promise<DispatcherServer> {
  return request<DispatcherServer>('/dispatcher/config', {
    method: 'POST',
    form: { config: JSON.stringify(config) },
    timeout: 30000
  })
}

/** 查询服务单元实时状态 */
export function fetchServiceStatus(tag: string, path: string): Promise<ServiceServer> {
  return request<ServiceServer>('/service/status', {
    method: 'POST',
    form: { tag, path }
  })
}

/** 启动服务单元，`password` 需为 MD5 摘要 */
export function startService(tag: string, path: string, password: string): Promise<ServiceServer> {
  return request<ServiceServer>('/service/start', {
    method: 'POST',
    form: { tag, path, pwd: password },
    timeout: 60000
  })
}

/** 关停服务单元，`password` 需为 MD5 摘要 */
export function stopService(tag: string, path: string, password: string): Promise<ServiceServer> {
  return request<ServiceServer>('/service/stop', {
    method: 'POST',
    form: { tag, path, pwd: password },
    timeout: 60000
  })
}

/** 已接入的域列表 */
export function fetchDomains(): Promise<ListResponse<string>> {
  return request<ListResponse<string>>('/auth/domain')
}

/** 查询昨天的用户统计（响应里附带 DNU） */
export function fetchRecentStatistic(domain: string): Promise<StatisticResponse> {
  return request<StatisticResponse>('/statistic/recent', { query: { domain } })
}

/**
 * 查询指定日期的用户统计
 */
export function fetchDailyStatistic(
  domain: string,
  year: number,
  month: number,
  date: number
): Promise<StatisticResponse> {
  return request<StatisticResponse>('/statistic/daily', {
    query: { domain, year, month, date }
  })
}

/**
 * 查询昨天的 AI 单元概览。
 *
 * AI 单元是 ID 十进制位数大于等于 6 且小于 8 位的联系人。
 */
export function fetchUnitOverview(domain: string): Promise<UnitOverviewResponse> {
  return request<UnitOverviewResponse>('/statistic/units', { query: { domain } })
}

/** 轮询控制台自身日志，`start` 为上次返回的游标 */
export function queryConsoleLogs(start: number): Promise<LogResponse> {
  return request<LogResponse>('/log/console', { query: { start } })
}

/** 轮询某台服务器的日志，`start` 为上次返回的游标 */
export function queryServerLogs(name: string, start: number): Promise<LogResponse> {
  return request<LogResponse>('/log/server', { query: { name, start } })
}

/** 查询 JVM 报告序列 */
export function queryJVMReport(
  name: string,
  num: number,
  time: number = Date.now()
): Promise<JVMReportResponse> {
  return request<JVMReportResponse>('/server-report', {
    query: { report: JVM_REPORT_NAME, name, num, time }
  })
}

/** 查询性能报告，`time` 为 0 表示取最近一份 */
export function queryPerformanceReport(
  name: string,
  time = 0,
  detail = false
): Promise<PerformanceReportResponse> {
  return request<PerformanceReportResponse>('/server-report', {
    query: { report: PERFORMANCE_REPORT_NAME, name, time, detail }
  })
}

/**
 * 提取列表接口的数组内容。
 *
 * 后端在无数据时可能返回空对象，这里统一兜底为空数组。
 */
export function pickList<T>(response: ListResponse<T> | undefined | null): T[] {
  if (!response || !Array.isArray(response.list)) {
    return []
  }
  return response.list
}

/**
 * 判断错误是否代表「服务端暂无该数据」。
 *
 * `/servers/*` 之外的查询接口在无数据时返回 404。
 */
export function isEmptyDataError(error: unknown): boolean {
  return error instanceof ApiError && error.isNotFound
}

/** 查询 JVM 报告并只返回报告数组 */
export async function queryJVMReports(name: string, num: number, time?: number): Promise<JVMReport[]> {
  const response = await queryJVMReport(name, num, time)
  return Array.isArray(response.list) ? response.list : []
}
