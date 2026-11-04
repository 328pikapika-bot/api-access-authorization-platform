<template>
  <div class="content-index-page">
    <div class="header-column-bar">
      <div class="column-content">
        <t-space break-line size="8px">
          <t-button
            v-for="column in platformStore.columns"
            :key="column.id"
            :theme="platformStore.currentColumnId === column.id ? 'primary' : 'default'"
            :variant="platformStore.currentColumnId === column.id ? 'base' : 'text'"
            @click="handleColumnClick(column)"
          >
            {{ column.name }}
          </t-button>
        </t-space>
        <span v-if="platformStore.columns.length === 0" class="empty-column-hint">
          {{ authorizationEmptyDescription }}
        </span>
      </div>
    </div>

    <div class="main-container">
      <div class="category-sidebar">
        <div class="sidebar-content">
          <t-loading v-if="categoryLoading" size="small" />
          <div v-else class="custom-tree">
            <tree-node
              v-for="node in categories"
              :key="node.id"
              :node="node"
              :active-ids="activeIds"
              :default-expanded-ids="defaultExpandedIds"
              @select="handleNodeSelect"
            />
          </div>
          <t-empty
            v-if="!categoryLoading && categories.length === 0"
            :description="authorizationEmptyDescription"
            size="small"
          />
        </div>
      </div>

      <div class="content-body">
        <div v-if="contentLoading" class="content-loading">
          <t-loading />
        </div>

        <div v-else-if="hasValidContent" class="markdown-wrapper">
          <div v-if="currentContent.type === 'api'" class="steps-wizard">
            <div class="step-item">
              <div class="step-number step-active">1</div>
              <div class="step-content">
                <div class="step-title">阅读接口文档</div>
                <div class="step-desc">先了解接口能力与调用规范</div>
              </div>
            </div>
            <div class="step-arrow">
              <t-icon name="chevron-right" size="24px" />
            </div>
            <div class="step-item step-clickable" @click="handleSandboxTest">
              <div class="step-number step-clickable-num">2</div>
              <div class="step-content">
                <div class="step-title step-title-clickable">在线沙箱测试</div>
                <div class="step-desc">快速验证接口是否返回预期结果</div>
              </div>
            </div>
            <div class="step-arrow">
              <t-icon name="chevron-right" size="24px" />
            </div>
            <div class="step-item">
              <div class="step-number step-active">3</div>
              <div class="step-content">
                <div class="step-title">本地开发联调</div>
                <div class="step-desc">在本地系统中接入并完成开发验证</div>
              </div>
            </div>
            <div class="step-arrow">
              <t-icon name="chevron-right" size="24px" />
            </div>
            <div class="step-item step-clickable" @click="handleViewCredential">
              <div class="step-number step-clickable-num">4</div>
              <div class="step-content">
                <div class="step-title step-title-clickable">查看授权信息</div>
                <div class="step-desc">查看当前接口对应的凭证与接入说明</div>
              </div>
            </div>
          </div>

          <md-preview
            :model-value="currentContent.docContent"
            :editor-id="editorId"
            :key="contentRenderKey"
            preview-theme="github"
          />
        </div>

        <div v-else class="empty-content">
          <t-empty v-if="!selectedCategoryName" :description="authorizationEmptyDescription">
            <template #icon>
              <t-icon name="folder-open" size="48px" />
            </template>
          </t-empty>

          <t-empty v-else-if="!currentContent" :description="`【${selectedCategoryName}】暂无已发布内容`">
            <template #icon>
              <t-icon name="file-unknown" size="48px" />
            </template>
            <div class="empty-subtitle">该分类下暂未发布内容</div>
          </t-empty>

          <t-empty v-else :description="`【${selectedCategoryName}】内容为空`">
            <template #icon>
              <t-icon name="file-edit" size="48px" />
            </template>
            <div class="empty-subtitle">该分类已发布，但正文内容为空</div>
          </t-empty>
        </div>
      </div>

      <div class="catalog-sidebar">
        <div class="catalog-header">本页目录</div>
        <md-catalog :editor-id="editorId" :scroll-element="scrollElement" :key="catalogRenderKey" />
      </div>
    </div>

    <SandboxTestDialog
      v-model="sandboxDialogVisible"
      :category-id="selectedCategory?.id"
      :api-key="sandboxApiKey"
    />

    <CredentialDialog
      v-model="credentialDialogVisible"
      :title="selectedCategory?.name || '当前接口'"
      :credential="currentViewCredential"
    />
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { Icon as TIcon } from 'tdesign-icons-vue-next'
import { MdCatalog, MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { getMyApplications, getMyCredentials } from '@/api/application'
import { getCategoryTree } from '@/api/content-category'
import { getContentList } from '@/api/contents'
import SandboxTestDialog from '@/views/components/SandboxTestDialog.vue'
import CredentialDialog from '@/views/components/CredentialDialog.vue'
import { usePlatformStore } from '@/stores/platform'
import { useUserStore } from '@/stores/user'
import { ensurePortalSession } from '@/services/unifiedAuth'

const TreeNode = {
  name: 'TreeNode',
  props: {
    node: { type: Object, required: true },
    activeIds: { type: Array, default: () => [] },
    defaultExpandedIds: { type: Array, default: () => [] },
    level: { type: Number, default: 0 }
  },
  emits: ['select'],
  setup(props, { emit }) {
    const expanded = ref(props.defaultExpandedIds.includes(props.node.id))

    watch(
      () => props.defaultExpandedIds,
      (newIds) => {
        if (newIds.includes(props.node.id)) {
          expanded.value = true
        }
      },
      { deep: true }
    )

    const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)
    const isActive = computed(() => props.activeIds.includes(props.node.id))

    const toggleExpand = () => {
      if (hasChildren.value) {
        expanded.value = !expanded.value
      }
    }

    const handleSelect = () => emit('select', props.node)
    const handleArrowClick = (event) => {
      event.stopPropagation()
      toggleExpand()
      emit('select', props.node)
    }

    return {
      expanded,
      hasChildren,
      isActive,
      handleSelect,
      handleArrowClick
    }
  },
  render() {
    const arrowIcon = h(
      'svg',
      { viewBox: '0 0 24 24', width: '14', height: '14', fill: 'currentColor' },
      [h('path', { d: 'M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z' })]
    )

    const arrow = this.hasChildren
      ? h('span', {
          class: ['tree-node-arrow', { 'is-expanded': this.expanded }],
          onClick: this.handleArrowClick
        }, [arrowIcon])
      : h('span', { class: 'tree-node-arrow-placeholder' })

    const label = h('span', { class: 'tree-node-label' }, this.node.name)
    const content = h('div', {
      class: ['tree-node-content', `tree-node-level-${this.level}`, { 'is-active': this.isActive }],
      onClick: this.handleSelect
    }, [arrow, label])

    const children = this.hasChildren && this.expanded
      ? h('div', { class: 'tree-node-children' }, this.node.children.map((child) => h(TreeNode, {
          key: child.id,
          node: child,
          activeIds: this.activeIds,
          defaultExpandedIds: this.defaultExpandedIds,
          level: this.level + 1,
          onSelect: (node) => this.$emit('select', node)
        })))
      : null

    return h('div', { class: 'tree-node' }, [content, children])
  }
}

