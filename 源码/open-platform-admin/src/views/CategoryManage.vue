<template>
  <div class="category-manage" :class="{ 'category-expanded': categoryExpanded }">
    <t-row :gutter="[16, 16]" class="manage-grid">
      <t-col v-show="!categoryExpanded" :xs="12" :sm="12" :md="4" :lg="3" class="panel-col panel-platform">
        <t-card :bordered="false" title="系统平台" header-bordered class="category-card">
          <template #actions>
            <t-space size="small">
              <t-tooltip content="刷新">
                <t-button variant="text" shape="circle" @click="fetchPlatforms">
                  <template #icon><t-icon name="refresh" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="全部发布">
                <t-button variant="text" shape="circle" theme="success" :disabled="!platformList.length" @click="handlePublishAllPlatforms">
                  <template #icon><t-icon name="upload" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="全部撤回">
                <t-button variant="text" shape="circle" theme="warning" :disabled="!platformList.length" @click="handleWithdrawAllPlatforms">
                  <template #icon><t-icon name="download" /></template>
                </t-button>
              </t-tooltip>
              <t-button variant="text" shape="circle" @click="handleAddPlatform">
                <template #icon><t-icon name="add" /></template>
              </t-button>
            </t-space>
          </template>

          <t-table
            row-key="id"
            :data="platformList"
            :columns="platformColumns"
            :loading="platformLoading"
            hover
            bordered
            size="small"
            v-model:active-row-keys="activeRowKeysPlatform"
            active-row-type="single"
            @row-click="handlePlatformClick"
          >
            <template #name="{ row }">
              <span class="ellipsis-text" :title="row.name">{{ row.name }}</span>
            </template>
            <template #isPublished="{ row }">
              <t-tag :theme="getPublishStatusTheme(row.isPublished)" variant="light-outline" size="small">
                {{ getPublishStatusLabel(row.isPublished) }}
              </t-tag>
            </template>
            <template #operation="{ row }">
              <div class="action-group">
                <t-link v-if="row.isPublished !== 1" theme="success" hover="color" @click.stop="handlePublishPlatform(row)">
                  <t-icon name="upload" />
                </t-link>
                <t-link v-else theme="warning" hover="color" @click.stop="handleWithdrawPlatform(row)">
                  <t-icon name="download" />
                </t-link>
                <t-link theme="primary" hover="color" @click.stop="handleEditPlatform(row)">
                  <t-icon name="edit" />
                </t-link>
                <t-link theme="danger" hover="color" @click.stop="handleDeletePlatform(row)">
                  <t-icon name="delete" />
                </t-link>
              </div>
            </template>
          </t-table>
        </t-card>
      </t-col>

      <t-col v-show="!categoryExpanded" :xs="12" :sm="12" :md="4" :lg="3" class="panel-col panel-column">
        <t-card :bordered="false" :title="selectedPlatform ? `栏目列表 - ${selectedPlatform.name}` : '栏目列表'" header-bordered class="category-card">
          <template #actions>
            <t-space size="small">
              <t-tooltip content="刷新">
                <t-button variant="text" shape="circle" :disabled="!selectedPlatform" @click="fetchColumns(selectedPlatform.id)">
                  <template #icon><t-icon name="refresh" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="全部发布">
                <t-button variant="text" shape="circle" theme="success" :disabled="!columnList.length" @click="handlePublishAllColumns">
                  <template #icon><t-icon name="upload" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="全部撤回">
                <t-button variant="text" shape="circle" theme="warning" :disabled="!columnList.length" @click="handleWithdrawAllColumns">
                  <template #icon><t-icon name="download" /></template>
                </t-button>
              </t-tooltip>
              <t-button variant="text" shape="circle" :disabled="!selectedPlatform" @click="handleAddColumn">
                <template #icon><t-icon name="add" /></template>
              </t-button>
            </t-space>
          </template>

          <div v-if="selectedPlatform" class="card-table-wrap">
            <t-table
              row-key="id"
              :data="columnList"
              :columns="columnColumns"
              :loading="columnLoading"
              hover
              bordered
              size="small"
              v-model:active-row-keys="activeRowKeysColumn"
              active-row-type="single"
              @row-click="handleColumnClick"
            >
              <template #name="{ row }">
                <span class="ellipsis-text" :title="row.name">{{ row.name }}</span>
              </template>
              <template #isPublished="{ row }">
                <t-tag :theme="getPublishStatusTheme(row.isPublished)" variant="light-outline" size="small">
                  {{ getPublishStatusLabel(row.isPublished) }}
                </t-tag>
              </template>
              <template #operation="{ row }">
                <div class="action-group">
                  <t-link v-if="row.isPublished !== 1" theme="success" hover="color" @click.stop="handlePublishColumn(row)">
                    <t-icon name="upload" />
                  </t-link>
                  <t-link v-else theme="warning" hover="color" @click.stop="handleWithdrawColumn(row)">
                    <t-icon name="download" />
                  </t-link>
                  <t-link theme="primary" hover="color" @click.stop="handleEditColumn(row)">
                    <t-icon name="edit" />
                  </t-link>
                  <t-link theme="danger" hover="color" @click.stop="handleDeleteColumn(row)">
                    <t-icon name="delete" />
                  </t-link>
                </div>
              </template>
            </t-table>
          </div>
          <t-empty v-else description="请选择平台" />
        </t-card>
      </t-col>

      <t-col :xs="12" :sm="12" :md="12" :lg="categoryExpanded ? 12 : 6" class="panel-col panel-category">
        <t-card :bordered="false" :title="selectedColumn ? `分类与内容 - ${selectedColumn.name}` : '分类与内容'" header-bordered class="category-card category-card-auto">
          <template #actions>
            <t-space size="small">
              <t-tooltip :content="categoryExpanded ? '缩小' : '扩大'">
                <t-button variant="text" shape="circle" @click="categoryExpanded = !categoryExpanded">
                  <template #icon><t-icon :name="categoryExpanded ? 'fullscreen-exit' : 'fullscreen'" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="刷新">
                <t-button variant="text" shape="circle" :disabled="!selectedColumn" @click="fetchCategoryTree(selectedColumn.id)">
                  <template #icon><t-icon name="refresh" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="全部发布">
                <t-button variant="text" shape="circle" theme="success" :disabled="!flatCategoryList.length" @click="handlePublishAllCategories">
                  <template #icon><t-icon name="upload" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="全部撤回">
                <t-button variant="text" shape="circle" theme="warning" :disabled="!flatCategoryList.length" @click="handleWithdrawAllCategories">
                  <template #icon><t-icon name="download" /></template>
                </t-button>
              </t-tooltip>
              <t-button variant="outline" theme="primary" :disabled="!selectedColumn" @click="handleAddRootCategory">
                新增根分类
              </t-button>
            </t-space>
          </template>

          <div v-if="selectedColumn" class="card-table-wrap category-table-wrap">
            <t-table
              row-key="id"
              :data="flatCategoryList"
              :columns="categoryColumns"
              :loading="categoryLoading"
              hover
              bordered
              size="small"
              table-layout="fixed"
            >
              <template #name="{ row }">
                <span class="tree-name" :style="{ paddingLeft: `${row._level * 20}px` }" :title="row.name">
                  <t-icon v-if="row._hasChildren" name="folder" class="tree-icon folder-icon" />
                  <t-icon v-else-if="row.type === 'api'" name="internet" class="tree-icon api-icon" />
                  <t-icon v-else name="file" class="tree-icon file-icon" />
                  <span class="ellipsis-text">{{ row.name }}</span>
                </span>
              </template>

              <template #type="{ row }">
                <t-tag :theme="row.type === 'api' ? 'primary' : 'default'" size="small">
                  {{ row.type === 'api' ? '接口' : '内容' }}
                </t-tag>
              </template>

              <template #isPublished="{ row }">
                <t-tag :theme="getPublishStatusTheme(row.isPublished)" variant="light-outline" size="small">
                  {{ getPublishStatusLabel(row.isPublished) }}
                </t-tag>
              </template>

              <template #isOpen="{ row }">
                <t-tooltip :content="row.isOpen === 1 ? '关闭后类型会自动切换为内容' : '开启后类型会自动切换为接口'">
                  <t-switch :value="row.isOpen === 1" size="small" @change="(val) => handleOpenChange(row, val)" />
                </t-tooltip>
              </template>

              <template #title="{ row }">
                <span class="ellipsis-text" :title="row.title || '-'">{{ row.title || '-' }}</span>
              </template>

              <template #hasContent="{ row }">
                <t-icon v-if="row.docContent && row.docContent.trim()" name="check" class="status-icon success-icon" />
                <t-icon v-else name="close" class="status-icon empty-icon" />
              </template>

              <template #updateTime="{ row }">
                <span class="meta-text">{{ formatDate(row.updateTime) }}</span>
              </template>

              <template #operation="{ row }">
                <div class="action-group">
                  <t-link theme="primary" hover="color" @click="handleEditContent(row)">
                    <t-icon name="article" />
                  </t-link>
                  <t-link v-if="row.type === 'api'" theme="primary" hover="color" @click="handleConfigInterface(row)">
                    <t-icon name="setting" />
                  </t-link>
                  <t-link v-if="row.isPublished !== 1" theme="success" hover="color" @click="handlePublish(row)">
                    <t-icon name="upload" />
                  </t-link>
                  <t-link v-else theme="warning" hover="color" @click="handleWithdraw(row)">
                    <t-icon name="download" />
                  </t-link>
                  <t-link theme="primary" hover="color" @click="handleAddChildCategory(row)">
                    <t-icon name="add" />
                  </t-link>
                  <t-link theme="default" hover="color" @click="handleEditCategory(row)">
                    <t-icon name="edit" />
                  </t-link>
                  <t-link theme="danger" hover="color" @click="handleDeleteCategory(row)">
                    <t-icon name="delete" />
                  </t-link>
                </div>
              </template>
            </t-table>
          </div>
          <t-empty v-else description="请选择栏目" />
        </t-card>
      </t-col>
    </t-row>

    <t-dialog
      v-model:visible="platformDialog.visible"
      :header="platformDialog.title"
      :on-confirm="submitPlatform"
      :confirm-loading="submitting"
      width="500px"
    >
      <t-form ref="platformFormRef" :data="platformForm" :rules="platformRules" label-align="right" label-width="100px">
        <t-form-item label="平台名称" name="name">
          <t-input v-model="platformForm.name" placeholder="请输入平台名称" />
        </t-form-item>
        <t-form-item label="排序" name="sort">
          <t-input-number v-model="platformForm.sort" :min="0" :max="9999" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea
            v-model="platformForm.description"
            placeholder="请输入描述"
            :maxlength="5000"
            :autosize="{ minRows: 4, maxRows: 8 }"
            show-limit-number
          />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="columnDialog.visible"
      :header="columnDialog.title"
      :on-confirm="submitColumn"
      :confirm-loading="submitting"
      width="500px"
    >
      <t-form ref="columnFormRef" :data="columnForm" :rules="columnRules" label-align="right" label-width="100px">
        <t-form-item label="所属平台">
          <t-input :value="selectedPlatform?.name" disabled />
        </t-form-item>
        <t-form-item label="栏目名称" name="name">
          <t-input v-model="columnForm.name" placeholder="请输入栏目名称" />
        </t-form-item>
        <t-form-item label="排序" name="sort">
          <t-input-number v-model="columnForm.sort" :min="0" :max="9999" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="categoryDialog.visible"
      :header="categoryDialog.title"
      :on-confirm="submitCategory"
      :confirm-loading="submitting"
      width="500px"
    >
      <t-form ref="categoryFormRef" :data="categoryForm" :rules="categoryRules" label-align="right" label-width="100px">
        <t-form-item label="所属栏目">
          <t-input :value="selectedColumn?.name" disabled />
        </t-form-item>
        <t-form-item v-if="parentCategoryName" label="父级分类">
          <t-input :value="parentCategoryName" disabled />
        </t-form-item>
        <t-form-item label="分类名称" name="name">
          <t-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </t-form-item>
        <t-form-item label="类型" name="type">
          <t-radio-group v-model="categoryForm.type">
            <t-radio value="content">内容</t-radio>
            <t-radio value="api">接口</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="排序" name="sort">
          <t-input-number v-model="categoryForm.sort" :min="0" :max="9999" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <InterfaceConfigDialog
      v-model="interfaceConfigDialogVisible"
      :category-id="selectedCategoryId"
      :category-name="selectedCategoryName"
      @success="handleConfigSuccess"
    />

    <t-dialog
      v-model:visible="contentDialog.visible"
      :header="`编辑内容 - ${contentForm.name}`"
      width="1200px"
      top="20px"
      :on-confirm="submitContent"
      :confirm-loading="submitting"
    >
      <t-form ref="contentFormRef" :data="contentForm" :rules="contentRules" label-width="80px">
        <t-row :gutter="[16, 16]">
          <t-col :span="16">
            <t-form-item label="内容标题" name="title">
              <t-input v-model="contentForm.title" placeholder="请输入内容标题" />
            </t-form-item>
          </t-col>
          <t-col :span="8">
            <t-form-item label="接口开放">
              <t-radio-group v-model="contentForm.isOpen">
                <t-radio :value="1">开启</t-radio>
                <t-radio :value="0">关闭</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item label="文档内容" class="content-editor-item">
          <div class="editor-shell">
            <MdEditor v-model="contentForm.docContent" style="height: 450px" @onUploadImg="handleUploadImg" />
          </div>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import { Icon as TIcon } from 'tdesign-icons-vue-next'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import InterfaceConfigDialog from './components/InterfaceConfigDialog.vue'
