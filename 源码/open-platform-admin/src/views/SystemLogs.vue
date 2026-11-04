<template>
  <div class="system-logs">
    <t-card :bordered="false" class="filter-card">
      <t-form :data="filters" layout="inline" label-width="auto">
        <t-form-item label="用户名：" name="username">
          <t-input
            v-model="filters.username"
            placeholder="请输入用户名或昵称"
            clearable
            @enter="handleSearch"
          />
        </t-form-item>
        <t-form-item label="模块名称：" name="moduleName">
          <t-input
            v-model="filters.moduleName"
            placeholder="请输入模块名称"
            clearable
            @enter="handleSearch"
          />
        </t-form-item>
        <t-form-item label="操作类型：" name="operationType">
          <t-select v-model="filters.operationType" placeholder="全部类型" clearable style="width: 160px">
            <t-option
              v-for="item in operationTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </t-select>
        </t-form-item>
        <t-form-item label="操作状态：" name="operationStatus">
          <t-select v-model="filters.operationStatus" placeholder="全部状态" clearable style="width: 160px">
            <t-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </t-select>
        </t-form-item>
        <t-form-item label="客户端IP：" name="clientIp">
          <t-input
            v-model="filters.clientIp"
            placeholder="请输入IP"
            clearable
            @enter="handleSearch"
          />
        </t-form-item>
        <t-form-item label="操作时间：" name="timeRange">
          <t-date-range-picker
            v-model="filters.timeRange"
            enable-time-picker
            allow-input
            clearable
            :placeholder="['开始时间', '结束时间']"
            format="YYYY-MM-DD HH:mm:ss"
            value-type="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
            :need-confirm="false"
            :default-time="['00:00:00', '23:59:59']"
            :range-input-props="timeRangeInputProps"
            @change="syncTimeRange"
            @input="syncTimeRange"
            @blur="syncTimeRange"
          />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" @click="handleSearch">查询</t-button>
          <t-button variant="outline" @click="handleReset" style="margin-left: 12px">重置</t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <t-card :bordered="false" style="margin-top: 16px">
      <template #title>
        <div class="card-header">
          <div>
            <div class="card-title">系统日志</div>
            <div class="card-subtitle">展示审计日志，支持按时间、类型、模块和状态筛选</div>
          </div>
        </div>
      </template>

      <t-table
        row-key="id"
        :data="logList"
        :columns="tableColumns"
        :loading="loading"
        hover
        bordered
      >
        <template #operationType="{ row }">
          <t-tag :theme="getOperationTypeTag(row.operationType)" variant="light-outline">
            {{ getOperationTypeLabel(row.operationType) }}
          </t-tag>
        </template>
        <template #operationStatus="{ row }">
          <t-tag :theme="row.operationStatus === 1 ? 'success' : 'danger'" variant="light">
            {{ row.operationStatus === 1 ? '成功' : '失败' }}
          </t-tag>
        </template>
        <template #requestMethod="{ row }">
          <t-tag :theme="getRequestMethodTag(row.requestMethod)" variant="outline">
            {{ row.requestMethod || '-' }}
          </t-tag>
        </template>
        <template #operation="{ row }">
          <t-link theme="primary" @click="showDetail(row)">详情</t-link>
        </template>
      </t-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <t-pagination
          v-model:current="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-size-options="[10, 20, 50, 100]"
          @change="onPaginationChange"
        />
      </div>
    </t-card>

    <t-dialog v-model:visible="detailDialogVisible" header="日志详情" width="960px" :footer="false">
      <t-descriptions :column="2" bordered>
        <t-descriptions-item label="日志ID">{{ currentLog?.id || '-' }}</t-descriptions-item>
        <t-descriptions-item label="操作时间">{{ currentLog?.createTime || '-' }}</t-descriptions-item>
        <t-descriptions-item label="用户昵称">{{ currentLog?.nickname || '-' }}</t-descriptions-item>
        <t-descriptions-item label="用户名">{{ currentLog?.username || '-' }}</t-descriptions-item>
        <t-descriptions-item label="用户ID">{{ currentLog?.userId || '-' }}</t-descriptions-item>
        <t-descriptions-item label="模块名称">{{ currentLog?.moduleName || '-' }}</t-descriptions-item>
        <t-descriptions-item label="业务类型">{{ currentLog?.businessType || '-' }}</t-descriptions-item>
        <t-descriptions-item label="操作类型">
          <t-tag :theme="getOperationTypeTag(currentLog?.operationType)">
            {{ getOperationTypeLabel(currentLog?.operationType) }}
          </t-tag>
        </t-descriptions-item>
        <t-descriptions-item label="操作状态">
          <t-tag :theme="currentLog?.operationStatus === 1 ? 'success' : 'danger'">
            {{ currentLog?.operationStatus === 1 ? '成功' : '失败' }}
          </t-tag>
        </t-descriptions-item>
        <t-descriptions-item label="请求方式">{{ currentLog?.requestMethod || '-' }}</t-descriptions-item>
        <t-descriptions-item label="客户端IP">{{ currentLog?.clientIp || '-' }}</t-descriptions-item>
        <t-descriptions-item label="请求地址" :span="2">{{ currentLog?.requestUrl || '-' }}</t-descriptions-item>
        <t-descriptions-item label="耗时" :span="2">{{ formatCostTime(currentLog?.costTime, 'ms') }}</t-descriptions-item>
        <t-descriptions-item label="错误信息" :span="2">{{ currentLog?.errorMessage || '-' }}</t-descriptions-item>
      </t-descriptions>

      <div class="detail-section">
        <div class="section-title">请求参数</div>
        <pre>{{ formatJson(currentLog?.requestParams) }}</pre>
      </div>
      <div class="detail-section">
        <div class="section-title">响应结果</div>
        <pre>{{ formatJson(currentLog?.responseResult) }}</pre>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { getAuditLogDetail, getAuditLogPage } from '@/api/log'