const platformStore = usePlatformStore()
const userStore = useUserStore()
const route = useRoute()
const editorId = computed(() => `preview-${selectedCategory.value?.id || currentContent.value?.id || currentContent.value?.categoryId || 'empty'}`)
const contentRenderKey = computed(() => `${selectedCategory.value?.id || 'empty'}-${currentContent.value?.id || currentContent.value?.categoryId || 'none'}`)
const catalogRenderKey = computed(() => `${editorId.value}-${contentRenderKey.value}`)
const scrollElement = '.content-body'

const categoryLoading = ref(false)
const contentLoading = ref(false)
const categories = ref([])
const currentContent = ref(null)
const activeIds = ref([])
const defaultExpandedIds = ref([])
const selectedCategory = ref(null)
const selectedCategoryName = ref('')

const sandboxDialogVisible = ref(false)
const sandboxApiKey = ref('')
const credentialDialogVisible = ref(false)
const currentViewCredential = ref(null)

const authorizedCategoryIds = ref(new Set())
const categoryToAppKey = ref(new Map())
const categoryToAppId = ref(new Map())

const hasValidContent = computed(() => {
  if (!currentContent.value) {
    return false
  }
  return String(currentContent.value.docContent ?? '').trim().length > 0
})

const authorizationEmptyDescription = computed(() =>
  userStore.isLoggedIn ? '当前账号暂无已分配的访问权限' : '请先登录后查看已授权内容'
)

