<template>
  <div class="user-center-page">
    <div class="user-container">
      <t-breadcrumb separator="/" class="breadcrumb">
        <t-breadcrumb-item to="/">首页</t-breadcrumb-item>
        <t-breadcrumb-item>个人中心</t-breadcrumb-item>
      </t-breadcrumb>

      <t-row :gutter="24">
        <t-col :span="3">
          <t-card :bordered="false" class="user-profile-card shadow-card">
            <div class="user-info-section">
              <t-avatar size="100px" :image="avatarSrc" :hide-on-load-failed="false">
                {{ avatarFallback }}
              </t-avatar>
              <h3 class="username">{{ userStore.username }}</h3>
              <t-divider />
              <div class="user-stats">
                <div class="stat-item">
                  <div class="stat-value">{{ approvedColumnCount }}</div>
                  <div class="stat-label">已开通接口</div>
                </div>
              </div>
            </div>
          </t-card>
        </t-col>

        <t-col :span="9">
          <t-card :bordered="false" class="interfaces-card shadow-card">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <t-icon name="app" size="20px" />
                  <span>我的授权接口</span>
                </div>
              </div>
            </template>

            <t-enhanced-table
              row-key="id"
              :data="authorizedInterfaces"
              :columns="interfaceColumns"
              :loading="loading"
              hover
              bordered
              :tree="{ childrenKey: 'children', treeNodeColumnIndex: 0, defaultExpandAll: true }"
            >
              <template #name="{ row }">
                <t-link
                  v-if="row.type === 'api'"
                  theme="primary"
                  hover="underline"
                  @click="openInterfaceDetail(row)"
                >
                  {{ row.name }}
                </t-link>
                <span v-else>{{ row.name }}</span>
              </template>

              <template #operation="{ row }">
                <t-button
                  v-if="row.isAuthorized && row.hasCredential"
                  theme="primary"
                  variant="text"
                  @click="handleViewCredential(row)"
                >
                  查看凭证
                </t-button>
                <span
                  v-else-if="row.isAuthorized && !row.hasCredential"
                  class="no-credential-text"
                >
                  未分配
                </span>
                <t-button
                  v-if="row.isAuthorized && row.type === 'api'"
                  theme="warning"
                  variant="text"
                  @click="handleSandboxClick(row.id, row)"
                >
                  沙箱测试
                </t-button>
              </template>
            </t-enhanced-table>
          </t-card>
        </t-col>
      </t-row>

      <t-dialog
        v-model:visible="credentialDialogVisible"
        :header="`接口凭证 - ${currentViewAppName}`"
        width="480px"
        :footer="false"
      >
        <div v-if="currentViewCredential && Object.keys(currentViewCredential).length > 0" class="credential-detail">
          <div v-for="(value, label) in currentViewCredential" :key="label" class="credential-row">
            <div class="credential-label">{{ label }}</div>
            <div class="credential-value-row">
              <t-textarea
                v-if="label === '说明'"
                :model-value="value"
                :readonly="true"
                :autosize="{ minRows: 2, maxRows: 6 }"
                class="credential-textarea"
              />
              <template v-else>
                <div class="credential-value">{{ value }}</div>
                <t-button theme="primary" variant="outline" size="small" class="copy-btn" @click="copyToClipboard(value)">
                  复制
                </t-button>
              </template>
            </div>
          </div>
        </div>
        <div v-else class="credential-empty">
          <t-icon name="info-circle-filled" size="24px" />
          <span>暂无凭证信息</span>
        </div>
      </t-dialog>

      <SandboxTestDialog
        v-model="sandboxDialogVisible"
        :category-id="selectedCategoryId"
        :api-key="selectedApiKey"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { Icon as TIcon } from 'tdesign-icons-vue-next'
import dayjs from 'dayjs'
import { getMyApplications } from '@/api/application'
import { getCategoryTree } from '@/api/content-category'
import { getMyCredentials } from '@/api/apiCredential'
import SandboxTestDialog from '@/views/components/SandboxTestDialog.vue'
import { useUserStore } from '@/stores/user'
import { copyWithFeedback } from '@/utils/clipboard'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const applications = ref([])
const authorizedInterfaces = ref([])
const credentialDialogVisible = ref(false)
const sandboxDialogVisible = ref(false)
const selectedCategoryId = ref(null)
const selectedApiKey = ref('')
const currentViewCredential = ref(null)
const currentViewAppName = ref('')
const defaultAvatar = `${import.meta.env.BASE_URL}images/touxiang.png`

