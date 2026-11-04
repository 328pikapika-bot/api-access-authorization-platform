<template>
  <div class="api-permission-assign">
    <t-card :bordered="false" class="filter-card">
      <t-form :data="filter" layout="inline" label-width="auto">
        <t-form-item label="平台" name="platformId">
          <t-select
            v-model="filter.platformId"
            clearable
            filterable
            placeholder="选择平台"
            :options="filterPlatformOptions"
            style="width: 180px"
            @change="handleFilterPlatformChange"
          />
        </t-form-item>
        <t-form-item label="栏目" name="columnId">
          <t-select
            v-model="filter.columnId"
            clearable
            filterable
            placeholder="选择栏目"
            :disabled="!filter.platformId"
            :options="filterColumnOptions"
            style="width: 180px"
            @change="handleFilterSubmit"
          />
        </t-form-item>
        <t-form-item label="状态" name="status">
          <t-select
            v-model="filter.status"
            clearable
            placeholder="全部状态"
            :options="statusOptions"
            style="width: 140px"
            @change="handleFilterSubmit"
          />
        </t-form-item>
        <t-form-item label="关键字" name="keyword">
          <t-input
            v-model="filter.keyword"
            clearable
            placeholder="用户ID/用户名/昵称/授权范围"
            style="width: 280px"
            @enter="handleFilterSubmit"
          />
        </t-form-item>
        <t-form-item label="时间范围" name="timeRange">
          <t-date-range-picker
            v-model="filter.timeRange"
            enable-time-picker
            allow-input
            clearable
            :placeholder="['开始时间', '结束时间']"
            format="YYYY-MM-DD HH:mm:ss"
            value-type="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
            :need-confirm="false"
            :default-time="['00:00:00', '23:59:59']"
            @change="syncFilterTimeRange"
            @input="syncFilterTimeRange"
            @blur="syncFilterTimeRange"
          />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" @click="handleFilterSubmit">查询</t-button>
          <t-button variant="outline" style="margin-left: 12px" @click="handleResetFilter">重置</t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <t-card :bordered="false">
      <template #title>
        <div class="card-header">
          <div>
            <div class="card-title">接口权限分配</div>
            <div class="card-subtitle">每个用户保留一条配置，可一次分配多个平台、栏目和一级分类</div>
          </div>
          <t-button theme="primary" @click="openAssignDialog">新增权限分配</t-button>
        </div>
      </template>

      <t-table
        row-key="id"
        hover
        bordered
        :data="applicationList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @page-change="handlePageChange"
      >
        <template #userCell="{ row }">
          <div class="user-block">
            <div class="user-name">{{ row.nickname || '-' }}</div>
            <div class="user-sub">{{ row.userName || '-' }}</div>
            <div class="user-sub">用户ID：{{ row.businessUserId || '-' }}</div>
          </div>
        </template>

        <template #orgCell="{ row }">
          <div class="user-block">
            <div class="user-name">{{ row.unitName || '-' }}</div>
            <div class="user-sub">{{ row.deptName || '-' }}</div>
          </div>
        </template>

        <template #scopeCell="{ row }">
          <div class="scope-list">
            <div v-for="item in getScopeItems(row)" :key="`${row.id}-${item.categoryId}`" class="scope-item">
              <span class="scope-path">{{ item.path }}</span>
              <t-tag :theme="item.saved ? 'success' : 'default'" variant="light" size="small">
                {{ item.saved ? '已分配凭证' : '未分配凭证' }}
              </t-tag>
            </div>
          </div>
        </template>

        <template #statusCell="{ row }">
          <t-tag :theme="getStatusTheme(row.status)" variant="light-outline">
            {{ getStatusText(row.status) }}
          </t-tag>
        </template>

        <template #validityCell="{ row }">
          <div class="validity-block">
            <t-tag :theme="row.validAssignment === false ? 'danger' : 'success'" variant="light-outline">
              {{ row.validAssignment === false ? '已失效' : '有效' }}
            </t-tag>
            <div v-if="row.validAssignment === false" class="invalid-reason">
              {{ row.invalidReason || '关联对象不可用' }}
            </div>
          </div>
        </template>

        <template #timeCell="{ row }">
          <span class="time-text">{{ formatTime(row.applyTime) }}</span>
        </template>

        <template #operationCell="{ row }">
          <div class="operation-group">
            <t-button theme="primary" variant="text" :disabled="!canOperateRecord(row)" @click="openEditDialog(row)">
              管理接口权限
            </t-button>
            <t-button theme="primary" variant="text" :disabled="!canOperateRecord(row)" @click="openCredentialList(row)">
              凭证管理
            </t-button>
            <t-button theme="danger" variant="text" @click="handleDelete(row)">删除</t-button>
          </div>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="permissionDialog.visible"
      :header="permissionDialog.mode === 'assign' ? '新增权限分配' : '管理接口权限'"
      width="860px"
      :confirm-btn="{ content: permissionDialog.mode === 'assign' ? '新增并生效' : '保存权限', loading: permissionDialog.loading }"
      :on-confirm="handlePermissionConfirm"
      :on-close="handlePermissionClose"
    >
      <t-form label-width="108px">
        <t-form-item v-if="permissionDialog.mode === 'assign'" label="系统用户" required>
          <t-select
            v-model="permissionDialog.form.userId"
            clearable
            filterable
            placeholder="请选择系统用户"
            :options="userOptions"
            @change="handleDialogUserChange"
          />
        </t-form-item>

        <template v-if="dialogUser">
          <t-row :gutter="[16, 16]" class="dialog-user-grid">
            <t-col :span="6">
              <div class="readonly-field">
                <div class="readonly-label">用户ID</div>
                <div class="readonly-value">{{ dialogUser.userId || '-' }}</div>
              </div>
            </t-col>
            <t-col :span="6">
              <div class="readonly-field">
                <div class="readonly-label">用户名</div>
                <div class="readonly-value">{{ dialogUser.userName || '-' }}</div>
              </div>
            </t-col>
            <t-col :span="6">
              <div class="readonly-field">
                <div class="readonly-label">昵称</div>
                <div class="readonly-value">{{ dialogUser.nickname || '-' }}</div>
              </div>
            </t-col>
            <t-col :span="6">
              <div class="readonly-field">
                <div class="readonly-label">单位</div>
                <div class="readonly-value">{{ dialogUser.unitName || '-' }}</div>
              </div>
            </t-col>
          </t-row>
        </template>

        <t-form-item label="单位">
          <t-input v-model="permissionDialog.form.unitName" placeholder="默认带出用户单位，可按需调整" />
        </t-form-item>
        <t-form-item label="部门">
          <t-input v-model="permissionDialog.form.deptName" placeholder="选填" />
        </t-form-item>
        <t-form-item label="授权范围" required>
          <div class="scope-tree-field">
            <div v-if="permissionDialog.scopeLoading" class="tree-placeholder">正在加载授权范围...</div>
            <div v-else-if="permissionDialog.scopeTree.length === 0" class="tree-placeholder">
              暂无可分配的已发布平台、栏目或一级分类
            </div>
            <t-tree
              v-else
              v-model="permissionDialog.form.scopeKeys"
              checkable
              check-strictly
              expand-all
              hover
              :keys="treeKeys"
              :data="permissionDialog.scopeTree"
              class="permission-tree"
              @change="handleScopeTreeChange"
            />
            <div class="tree-help">
              仅一级分类会作为接口权限保存；选中平台或栏目会自动选中其下全部一级分类。
            </div>
          </div>
        </t-form-item>
        <t-form-item label="备注">
          <t-textarea
            v-model="permissionDialog.form.reason"
            :maxlength="200"
            show-limit-number
            :autosize="{ minRows: 3, maxRows: 5 }"
            placeholder="选填"
          />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog v-model:visible="credentialListDialog.visible" header="凭证管理" width="760px" :footer="false">
      <div v-if="credentialListDialog.row" class="credential-list-dialog">
        <div class="credential-header">
          <div>用户：{{ credentialListDialog.row.nickname || '-' }} / {{ credentialListDialog.row.userName || '-' }}</div>
          <div>授权范围：{{ credentialListDialog.items.length }} 个一级分类</div>
        </div>
        <div class="credential-list">
          <div v-for="item in credentialListDialog.items" :key="item.categoryId" class="credential-list-item">
            <div class="credential-item-main">
              <span class="scope-path">{{ item.path }}</span>
              <t-tag :theme="item.saved ? 'success' : 'default'" variant="light" size="small">
                {{ item.saved ? '已分配' : '未分配' }}
              </t-tag>
            </div>
            <div class="credential-actions">
              <t-button theme="primary" variant="outline" size="small" @click="openCredentialForm(item)">
                接口凭证分配
              </t-button>
              <t-button theme="default" variant="outline" size="small" @click="openAppCredentialDialog(credentialListDialog.row)">
                查看凭证
              </t-button>
            </div>
          </div>
        </div>
      </div>
    </t-dialog>

    <t-dialog
      v-model:visible="credentialFormDialog.visible"
      :header="`接口凭证分配 - ${credentialFormDialog.categoryName || ''}`"
      width="520px"
      :confirm-btn="{ content: '保存', loading: credentialFormDialog.loading }"
      :on-confirm="handleCredentialSave"
    >
      <t-form label-align="top">
        <t-form-item label="Open ID">
          <t-input v-model="credentialFormDialog.form.openId" clearable />
        </t-form-item>
        <t-form-item label="Client ID">
          <t-input v-model="credentialFormDialog.form.clientId" clearable />
        </t-form-item>
        <t-form-item label="Token">
          <t-input v-model="credentialFormDialog.form.token" clearable />
        </t-form-item>
        <t-form-item label="Secret Key">
          <t-input v-model="credentialFormDialog.form.secretKey" type="password" clearable />
        </t-form-item>
        <t-form-item label="说明">
          <t-textarea v-model="credentialFormDialog.form.description" :autosize="{ minRows: 3, maxRows: 5 }" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog v-model:visible="appCredentialDialog.visible" header="应用凭证" width="520px" :footer="false">
      <t-form v-if="appCredentialDialog.row" label-align="top">
        <t-form-item label="AppKey">
          <t-input :value="appCredentialDialog.row.appKey" readonly>
            <template #suffix>
              <t-button theme="primary" variant="text" @click="copyWithFeedback(appCredentialDialog.row.appKey)">
                复制
              </t-button>
            </template>
          </t-input>
        </t-form-item>
        <t-form-item label="AppSecret">
          <t-input :value="appCredentialDialog.row.appSecret" type="password" readonly>
            <template #suffix>
              <t-button theme="primary" variant="text" @click="copyWithFeedback(appCredentialDialog.row.appSecret)">
                复制
              </t-button>
            </template>
          </t-input>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import {
  assignApplication,
  deleteApplication,
  getApplicationList,
  updateApplication
} from '@/api/application'
import { saveApiCredentials, getApiCredentials } from '@/api/apiCredential'
import { getCategoryTree } from '@/api/content-category'
import { listColumns } from '@/api/platform-column'
import { listPlatforms } from '@/api/platform'
import { getUserPage } from '@/api/user'
import { copyWithFeedback } from '@/utils/clipboard'
import { isAdminSessionRequestError } from '@/utils/logoutState'
import {
  buildTimeRangeParams,
  normalizeTimeRangeValue,
  pickUsableTimeRange
} from '@/utils/timeRange'

