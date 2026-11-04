<template>
  <div class="dashboard">
    <t-row :gutter="[24, 24]" class="stats-row">
      <t-col :xs="12" :sm="4" :lg="4">
        <t-card :bordered="false" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total">
              <t-icon name="view-list" size="32px" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.publishedCount }}</div>
              <div class="stat-label">已发布内容</div>
            </div>
          </div>
        </t-card>
      </t-col>
      <t-col :xs="12" :sm="4" :lg="4">
        <t-card :bordered="false" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon approved">
              <t-icon name="user-checked" size="32px" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">已开通用户</div>
            </div>
          </div>
        </t-card>
      </t-col>
      <t-col :xs="12" :sm="4" :lg="4">
        <t-card :bordered="false" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon calls">
              <t-icon name="chart-line" size="32px" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayVisitCount }}</div>
              <div class="stat-label">今日访问次数</div>
            </div>
          </div>
        </t-card>
      </t-col>
    </t-row>

    <t-row :gutter="[24, 24]" class="charts-row">
      <t-col :xs="12" :lg="7">
        <t-card :bordered="false" title="访问趋势">
          <div ref="chartRef" class="chart-panel"></div>
        </t-card>
      </t-col>
      <t-col :xs="12" :lg="5">
        <t-card :bordered="false" title="访问分布">
          <div ref="pieChartRef" class="chart-panel"></div>
        </t-card>
      </t-col>
    </t-row>

    <t-row :gutter="[24, 24]" class="recent-row">
      <t-col :span="12">
        <t-card :bordered="false" :loading="loading">
          <template #header>
            <div class="card-header">
              <span>最新沙箱请求日志</span>
              <t-link theme="primary" hover="color" @click="router.push('/content-logs')">查看全部</t-link>
            </div>
          </template>
          <t-table row-key="id" :data="recentSandboxLogs" :columns="tableColumns" hover bordered>
            <template #user="{ row }">
              <span>{{ row.nickname || row.username || row.businessUserId || '未识别用户' }}</span>
            </template>
            <template #category="{ row }">
              <span>{{ row.interfaceName || row.categoryName || (row.categoryId ? `分类 ${row.categoryId}` : '-') }}</span>
            </template>
            <template #statusCode="{ row }">
              <t-tag :theme="row.statusCode === 200 ? 'success' : 'danger'" variant="light-outline">
                {{ row.statusCode || '-' }}
              </t-tag>
            </template>
            <template #createTime="{ row }">
              <span>{{ formatDateTime(row.createTime) }}</span>
            </template>
          </t-table>
        </t-card>
      </t-col>
    </t-row>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Icon as TIcon } from 'tdesign-icons-vue-next'
import { MessagePlugin } from 'tdesign-vue-next'
import { getDashboardStats } from '@/api/dashboard'
import { isAdminLoggingOut } from '@/utils/logoutState'

const router = useRouter()
const chartRef = ref(null)
const pieChartRef = ref(null)
const loading = ref(false)
const recentSandboxLogs = ref([])

let lineChart = null
let pieChart = null
let latestPieData = []

const stats = reactive({
  publishedCount: 0,
  userCount: 0,
  todayVisitCount: 0
})

const tableColumns = [
  { colKey: 'user', title: '用户', width: 140, ellipsis: true, cell: 'user' },
  { colKey: 'platformName', title: '平台', width: 150, ellipsis: true },
  { colKey: 'category', title: '分类/接口', minWidth: 180, ellipsis: true, cell: 'category' },
  { colKey: 'requestMethod', title: '方法', width: 80, align: 'center' },
  { colKey: 'statusCode', title: '状态码', width: 90, align: 'center', cell: 'statusCode' },
  { colKey: 'costTime', title: '耗时', width: 90, align: 'center', cell: (h, { row }) => `${row.costTime || 0}ms` },
  { colKey: 'createTime', title: '请求时间', width: 180, ellipsis: true }
]

