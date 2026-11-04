<template>
  <div class="callback-container">
    <div class="loading-box">
      <t-loading size="large" text="登录中..." />
      <p style="margin-top: 16px; color: var(--td-text-color-placeholder)">正在处理您的登录请求，请稍候</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { ssoCallback } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { buildSsoCallbackUrl } from '@/utils/sso'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const normalizeQueryValue = (value) => (Array.isArray(value) ? value[0] : value)

const formatSsoErrorMessage = (fallback = '登录失败，请重试') => {
  const error = normalizeQueryValue(route.query.error || route.query.err)
  const description = normalizeQueryValue(route.query.error_description || route.query.msg)
  const detail = [error, description, fallback].filter(Boolean).join(' ')
  if (/未认证授权|unauthorized|invalid.*redirect|redirect_uri/i.test(detail)) {
    return '当前回调地址未在统一认证白名单中，请检查 redirect_uri'
  }
  if (/token|accessToken|授权码|code/i.test(detail)) {
    return '统一认证授权码换取用户信息失败，请检查 redirect_uri 是否与发起登录地址完全一致'
  }
  return description || error || fallback
}

onMounted(async () => {
  const code = normalizeQueryValue(route.query.code)
  const redirect = sessionStorage.getItem('admin_sso_redirect')
    || normalizeQueryValue(route.query.redirect)
    || '/dashboard'

  if (!code) {
    MessagePlugin.error(formatSsoErrorMessage('登录失败：未获取到授权码'))
    setTimeout(() => {
      router.push('/login')
    }, 1500)
    return
  }

  try {
    const cachedRedirectUri = sessionStorage.getItem('admin_sso_redirect_uri')
    const cachedState = sessionStorage.getItem('admin_sso_state')
    const returnedState = normalizeQueryValue(route.query.state)
    const redirectUri = cachedRedirectUri || buildSsoCallbackUrl()

    sessionStorage.removeItem('admin_sso_redirect_uri')
    sessionStorage.removeItem('admin_sso_redirect')
    sessionStorage.removeItem('admin_sso_state')

    if (cachedState && returnedState && cachedState !== returnedState) {
      throw new Error('统一认证登录状态校验失败，请重新登录')
    }

    const res = await ssoCallback(code, redirectUri, 'admin')

    if (res.code === 200) {
      if (res.data.userInfo.userRole !== 1) {
        MessagePlugin.error('您没有管理员权限，无法访问后台管理系统')
        userStore.clearLoginInfo()
        setTimeout(() => {
          router.push('/login')
        }, 1500)
        return
      }

      if (res.data.userInfo.status !== 1) {
        MessagePlugin.error('当前账号已被禁用，请联系管理员')
        userStore.clearLoginInfo()
        setTimeout(() => {
          router.push('/login')
        }, 1500)
        return
      }

      userStore.setLoginInfo(res.data.token, res.data.userInfo)
      MessagePlugin.success('登录成功')

      setTimeout(() => {
        router.push(redirect)
      }, 500)
    } else {
      MessagePlugin.error(formatSsoErrorMessage(res.message || '登录失败'))
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    }
  } catch (error) {
    console.error('登录失败:', error)
    MessagePlugin.error(formatSsoErrorMessage(error.message || '登录失败，请重试'))
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  }
})
</script>

<style scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: var(--td-bg-color-page);
}

.loading-box {
  text-align: center;
  background-color: var(--td-bg-color-container);
  padding: 48px;
  border-radius: var(--td-radius-large);
  box-shadow: var(--td-shadow-1);
}
</style>