const loadUserAuthorizedCategories = async () => {
  authorizedCategoryIds.value = new Set()
  categoryToAppKey.value = new Map()
  categoryToAppId.value = new Map()

  if (!localStorage.getItem('token')) {
    return
  }

  try {
    const res = await getMyApplications()
    if (res.code !== 200 || !Array.isArray(res.data)) {
      return
    }

    const ids = new Set()
    const appKeyMap = new Map()
    const appIdMap = new Map()

    res.data
      .filter((app) => app.status === 1 && app.validAssignment !== false)
      .forEach((app) => {
        try {
          const categoryIds = JSON.parse(app.categoryIds || '[]')
          categoryIds.forEach((id) => {
            const numericId = Number(id)
            ids.add(numericId)
            appKeyMap.set(numericId, app.appKey)
            appIdMap.set(numericId, app.id)
          })
        } catch (error) {
          console.error('Failed to parse authorized categoryIds', error)
        }
      })

    authorizedCategoryIds.value = ids
    categoryToAppKey.value = appKeyMap
    categoryToAppId.value = appIdMap
  } catch (error) {
    console.error('Failed to load user authorizations', error)
  }
}

const filterCategoryTree = (nodes, ancestorAuthorized = false) => {
  if (!Array.isArray(nodes)) {
    return []
  }

  return nodes.reduce((result, node) => {
    const branchAuthorized = ancestorAuthorized || authorizedCategoryIds.value.has(Number(node.id))
    const filteredChildren = filterCategoryTree(node.children || [], branchAuthorized)
    if (!branchAuthorized && filteredChildren.length === 0) {
      return result
    }

    result.push({
      ...node,
      children: filteredChildren.length > 0 ? filteredChildren : undefined
    })
    return result
  }, [])
}

const findNodePath = (nodes, targetId, path = []) => {
  for (const node of nodes) {
    const currentPath = [...path, node]
    if (Number(node.id) === Number(targetId)) {
      return currentPath
    }
    if (node.children?.length) {
      const match = findNodePath(node.children, targetId, currentPath)
      if (match) {
        return match
      }
    }
  }
  return null
}

const selectCategoryNode = async (node, expandedIds = []) => {
  if (!node) {
    selectedCategory.value = null
    selectedCategoryName.value = ''
    activeIds.value = []
    defaultExpandedIds.value = []
    currentContent.value = null
    return
  }

  selectedCategory.value = node
  selectedCategoryName.value = node.name || ''
  activeIds.value = [node.id]
  defaultExpandedIds.value = expandedIds
  currentContent.value = null
  await fetchContent(node.id, node.name)
}

const fetchCategories = async (columnId) => {
  if (!columnId) {
    return
  }

  categoryLoading.value = true
  try {
    await loadUserAuthorizedCategories()
    const res = await getCategoryTree({ columnId })
    if (res.code !== 200) {
      categories.value = []
      return
    }

    categories.value = filterCategoryTree(res.data || [])

    const targetCategoryId = route.query.categoryId ? Number(route.query.categoryId) : null
    if (targetCategoryId) {
      const path = findNodePath(categories.value, targetCategoryId)
      if (path?.length) {
        const targetNode = path[path.length - 1]
        await selectCategoryNode(targetNode, path.slice(0, -1).map((item) => item.id))
        return
      }
    }

    const firstNode = categories.value[0]
    if (firstNode) {
      await selectCategoryNode(firstNode, [])
      return
    }

    await selectCategoryNode(null)
  } catch (error) {
    console.error('Failed to fetch categories', error)
    categories.value = []
    await selectCategoryNode(null)
  } finally {
    categoryLoading.value = false
  }
}

const fetchContent = async (categoryId, categoryName = '') => {
  selectedCategoryName.value = categoryName || ''
  if (!categoryId) {
    currentContent.value = null
    return
  }

  contentLoading.value = true
  try {
    const res = await getContentList({ categoryId })
    if (res.code === 200 && Array.isArray(res.data) && res.data.length > 0) {
      currentContent.value = res.data[0]
    } else {
      currentContent.value = null
    }
  } catch (error) {
    console.error('Failed to fetch content', error)
    currentContent.value = null
  } finally {
    contentLoading.value = false
  }
}

const handleColumnClick = (column) => {
  platformStore.setCurrentColumnId(column.id)
}

const handleNodeSelect = async (node) => {
  const path = findNodePath(categories.value, node.id) || []
  await selectCategoryNode(node, path.slice(0, -1).map((item) => item.id))
}

const findNodePathInTree = (nodes, targetId, path = []) => {
  for (const node of nodes) {
    const currentPath = [...path, node]
    if (Number(node.id) === Number(targetId)) {
      return currentPath
    }
    if (node.children?.length) {
      const found = findNodePathInTree(node.children, targetId, currentPath)
      if (found) {
        return found
      }
    }
  }
  return null
}