const loading = ref(false)
const applicationList = ref([])
const filterPlatforms = ref([])
const filterColumns = ref([])
const userList = ref([])
const credentialMap = reactive(new Map())
const scopeNodeMap = reactive(new Map())

const filter = reactive({
  platformId: null,
  columnId: null,
  status: null,
  keyword: '',
  timeRange: []
})
const latestFilterTimeRange = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const permissionDialog = reactive({
  visible: false,
  loading: false,
  scopeLoading: false,
  mode: 'assign',
  recordId: null,
  row: null,
  scopeTree: [],
  form: createPermissionForm()
})

const credentialListDialog = reactive({
  visible: false,
  row: null,
  items: []
})

const credentialFormDialog = reactive({
  visible: false,
  loading: false,
  applicationId: null,
  categoryId: null,
  categoryName: '',
  form: createCredentialForm()
})

const appCredentialDialog = reactive({
  visible: false,
  row: null
})

const treeKeys = {
  value: 'value',
  label: 'label',
  children: 'children'
}

const statusOptions = [
  { label: '已生效', value: 1 },
  { label: '未生效', value: 0 },
  { label: '已停用', value: 2 }
]

const columns = [
  { colKey: 'user', title: '用户信息', minWidth: 220, cell: 'userCell' },
  { colKey: 'organization', title: '单位/部门', minWidth: 180, cell: 'orgCell' },
  { colKey: 'scope', title: '平台/栏目/一级分类', minWidth: 360, cell: 'scopeCell' },
  { colKey: 'status', title: '分配状态', width: 120, cell: 'statusCell' },
  { colKey: 'validity', title: '分配有效性', width: 180, cell: 'validityCell' },
  { colKey: 'applyTime', title: '时间', width: 170, cell: 'timeCell' },
  { colKey: 'operation', title: '操作', width: 260, fixed: 'right', cell: 'operationCell' }
]

