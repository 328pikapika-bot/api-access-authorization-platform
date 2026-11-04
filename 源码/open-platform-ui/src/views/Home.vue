<template>
  <t-layout class="home-layout">
    <t-header class="home-header">
      <div class="header-content">
        <div class="header-left">
          <div class="logo-box" @click="router.push('/')">
            <img :src="logoSrc" alt="NSTL" class="logo-img" />
          </div>
          <div class="title-group" @click="router.push('/')">
            <span class="main-title">NSTL接口平台</span>
          </div>
          <t-divider layout="vertical" class="header-divider" />
          <t-dropdown
            :options="platformOptions"
            trigger="click"
            class="platform-dropdown"
            :popup-props="{ overlayStyle: { width: 'auto', minWidth: '200px', maxWidth: '400px' } }"
            @click="handlePlatformChange"
          >
            <div class="platform-selector">
              <span class="platform-name">{{ platformStore.currentPlatformName }}</span>
              <t-icon name="chevron-down" size="14px" class="dropdown-icon" />
            </div>
          </t-dropdown>
        </div>

        <div class="header-right">
          <t-button variant="text" @click="router.push('/contents')">首页</t-button>
          <t-button v-if="userStore.isLoggedIn" variant="text" @click="router.push('/user')">
            个人中心
          </t-button>

          <div class="user-area">
            <template v-if="!userStore.isLoggedIn">
              <t-button theme="primary" @click="handleLogin">统一认证登录</t-button>
            </template>
            <template v-else>
              <t-dropdown @click="handleDropdownClick">
                <div class="user-info">
                  <t-avatar size="small">{{ userStore.username?.charAt(0) || 'U' }}</t-avatar>
                  <span class="username">{{ userStore.username }}</span>
                </div>
                <t-dropdown-menu>
                  <t-dropdown-item value="profile">个人中心</t-dropdown-item>
                  <t-dropdown-item value="logout">退出登录</t-dropdown-item>
                </t-dropdown-menu>
              </t-dropdown>
            </template>
          </div>
        </div>
      </div>
    </t-header>

    <t-content class="main-content">
      <router-view />
    </t-content>

    <LandingFooter />
  </t-layout>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon as TIcon } from 'tdesign-icons-vue-next'
import { MessagePlugin } from 'tdesign-vue-next'
import { useUserStore } from '@/stores/user'
import { usePlatformStore } from '@/stores/platform'
import LandingFooter from './components/LandingFooter.vue'
import { ensurePortalSession, logoutPortal } from '@/services/unifiedAuth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const platformStore = usePlatformStore()
const logoSrc = `${import.meta.env.BASE_URL}images/logo.png`

const platformOptions = computed(() =>
  platformStore.platforms.map((platform) => ({
    content: platform.name,
    value: platform.id,
  })),
)

const handlePlatformChange = (data) => {
  const platform = platformStore.platforms.find((item) => String(item.id) === String(data.value))
  if (!platform) {
    return
  }

  platformStore.setCurrentPlatform(platform)
  router.push('/contents')
}

onMounted(() => {
  platformStore.fetchPlatforms()
})

watch(
  () => userStore.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn) {
      platformStore.fetchPlatforms()
    } else {
      platformStore.clearCatalog()
    }
  }
)

const handleLogin = async () => {
  const redirect = route.fullPath === '/' ? '/contents' : route.fullPath
  try {
    await ensurePortalSession(undefined, redirect, { interactive: true })
  } catch (error) {
    console.error('Failed to start NSTL unified login', error)
    MessagePlugin.error(error.message || '统一认证登录失败，请稍后重试')
  }
}

const handleDropdownClick = async ({ value }) => {
  if (value === 'profile') {
    router.push('/user')
    return
  }

  if (value === 'logout') {
    try {
      await logoutPortal(userStore.$pinia)
      MessagePlugin.success('已退出登录')
    } catch (error) {
      console.error('Failed to logout portal', error)
      MessagePlugin.error(error.message || '退出登录失败')
    }
  }
}
</script>

<style lang="less" scoped>
.home-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.home-header {
  background: #fff;
  box-shadow: 0 1px 10px rgba(0, 0, 0, 0.05);
  height: 80px !important;
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 1000;

  .header-content {
    width: 100%;
    padding: 0 40px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    cursor: pointer;

    .logo-box {
      width: 317px;
      height: 51px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;

      .logo-img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
    }

    .title-group {
      cursor: pointer;

      .main-title {
        font-size: 26px;
        font-weight: 600;
        color: var(--td-text-color-primary);
      }
    }

    .header-divider {
      height: 24px;
      margin: 0 16px;
      border-color: var(--td-component-border);
    }

    .platform-selector {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 10px;
      border-radius: var(--td-radius-default);
      transition: background 0.2s;
      cursor: pointer;

      &:hover {
        background: var(--td-bg-color-container-hover);
      }

      .platform-name {
        font-size: 20px;
        color: var(--td-brand-color);
        font-weight: 700;
      }

      .dropdown-icon {
        color: var(--td-text-color-placeholder);
        transition: transform 0.2s;
      }
    }

    .platform-dropdown {
      &.t-dropdown--visible {
        .dropdown-icon {
          transform: rotate(180deg);
        }
      }
    }
  }

  :deep(.t-dropdown__item-content),
  :deep(.t-dropdown__item-text),
  :deep(.t-dropdown__item) span {
    white-space: nowrap !important;
    overflow: visible !important;
    text-overflow: clip !important;
    flex: none !important;
  }

  :deep(.t-dropdown__menu) {
    width: auto !important;
    min-width: 200px;
    max-width: none !important;
  }

  :deep(.t-dropdown__item) {
    max-width: none !important;
    width: auto !important;
    display: inline-flex !important;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .user-area {
      margin-left: 20px;
      padding-left: 20px;
      border-left: 1px solid var(--td-component-border);
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: var(--td-radius-default);
      transition: background 0.2s;

      &:hover {
        background: var(--td-bg-color-container-hover);
      }

      .username {
        font-size: 14px;
        color: var(--td-text-color-primary);
      }
    }
  }
}

.main-content {
  padding: 0;
  width: 100%;
  margin: 0;
}
</style>
