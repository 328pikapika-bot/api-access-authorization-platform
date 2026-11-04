<template>
  <div class="mockoon-service-manage">
    <!-- 筛选区域 -->
    <t-card :bordered="false" class="filter-card">
      <t-form layout="inline" label-width="auto" @submit="handleSearch">
        <t-form-item label="服务查询：" name="searchKeyword">
          <t-input
            v-model="searchKeyword"
            placeholder="请输入服务名称或说明"
            style="width: 300px"
            @enter="handleSearch"
            clearable
          />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" type="submit">查询</t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 列表区域 -->
    <t-card :bordered="false" style="margin-top: 16px">
      <template #title>
        <div class="card-title">沙箱服务管理</div>
        <div class="card-subtitle">管理并配置 Mockoon 服务实例，为沙箱调试提供支持</div>
      </template>
      <template #actions>
        <t-button theme="primary" @click="handleAdd">
          <template #icon><add-icon /></template>
          新增服务
        </t-button>
      </template>

      <!-- 数据表格 -->
      <t-table
        :data="tableData"
        :columns="columns"
        :loading="loading"
        row-key="id"
        hover
        bordered
      >
        <!-- 状态列 -->
        <template #status="{ row }">
          <t-tag :theme="row.status === 1 ? 'success' : 'default'" variant="light">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </t-tag>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <t-space>
            <t-button variant="text" theme="primary" @click="handleEdit(row)">
              编辑
            </t-button>
            <t-popconfirm
              content="确定要删除该服务吗？"
              @confirm="handleDelete(row)"
            >
              <t-button variant="text" theme="danger">删除</t-button>
            </t-popconfirm>
          </t-space>
        </template>
      </t-table>

      <!-- 分页 -->
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <t-pagination
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :show-jumper="true"
          :show-page-size="true"
          @change="onPageChange"
        />
      </div>
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog
      v-model:visible="dialogVisible"
      :header="isEdit ? '编辑服务' : '新增服务'"
      :width="600"
      :confirm-btn="{ content: '保存', loading: submitLoading }"
      @confirm="handleSubmit"
    >
      <t-form :data="formData" :rules="formRules" ref="formRef">
        <t-form-item label="服务名称" name="name">
          <t-input v-model="formData.name" placeholder="请输入服务名称" :maxlength="100" show-limit-number />
        </t-form-item>
        <t-form-item label="服务地址" name="serviceUrl">
          <t-input v-model="formData.serviceUrl" placeholder="http://localhost:8082" :maxlength="255" />
        </t-form-item>
        <t-form-item label="服务说明" name="description">
          <t-textarea v-model="formData.description" placeholder="请输入服务说明" :rows="3" :maxlength="500" show-limit-number />
        </t-form-item>
        <t-form-item label="状态" name="status">
          <t-radio-group v-model="formData.status">
            <t-radio :value="1">启用</t-radio>
            <t-radio :value="0">禁用</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="排序" name="sort">
          <t-input-number v-model="formData.sort" :min="0" :max="9999" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { AddIcon, SearchIcon } from 'tdesign-icons-vue-next'
import { isAdminSessionRequestError } from '@/utils/logoutState'
import {
  getMockoonServiceList,
  createMockoonService,
  updateMockoonService,
  deleteMockoonService
} from '@/api/mockoonService'

// 表格数据
const tableData = ref([])
const loading = ref(false)
const searchKeyword = ref('')

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showJumper: true,
  showPageSize: true
})

// 表格列定义
const columns = [
  { colKey: 'id', title: 'ID', width: 80 },
  { colKey: 'name', title: '服务名称', width: 200 },
  { colKey: 'serviceUrl', title: '服务地址', ellipsis: true },
  { colKey: 'description', title: '说明', ellipsis: true },
  { colKey: 'status', title: '状态', width: 100 },
  { colKey: 'sort', title: '排序', width: 80 },
  { colKey: 'operation', title: '操作', width: 150, fixed: 'right' }
]

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  serviceUrl: '',
  description: '',
  status: 1,
  sort: 0
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入服务名称', type: 'error' },
    { max: 100, message: '服务名称长度不能超过100个字符', type: 'error' }
  ],
  serviceUrl: [
    { required: true, message: '请输入服务地址', type: 'error' },
    {
      validator: (val) => val.startsWith('http://') || val.startsWith('https://'),
      message: '地址需要以 http:// 或 https:// 开头',
      type: 'error'
    },
    { max: 255, message: '服务地址长度不能超过255个字符', type: 'error' }
  ],
  description: [{ max: 500, message: '服务说明长度不能超过500个字符', type: 'error' }],
  sort: [{
    validator: (val) => Number.isInteger(val) && val >= 0 && val <= 9999,
    message: '排序值必须为0到9999之间的整数',
    type: 'error'
  }]
}

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMockoonServiceList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    if (isAdminSessionRequestError(error)) return
    MessagePlugin.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 分页变化
const onPageChange = (pageInfo) => {
  pagination.current = pageInfo.current
  pagination.pageSize = pageInfo.pageSize
  fetchData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name || '',
    serviceUrl: row.serviceUrl || '',
    description: row.description || '',
    status: row.status ?? 1,
    sort: row.sort ?? 0
  })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    const res = await deleteMockoonService(row.id)
    if (res.code === 200) {
      MessagePlugin.success('删除成功')
      fetchData()
    } else {
      MessagePlugin.error(res.message || '删除失败')
    }
  } catch (error) {
    MessagePlugin.error(error?.message || error?.response?.data?.message || '删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  const validateResult = await formRef.value.validate()
  if (validateResult !== true) return

  submitLoading.value = true
  try {
    const api = isEdit.value ? updateMockoonService : createMockoonService
    const payload = {
      name: formData.name?.trim(),
      serviceUrl: formData.serviceUrl?.trim(),
      description: formData.description?.trim(),
      status: formData.status,
      sort: formData.sort
    }
    const params = isEdit.value ? [formData.id, payload] : [payload]
    const res = await api(...params)

    if (res.code === 200) {
      MessagePlugin.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      fetchData()
    } else {
      MessagePlugin.error(res.message || '保存失败')
    }
  } catch (error) {
    MessagePlugin.error(error?.message || error?.response?.data?.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.name = ''
  formData.serviceUrl = ''
  formData.description = ''
  formData.status = 1
  formData.sort = 0
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.mockoon-service-manage {
  margin: 16px;
}

.mockoon-service-manage :deep(.t-form__label) {
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


</style>