const filterPlatformOptions = computed(() =>
  filterPlatforms.value.map(item => ({ label: item.name, value: item.id }))
)

const filterColumnOptions = computed(() =>
  filterColumns.value.map(item => ({ label: item.name, value: item.id }))
)

const userOptions = computed(() =>
  userList.value.map(item => ({
    label: [item.userId, item.userName, item.nickname].filter(Boolean).join(' / '),
    value: item.id
  }))
)

const dialogUser = computed(() => {
  if (permissionDialog.mode === 'edit') {
    const matchedUser = userList.value.find(item => item.id === permissionDialog.row?.userId)
    return matchedUser || permissionDialog.row
  }
  return userList.value.find(item => item.id === permissionDialog.form.userId) || null
})

function createPermissionForm() {
  return {
    userId: null,
    unitName: '',
    deptName: '',
    scopeKeys: [],
    reason: ''
  }
}

function createCredentialForm() {
  return {
    openId: '',
    clientId: '',
    token: '',
    secretKey: '',
    description: ''
  }
}

function parseJsonArray(value) {
  if (!value) return []
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

function parseCategoryIds(categoryIds) {
  return parseJsonArray(categoryIds).map(item => Number(item)).filter(Boolean)
}

function getScopeItems(row) {
  const savedSet = credentialMap.get(row.id) || new Set()
  if (Array.isArray(row.scopeConfigs) && row.scopeConfigs.length) {
    return row.scopeConfigs.map(item => ({
      platformId: item.platformId,
      columnId: item.columnId,
      categoryId: item.categoryId,
      categoryName: item.categoryName,
      path: item.path || [item.platformName, item.columnName, item.categoryName].filter(Boolean).join('/'),
      saved: savedSet.has(Number(item.categoryId))
    }))
  }
  const ids = parseCategoryIds(row.categoryIds)
  const names = (row.categoryNames || '').split(',').map(item => item.trim()).filter(Boolean)
  return ids.map((id, index) => ({
    platformId: row.platformId,
    columnId: row.columnId,
    categoryId: id,
    categoryName: names[index] || `分类 ${id}`,
    path: [row.platformName, row.columnName, names[index] || `分类 ${id}`].filter(Boolean).join('/'),
    saved: savedSet.has(id)
  }))
}

function getStatusTheme(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'default'
}

function getStatusText(status) {
  return { 0: '未生效', 1: '已生效', 2: '已停用' }[status] || '未知'
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function canOperateRecord(row) {
  return row.status === 1 && row.validAssignment !== false
}

async function loadFilterPlatforms() {
  const res = await listPlatforms()
  if (res.code === 200) {
    filterPlatforms.value = res.data || []
  }
}

async function loadFilterColumns(platformId) {
  if (!platformId) {
    filterColumns.value = []
    return
  }
  const res = await listColumns({ platformId })
  filterColumns.value = res.code === 200 ? (res.data || []) : []
}

async function ensureUserList() {
  if (userList.value.length > 0) return
  const res = await getUserPage({ current: 1, size: 500 })
  if (res.code === 200) {
    userList.value = (res.data.records || []).filter(item => item.status !== 0)
  }
}

async function loadCredentialMap(records) {
  credentialMap.clear()
  await Promise.all((records || []).map(async item => {
    try {
      const res = await getApiCredentials(item.id)
      const savedIds = res.code === 200 && Array.isArray(res.data)
        ? new Set(res.data.map(credential => Number(credential.categoryId)))
        : new Set()
      credentialMap.set(item.id, savedIds)
    } catch (error) {
      credentialMap.set(item.id, new Set())
    }
  }))
}

async function loadApplications() {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      status: filter.status,
      platformId: filter.platformId,
      columnId: filter.columnId,
      keyword: filter.keyword || undefined
    }
    Object.assign(params, buildTimeRangeParams(pickUsableTimeRange(filter.timeRange, latestFilterTimeRange.value)))
    const res = await getApplicationList(params)
    if (res.code !== 200) {
      MessagePlugin.error(res.message || '加载权限分配列表失败')
      return
    }
    applicationList.value = res.data.records || []
    pagination.total = res.data.total || 0
    await loadCredentialMap(applicationList.value)
  } catch (error) {
    console.error(error)
    if (isAdminSessionRequestError(error)) return
    MessagePlugin.error('加载权限分配列表失败')
  } finally {
    loading.value = false
  }
}

