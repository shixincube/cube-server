/**
 * 主机硬件与性能接口封装。
 *
 * 对应后端 `HostInfoHandler`（`/host/static` 与 `/host/metrics`）。
 * 静态配置几乎不变，只需拉取一次；动态指标按秒级轮询，两者分开调用。
 */

import { request } from './client'
import type { HostMetricsResponse, HostStaticResponse } from './types'

/** 主机静态配置（CPU 规格、内存、磁盘、网卡、操作系统、运行时） */
export function fetchHostStatic(): Promise<HostStaticResponse> {
  return request<HostStaticResponse>('/host/static')
}

/** 主机动态性能（CPU 使用率、内存、网络 / 磁盘吞吐、分区占用） */
export function fetchHostMetrics(): Promise<HostMetricsResponse> {
  return request<HostMetricsResponse>('/host/metrics')
}