const avatarSrc = computed(() => {
  const candidates = [
    userStore.userInfo?.avatar,
    userStore.userInfo?.avatarUrl,
    userStore.userInfo?.headImg,
    userStore.userInfo?.headImgUrl,
    defaultAvatar
  ]
  return candidates.find((item) => typeof item === 'string' && item.trim()) || defaultAvatar
})

const avatarFallback = computed(() => userStore.username?.charAt(0) || 'U')

const interfaceColumns = [
  { colKey: 'name', title: '接口名称', width: 320, cell: 'name' },
  { colKey: 'platformName', title: '所属平台', width: 150 },
  { colKey: 'columnName', title: '所属栏目', width: 150 },
  { colKey: 'operation', title: '操作', width: 160, fixed: 'right', cell: 'operation', align: 'center' }
]

const approvedColumnCount = computed(() => {
  let count = 0
  applications.value
    .filter((app) => app.status === 1)
    .forEach((app) => {
      if (app.catIds && Array.isArray(app.catIds)) {
        count += app.catIds.length
      }
    })
  return count
})

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getMyApplications()
    const list = Array.isArray(res.data) ? res.data : []
    applications.value = list.map((item) => {
      let catIds = []
      try {
        catIds = JSON.parse(item.categoryIds || '[]')
      } catch (error) {
        console.error('解析 categoryIds 失败', error)
      }
      return {
        ...item,
        catIds,
        applyTime: item.applyTime ? dayjs(item.applyTime).format('YYYY-MM-DD HH:mm:ss') : '-',
        auditTime: item.auditTime ? dayjs(item.auditTime).format('YYYY-MM-DD HH:mm:ss') : undefined
      }
    })
    await loadAuthorizedInterfaces()
  } catch (error) {
    console.error('加载授权列表失败:', error)
    MessagePlugin.error('加载授权列表失败')
  } finally {
    loading.value = false
  }
}

const loadAuthorizedInterfaces = async () => {
  if (applications.value.length === 0) {
    authorizedInterfaces.value = []
    return
  }

  const catIdToAppKey = {}
  const catIdToAppId = {}
  const columnInfoMap = {}
  applications.value.forEach((app) => {
    const colId = Number(app.columnId)
    if (!columnInfoMap[colId]) {
      columnInfoMap[colId] = {
        columnName: app.columnName,
        platformName: app.platformName,
        platformId: app.platformId,
        columnId: colId
      }
    }
    if (app.status === 1 && Array.isArray(app.catIds)) {
      app.catIds.forEach((id) => {
        const numericId = Number(id)
        catIdToAppKey[numericId] = app.appKey
        catIdToAppId[numericId] = app.id
      })
    }
  })

  const credentialsMap = {}
  for (const app of applications.value.filter((item) => item.status === 1)) {
    try {
      const credRes = await getMyCredentials(app.id)
      if (credRes.code === 200 && Array.isArray(credRes.data)) {
        credRes.data.forEach((cred) => {
          credentialsMap[cred.categoryId] = cred
        })
      }
    } catch (error) {
      console.error(`加载授权记录 ${app.id} 的凭证失败`, error)
    }
  }

  const result = []
  for (const colId of Object.keys(columnInfoMap)) {
    try {
      const res = await getCategoryTree({ columnId: colId })
      if (res.code !== 200 || !Array.isArray(res.data)) {
        continue
      }
      const columnInfo = columnInfoMap[colId]
      const filteredTree = filterAuthorizedTree(
        res.data,
        credentialsMap,
        columnInfo,
        catIdToAppKey,
        catIdToAppId,
        null,
        null
      )
      result.push(...filteredTree)
    } catch (error) {
      console.error(`加载栏目 ${colId} 的接口树失败`, error)
    }
  }
  authorizedInterfaces.value = result
}

