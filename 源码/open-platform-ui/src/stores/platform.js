import { defineStore } from 'pinia'
import { getPlatformList } from '@/api/platform'
import { getColumnList } from '@/api/column'

export const usePlatformStore = defineStore('platform', {
  state: () => ({
    platforms: [],
    columns: [],
    currentPlatformId: localStorage.getItem('currentPlatformId') || null,
    currentPlatformName: localStorage.getItem('currentPlatformName') || '加载中...',
    currentColumnId: null
  }),

  getters: {
    currentPlatform: (state) => state.platforms.find(p => String(p.id) === String(state.currentPlatformId))
  },

  actions: {
    clearCatalog() {
      this.platforms = []
      this.columns = []
      this.currentPlatformId = null
      this.currentPlatformName = '暂无已授权平台'
      this.currentColumnId = null
      localStorage.removeItem('currentPlatformId')
      localStorage.removeItem('currentPlatformName')
    },

    async fetchPlatforms() {
      if (!localStorage.getItem('token')) {
        this.clearCatalog()
        return
      }
      try {
        const res = await getPlatformList()
        if (res.code === 200) {
          this.platforms = Array.isArray(res.data) ? res.data : []
          const current = this.platforms.find(p => String(p.id) === String(this.currentPlatformId))
          const selected = current || this.platforms[0]
          if (!selected) {
            this.clearCatalog()
            return
          }
          this.currentPlatformId = selected.id
          this.currentPlatformName = selected.name
          localStorage.setItem('currentPlatformId', selected.id)
          localStorage.setItem('currentPlatformName', selected.name)
          await this.fetchColumns(selected.id)
        }
      } catch (error) {
        console.error('获取平台列表失败:', error)
        this.clearCatalog()
      }
    },

    async fetchColumns(platformId) {
      if (!platformId || !localStorage.getItem('token')) {
        this.columns = []
        this.currentColumnId = null
        return
      }
      try {
        const res = await getColumnList(platformId)
        if (res.code === 200) {
          this.columns = Array.isArray(res.data) ? res.data : []
          // 默认选中逻辑：
          // 1. 如果当前已经有 currentColumnId，且它在获取到的 columns 列表中，则保持不变（用于状态还原）
          // 2. 否则，默认选中第一个栏目
          const exists = this.columns.some(c => String(c.id) === String(this.currentColumnId))
          if (!exists) {
            if (this.columns.length > 0) {
              this.currentColumnId = this.columns[0].id
            } else {
              this.currentColumnId = null
            }
          }
        }
      } catch (error) {
        console.error('获取栏目列表失败:', error)
        this.columns = []
        this.currentColumnId = null
      }
    },

    setCurrentPlatform(platform) {
      this.currentPlatformId = platform.id
      this.currentPlatformName = platform.name
      localStorage.setItem('currentPlatformId', platform.id)
      localStorage.setItem('currentPlatformName', platform.name)
      // 切换平台后刷新栏目
      return this.fetchColumns(platform.id)
    },

    setCurrentColumnId(columnId) {
      this.currentColumnId = columnId
    }
  }
})