import {
  addCategory,
  batchPublishCategories,
  batchWithdrawCategories,
  deleteCategory,
  getCategoryDetail,
  getCategoryTree,
  publishCategory,
  updateCategory,
  updateCategoryContent,
  updateCategoryOpen,
  uploadImage,
  withdrawCategory
} from '@/api/content-category'
import {
  addColumn,
  batchPublishColumns,
  batchWithdrawColumns,
  deleteColumn,
  listColumns,
  publishColumn,
  updateColumn,
  withdrawColumn
} from '@/api/platform-column'
import {
  addPlatform,
  batchPublishPlatforms,
  batchWithdrawPlatforms,
  deletePlatform,
  listPlatforms,
  publishPlatform,
  updatePlatform,
  withdrawPlatform
} from '@/api/platform'

const platformColumns = [
  { colKey: 'name', title: '名称', ellipsis: true, cell: 'name' },
  { colKey: 'isPublished', title: '状态', width: 88, align: 'center', cell: 'isPublished' },
  { colKey: 'operation', title: '操作', width: 120, align: 'center', cell: 'operation' }
]

const columnColumns = [
  { colKey: 'name', title: '名称', ellipsis: true, cell: 'name' },
  { colKey: 'isPublished', title: '状态', width: 88, align: 'center', cell: 'isPublished' },
  { colKey: 'operation', title: '操作', width: 120, align: 'center', cell: 'operation' }
]

