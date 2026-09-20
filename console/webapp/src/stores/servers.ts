/**
 * 服务器清单。
 *
 * 调度机/服务单元列表被侧边栏徽标、服务器概览、调度机页、服务单元页共同消费，
 * 因此集中在本 store，避免各页面重复拉取。
 */

import { defineStore } from 'pinia'
import { fetchDispatchers, fetchServices, pickList } from '@/api/console'
import type { DispatcherServer, ServiceServer } from '@/api/types'

export const useServersStore = defineStore('servers', {
  state: () => ({
    dispatchers: [] as DispatcherServer[],
    services: [] as ServiceServer[],
    loading: false,
    error: '' as string
  }),

  getters: {
    dispatcherTotal: (state) => state.dispatchers.length,

    serviceTotal: (state) => state.services.length,

    dispatcherRunning: (state) => state.dispatchers.filter((item) => item.running).length,

    serviceRunning: (state) => state.services.filter((item) => item.running).length,

    /** 概览页的图表与日志视图按「调度机在前、服务单元在后」的顺序排布 */
    allServers: (state) => [
      ...state.dispatchers.map((item) => ({ kind: 'dispatcher' as const, server: item })),
      ...state.services.map((item) => ({ kind: 'service' as const, server: item }))
    ]
  },

  actions: {
    /** 拉取调度机列表 */
    async loadDispatchers(): Promise<void> {
      const response = await fetchDispatchers()
      this.dispatchers = pickList(response)
    },

    /** 拉取服务单元列表 */
    async loadServices(): Promise<void> {
      const response = await fetchServices()
      this.services = pickList(response)
    },

    /** 并行拉取两类服务器 */
    async loadAll(): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        const [dispatchers, services] = await Promise.all([
          fetchDispatchers(),
          fetchServices()
        ])
        this.dispatchers = pickList(dispatchers)
        this.services = pickList(services)
      } catch (error) {
        this.error = error instanceof Error ? error.message : '加载服务器列表失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    /** 按 `tag` + `deployPath` 定位调度机 */
    findDispatcher(tag: string, deployPath: string): DispatcherServer | undefined {
      return this.dispatchers.find(
        (item) => item.tag === tag && item.deployPath === deployPath
      )
    },

    /** 按 `name` 定位调度机 */
    findDispatcherByName(name: string): DispatcherServer | undefined {
      return this.dispatchers.find((item) => item.name === name)
    },

    /** 按 `tag` + `deployPath` 定位服务单元 */
    findService(tag: string, deployPath: string): ServiceServer | undefined {
      return this.services.find((item) => item.tag === tag && item.deployPath === deployPath)
    },

    /** 按 `name` 定位服务单元 */
    findServiceByName(name: string): ServiceServer | undefined {
      return this.services.find((item) => item.name === name)
    },

    /** 用最新的服务器数据覆盖列表中对应项 */
    patchDispatcher(server: DispatcherServer): void {
      const index = this.dispatchers.findIndex(
        (item) => item.tag === server.tag && item.deployPath === server.deployPath
      )
      if (index >= 0) {
        this.dispatchers[index] = server
      } else {
        this.dispatchers.push(server)
      }
    },

    /** 用最新的服务器数据覆盖列表中对应项 */
    patchService(server: ServiceServer): void {
      const index = this.services.findIndex(
        (item) => item.tag === server.tag && item.deployPath === server.deployPath
      )
      if (index >= 0) {
        this.services[index] = server
      } else {
        this.services.push(server)
      }
    }
  }
})