const getCategoryAppKey = (categoryId) => {
  if (categoryToAppKey.value.has(categoryId)) {
    return categoryToAppKey.value.get(categoryId)
  }
  const path = findNodePathInTree(categories.value, categoryId)
  if (!path) {
    return null
  }
  for (let index = path.length - 2; index >= 0; index -= 1) {
    const parentId = Number(path[index].id)
    if (categoryToAppKey.value.has(parentId)) {
      return categoryToAppKey.value.get(parentId)
    }
  }
  return null
}

const getCategoryAppId = (categoryId) => {
  if (categoryToAppId.value.has(categoryId)) {
    return categoryToAppId.value.get(categoryId)
  }
  const path = findNodePathInTree(categories.value, categoryId)
  if (!path) {
    return null
  }
  for (let index = path.length - 2; index >= 0; index -= 1) {
    const parentId = Number(path[index].id)
    if (categoryToAppId.value.has(parentId)) {
      return categoryToAppId.value.get(parentId)
    }
  }
  return null
}

const handleSandboxTest = async () => {
  if (!localStorage.getItem('token')) {
    const loggedIn = await ensurePortalSession(undefined, '/contents', { interactive: true })
    if (!loggedIn) return
  }
  if (!selectedCategory.value?.id) {
    MessagePlugin.warning('请先选择左侧分类')
    return
  }

  const appKey = getCategoryAppKey(Number(selectedCategory.value.id))
  if (!appKey) {
    MessagePlugin.warning('当前分类尚未获得接口授权')
    return
  }

  sandboxApiKey.value = appKey
  sandboxDialogVisible.value = true
}

const handleViewCredential = async () => {
  if (!localStorage.getItem('token')) {
    const loggedIn = await ensurePortalSession(undefined, '/contents', { interactive: true })
    if (!loggedIn) return
  }
  if (!selectedCategory.value?.id) {
    MessagePlugin.warning('请先选择左侧分类')
    return
  }

  const categoryId = Number(selectedCategory.value.id)
  const appId = getCategoryAppId(categoryId)
  if (!appId) {
    MessagePlugin.warning('未找到当前接口授权')
    return
  }

  try {
    const res = await getMyCredentials(appId)
    if (res.code !== 200 || !Array.isArray(res.data)) {
      MessagePlugin.warning('获取凭证失败')
      return
    }

    let credential = res.data.find((item) => item.categoryId === categoryId)
    if (!credential) {
      const path = findNodePathInTree(categories.value, categoryId) || []
      for (let index = path.length - 2; index >= 0; index -= 1) {
        const parentId = Number(path[index].id)
        credential = res.data.find((item) => item.categoryId === parentId)
        if (credential) {
          break
        }
      }
    }

    currentViewCredential.value = credential
      ? Object.fromEntries(
          Object.entries({
            'Open ID': credential.openId,
            'Client ID': credential.clientId,
            Token: credential.token,
            'Secret Key': credential.secretKey,
            说明: credential.description
          }).filter(([, value]) => value && String(value).trim())
        )
      : {}

    credentialDialogVisible.value = true
    if (!credential) {
      MessagePlugin.warning('当前接口尚未分配凭证')
    }
  } catch (error) {
    console.error('Failed to load credentials', error)
    MessagePlugin.error('获取凭证失败，请稍后重试')
  }
}