const categoryColumns = [
  { colKey: 'name', title: '分类名称', minWidth: 220, ellipsis: true, cell: 'name' },
  { colKey: 'type', title: '类型', width: 76, align: 'center', cell: 'type' },
  { colKey: 'title', title: '标题', minWidth: 180, ellipsis: true, cell: 'title' },
  { colKey: 'hasContent', title: '正文', width: 68, align: 'center', cell: 'hasContent' },
  { colKey: 'isPublished', title: '发布', width: 88, align: 'center', cell: 'isPublished' },
  { colKey: 'isOpen', title: '开放', width: 76, align: 'center', cell: 'isOpen' },
  { colKey: 'updateTime', title: '修改时间', width: 148, align: 'center', cell: 'updateTime' },
  { colKey: 'operation', title: '操作', width: 220, align: 'center', cell: 'operation' }
]

const platformList = ref([])
const columnList = ref([])
const categoryTree = ref([])
const flatCategoryList = ref([])
const selectedPlatform = ref(null)
const selectedColumn = ref(null)
const activeRowKeysPlatform = ref([])
const activeRowKeysColumn = ref([])

const platformLoading = ref(false)
const columnLoading = ref(false)
const categoryLoading = ref(false)
const submitting = ref(false)
const categoryExpanded = ref(false)

