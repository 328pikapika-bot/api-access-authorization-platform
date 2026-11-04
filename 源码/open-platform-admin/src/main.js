import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'

import App from './App.vue'
import router from './router'
import { useUserStore } from '@/stores/user'
import { setupAdminUnifiedAuth, syncAdminSession } from '@/services/unifiedAuth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

const userStore = useUserStore(pinia)
if (userStore.token) {
  userStore.fetchUserInfo()
  void syncAdminSession(pinia)
}

app.use(router)
app.use(TDesign)
setupAdminUnifiedAuth(router, pinia)

app.mount('#app')