async function handleFilterSubmit() {
  pagination.current = 1
  await nextTick()
  loadApplications()
}

function syncFilterTimeRange(value) {
  latestFilterTimeRange.value = normalizeTimeRangeValue(value)
}

async function handleFilterPlatformChange(platformId) {
  filter.columnId = null
  await loadFilterColumns(platformId)
  handleFilterSubmit()
}

function handleResetFilter() {
  filter.platformId = null
  filter.columnId = null
  filter.status = null
  filter.keyword = ''
  filter.timeRange = []
  latestFilterTimeRange.value = []
  filterColumns.value = []
  handleFilterSubmit()
}

function handlePageChange(pageInfo) {
  pagination.current = pageInfo.current
  pagination.pageSize = pageInfo.pageSize
  loadApplications()
}

async function loadScopeTree() {
  permissionDialog.scopeLoading = true
  permissionDialog.scopeTree = []
  scopeNodeMap.clear()
  try {
    const platformRes = await listPlatforms({ publishedOnly: true })
    const platforms = platformRes.code === 200 ? (platformRes.data || []) : []
    const platformNodes = []
    for (const platform of platforms) {
      const platformKey = `p-${platform.id}`
      const columnRes = await listColumns({ platformId: platform.id, publishedOnly: true })
      const columns = columnRes.code === 200 ? (columnRes.data || []) : []
      const columnNodes = []
      for (const column of columns) {
        const columnKey = `col-${platform.id}-${column.id}`
        const categoryRes = await getCategoryTree({ columnId: column.id, publishedOnly: true, firstLevelOnly: true })
        const categories = categoryRes.code === 200 ? (categoryRes.data || []) : []
        const categoryNodes = categories.map(category => {
          const key = `cat-${platform.id}-${column.id}-${category.id}`
          const meta = {
            type: 'category',
            value: key,
            platformId: platform.id,
            platformName: platform.name,
            columnId: column.id,
            columnName: column.name,
            categoryId: category.id,
            categoryName: category.name,
            path: `${platform.name}/${column.name}/${category.name}`
          }
          scopeNodeMap.set(key, meta)
          return { value: key, label: category.name }
        })
        if (categoryNodes.length) {
          scopeNodeMap.set(columnKey, { type: 'column', children: categoryNodes.map(item => item.value) })
          columnNodes.push({ value: columnKey, label: column.name, children: categoryNodes })
        }
      }
      if (columnNodes.length) {
        scopeNodeMap.set(platformKey, {
          type: 'platform',
          children: columnNodes.flatMap(column => column.children.map(item => item.value))
        })
        platformNodes.push({ value: platformKey, label: platform.name, children: columnNodes })
      }
    }
    permissionDialog.scopeTree = platformNodes
  } finally {
    permissionDialog.scopeLoading = false
  }
}