const platformDialog = reactive({ visible: false, title: '' })
const platformForm = reactive({ id: null, name: '', sort: 0, description: '' })
const platformFormRef = ref(null)

const columnDialog = reactive({ visible: false, title: '' })
const columnForm = reactive({ id: null, platformId: null, name: '', sort: 0 })
const columnFormRef = ref(null)

const categoryDialog = reactive({ visible: false, title: '' })
const categoryForm = reactive({ id: null, name: '', columnId: null, parentId: 0, sort: 0, type: 'content' })
const categoryFormRef = ref(null)
const parentCategoryName = ref('')

const contentDialog = reactive({ visible: false })
const contentForm = reactive({
  id: null,
  name: '',
  title: '',
  docContent: '',
  isOpen: 1
})
const contentFormRef = ref(null)

const interfaceConfigDialogVisible = ref(false)
const selectedCategoryId = ref(null)
const selectedCategoryName = ref('')

const requiredNameRule = (label) => ([
  { required: true, message: `请输入${label}`, type: 'error' },
  { validator: (value) => String(value ?? '').trim().length > 0, message: `${label}不能为空格`, type: 'warning' },
  { max: 100, message: `${label}不能超过100个字符`, type: 'warning' }
])

const sortRule = [
  { validator: (value) => Number.isInteger(Number(value)) && Number(value) >= 0 && Number(value) <= 9999, message: '排序必须为0到9999之间的整数', type: 'warning' }
]

const platformRules = { name: requiredNameRule('平台名称'), sort: sortRule }
const columnRules = { name: requiredNameRule('栏目名称'), sort: sortRule }
const categoryRules = { name: requiredNameRule('分类名称'), sort: sortRule }
const contentRules = {
  title: [{ max: 255, message: '内容标题不能超过255个字符', type: 'warning' }]
}

const normalizeText = (value) => {
  if (typeof value !== 'string') {
    return value
  }
  return value.trim()
}

const normalizePlatformPayload = () => ({
  ...platformForm,
  name: normalizeText(platformForm.name),
  description: normalizeText(platformForm.description)
})

const hasDuplicatePlatformName = (name, id) => platformList.value.some((item) =>
  normalizeText(item.name) === name && item.id !== id
)

const normalizeColumnPayload = () => ({
  ...columnForm,
  name: normalizeText(columnForm.name)
})