import { isAdminSessionRequestError } from '@/utils/logoutState'
import {
  buildTimeRangeParams,
  mergeTimeRangeInputValue,
  normalizeTimeRangeValue,
  pickUsableTimeRange
} from '@/utils/timeRange'

const loading = ref(false)
const detailDialogVisible = ref(false)
const currentLog = ref(null)
const logList = ref([])

const filters = reactive({
  username: '',
  moduleName: '',
  operationType: '',
  operationStatus: '',
  clientIp: '',
  timeRange: []
})
const latestTimeRange = ref([])
const timeRangeInputProps = {
  inputProps: [
    {
      onChange: (value) => syncTimeRangeInput(0, value),
      onEnter: () => handleSearch()
    },
    {
      onChange: (value) => syncTimeRangeInput(1, value),
      onEnter: () => handleSearch()
    }
  ]
}

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const tableColumns = [
  { colKey: 'createTime', title: '操作时间', width: 180 },
  { colKey: 'nickname', title: '用户昵称', ellipsis: true },
  { colKey: 'username', title: '用户名', ellipsis: true },
  { colKey: 'moduleName', title: '模块名称', ellipsis: true },
  { colKey: 'businessType', title: '业务类型', ellipsis: true },
  { colKey: 'operationType', title: '操作类型', width: 100, cell: 'operationType' },
  { colKey: 'operationStatus', title: '状态', width: 80, cell: 'operationStatus' },
  { colKey: 'requestMethod', title: '请求方式', width: 100, cell: 'requestMethod' },
  { colKey: 'clientIp', title: '客户端IP', width: 140 },
  { colKey: 'costTime', title: '耗时(ms)', width: 100, cell: (h, { row }) => formatCostTime(row.costTime) },
  { colKey: 'operation', title: '操作', width: 80, fixed: 'right', cell: 'operation' }
]

const operationTypeOptions = [
  { label: '查询', value: 1 },
  { label: '新增', value: 2 },
  { label: '修改', value: 3 },
  { label: '删除', value: 4 },
  { label: '导出', value: 5 },
  { label: '登录', value: 6 },
  { label: '登出', value: 7 },
  { label: '其他', value: 8 }
]

const statusOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

const syncTimeRange = (value) => {
  latestTimeRange.value = normalizeTimeRangeValue(value)
}

const syncTimeRangeInput = (index, value) => {
  const nextRange = mergeTimeRangeInputValue(filters.timeRange, latestTimeRange.value, index, value)
  filters.timeRange = nextRange
  latestTimeRange.value = nextRange
}

const buildParams = () => {
  const params = {
    current: pagination.current,
    size: pagination.size
  }

  if (filters.username?.trim()) params.username = filters.username.trim()
  if (filters.moduleName?.trim()) params.moduleName = filters.moduleName.trim()
  if (filters.operationType !== '' && filters.operationType !== null) params.operationType = filters.operationType
  if (filters.operationStatus !== '' && filters.operationStatus !== null) params.operationStatus = filters.operationStatus
  if (filters.clientIp?.trim()) params.clientIp = filters.clientIp.trim()
  Object.assign(params, buildTimeRangeParams(pickUsableTimeRange(filters.timeRange, latestTimeRange.value)))

  return params
}