function handleScopeTreeChange(value, context) {
  if (!context || !context.node) return
  const checkedSet = new Set(value)
  const nodeValue = context.node.value
  const meta = scopeNodeMap.get(nodeValue)
  if (meta?.children?.length) {
    if (checkedSet.has(nodeValue)) {
      meta.children.forEach(key => checkedSet.add(key))
    } else {
      meta.children.forEach(key => checkedSet.delete(key))
    }
    checkedSet.delete(nodeValue)
  }
  permissionDialog.form.scopeKeys = [...checkedSet].filter(key => key.startsWith('cat-'))
}

function buildInterfaceConfigsFromSelection() {
  const grouped = new Map()
  for (const key of permissionDialog.form.scopeKeys) {
    const meta = scopeNodeMap.get(key)
    if (!meta || meta.type !== 'category') continue
    const groupKey = `${meta.platformId}-${meta.columnId}`
    if (!grouped.has(groupKey)) {
      grouped.set(groupKey, {
        platformId: meta.platformId,
        columnId: meta.columnId,
        categoryIds: []
      })
    }
    grouped.get(groupKey).categoryIds.push(meta.categoryId)
  }
  return [...grouped.values()]
}

function scopeKeysFromRow(row) {
  return getScopeItems(row).map(item => `cat-${item.platformId}-${item.columnId}-${item.categoryId}`)
}