const hasDuplicateColumnName = (name, id) => columnList.value.some((item) =>
  normalizeText(item.name) === name && item.id !== id
)

const normalizeCategoryPayload = () => ({
  ...categoryForm,
  name: normalizeText(categoryForm.name),
  type: categoryForm.type === 'api' ? 'api' : 'content'
})

const normalizeCategoryParentId = (parentId) => Number(parentId || 0)
const normalizeComparableCategoryName = (name) => String(normalizeText(name) ?? '').toLocaleLowerCase()

const hasDuplicateCategoryName = (payload) => flatCategoryList.value.some((item) =>
  item.id !== payload.id
  && Number(item.columnId) === Number(payload.columnId)
  && normalizeCategoryParentId(item.parentId) === normalizeCategoryParentId(payload.parentId)
  && normalizeComparableCategoryName(item.name) === normalizeComparableCategoryName(payload.name)
)

const normalizeContentPayload = () => ({
  title: normalizeText(contentForm.title),
  docContent: contentForm.docContent,
  isOpen: contentForm.isOpen === 1 ? 1 : 0
})

const getPublishStatusTheme = (status) => {
  const map = { 0: 'default', 1: 'success', 2: 'warning' }
  return map[status] || 'default'
}

const getPublishStatusLabel = (status) => {
  const map = { 0: '草稿', 1: '已发布', 2: '已撤回' }
  return map[status] || '未知'
}

const formatDate = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const flattenTree = (tree, level = 0) => {
  const result = []
  tree.forEach((node) => {
    const hasChildren = Array.isArray(node.children) && node.children.length > 0
    result.push({ ...node, _level: level, _hasChildren: hasChildren })
    if (hasChildren) {
      result.push(...flattenTree(node.children, level + 1))
    }
  })
  return result
}

const fetchPlatforms = async () => {
  platformLoading.value = true
  try {
    const res = await listPlatforms()
    if (res.code === 200) {
      platformList.value = res.data || []
    }
  } finally {
    platformLoading.value = false
  }
}

const fetchColumns = async (platformId) => {
  if (!platformId) {
    columnList.value = []
    return
  }
  columnLoading.value = true
  try {
    const res = await listColumns({ platformId })
    if (res.code === 200) {
      columnList.value = res.data || []
    }
  } finally {
    columnLoading.value = false
  }
}

const fetchCategoryTree = async (columnId) => {
  if (!columnId) {
    categoryTree.value = []
    flatCategoryList.value = []
    return
  }
  categoryLoading.value = true
  try {
    const res = await getCategoryTree({ columnId })
    if (res.code === 200) {
      categoryTree.value = res.data || []
      flatCategoryList.value = flattenTree(categoryTree.value)
    }
  } finally {
    categoryLoading.value = false
  }
}

const refreshCategoryTree = async () => {
  if (selectedColumn.value?.id) {
    await fetchCategoryTree(selectedColumn.value.id)
  }
}

const confirmBatchAction = (config) => {
  const dialog = DialogPlugin.confirm({
    header: config.header,
    body: config.body,
    theme: config.theme,
    onConfirm: async () => {
      const res = await config.action()
      if (res.code === 200) {
        MessagePlugin.success(config.success(res))
        await config.afterSuccess?.()
      }
      dialog.hide()
    }
  })
}

const handlePlatformClick = ({ row }) => {
  selectedPlatform.value = row
  activeRowKeysPlatform.value = [row.id]
  selectedColumn.value = null
  activeRowKeysColumn.value = []
  columnList.value = []
  categoryTree.value = []
  flatCategoryList.value = []
  fetchColumns(row.id)
}

const handleColumnClick = ({ row }) => {
  selectedColumn.value = row
  activeRowKeysColumn.value = [row.id]
  categoryTree.value = []
  flatCategoryList.value = []
  fetchCategoryTree(row.id)
}

const handleAddPlatform = () => {
  platformDialog.title = '新增平台'
  Object.assign(platformForm, { id: null, name: '', sort: 0, description: '' })
  platformDialog.visible = true
}

const handleEditPlatform = (row) => {
  platformDialog.title = '编辑平台'
  Object.assign(platformForm, { ...row })
  platformDialog.visible = true
}

const handleAddColumn = () => {
  columnDialog.title = '新增栏目'
  Object.assign(columnForm, { id: null, platformId: selectedPlatform.value.id, name: '', sort: 0 })
  columnDialog.visible = true
}

const handleEditColumn = (row) => {
  columnDialog.title = '编辑栏目'
  Object.assign(columnForm, { ...row })
  columnDialog.visible = true
}

const handleAddRootCategory = () => {
  categoryDialog.title = '新增根分类'
  parentCategoryName.value = ''
  Object.assign(categoryForm, { id: null, columnId: selectedColumn.value.id, parentId: 0, name: '', sort: 0, type: 'content' })
  categoryDialog.visible = true
}

