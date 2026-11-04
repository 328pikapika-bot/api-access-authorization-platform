<template>
  <div class="permission-manage">
    <t-card :bordered="false" class="filter-card">
      <t-form :data="filters" layout="inline" label-width="auto">
        <t-form-item label="用户名：" name="userName">
          <t-input v-model="filters.userName" placeholder="请输入用户名" clearable @enter="handleSearch" />
        </t-form-item>
        <t-form-item label="邮箱：" name="email">
          <t-input v-model="filters.email" placeholder="请输入邮箱" clearable @enter="handleSearch" />
        </t-form-item>
        <t-form-item label="用户类型：" name="userType">
          <t-select v-model="filters.userType" placeholder="请选择" clearable style="width: 150px">
            <t-option label="内部用户" value="INTERNAL" />
            <t-option label="外部用户" value="EXTERNAL" />
          </t-select>
        </t-form-item>
        <t-form-item label="注册时间：" name="timeRange">
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

    <t-card :bordered="false" class="table-card" style="margin-top: 16px">
      <template #title>
        <div class="card-header">
          <div>
            <div class="card-title">权限管理</div>
            <div class="card-subtitle">展示并管理系统用户信息，支持角色分配、状态控制与无引用用户删除</div>
          </div>
        </div>
      </template>
      <template #actions>
        <t-button theme="primary" @click="handleAdd">
          <template #icon><add-icon /></template>
          新增用户
        </t-button>
      </template>

      <t-table
        row-key="id"
        :data="userList"
        :columns="tableColumns"
        :loading="loading"
        hover
        bordered
      >
        <template #userType="{ row }">
          <t-tag :theme="row.userType === 'INTERNAL' ? 'success' : 'default'" variant="light-outline">
            {{ row.userType === 'INTERNAL' ? '内部' : '外部' }}
          </t-tag>
        </template>
        <template #userRole="{ row }">
          <t-select
            v-model="row.userRole"
            size="small"
            auto-width
            @change="(val) => handleRoleChange(row, val)"
          >
            <t-option label="普通用户" :value="0" />
            <t-option label="管理员" :value="1" />
          </t-select>
        </template>
        <template #status="{ row }">
          <t-switch
            v-model="row.status"
            :custom-value="[1, 0]"
            @change="(val) => handleStatusChange(row, val)"
          />
        </template>
        <template #createTime="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
        <template #operation="{ row }">
          <t-button
            theme="danger"
            variant="text"
            :loading="deletingUserId === row.id"
            @click="handleDelete(row)"
          >
            删除
          </t-button>
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

    <!-- 新增用户弹窗 -->
    <t-dialog
      v-model:visible="addDialogVisible"
      header="新增用户"
      width="680px"
      placement="top"
      top="32px"
      dialog-class-name="add-user-dialog"
      :dialog-style="{ maxWidth: 'calc(100vw - 32px)' }"
      :confirm-btn="{ content: '确定', loading: addLoading }"
      :on-confirm="handleAddSubmit"
      :on-close="handleAddCancel"
    >
      <t-form ref="addFormRef" :data="addForm" :rules="addFormRules" label-width="120px">
        <t-form-item label="NSTL联合ID" name="nstlUnionId">
          <div class="union-id-field">
            <div class="union-id-row">
              <t-input
                v-model="addForm.nstlUnionId"
                placeholder="请输入NSTL统一认证联合ID"
                clearable
                @enter="handlePreviewUser"
              />
              <t-button
                class="verify-user-btn"
                theme="primary"
                :loading="previewLoading"
                :disabled="!addForm.nstlUnionId?.trim()"
                @click="handlePreviewUser"
              >
                <template #icon><user-search-icon /></template>
                {{ previewLoading ? '校验中' : '校验并回填' }}
              </t-button>
            </div>
            <div class="union-id-feedback" :class="{ success: previewVerified }">
              <check-circle-icon v-if="previewVerified" />
              <span>{{ previewVerified ? '联合ID校验通过，可继续设置用户权限' : '校验后将自动回填统一认证用户资料' }}</span>
            </div>
          </div>
        </t-form-item>
        <t-form-item label="用户角色" name="userRole">
          <t-radio-group v-model="addForm.userRole">
            <t-radio :value="0">普通用户</t-radio>
            <t-radio :value="1">管理员</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="用户类型" name="userType">
          <t-radio-group v-model="addForm.userType">
            <t-radio value="INTERNAL">内部用户</t-radio>
            <t-radio value="EXTERNAL">外部用户</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="用户名">
          <t-input :model-value="addForm.userName || '-'" readonly />
        </t-form-item>
        <t-form-item label="用户ID">
          <t-input :model-value="addForm.userId || '-'" readonly />
        </t-form-item>
        <t-form-item label="昵称">
          <t-input :model-value="addForm.nickname || '-'" readonly />
        </t-form-item>
        <t-form-item label="邮箱">
          <t-input :model-value="addForm.email || '-'" readonly />
        </t-form-item>
        <t-form-item label="手机号">
          <t-input :model-value="addForm.phone || '-'" readonly />
        </t-form-item>
        <t-form-item label="所属单位">
          <t-input :model-value="addForm.unitName || '-'" readonly />
        </t-form-item>
        <t-form-item label="注册站点">
          <t-input :model-value="[addForm.userLibName, addForm.userLibCode].filter(Boolean).join(' / ') || '-'" readonly />
        </t-form-item>
        <t-form-item label="NSTL用户类型">
          <t-input :model-value="addForm.userLibType || '-'" readonly />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch, nextTick } from 'vue'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