async function openAssignDialog() {
  permissionDialog.mode = 'assign'
  permissionDialog.recordId = null
  permissionDialog.row = null
  permissionDialog.form = createPermissionForm()
  await Promise.all([ensureUserList(), loadScopeTree()])
  permissionDialog.visible = true
}

async function openEditDialog(row) {
  if (!canOperateRecord(row)) return
  permissionDialog.mode = 'edit'
  permissionDialog.recordId = row.id
  permissionDialog.row = row
  permissionDialog.form = {
    userId: row.userId,
    unitName: row.unitName || '',
    deptName: row.deptName || '',
    scopeKeys: [],
    reason: row.reason || ''
  }
  await Promise.all([ensureUserList(), loadScopeTree()])
  permissionDialog.form.scopeKeys = scopeKeysFromRow(row)
  permissionDialog.visible = true
}

function handlePermissionClose() {
  permissionDialog.loading = false
}

function handleDialogUserChange(userId) {
  const selectedUser = userList.value.find(item => item.id === userId)
  permissionDialog.form.unitName = selectedUser?.unitName || ''
}

async function handlePermissionConfirm() {
  if (permissionDialog.mode === 'assign' && !permissionDialog.form.userId) {
    MessagePlugin.warning('请选择系统用户')
    return false
  }
  const interfaceConfigs = buildInterfaceConfigsFromSelection()
  if (!interfaceConfigs.length) {
    MessagePlugin.warning('请至少选择一个一级分类')
    return false
  }

  const first = interfaceConfigs[0]
  const payload = {
    userId: permissionDialog.mode === 'assign' ? permissionDialog.form.userId : permissionDialog.row.userId,
    platformId: first.platformId,
    columnId: first.columnId,
    categoryIds: JSON.stringify(interfaceConfigs.flatMap(item => item.categoryIds)),
    interfaceConfigs: JSON.stringify(interfaceConfigs),
    unitName: permissionDialog.form.unitName,
    deptName: permissionDialog.form.deptName,
    reason: permissionDialog.form.reason
  }

  permissionDialog.loading = true
  try {
    const res = permissionDialog.mode === 'assign'
      ? await assignApplication(payload)
      : await updateApplication(permissionDialog.recordId, payload)
    if (res.code !== 200) {
      MessagePlugin.error(res.message || '保存失败')
      return false
    }
    MessagePlugin.success(permissionDialog.mode === 'assign' ? '权限分配已生效' : '接口权限已更新')
    permissionDialog.visible = false
    await loadApplications()
    return true
  } catch (error) {
    console.error(error)
    if (isAdminSessionRequestError(error)) return false
    MessagePlugin.error('保存失败')
    return false
  } finally {
    permissionDialog.loading = false
  }
}

async function openCredentialList(row) {
  if (!canOperateRecord(row)) return
  try {
    const scopeItems = getScopeItems(row)
    const res = await getApiCredentials(row.id)
    const savedList = res.code === 200 && Array.isArray(res.data) ? res.data : []
    credentialListDialog.row = row
    credentialListDialog.items = scopeItems.map(item => {
      const saved = savedList.find(credential => Number(credential.categoryId) === Number(item.categoryId))
      return {
        categoryId: item.categoryId,
        categoryName: item.categoryName,
        path: item.path,
        saved: Boolean(saved),
        openId: saved?.openId || '',
        clientId: saved?.clientId || '',
        token: saved?.token || '',
        secretKey: saved?.secretKey || '',
        description: saved?.description || ''
      }
    })
    credentialListDialog.visible = true
  } catch (error) {
    console.error(error)
    if (isAdminSessionRequestError(error)) return
    MessagePlugin.error('加载接口凭证失败')
  }
}

function openCredentialForm(item) {
  credentialFormDialog.applicationId = credentialListDialog.row?.id || null
  credentialFormDialog.categoryId = item.categoryId
  credentialFormDialog.categoryName = item.path || item.categoryName
  credentialFormDialog.form = {
    openId: item.openId || '',
    clientId: item.clientId || '',
    token: item.token || '',
    secretKey: item.secretKey || '',
    description: item.description || ''
  }
  credentialFormDialog.visible = true
}