const handleAddChildCategory = (row) => {
  categoryDialog.title = '新增子分类'
  parentCategoryName.value = row.name
  Object.assign(categoryForm, { id: null, columnId: row.columnId, parentId: row.id, name: '', sort: 0, type: 'content' })
  categoryDialog.visible = true
}

const handleEditCategory = (row) => {
  categoryDialog.title = '编辑分类'
  parentCategoryName.value = ''
  Object.assign(categoryForm, { ...row })
  categoryDialog.visible = true
}

const submitPlatform = async () => {
  if (submitting.value) return
  const result = await platformFormRef.value.validate()
  if (result !== true) return

  const payload = normalizePlatformPayload()
  platformForm.name = payload.name
  if (hasDuplicatePlatformName(payload.name, payload.id)) {
    MessagePlugin.warning(`平台名称“${payload.name}”已存在，请更换后再保存`)
    return
  }

  submitting.value = true
  try {
    const action = payload.id ? updatePlatform : addPlatform
    const res = await action(payload)
    if (res.code === 200) {
      MessagePlugin.success('操作成功')
      platformDialog.visible = false
      await fetchPlatforms()
    } else {
      MessagePlugin.warning(res.message || '平台保存失败，请检查后重试')
    }
  } finally {
    submitting.value = false
  }
}

const submitColumn = async () => {
  if (submitting.value) return
  const result = await columnFormRef.value.validate()
  if (result !== true) return

  const payload = normalizeColumnPayload()
  columnForm.name = payload.name
  if (hasDuplicateColumnName(payload.name, payload.id)) {
    MessagePlugin.warning(`栏目名称“${payload.name}”已存在，请更换后再保存`)
    return
  }

  submitting.value = true
  try {
    const action = payload.id ? updateColumn : addColumn
    const res = await action(payload)
    if (res.code === 200) {
      MessagePlugin.success('操作成功')
      columnDialog.visible = false
      await fetchColumns(selectedPlatform.value.id)
    } else {
      MessagePlugin.warning(res.message || '栏目保存失败，请检查后重试')
    }
  } finally {
    submitting.value = false
  }
}

const submitCategory = async () => {
  if (submitting.value) return
  const result = await categoryFormRef.value.validate()
  if (result !== true) return

  const payload = normalizeCategoryPayload()
  categoryForm.name = payload.name
  if (hasDuplicateCategoryName(payload)) {
    MessagePlugin.warning(`当前层级已存在分类“${payload.name}”，接口分类和内容分类不能同名，请更换后再保存`)
    return
  }

  submitting.value = true
  try {
    const action = payload.id ? updateCategory : addCategory
    const res = await action(payload)
    if (res.code === 200) {
      MessagePlugin.success('操作成功')
      categoryDialog.visible = false
      await refreshCategoryTree()
    } else {
      MessagePlugin.warning(res.message || '分类保存失败，请检查后重试')
    }
  } catch (error) {
    console.error('Failed to save category', error)
    MessagePlugin.error(error?.message || '分类保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleEditContent = async (row) => {
  const res = await getCategoryDetail(row.id)
  if (res.code === 200) {
    Object.assign(contentForm, {
      id: res.data.id,
      name: res.data.name,
      title: res.data.title || '',
      docContent: res.data.docContent || '',
      isOpen: res.data.isOpen ?? 1
    })
    contentDialog.visible = true
  }
}

const submitContent = async () => {
  if (submitting.value) return
  const result = await contentFormRef.value.validate()
  if (result !== true) return

  const payload = normalizeContentPayload()
  contentForm.title = payload.title || ''
  submitting.value = true
  try {
    const res = await updateCategoryContent(contentForm.id, payload)
    if (res.code === 200) {
      MessagePlugin.success('内容保存成功')
      contentDialog.visible = false
      await refreshCategoryTree()
    }
  } finally {
    submitting.value = false
  }
}

const handleDeletePlatform = (row) => {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body: `确定删除平台“${row.name}”吗？`,
    onConfirm: async () => {
      const res = await deletePlatform(row.id)
      if (res.code === 200) {
        MessagePlugin.success('删除成功')
        if (selectedPlatform.value?.id === row.id) {
          selectedPlatform.value = null
          selectedColumn.value = null
          activeRowKeysPlatform.value = []
          activeRowKeysColumn.value = []
          columnList.value = []
          categoryTree.value = []
          flatCategoryList.value = []
        }
        await fetchPlatforms()
      } else {
        MessagePlugin.error(res.message || '删除失败')
      }
      dialog.hide()
    }
  })
}