const fetchLogList = async () => {
  loading.value = true
  try {
    const res = await getAuditLogPage(buildParams())
    if (res.code !== 200) {
      MessagePlugin.error(res.message || '获取系统日志失败')
      return
    }

    const pageData = res.data || {}
    logList.value = Array.isArray(pageData.records) ? pageData.records.map(normalizeLogRecord) : []
    pagination.total = Number(pageData.total || 0)
  } catch (error) {
    console.error('获取系统日志失败:', error)
    if (isAdminSessionRequestError(error)) return
    MessagePlugin.error(error.message || '获取系统日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pagination.current = 1
  await nextTick()
  fetchLogList()
}

const handleReset = () => {
  Object.assign(filters, {
    username: '',
    moduleName: '',
    operationType: '',
    operationStatus: '',
    clientIp: '',
    timeRange: []
  })
  latestTimeRange.value = []
  pagination.current = 1
  fetchLogList()
}

const onPaginationChange = (pageInfo) => {
  pagination.current = pageInfo.current
  pagination.size = pageInfo.pageSize
  fetchLogList()
}

const normalizeEmptyValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return value
}

const normalizeModuleName = (value) => {
  const moduleName = normalizeEmptyValue(value)
  const menuModuleMap = {
    审计日志: '系统日志',
    平台管理: '分类与内容',
    栏目管理: '分类与内容'
  }
  return menuModuleMap[moduleName] || moduleName
}

const normalizeLogRecord = (record) => ({
  ...record,
  createTime: normalizeEmptyValue(record?.createTime),
  nickname: normalizeEmptyValue(record?.nickname),
  username: normalizeEmptyValue(record?.username),
  userId: normalizeEmptyValue(record?.userId),
  moduleName: normalizeModuleName(record?.moduleName),
  businessType: normalizeEmptyValue(record?.businessType),
  requestMethod: normalizeEmptyValue(record?.requestMethod),
  clientIp: normalizeEmptyValue(record?.clientIp),
  requestUrl: normalizeEmptyValue(record?.requestUrl),
  errorMessage: normalizeEmptyValue(record?.errorMessage)
})

const showDetail = async (row) => {
  detailDialogVisible.value = true
  currentLog.value = normalizeLogRecord(row)
  try {
    const res = await getAuditLogDetail(row.id)
    if (res.code === 200 && res.data) {
      currentLog.value = normalizeLogRecord(res.data)
      return
    }
    MessagePlugin.error(res.message || '获取日志详情失败')
  } catch (error) {
    console.error('获取日志详情失败:', error)
    if (isAdminSessionRequestError(error)) return
    MessagePlugin.error(error.message || '获取日志详情失败')
  }
}

const getOperationTypeLabel = (value) => {
  const matched = operationTypeOptions.find(item => item.value === value)
  return matched?.label || '未知'
}

const getOperationTypeTag = (value) => {
  const tagMap = {
    1: 'default',
    2: 'success',
    3: 'warning',
    4: 'danger',
    5: 'primary',
    6: 'success',
    7: 'warning',
    8: 'default'
  }
  return tagMap[value] || 'default'
}

const getRequestMethodTag = (method) => {
  const methodMap = {
    GET: 'success',
    POST: 'warning',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return methodMap[method] || 'default'
}

const formatCostTime = (value, suffix = '') => {
  if (value === null || value === undefined || value === '') return '-'
  return `${value}${suffix}`
}

const formatJson = (value) => {
  if (!value || value === '-') return '-'
  try {
    if (typeof value === 'string') return JSON.stringify(JSON.parse(value), null, 2)
    return JSON.stringify(value, null, 2)
  } catch {
    return value
  }
}

onMounted(() => {
  fetchLogList()
})
</script>

<style scoped>
.system-logs{
  margin: 16px;
}
.system-logs :deep(.t-form__label) {
  padding-right: 5px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.card-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--td-text-color-placeholder);
}

.detail-section {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
}

pre {
  background: var(--td-bg-color-page);
  border-radius: 6px;
  padding: 12px;
  margin: 0;
  max-height: 260px;
  overflow: auto;
  line-height: 1.6;
  font-size: 12px;
  font-family: 'Courier New', monospace;
  color: var(--td-text-color-primary);
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