const categoryColumnIndex = tableColumns.findIndex((column) => column.colKey === 'category')
if (categoryColumnIndex !== -1) {
  tableColumns.splice(
    categoryColumnIndex + 1,
    0,
    { colKey: 'interfaceName', title: '接口名称', minWidth: 160, ellipsis: true },
    { colKey: 'requestUrl', title: '请求URL', minWidth: 280, ellipsis: true }
  )
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const fetchStats = async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    if (res.code !== 200) {
      if (isAdminLoggingOut()) {
        return
      }
      MessagePlugin.error(res.message || '获取数据失败')
      return
    }

    const data = res.data || {}
    stats.publishedCount = data.publishedCount || 0
    stats.userCount = data.userCount || 0
    stats.todayVisitCount = data.todayVisitCount || 0
    recentSandboxLogs.value = (data.recentSandboxLogs || []).map((item) => ({
      ...item,
      createTime: formatDateTime(item.createTime)
    }))

    initLineChart(data.visitTrend || {})
    initPieChart(data.visitDistribution || [])
  } catch (error) {
    console.error('Fetch dashboard stats error:', error)
    if (error.__adminLoggingOut || isAdminLoggingOut()) {
      return
    }
    MessagePlugin.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const initLineChart = (data) => {
  if (!chartRef.value) {
    return
  }

  if (!lineChart) {
    lineChart = echarts.init(chartRef.value)
  }

  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['访问次数'] },
    grid: { left: 24, right: 24, top: 48, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.dates || []
    },
    yAxis: { type: 'value' },
    series: [{
      name: '访问次数',
      type: 'line',
      smooth: true,
      data: data.counts || [],
      areaStyle: { color: 'rgba(24, 144, 255, 0.1)' }
    }]
  })
}

const truncateLabel = (value, maxLength = 8) => {
  if (!value) {
    return ''
  }
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value
}

const buildPieOption = (data) => {
  const containerWidth = pieChartRef.value?.clientWidth || 0
  const compact = containerWidth > 0 && containerWidth < 420

  return {
    tooltip: {
      trigger: 'item',
      formatter: ({ name, value, percent }) => `${name}<br/>访问量：${value}<br/>占比：${percent || 0}%`
    },
    legend: compact
      ? {
          type: 'scroll',
          bottom: 0,
          left: 'center',
          orient: 'horizontal'
        }
      : {
          type: 'scroll',
          orient: 'vertical',
          left: 8,
          top: 'middle'
        },
    series: [{
      type: 'pie',
      avoidLabelOverlap: true,
      radius: compact ? ['32%', '54%'] : ['30%', '58%'],
      center: compact ? ['50%', '40%'] : ['63%', '50%'],
      data,
      label: compact
        ? { show: false }
        : {
            formatter: ({ name }) => truncateLabel(name, 8),
            width: 90,
            overflow: 'truncate'
          },
      labelLine: {
        show: !compact,
        length: compact ? 8 : 12,
        length2: compact ? 6 : 10
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.35)'
        }
      }
    }]
  }
}

const initPieChart = (data) => {
  if (!pieChartRef.value) {
    return
  }

  latestPieData = data || []
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

  pieChart.setOption(buildPieOption(latestPieData), true)
}

const handleResize = () => {
  lineChart?.resize()
  if (pieChart) {
    pieChart.resize()
    pieChart.setOption(buildPieOption(latestPieData), true)
  }
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  margin: 16px;
}

.stats-row,
.charts-row {
  margin-bottom: 24px;
}

.chart-panel {
  height: 350px;
}

.stat-card .stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card .stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-card .stat-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card .stat-icon.pending {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card .stat-icon.approved {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card .stat-icon.calls {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-card .stat-info {
  flex: 1;
}

.stat-card .stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin-bottom: 4px;
}

.stat-card .stat-label {
  font-size: 14px;
  color: var(--td-text-color-placeholder);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