const handleDeleteColumn = (row) => {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body: `确定删除栏目“${row.name}”吗？`,
    onConfirm: async () => {
      const res = await deleteColumn(row.id)
      if (res.code === 200) {
        MessagePlugin.success('删除成功')
        if (selectedColumn.value?.id === row.id) {
          selectedColumn.value = null
          categoryTree.value = []
          flatCategoryList.value = []
        }
        await fetchColumns(selectedPlatform.value.id)
      } else {
        MessagePlugin.error(res.message || '删除失败')
      }
      dialog.hide()
    }
  })
}

const handleDeleteCategory = (row) => {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body: `确定删除分类“${row.name}”吗？`,
    onConfirm: async () => {
      const res = await deleteCategory(row.id)
      if (res.code === 200) {
        MessagePlugin.success('删除成功')
        await refreshCategoryTree()
      } else {
        MessagePlugin.error(res.message || '删除失败')
      }
      dialog.hide()
    }
  })
}

const handlePublishPlatform = (row) => {
  confirmBatchAction({
    header: '确认发布',
    body: `确定发布平台“${row.name}”吗？`,
    action: () => publishPlatform(row.id),
    success: () => '发布成功',
    afterSuccess: fetchPlatforms
  })
}

const handleWithdrawPlatform = (row) => {
  confirmBatchAction({
    header: '确认撤回',
    body: `确定撤回平台“${row.name}”吗？`,
    theme: 'warning',
    action: () => withdrawPlatform(row.id),
    success: () => '撤回成功',
    afterSuccess: fetchPlatforms
  })
}

const handlePublishColumn = (row) => {
  confirmBatchAction({
    header: '确认发布',
    body: `确定发布栏目“${row.name}”吗？`,
    action: () => publishColumn(row.id),
    success: () => '发布成功',
    afterSuccess: () => fetchColumns(selectedPlatform.value.id)
  })
}

const handleWithdrawColumn = (row) => {
  confirmBatchAction({
    header: '确认撤回',
    body: `确定撤回栏目“${row.name}”吗？`,
    theme: 'warning',
    action: () => withdrawColumn(row.id),
    success: () => '撤回成功',
    afterSuccess: () => fetchColumns(selectedPlatform.value.id)
  })
}

const handlePublish = (row) => {
  confirmBatchAction({
    header: '确认发布',
    body: `确定发布分类“${row.name}”的内容吗？`,
    action: () => publishCategory(row.id),
    success: () => '发布成功',
    afterSuccess: refreshCategoryTree
  })
}

const handleWithdraw = (row) => {
  confirmBatchAction({
    header: '确认撤回',
    body: `确定撤回分类“${row.name}”的内容吗？`,
    theme: 'warning',
    action: () => withdrawCategory(row.id),
    success: () => '撤回成功',
    afterSuccess: refreshCategoryTree
  })
}

const handlePublishAllPlatforms = () => {
  const ids = platformList.value.filter((item) => item.isPublished !== 1).map((item) => item.id)
  if (!ids.length) {
    MessagePlugin.info('没有需要发布的平台')
    return
  }
  confirmBatchAction({
    header: '确认全部发布',
    body: `确定发布全部 ${ids.length} 个平台吗？`,
    action: () => batchPublishPlatforms(ids),
    success: (res) => `成功发布 ${res.data} 个平台`,
    afterSuccess: fetchPlatforms
  })
}

const handleWithdrawAllPlatforms = () => {
  const ids = platformList.value.filter((item) => item.isPublished === 1).map((item) => item.id)
  if (!ids.length) {
    MessagePlugin.info('没有需要撤回的平台')
    return
  }
  confirmBatchAction({
    header: '确认全部撤回',
    body: `确定撤回全部 ${ids.length} 个平台吗？`,
    theme: 'warning',
    action: () => batchWithdrawPlatforms(ids),
    success: (res) => `成功撤回 ${res.data} 个平台`,
    afterSuccess: fetchPlatforms
  })
}

const handlePublishAllColumns = () => {
  const ids = columnList.value.filter((item) => item.isPublished !== 1).map((item) => item.id)
  if (!ids.length) {
    MessagePlugin.info('没有需要发布的栏目')
    return
  }
  confirmBatchAction({
    header: '确认全部发布',
    body: `确定发布全部 ${ids.length} 个栏目吗？`,
    action: () => batchPublishColumns(ids),
    success: (res) => `成功发布 ${res.data} 个栏目`,
    afterSuccess: () => fetchColumns(selectedPlatform.value.id)
  })
}

const handleWithdrawAllColumns = () => {
  const ids = columnList.value.filter((item) => item.isPublished === 1).map((item) => item.id)
  if (!ids.length) {
    MessagePlugin.info('没有需要撤回的栏目')
    return
  }
  confirmBatchAction({
    header: '确认全部撤回',
    body: `确定撤回全部 ${ids.length} 个栏目吗？`,
    theme: 'warning',
    action: () => batchWithdrawColumns(ids),
    success: (res) => `成功撤回 ${res.data} 个栏目`,
    afterSuccess: () => fetchColumns(selectedPlatform.value.id)
  })
}