const filterAuthorizedTree = (nodes, credentialsMap, columnInfo, appKeyMap, appIdMap, inheritedAppKey, inheritedAppId) => {
  const result = []
  for (const node of nodes) {
    const nodeId = Number(node.id)
    const currentAppKey = appKeyMap[nodeId] || inheritedAppKey
    const currentAppId = appIdMap[nodeId] || inheritedAppId
    const isAuthorized = Boolean(currentAppKey)
    const children = Array.isArray(node.children)
      ? filterAuthorizedTree(node.children, credentialsMap, columnInfo, appKeyMap, appIdMap, currentAppKey, currentAppId)
      : []

    const keepNode = (node.type === 'api' && isAuthorized) || children.length > 0
    if (!keepNode) {
      continue
    }

    result.push({
      ...node,
      isAuthorized,
      hasCredential: Boolean(credentialsMap[nodeId]),
      appKey: currentAppKey,
      applicationId: currentAppId,
      columnName: columnInfo.columnName,
      platformName: columnInfo.platformName,
      platformId: columnInfo.platformId,
      columnId: columnInfo.columnId,
      children: children.length > 0 ? children : undefined
    })
  }
  return result
}

const handleSandboxClick = (categoryId, row) => {
  selectedCategoryId.value = Number(categoryId)
  selectedApiKey.value = row.appKey || ''
  sandboxDialogVisible.value = true
}

const handleViewCredential = async (row) => {
  try {
    const res = await getMyCredentials(row.applicationId)
    if (res.code !== 200 || !Array.isArray(res.data)) {
      MessagePlugin.warning('获取凭证失败')
      return
    }
    const cred = res.data.find((item) => item.categoryId === Number(row.id))
    if (!cred) {
      MessagePlugin.warning('该接口尚未分配凭证')
      return
    }
    currentViewCredential.value = Object.fromEntries(
      Object.entries({
        'Open ID': cred.openId,
        'Client ID': cred.clientId,
        Token: cred.token,
        'Secret Key': cred.secretKey,
        '说明': cred.description
      }).filter(([, value]) => value && String(value).trim())
    )
    currentViewAppName.value = row.name
    credentialDialogVisible.value = true
  } catch (error) {
    console.error('获取凭证失败', error)
    MessagePlugin.error('获取凭证失败')
  }
}

const openInterfaceDetail = (row) => {
  const detailUrl = router.resolve({
    path: '/contents',
    query: {
      platformId: row.platformId,
      columnId: row.columnId,
      categoryId: row.id,
    },
  }).href
  window.open(detailUrl, '_blank')
}

const copyToClipboard = (text) => copyWithFeedback(text)

onMounted(() => {
  loadApplications()
})
</script>

<style lang="less" scoped>
.user-center-page {
  background-color: #f5f7fa;
  min-height: calc(100vh - 80px);
  padding: 32px 0;

  .user-container {
    max-width: 1440px;
    margin: 0 auto;
    padding: 0 40px;
  }

  .breadcrumb {
    margin-bottom: 24px;
  }
}

.shadow-card {
  box-shadow: 0 1px 10px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
}

.user-profile-card {
  .user-info-section {
    padding: 16px 8px;
    text-align: center;

    .username {
      margin-top: 16px;
      font-size: 20px;
      font-weight: 600;
      color: var(--td-text-color-primary);
    }
  }

  .user-stats {
    display: flex;
    justify-content: space-around;
    padding: 16px 0 8px;

    .stat-item {
      text-align: center;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: var(--td-text-color-primary);
    }

    .stat-label {
      font-size: 12px;
      color: var(--td-text-color-secondary);
      margin-top: 4px;
    }
  }
}

.interfaces-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    span {
      font-size: 18px;
      font-weight: 600;
      line-height: 1.4;
    }
  }
}

.no-credential-text {
  color: var(--td-text-color-secondary);
  font-size: 14px;
}

.credential-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}

.credential-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.credential-label {
  font-size: 13px;
  color: var(--td-text-color-secondary);
  font-weight: 500;
}

.credential-value-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.credential-value {
  flex: 1;
  font-size: 14px;
  color: var(--td-text-color-primary);
  word-break: break-all;
  padding: 6px 10px;
  background-color: var(--td-bg-color-container-hover);
  border-radius: 4px;
  font-family: monospace;
}

.credential-textarea {
  flex: 1;

  :deep(.t-textarea__inner) {
    background-color: var(--td-bg-color-container-hover);
    font-family: monospace;
    resize: none;
  }
}

.copy-btn {
  flex-shrink: 0;
}

.credential-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--td-text-color-secondary);
  font-size: 14px;
}
</style>
