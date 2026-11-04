<template>
  <div class="callback-container">
    <t-card v-if="!isError" :bordered="false" class="callback-card shadow-card">
      <div class="callback-inner">
        <t-loading :loading="true" size="large" />
        <p class="text">{{ loadingText }}</p>
      </div>
    </t-card>

    <t-card v-else :bordered="false" class="error-card shadow-card">
      <div class="error-inner">
        <div class="error-icon">
          <t-icon name="error-circle" size="64px" />
        </div>
        <h2 class="error-title">Login failed</h2>
        <p class="error-message">{{ errorMessage }}</p>
      </div>
    </t-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ssoCallback } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { MessagePlugin } from 'tdesign-vue-next'
import { ErrorCircleIcon as TIcon } from 'tdesign-icons-vue-next'
import { buildSsoCallbackUrl } from '@/utils/sso'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loadingText = ref('Signing in with SSO...')
const isError = ref(false)
const errorMessage = ref('')

const normalizeQueryValue = (value) => (Array.isArray(value) ? value[0] : value)

const formatSsoErrorMessage = (fallback = 'Login failed, please try again.') => {
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
  if (!code) {
    isError.value = true
    errorMessage.value = formatSsoErrorMessage('未获取到统一认证授权码')
    MessagePlugin.error(errorMessage.value)
    setTimeout(() => {
      router.push('/contents')
    }, 1500)
    return
  }

  try {
    loadingText.value = 'Fetching user information...'
    const redirect = sessionStorage.getItem('portal_sso_redirect')
      || normalizeQueryValue(route.query.redirect)
      || '/contents'
    const cachedRedirectUri = sessionStorage.getItem('portal_sso_redirect_uri')
    const cachedState = sessionStorage.getItem('portal_sso_state')
    const returnedState = normalizeQueryValue(route.query.state)
    const redirectUri = cachedRedirectUri || buildSsoCallbackUrl()
    sessionStorage.removeItem('portal_sso_redirect_uri')
    sessionStorage.removeItem('portal_sso_redirect')
    sessionStorage.removeItem('portal_sso_state')

    if (cachedState && returnedState && cachedState !== returnedState) {
      throw new Error('统一认证登录状态校验失败，请重新登录')
    }

    const res = await ssoCallback(code, redirectUri)

    if (res.code === 200) {
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.userInfo)

      loadingText.value = 'Login successful, redirecting...'
      MessagePlugin.success('Login successful')

      setTimeout(() => {
        router.push(redirect)
      }, 500)
      return
    }

    throw new Error(formatSsoErrorMessage(res.message || 'Login failed'))
  } catch (error) {
    console.error('Portal SSO callback failed:', error)
    isError.value = true
    errorMessage.value = formatSsoErrorMessage(error.message || 'Login failed, please try again.')
    MessagePlugin.error(errorMessage.value)
  }
})
</script>

<style lang="less" scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  background-color: #f5f7fa;
}

.callback-card {
  width: 400px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);

  .callback-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
    padding: 64px 32px;
  }

  .text {
    font-size: 16px;
    color: var(--td-text-color-secondary);
    margin: 0;
    font-weight: 500;
  }
}

.error-card {
  width: 480px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);

  .error-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    padding: 48px 32px;
  }

  .error-icon {
    color: var(--td-error-color);
    margin-bottom: 8px;
  }

  .error-title {
    font-size: 22px;
    font-weight: 600;
    color: var(--td-text-color-primary);
    margin: 0;
  }

  .error-message {
    font-size: 15px;
    color: var(--td-text-color-secondary);
    text-align: center;
    margin: 0;
    line-height: 1.6;
    max-width: 360px;
  }
}
</style>