const handlePublishAllCategories = () => {
  const ids = flatCategoryList.value.filter((item) => item.isPublished !== 1).map((item) => item.id)
  if (!ids.length) {
    MessagePlugin.info('没有需要发布的分类')
    return
  }
  confirmBatchAction({
    header: '确认全部发布',
    body: `确定发布全部 ${ids.length} 个分类吗？`,
    action: () => batchPublishCategories(ids),
    success: (res) => `成功发布 ${res.data} 个分类`,
    afterSuccess: refreshCategoryTree
  })
}

const handleWithdrawAllCategories = () => {
  const ids = flatCategoryList.value.filter((item) => item.isPublished === 1).map((item) => item.id)
  if (!ids.length) {
    MessagePlugin.info('没有需要撤回的分类')
    return
  }
  confirmBatchAction({
    header: '确认全部撤回',
    body: `确定撤回全部 ${ids.length} 个分类吗？`,
    theme: 'warning',
    action: () => batchWithdrawCategories(ids),
    success: (res) => `成功撤回 ${res.data} 个分类`,
    afterSuccess: refreshCategoryTree
  })
}

const handleOpenChange = async (row, value) => {
  const isOpen = value ? 1 : 0
  const res = await updateCategoryOpen(row.id, isOpen)
  if (res.code === 200) {
    MessagePlugin.success(isOpen === 1 ? '接口已开启，类型已切换为接口' : '接口已关闭，类型已切换为内容')
    await refreshCategoryTree()
  }
}

const handleConfigInterface = (row) => {
  selectedCategoryId.value = row.id
  selectedCategoryName.value = row.name
  interfaceConfigDialogVisible.value = true
}

const handleConfigSuccess = async () => {
  MessagePlugin.success('接口配置已保存')
  await refreshCategoryTree()
}

const handleUploadImg = async (files, callback) => {
  const urls = await Promise.all(files.map(async (file) => {
    try {
      const res = await uploadImage(file)
      if (res.code === 200 && res.data?.url) {
        return res.data.url
      }
      MessagePlugin.error(res.message || '图片上传失败')
      return null
    } catch (error) {
      console.error('Upload image failed:', error)
      MessagePlugin.error('图片上传失败')
      return null
    }
  }))

  const validUrls = urls.filter(Boolean)
  if (validUrls.length) {
    callback(validUrls)
  }
}

onMounted(() => {
  fetchPlatforms()
})
</script>

<style scoped>
.category-manage {
  margin: 16px;
  overflow-x: hidden;
}

.manage-grid {
  align-items: stretch;
}

.panel-col {
  display: flex;
}

.panel-platform,
.panel-column {
  min-width: 0;
}

.panel-category {
  min-width: 0;
}

.category-card {
  width: 100%;
  height: 600px;
}

.category-card :deep(.t-card__body) {
  padding: 0;
  height: calc(600px - 48px);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.category-card-auto {
  height: auto;
}

.category-card-auto :deep(.t-card__body) {
  height: auto;
  min-height: 300px;
}

.card-table-wrap {
  width: 100%;
  min-width: 0;
  overflow: auto;
}

.category-table-wrap {
  overflow-x: auto;
}

.category-expanded .category-card-auto {
  min-height: calc(100vh - 112px);
}

.category-expanded .category-card-auto :deep(.t-card__body) {
  min-height: calc(100vh - 160px);
}

.category-expanded .category-table-wrap {
  max-height: calc(100vh - 176px);
}

.action-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
}

.tree-icon {
  flex-shrink: 0;
}

.folder-icon {
  color: #0052d9;
}

.api-icon {
  color: #1890ff;
}

.file-icon {
  color: #888;
}

.ellipsis-text {
  display: inline-block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-text {
  font-size: 12px;
  color: #666;
}

.status-icon {
  font-size: 16px;
}

.success-icon {
  color: #2ba471;
}

.empty-icon {
  color: #ccc;
}

.content-editor-item :deep(.t-form__controls-content) {
  width: 100%;
}

.editor-shell {
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  overflow: hidden;
}

.category-card :deep(.t-empty) {
  margin: auto;
}

@media (max-width: 1536px) {
  .category-card {
    height: 560px;
  }

  .category-card :deep(.t-card__body) {
    height: calc(560px - 48px);
  }
}

@media (max-width: 1366px) {
  .panel-platform,
  .panel-column {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .panel-category {
    flex: 0 0 100%;
    max-width: 100%;
  }
}
</style>
