import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'

import App from './App.vue'
import router from './router'
import './assets/styles/main.scss'
import { useUserStore } from '@/stores/user'
import { setupPortalUnifiedAuth, syncPortalSession } from '@/services/unifiedAuth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

const userStore = useUserStore(pinia)
if (userStore.token) {
  userStore.getUserInfo()
  void syncPortalSession(pinia)
}

app.use(router)
app.use(TDesign)
setupPortalUnifiedAuth(router, pinia)

app.mount('#app')