watch(
  () => platformStore.currentColumnId,
  async (newId, oldId) => {
    if (newId && newId !== oldId) {
      categories.value = []
      currentContent.value = null
      selectedCategory.value = null
      selectedCategoryName.value = ''
      activeIds.value = []
      defaultExpandedIds.value = []
      await fetchCategories(newId)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await platformStore.fetchPlatforms()
  const queryPlatformId = route.query.platformId
  const queryColumnId = route.query.columnId

  if (queryPlatformId) {
    const queryPlatform = platformStore.platforms.find(
      (item) => String(item.id) === String(queryPlatformId)
    )
    if (queryPlatform) {
      await platformStore.setCurrentPlatform(queryPlatform)
    }
  }

  if (queryColumnId && platformStore.columns.some((item) => String(item.id) === String(queryColumnId))) {
    platformStore.setCurrentColumnId(Number(queryColumnId))
  } else if (!platformStore.currentColumnId && platformStore.columns.length > 0) {
    platformStore.setCurrentColumnId(platformStore.columns[0].id)
  }
})
</script>

<style lang="less" scoped>
.content-index-page {
  width: 100%;
  height: calc(100vh - 80px);
  background-color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header-column-bar {
  background: #fff;
  border-bottom: 1px solid var(--td-component-border);
  padding: 12px 0;
  flex-shrink: 0;
  z-index: 100;

  .column-content {
    width: 100%;
    padding: 0 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .empty-column-hint {
    color: var(--td-text-color-placeholder);
    font-size: 14px;
  }
}

.main-container {
  display: flex;
  flex: 1;
  width: 100%;
  min-height: 0;
  overflow: hidden;
}

.category-sidebar {
  width: 300px;
  border-right: 1px solid var(--td-component-border);
  background: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100%;

  .sidebar-content {
    padding: 16px;
    overflow-y: auto;
    flex: 1;

    :deep(.custom-tree) {
      .tree-node-content {
        display: flex;
        align-items: center;
        padding: 10px 8px;
        cursor: pointer;
        border-radius: 4px;
        transition: all 0.2s;

        &:hover {
          background-color: var(--td-bg-color-container-hover, #f3f3f3);
        }

        &.is-active .tree-node-label {
          color: #00b42a;
          font-weight: 600;
        }

        &.tree-node-level-0 .tree-node-label {
          font-size: 18px;
          font-weight: 600;
        }

        &.tree-node-level-1 {
          padding-left: 16px;
        }

        &.tree-node-level-1 .tree-node-label {
          font-size: 16px;
          font-weight: 600;
        }

        &.tree-node-level-2,
        &.tree-node-level-3,
        &.tree-node-level-4 {
          padding-left: 40px;
        }

        &.tree-node-level-2 .tree-node-label,
        &.tree-node-level-3 .tree-node-label,
        &.tree-node-level-4 .tree-node-label,
        &.tree-node-level-5 .tree-node-label,
        &.tree-node-level-6 .tree-node-label {
          font-size: 16px;
          font-weight: 400;
        }

        &.tree-node-level-5,
        &.tree-node-level-6 {
          padding-left: 60px;
        }
      }

      .tree-node-arrow,
      .tree-node-arrow-placeholder {
        width: 20px;
        height: 20px;
        margin-right: 8px;
        flex-shrink: 0;
      }

      .tree-node-arrow {
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s;

        &.is-expanded {
          transform: rotate(90deg);
        }
      }

      .tree-node-label {
        color: var(--td-text-color-primary);
        user-select: none;
        line-height: 1.5;
      }
    }
  }
}

.content-body {
  flex: 1;
  padding: 40px 60px;
  overflow-y: auto;
  background: #fff;
  height: 100%;

  .content-loading {
    display: flex;
    justify-content: center;
    padding-top: 100px;
  }

  .markdown-wrapper {
    width: 100%;

    :deep(.md-editor-preview) {
      padding: 0;
    }
  }
}

.empty-content {
  padding-top: 100px;
  text-align: center;

  .empty-subtitle {
    margin-top: 8px;
    font-size: 14px;
    color: var(--td-text-color-secondary);
  }
}

.catalog-sidebar {
  width: 280px;
  padding: 40px 24px;
  border-left: 1px solid var(--td-component-border);
  background: #fafafa;
  flex-shrink: 0;
  height: 100%;
  overflow-y: auto;

  .catalog-header {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--td-text-color-primary);
    border-left: 3px solid var(--td-brand-color);
    padding-left: 10px;
  }
}

@media (max-width: 1200px) {
  .catalog-sidebar {
    display: none;
  }

  .content-body {
    padding: 30px;
  }
}

@media (max-width: 768px) {
  .category-sidebar {
    position: fixed;
    left: -300px;
    height: 100%;
    z-index: 1000;
    transition: left 0.3s;
  }
}

.steps-wizard {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.step-clickable {
  cursor: pointer;

  &:hover {
    background-color: rgba(0, 82, 217, 0.08);
    transform: translateY(-2px);
  }
}

.step-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #fff;
  color: #666;
  border: 2px solid #c0c0c0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.step-active {
  background-color: #fff;
  color: #0052d9;
  border-color: #0052d9;
  box-shadow: 0 2px 8px rgba(0, 82, 217, 0.3);
}

.step-clickable-num {
  background-color: #fff;
  color: #0052d9;
  border-color: #0052d9;
}

.step-clickable:hover .step-clickable-num {
  background-color: #0052d9;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 82, 217, 0.3);
}

.step-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.step-title-clickable {
  color: #0052d9;
}

.step-clickable:hover .step-title-clickable {
  color: #003bb3;
}

.step-desc {
  font-size: 12px;
  color: #999;
}

.step-arrow {
  display: flex;
  align-items: center;
  align-self: center;
  color: #c0c0c0;
  padding: 0 8px;
}
</style>