import { CheckCircleIcon, UserSearchIcon } from 'tdesign-icons-vue-next'
import { getUserPage, updateUserStatus, updateUserRole, deleteUser, addUser, previewUser } from '@/api/user'
import {
  buildTimeRangeParams,
  mergeTimeRangeInputValue,
  normalizeTimeRangeValue,
  pickUsableTimeRange
} from '@/utils/timeRange'

const loading = ref(false)
const deletingUserId = ref(null)
const userList = ref([])
const filters = reactive({
  userName: '',
  email: '',
  userType: '',
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

// 新增用户弹窗
const addDialogVisible = ref(false)
const addForm = reactive({
  nstlUnionId: '',
  userName: '',
  userId: '',
  nickname: '',
  email: '',
  phone: '',
  unitName: '',
  userLibCode: '',
  userLibName: '',
  userLibType: '',
  userRole: 0,
  userType: 'INTERNAL'
})
const addFormRules = {
  nstlUnionId: [{ required: true, message: '请输入NSTL联合ID', trigger: 'blur' }]
}
const addFormRef = ref(null)
const addLoading = ref(false)
const previewLoading = ref(false)
const previewVerified = ref(false)

const resetPreviewFields = () => {
  previewVerified.value = false
  Object.assign(addForm, {
    userName: '',
    userId: '',
    nickname: '',
    email: '',
    phone: '',
    unitName: '',
    userLibCode: '',
    userLibName: '',
    userLibType: ''
  })
}

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const tableColumns = [
  { colKey: 'nstlUnionId', title: 'NSTL联合ID', minWidth: 180 },
  { colKey: 'userId', title: '用户ID', minWidth: 120 },
  { colKey: 'userName', title: '用户名', minWidth: 80 },
  { colKey: 'nickname', title: '昵称', minWidth: 80 },
  // { colKey: 'unitName', title: '用户单位', minWidth: 150 },
  { colKey: 'phone', title: '手机号', minWidth: 80 },
  { colKey: 'email', title: '邮箱', minWidth: 180 },
  { colKey: 'userType', title: '用户类型', width: 100, cell: 'userType' },
  { colKey: 'userRole', title: '角色', width: 140, cell: 'userRole' },
  { colKey: 'status', title: '状态', width: 80, cell: 'status' },
  { colKey: 'createTime', title: '注册时间', width: 180, cell: 'createTime' },
  { colKey: 'operation', title: '操作', width: 100, fixed: 'right', cell: 'operation' }
]

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const syncTimeRange = (value) => {
  latestTimeRange.value = normalizeTimeRangeValue(value)
}

const syncTimeRangeInput = (index, value) => {
  const nextRange = mergeTimeRangeInputValue(filters.timeRange, latestTimeRange.value, index, value)
  filters.timeRange = nextRange
  latestTimeRange.value = nextRange
}

const fetchUserList = async () => {
  loading.value = true
  const params = {
    current: pagination.current,
    size: pagination.size
  }
  if (filters.userName?.trim()) params.userName = filters.userName.trim()
  if (filters.email?.trim()) params.email = filters.email.trim()
  if (filters.userType) params.userType = filters.userType
  Object.assign(params, buildTimeRangeParams(pickUsableTimeRange(filters.timeRange, latestTimeRange.value)))

  try {
    const res = await getUserPage(params)
    if (res.code === 200) {
      userList.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pagination.current = 1
  await nextTick()
  fetchUserList()
}

const handleReset = () => {
  Object.assign(filters, {
    userName: '',
    email: '',
    userType: '',
    timeRange: []
  })
  latestTimeRange.value = []
  handleSearch()
}

const onPaginationChange = (pageInfo) => {
  pagination.current = pageInfo.current
  pagination.size = pageInfo.pageSize
  fetchUserList()
}

const handleStatusChange = async (row, val) => {
  const previousStatus = val === 1 ? 0 : 1
  const statusName = val === 1 ? '启用' : '禁用'
  const confirmDialog = DialogPlugin.confirm({
    header: '提示',
    body: `确定要${statusName}用户 ${row.userName} 吗？`,
    onConfirm: async () => {
      try {
        const res = await updateUserStatus(row.id, val)
        if (res.code === 200) {
          MessagePlugin.success(`${statusName}成功`)
        } else {
          row.status = previousStatus
          MessagePlugin.error(res.message || '更新失败')
        }
      } catch (error) {
        row.status = previousStatus
        MessagePlugin.error(error?.message || error?.response?.data?.message || '更新失败')
      }
      confirmDialog.hide()
    },
    onCancel: () => {
      row.status = previousStatus
      confirmDialog.hide()
    },
    onClose: () => {
      row.status = previousStatus
      confirmDialog.hide()
    }
  })
}

const handleRoleChange = async (row, val) => {
  const previousRole = val === 1 ? 0 : 1
  const roleName = val === 1 ? '管理员' : '普通用户'
  const confirmDialog = DialogPlugin.confirm({
    header: '提示',
    body: `确定要将用户 ${row.userName} 的角色修改为 ${roleName} 吗？`,
    onConfirm: async () => {
      try {
        const res = await updateUserRole(row.id, val)
        if (res.code === 200) {
          MessagePlugin.success('角色修改成功')
        } else {
          row.userRole = previousRole
          MessagePlugin.error(res.message || '更新失败')
        }
      } catch (error) {
        row.userRole = previousRole
        MessagePlugin.error(error?.message || error?.response?.data?.message || '更新失败')
      }
      confirmDialog.hide()
    },
    onCancel: () => {
      row.userRole = previousRole
      confirmDialog.hide()
    },
    onClose: () => {
      row.userRole = previousRole
      confirmDialog.hide()
    }
  })
}

const handleDelete = (row) => {
  const displayName = row.nickname || row.userName || row.userId || row.id
  const confirmDialog = DialogPlugin.confirm({
    header: '确认删除',
    body: `确定删除用户“${displayName}”吗？删除后该账号将不能登录接口平台。`,
    theme: 'danger',
    onConfirm: async () => {
      deletingUserId.value = row.id
      try {
        const res = await deleteUser(row.id)
        if (res.code === 200) {
          MessagePlugin.success('删除成功')
          if (userList.value.length === 1 && pagination.current > 1) {
            pagination.current -= 1
          }
          await fetchUserList()
        } else {
          MessagePlugin.error(res.message || '删除失败')
        }
      } catch (error) {
        MessagePlugin.error(error?.response?.data?.message || error?.message || '删除失败')
      } finally {
        deletingUserId.value = null
        confirmDialog.hide()
      }
    },
    onCancel: () => confirmDialog.hide(),
    onClose: () => confirmDialog.hide()
  })
}

const handleAdd = () => {
  // 重置表单
  previewVerified.value = false
  Object.assign(addForm, {
    nstlUnionId: '',
    userName: '',
    userId: '',
    nickname: '',
    email: '',
    phone: '',
    unitName: '',
    userLibCode: '',
    userLibName: '',
    userLibType: '',
    userRole: 0,
    userType: 'INTERNAL'
  })
  addDialogVisible.value = true
}

const handlePreviewUser = async () => {
  if (previewLoading.value) return

  if (!addForm.nstlUnionId?.trim()) {
    MessagePlugin.warning('请先输入NSTL联合ID')
    return
  }

  previewLoading.value = true
  resetPreviewFields()
  try {
    const res = await previewUser({
      nstlUnionId: addForm.nstlUnionId.trim(),
      userRole: addForm.userRole,
      userType: addForm.userType
    })
    if (res.code === 200 && res.data) {
      previewVerified.value = true
      Object.assign(addForm, {
        nstlUnionId: res.data.nstlUnionId || addForm.nstlUnionId.trim(),
        userName: res.data.userName || '',
        userId: res.data.userId || '',
        nickname: res.data.nickname || '',
        email: res.data.email || '',
        phone: res.data.phone || '',
        unitName: res.data.unitName || '',
        userLibCode: res.data.userLibCode || '',
        userLibName: res.data.userLibName || '',
        userLibType: res.data.userLibType || ''
      })
      MessagePlugin.success('联合ID校验通过，已自动回填NSTL用户信息')
    } else {
      MessagePlugin.error(res.message || '联合ID校验失败')
    }
  } catch (error) {
    MessagePlugin.error(error?.message || error?.response?.data?.message || '联合ID校验失败')
  } finally {
    previewLoading.value = false
  }
}

const handleAddSubmit = async () => {
  if (!addFormRef.value) return

  const validateResult = await addFormRef.value.validate()
  if (validateResult !== true) return

  addLoading.value = true
  try {
    const res = await addUser(addForm)
    if (res.code === 200) {
      MessagePlugin.success('新增用户成功')
      addDialogVisible.value = false
      fetchUserList() // 刷新列表
    } else {
      MessagePlugin.error(res.message || '新增用户失败')
    }
  } catch (error) {
    console.error('新增用户失败:', error)
    MessagePlugin.error(error?.message || error?.response?.data?.message || '新增用户失败')
  } finally {
    addLoading.value = false
  }
}

const handleAddCancel = () => {
  resetPreviewFields()
  addDialogVisible.value = false
}

watch(() => addForm.nstlUnionId, (value, previousValue) => {
  if ((value || '').trim() !== (previousValue || '').trim()) {
    resetPreviewFields()
  }
})

onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.permission-manage {
  margin: 16px;
}

.permission-manage :deep(.t-form__label) {
  padding-right: 10px;
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

.filter-card {
  margin-bottom: 16px;
}

.union-id-field {
  width: 100%;
}

.union-id-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.verify-user-btn {
  min-width: 132px;
  height: 32px;
  padding: 0 14px;
  white-space: nowrap;
}

.union-id-feedback {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 20px;
  margin-top: 5px;
  color: var(--td-text-color-placeholder);
  font-size: 12px;
  line-height: 20px;
}

.union-id-feedback.success {
  color: var(--td-success-color);
}

:global(.add-user-dialog .t-dialog__body) {
  max-height: calc(100vh - 210px);
  overflow-y: auto;
}

@media (max-width: 600px) {
  .union-id-row {
    grid-template-columns: 1fr;
  }

  .verify-user-btn {
    width: 100%;
  }
}
</style>
