<template>
  <t-layout v-if="showLayout" class="admin-layout">
    <t-aside class="admin-sidebar" :style="{ width: collapsed ? '64px' : '232px' }">
      <div class="logo">
        <t-icon name="view-module" size="24px" />
        <span v-if="!collapsed">管理后台</span>
      </div>
      <t-menu
        v-model="activeMenu"
        :collapsed="collapsed"
        theme="dark"
        height="calc(100vh - 64px)"
        @change="handleMenuClick"
      >
        <t-menu-item value="/dashboard">
          <template #icon><t-icon name="dashboard" /></template>
          数据总览
        </t-menu-item>
        <t-menu-item value="/category-manage">
          <template #icon><t-icon name="view-module" /></template>
          分类与内容
        </t-menu-item>
        <t-menu-item value="/api-permission-assign">
          <template #icon><t-icon name="assignment-checked" /></template>
          接口权限分配
        </t-menu-item>
        <t-menu-item value="/content-logs">
          <template #icon><t-icon name="bulletpoint" /></template>
          沙箱请求日志
        </t-menu-item>
        <t-menu-item value="/system-logs">
          <template #icon><t-icon name="undertake" /></template>
          系统日志
        </t-menu-item>
        <t-menu-item value="/mockoon-service">
          <template #icon><t-icon name="server" /></template>
          沙箱服务管理
        </t-menu-item>
        <t-menu-item value="/permission-manage">
          <template #icon><t-icon name="lock-on" /></template>
          权限管理
        </t-menu-item>
        <template #operations>
          <t-button variant="text" shape="square" @click="collapsed = !collapsed">
            <template #icon>
              <t-icon name="menu-fold" v-if="!collapsed" />
              <t-icon name="menu-unfold" v-else />
            </template>
          </t-button>
        </template>
      </t-menu>
    </t-aside>

    <t-layout class="main-layout">
      <t-header class="admin-header">
        <div class="header-left">
          <h2>NSTL接口平台管理系统</h2>
        </div>
        <div class="header-right">
          <t-dropdown @click="handleCommand">
            <t-button variant="text" class="user-dropdown">
              <t-avatar size="small">{{ userStore.username?.charAt(0) || 'A' }}</t-avatar>
              <span class="user-name">{{ userStore.username }}</span>
              <t-icon name="chevron-down" size="16px" />
            </t-button>
            <t-dropdown-menu>
              <t-dropdown-item value="profile">个人资料</t-dropdown-item>
              <t-dropdown-item value="logout">退出登录</t-dropdown-item>
            </t-dropdown-menu>
          </t-dropdown>
        </div>
      </t-header>

      <t-content class="admin-main">
        <router-view />
      </t-content>
    </t-layout>
  </t-layout>
  <router-view v-else />
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { Icon as TIcon } from 'tdesign-icons-vue-next'
import { useUserStore } from '@/stores/user'
import { logoutAdmin } from '@/services/unifiedAuth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)
const showLayout = computed(() => userStore.isLoggedIn && route.meta.requiresAuth !== false)

const redirectToLogin = (error) => {
  userStore.clearLoginInfo(false)
  if (router.currentRoute.value.path !== '/login') {
    router.replace({
      path: '/login',
      query: error ? { error } : {}
    })
  }
}

const handleAuthStorage = (event) => {
  if (event.key === 'admin_logout_at') {
    redirectToLogin()
  } else if (event.key === 'admin_token' && !event.newValue) {
    redirectToLogin('logged_out')
  }
}

onMounted(() => {
  window.addEventListener('storage', handleAuthStorage)
})

onBeforeUnmount(() => {
  window.removeEventListener('storage', handleAuthStorage)
})

const handleMenuClick = (value) => {
  router.push(value)
}

const handleCommand = async (data) => {
  if (data.value === 'profile') {
    MessagePlugin.info('个人资料功能开发中')
    return
  }

  if (data.value === 'logout') {
    try {
      await logoutAdmin(userStore.$pinia)
      MessagePlugin.success('已退出登录')
    } catch (error) {
      console.error('Failed to logout admin', error)
      MessagePlugin.error(error.message || '退出登录失败')
    }
  }
}
</script>

<style lang="less">
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
}
</style>

<style lang="less" scoped>
.admin-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  flex-direction: row;
}

.admin-sidebar {
  transition: width 0.3s ease;
  z-index: 100;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  position: relative;
  flex-shrink: 0;

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    padding: 0 24px;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    gap: 12px;
    background: #001529;
    overflow: hidden;
    white-space: nowrap;
  }
}

.main-layout {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header {
  background: #fff;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
  height: 64px;
  flex-shrink: 0;

  h2 {
    margin: 0;
    font-size: 18px;
    color: var(--td-brand-color);
  }
}

.admin-main {
  background: var(--td-bg-color-page);
  padding: 0;
  overflow-y: auto;
  flex: 1;
}

.user-dropdown {
  display: flex;
  align-items: center;
  padding: 0 8px;
}

.user-name {
  margin-left: 8px;
}
</style>
