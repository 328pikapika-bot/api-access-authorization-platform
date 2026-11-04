<template>
  <div class="content-logs">
    <t-card :bordered="false" class="filter-card">
      <t-form layout="inline" :data="formData" label-width="auto">
        <t-form-item label="用户名" name="username">
          <t-input v-model="formData.username" placeholder="请输入用户名或昵称" clearable @enter="onSearch" />
        </t-form-item>
        <t-form-item label="接口名称" name="interfaceName">
          <t-input v-model="formData.interfaceName" placeholder="请输入接口名称或分类名称" clearable @enter="onSearch" />
        </t-form-item>
        <t-form-item label="时间范围" name="dateRange">
          <t-date-range-picker
            v-model="formData.dateRange"
            :placeholder="['开始时间', '结束时间']"
            enable-time-picker
            allow-input
            clearable
            format="YYYY-MM-DD HH:mm:ss"
            value-type="YYYY-MM-DD HH:mm:ss"
            :need-confirm="false"
            :default-time="['00:00:00', '23:59:59']"
            :range-input-props="dateRangeInputProps"
            @change="syncDateRange"
            @input="syncDateRange"
            @blur="syncDateRange"
          />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" @click="onSearch">查询</t-button>
          <t-button variant="outline" style="margin-left: 12px" @click="onReset">重置</t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <t-card :bordered="false" style="margin-top: 16px">
      <template #title>
        <div class="card-header">
          <div>
            <div class="card-title">沙箱请求日志</div>
            <div class="card-subtitle">展示真实上游状态码，并支持后端详情接口重新拉取</div>
          </div>
        </div>
      </template>

      <t-table row-key="id" :data="logList" :columns="tableColumns" :loading="loading" hover bordered>
        <template #statusCode="{ row }">
          <t-tag :theme="row.statusCode === 200 ? 'success' : 'danger'" variant="light-outline">
            {{ row.statusCode }}
          </t-tag>
        </template>
        <template #operation="{ row }">
          <t-link theme="primary" @click="showDetail(row)">查看详情</t-link>
        </template>
      </t-table>

      <div class="pagination-wrap">
        <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-size-options="[10, 20, 50, 100]"
          @change="onPaginationChange"
        />
      </div>
    </t-card>

    <t-dialog v-model:visible="detailDialogVisible" header="请求详情" width="800px" :footer="false">
      <div class="log-detail">
        <t-descriptions :column="2" bordered title="基本信息">
          <t-descriptions-item label="日志ID">{{ currentLog?.id }}</t-descriptions-item>
          <t-descriptions-item label="请求时间">{{ currentLog?.createTime }}</t-descriptions-item>
          <t-descriptions-item label="用户ID">{{ currentLog?.businessUserId || '-' }}</t-descriptions-item>
          <t-descriptions-item label="用户名">{{ currentLog?.username || '-' }}</t-descriptions-item>
          <t-descriptions-item label="昵称">{{ currentLog?.nickname || '-' }}</t-descriptions-item>
          <t-descriptions-item label="平台">{{ currentLog?.platformName || '-' }}</t-descriptions-item>
          <t-descriptions-item label="分类名称">{{ currentLog?.categoryName || '-' }}</t-descriptions-item>
          <t-descriptions-item label="接口名称">{{ currentLog?.interfaceName || '-' }}</t-descriptions-item>
          <t-descriptions-item label="凭证">{{ currentLog?.appKey || '-' }}</t-descriptions-item>
          <t-descriptions-item label="HTTP 方法">{{ currentLog?.requestMethod || '-' }}</t-descriptions-item>
          <t-descriptions-item label="状态码">
            <t-tag :theme="currentLog?.statusCode === 200 ? 'success' : 'danger'">
              {{ currentLog?.statusCode }}
            </t-tag>
          </t-descriptions-item>
          <t-descriptions-item label="耗时">{{ currentLog?.costTime }}ms</t-descriptions-item>
        </t-descriptions>

        <div class="detail-section">
          <div class="section-title">请求 URL</div>
          <div class="url-text">{{ currentLog?.requestUrl || '-' }}</div>
        </div>

        <div v-if="currentLog?.requestHeaders" class="detail-section">
          <div class="section-title">请求头</div>
          <pre>{{ formatJson(currentLog?.requestHeaders) }}</pre>
        </div>

        <div v-if="currentLog?.requestParams" class="detail-section">
          <div class="section-title">请求参数</div>
          <pre>{{ formatJson(currentLog?.requestParams) }}</pre>
        </div>

        <div v-if="currentLog?.requestBody" class="detail-section">
          <div class="section-title">请求体</div>
          <pre>{{ formatJson(currentLog?.requestBody) }}</pre>
        </div>

        <div class="detail-section">
          <div class="section-title">响应内容</div>
          <pre>{{ formatJson(currentLog?.responseContent) }}</pre>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { getSandboxLogDetail, getSandboxLogPage } from '@/api/log'
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

