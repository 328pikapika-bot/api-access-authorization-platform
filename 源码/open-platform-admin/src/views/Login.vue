<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <t-icon name="control-platform" size="48px" style="color: var(--td-brand-color); margin-bottom: 16px" />
        <h1>NSTL接口平台管理系统</h1>
        <p>管理员统一认证登录</p>
      </div>

      <div class="login-content">
        <t-button
          theme="primary"
          size="large"
          block
          :loading="loading"
          class="login-btn"
          @click="handleLogin"
        >
          <template #icon>
            <t-icon name="internet" />
          </template>
          统一认证登录
        </t-button>
      </div>

      <div class="login-footer">
        <p>国家科技图书文献中心 © 2024</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { Icon as TIcon } from 'tdesign-icons-vue-next'
import { ensureAdminSession } from '@/services/unifiedAuth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const errorMessages = {
  no_admin: '当前账号没有接口平台后台管理员权限。',
  disabled: '当前账号已被禁用，请联系管理员。',
  unauthorized: '登录状态已失效，请重新登录。',
  logged_out: '当前登录已退出，请重新登录。',
}

onMounted(() => {
  const error = route.query.error
  if (error) {
    const message = errorMessages[error] || String(error)
    if (error === 'logged_out') {
      MessagePlugin.warning(message)
    } else {
      MessagePlugin.error(message)
    }
  }
})

const handleLogin = async () => {
  loading.value = true
  try {
    const redirectQuery = route.query.redirect
    const redirect = (Array.isArray(redirectQuery) ? redirectQuery[0] : redirectQuery) || '/dashboard'
    const loggedIn = await ensureAdminSession(undefined, redirect, { interactive: true })
    if (loggedIn) {
      await router.push(redirect)
    }
  } catch (error) {
    console.error('Failed to start admin login', error)
    MessagePlugin.error(error.message || '统一认证登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #6674e8 0%, #7d63d3 100%);
}

.login-box {
  width: 400px;
  background: var(--td-bg-color-container);
  border-radius: var(--td-radius-large);
  box-shadow: var(--td-shadow-3);
  overflow: hidden;
}

.login-header {
  text-align: center;
  padding: 48px 40px 32px;
}

.login-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  color: var(--td-text-color-primary);
}

.login-header p {
  margin: 0;
  font-size: 14px;
  color: var(--td-text-color-placeholder);
}

.login-content {
  padding: 0 40px 48px;
}

.login-btn {
  height: 48px;
  font-size: 16px;
}

.login-footer {
  background: var(--td-bg-color-secondarycontainer);
  padding: 16px;
  text-align: center;
}

.login-footer p {
  margin: 0;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}
</style>