async function handleCredentialSave() {
  if (!credentialFormDialog.applicationId || !credentialFormDialog.categoryId) {
    MessagePlugin.error('缺少必要参数')
    return false
  }
  credentialFormDialog.loading = true
  try {
    const res = await saveApiCredentials(credentialFormDialog.applicationId, [{
      categoryId: credentialFormDialog.categoryId,
      openId: credentialFormDialog.form.openId,
      clientId: credentialFormDialog.form.clientId,
      token: credentialFormDialog.form.token,
      secretKey: credentialFormDialog.form.secretKey,
      description: credentialFormDialog.form.description
    }])
    if (res.code !== 200) {
      MessagePlugin.error(res.message || '保存接口凭证失败')
      return false
    }
    const savedSet = credentialMap.get(credentialFormDialog.applicationId) || new Set()
    savedSet.add(Number(credentialFormDialog.categoryId))
    credentialMap.set(credentialFormDialog.applicationId, savedSet)
    const target = credentialListDialog.items.find(item => Number(item.categoryId) === Number(credentialFormDialog.categoryId))
    if (target) {
      Object.assign(target, credentialFormDialog.form, { saved: true })
    }
    MessagePlugin.success('接口凭证已保存')
    credentialFormDialog.visible = false
    return true
  } catch (error) {
    console.error(error)
    if (isAdminSessionRequestError(error)) return false
    MessagePlugin.error('保存接口凭证失败')
    return false
  } finally {
    credentialFormDialog.loading = false
  }
}

function openAppCredentialDialog(row) {
  appCredentialDialog.row = row
  appCredentialDialog.visible = true
}

function handleDelete(row) {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body: '删除后无法恢复，确认继续吗？',
    theme: 'danger',
    onConfirm: async () => {
      try {
        const res = await deleteApplication(row.id)
        if (res.code === 200) {
          MessagePlugin.success('删除成功')
          await loadApplications()
        } else {
          MessagePlugin.error(res.message || '删除失败')
        }
      } catch (error) {
        console.error(error)
        if (isAdminSessionRequestError(error)) return
        MessagePlugin.error('删除失败')
      } finally {
        dialog.hide()
      }
    },
    onClose: () => dialog.hide()
  })
}

onMounted(async () => {
  await loadFilterPlatforms()
  await loadApplications()
})
</script>

<style scoped>
.api-permission-assign {
  margin: 16px;
}

.api-permission-assign :deep(.t-form__label) {
  padding-right: 10px;
}

.filter-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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

.user-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  color: var(--td-text-color-primary);
  font-weight: 500;
}

.user-sub {
  color: var(--td-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.scope-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.scope-item,
.credential-item-main,
.credential-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.scope-path {
  color: var(--td-text-color-primary);
  line-height: 1.5;
  word-break: break-all;
}

.validity-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.invalid-reason {
  color: var(--td-error-color);
  font-size: 12px;
  line-height: 1.5;
}

.time-text {
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.operation-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.dialog-user-grid {
  margin-bottom: 16px;
}

.readonly-field {
  height: 100%;
  padding: 10px 12px;
  border-radius: var(--td-radius-default);
  background: var(--td-bg-color-secondarycontainer);
}

.readonly-label {
  margin-bottom: 6px;
  color: var(--td-text-color-secondary);
  font-size: 12px;
}

.readonly-value {
  color: var(--td-text-color-primary);
  font-size: 14px;
  line-height: 1.5;
  word-break: break-all;
}

.scope-tree-field {
  width: 100%;
}

.tree-help {
  margin-top: 8px;
  color: var(--td-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.tree-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 88px;
  padding: 12px;
  border: 1px dashed var(--td-border-level-2-color);
  border-radius: var(--td-radius-default);
  color: var(--td-text-color-secondary);
  background: var(--td-bg-color-secondarycontainer);
}

.permission-tree {
  width: 100%;
  max-height: 360px;
  box-sizing: border-box;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--td-border-level-1-color);
  border-radius: var(--td-radius-default);
}

.credential-list-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.credential-header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--td-border-level-1-color);
  color: var(--td-text-color-primary);
}

.credential-list {
  display: flex;
  flex-direction: column;
}

.credential-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--td-border-level-1-color);
}

.credential-list-item:last-child {
  border-bottom: none;
}
</style>