const formData = reactive({
  username: '',
  interfaceName: '',
  dateRange: []
})
const latestDateRange = ref([])
const dateRangeInputProps = {
  inputProps: [
    {
      onChange: (value) => syncDateRangeInput(0, value),
      onEnter: () => onSearch()
    },
    {
      onChange: (value) => syncDateRangeInput(1, value),
      onEnter: () => onSearch()
    }
  ]
}

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const tableColumns = [
  { colKey: 'id', title: '日志ID', width: 80, align: 'center' },
  { colKey: 'businessUserId', title: '用户ID', width: 140, align: 'center' },
  { colKey: 'username', title: '用户名', width: 120, ellipsis: true },
  { colKey: 'nickname', title: '昵称', width: 120, ellipsis: true },
  { colKey: 'platformName', title: '平台', width: 150, ellipsis: true },
  { colKey: 'categoryName', title: '分类名称', width: 150, ellipsis: true },
  { colKey: 'interfaceName', title: '接口名称', width: 150, ellipsis: true },
  { colKey: 'requestUrl', title: '请求URL', minWidth: 200, ellipsis: true },
  { colKey: 'requestMethod', title: '方法', width: 80, align: 'center' },
  { colKey: 'statusCode', title: '状态码', width: 90, align: 'center', cell: 'statusCode' },
  { colKey: 'costTime', title: '耗时', width: 90, align: 'center', cell: (h, { row }) => `${row.costTime}ms` },
  { colKey: 'createTime', title: '请求时间', width: 180, align: 'center' },
  { colKey: 'operation', title: '操作', width: 100, fixed: 'right', align: 'center', cell: 'operation' }
]

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const businessUserColumn = tableColumns.find((column) => column.colKey === 'businessUserId')
if (businessUserColumn) {
  businessUserColumn.width = 180
}

const usernameColumn = tableColumns.find((column) => column.colKey === 'username')
if (usernameColumn) {
  usernameColumn.width = 96
}

function syncDateRange(value) {
  latestDateRange.value = normalizeTimeRangeValue(value)
}

function syncDateRangeInput(index, value) {
  const nextRange = mergeTimeRangeInputValue(formData.dateRange, latestDateRange.value, index, value)
  formData.dateRange = nextRange
  latestDateRange.value = nextRange
}

async function fetchLogs() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (formData.username?.trim()) params.username = formData.username.trim()
    if (formData.interfaceName?.trim()) params.interfaceName = formData.interfaceName.trim()
    Object.assign(params, buildTimeRangeParams(pickUsableTimeRange(formData.dateRange, latestDateRange.value)))
    const res = await getSandboxLogPage(params)
    if (res.code === 200) {
      logList.value = (res.data.records || []).map((item) => ({
        ...item,
        createTime: formatDateTime(item.createTime)
      }))
      pagination.value.total = res.data.total || 0
    } else {
      MessagePlugin.error(res.message || '获取日志失败')
    }
  } catch (error) {
    console.error(error)
    if (isAdminSessionRequestError(error)) return
    MessagePlugin.error('获取日志失败')
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  pagination.value.page = 1
  await nextTick()
  fetchLogs()
}

function onReset() {
  formData.username = ''
  formData.interfaceName = ''
  formData.dateRange = []
  latestDateRange.value = []
  onSearch()
}

function onPaginationChange(pageInfo) {
  pagination.value.page = pageInfo.current
  pagination.value.size = pageInfo.pageSize
  fetchLogs()
}

async function showDetail(row) {
  try {
    const res = await getSandboxLogDetail(row.id)
    if (res.code === 200) {
      currentLog.value = {
        ...res.data,
        createTime: formatDateTime(res.data?.createTime)
      }
      detailDialogVisible.value = true
    } else {
      MessagePlugin.error(res.message || '获取详情失败')
    }
  } catch (error) {
    console.error(error)
    if (isAdminSessionRequestError(error)) return
    MessagePlugin.error('获取详情失败')
  }
}

function formatJson(json) {
  if (!json) return ''
  try {
    if (typeof json === 'string') {
      return JSON.stringify(JSON.parse(json), null, 2)
    }
    return JSON.stringify(json, null, 2)
  } catch (error) {
    return json
  }
}

onMounted(fetchLogs)
</script>

<style scoped>
.content-logs { margin: 16px; }
.content-logs :deep(.t-form__label) { padding-right: 10px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 16px; font-weight: 600; color: var(--td-text-color-primary); }
.card-subtitle { margin-top: 4px; font-size: 13px; color: var(--td-text-color-placeholder); }
.filter-card { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.detail-section { margin-top: 16px; }
.section-title { font-weight: 600; margin-bottom: 8px; color: var(--td-text-color-primary); border-left: 3px solid var(--td-brand-color); padding-left: 8px; }
.url-text { background: var(--td-bg-color-page); padding: 8px 12px; border-radius: 4px; word-break: break-all; font-family: monospace; }
pre { background: var(--td-bg-color-page); padding: 12px; border-radius: 4px; overflow-x: auto; margin: 0; font-family: 'Courier New', monospace; font-size: 13px; color: var(--td-text-color-primary); white-space: pre-wrap; word-break: break-all; }
.log-detail { max-height: 600px; overflow-y: auto; padding-right: 8px; }
</style>
