const DEFAULT_CONFIG = {
  loginServiceUrl: 'https://login.nstl.gov.cn',
  userInfoEndpoint: '/api/service/nstl/web/vilidate/isLogin',
  userInfoOrigin: '',
  userInfoRequestTimeout: 5000,
  loginPollInterval: 2000,
  loginPollTimeout: 5 * 60 * 1000,
  loginStatusSyncTimeout: 90 * 1000,
  fallbackSyncMaxAttempts: 3,
  sessionTimeout: 24 * 60 * 60 * 1000,
  storageKey: 'nstl_unified_session',
  trustedOrigins: ['https://login.nstl.gov.cn', 'https://www.nstl.gov.cn'],
  modalConfig: {
    iframeUrl: 'https://login.nstl.gov.cn',
    width: '453px',
    height: '540px',
    zIndex: 10000,
    timeout: 5 * 60 * 1000,
    showLogo: false,
    allowClose: false,
    sandbox: 'allow-scripts allow-forms allow-same-origin allow-popups',
  },
}

export const NSTL_UNIFIED_LOGOUT_EVENT_KEY = 'nstl_unified_logout_at'

export function isNstlUnifiedLogoutStorageEvent(event) {
  return event?.key === NSTL_UNIFIED_LOGOUT_EVENT_KEY
}

class SessionManager {
  constructor(config) {
    this.config = config
    this.cache = null
    this.cacheTime = 0
    this.cacheDuration = 5000
    this.storageWarningShown = false
  }

  createSession(userInfo) {
    const session = {
      version: '1.0',
      user: {
        userId: userInfo.userId,
        username: userInfo.username,
        email: userInfo.email || '',
        nickname: userInfo.nickname || userInfo.username || '',
        phone: userInfo.phone || '',
        account: userInfo.account || '',
        nstlUnionId: userInfo.nstlUnionId || '',
        ssoId: userInfo.ssoId || '',
        unitName: userInfo.unitName || '',
      },
      session: {
        token: userInfo.sessionToken,
        nonce: userInfo.nonce,
        loginTime: userInfo.loginTime,
        expiresAt: userInfo.loginTime + this.config.sessionTimeout,
      },
      metadata: {
        userAgent: navigator.userAgent,
        loginPage: window.location.pathname,
        lastActivity: Date.now(),
      },
    }

    this.saveToStorage(this.config.storageKey, session)
    this.cache = session
    this.cacheTime = Date.now()
  }

  getSession() {
    if (this.cache && Date.now() - this.cacheTime < this.cacheDuration) {
      return this.cache
    }

    const session = this.loadFromStorage(this.config.storageKey)
    if (session) {
      this.cache = session
      this.cacheTime = Date.now()
    }
    return session
  }

  validateSession() {
    const session = this.getSession()
    if (!session?.user?.userId || !session?.session?.token) {
      return false
    }

    if (this.isSessionExpired(session)) {
      this.clearSession()
      return false
    }

    return true
  }

  isSessionExpired(session = this.getSession()) {
    if (!session) {
      return true
    }
    return Date.now() > session.session.expiresAt
  }

  clearSession() {
    this.removeFromStorage(this.config.storageKey)
    this.cache = null
    this.cacheTime = 0
  }

  saveToStorage(key, value) {
    const data = JSON.stringify(value)

    try {
      localStorage.setItem(key, data)
      return true
    } catch (error) {
      console.warn('[SessionManager] localStorage unavailable, trying sessionStorage', error)
      this.showStorageWarning('localStorage')
    }

    try {
      sessionStorage.setItem(key, data)
      return true
    } catch (error) {
      console.error('[SessionManager] Both localStorage and sessionStorage unavailable', error)
      this.showStorageWarning('both')
      return false
    }
  }

  loadFromStorage(key) {
    try {
      const data = localStorage.getItem(key) || sessionStorage.getItem(key)
      return data ? JSON.parse(data) : null
    } catch (error) {
      console.error('[SessionManager] Failed to load session', error)
      return null
    }
  }

  removeFromStorage(key) {
    try {
      localStorage.removeItem(key)
      sessionStorage.removeItem(key)
    } catch (error) {
      console.error('[SessionManager] Failed to clear session', error)
    }
  }

  showStorageWarning(storageType) {
    if (this.storageWarningShown) {
      return
    }

    this.storageWarningShown = true
    const warning = document.createElement('div')
    warning.className = 'storage-warning'
    warning.textContent = storageType === 'localStorage'
      ? '\u6d4f\u89c8\u5668\u672c\u5730\u5b58\u50a8\u4e0d\u53ef\u7528\uff0c\u767b\u5f55\u72b6\u6001\u4ec5\u5728\u5f53\u524d\u6807\u7b7e\u9875\u6709\u6548'
      : '\u6d4f\u89c8\u5668\u5b58\u50a8\u4e0d\u53ef\u7528\uff0c\u65e0\u6cd5\u4fdd\u5b58\u767b\u5f55\u72b6\u6001\uff0c\u8bf7\u68c0\u67e5\u6d4f\u89c8\u5668\u8bbe\u7f6e'
    warning.style.cssText = `
      position: fixed;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      padding: 12px 24px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      z-index: 10001;
      background: #f59e0b;
      color: #ffffff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      max-width: 90%;
      text-align: center;
    `
    document.body.appendChild(warning)
    window.setTimeout(() => warning.remove(), 5000)
  }
}

class LoginModal {
  constructor(config) {
    this.config = config
    this.modalElement = null
    this.iframeElement = null
    this.timeoutTimer = 0
    this.callbacks = {
      onClose: null,
      onTimeout: null,
    }
  }

  show() {
    if (this.isVisible()) {
      console.warn('[LoginModal] Modal is already visible')
      return
    }

    this.createModal()
    this.startTimeout()
  }

  hide() {
    if (!this.isVisible()) {
      return
    }
    this.destroyModal()
  }

  isVisible() {
    return Boolean(this.modalElement && document.body.contains(this.modalElement))
  }

  get iframe() {
    return this.iframeElement
  }

  onClose(callback) {
    this.callbacks.onClose = callback
  }

  onTimeout(callback) {
    this.callbacks.onTimeout = callback
  }

  createModal() {
    const overlay = document.createElement('div')
    overlay.className = 'login-modal-overlay'
    overlay.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.4);
      z-index: ${this.config.zIndex};
    `

    this.iframeElement = this.createIframe()
    this.iframeElement.className = 'login-modal-iframe'
    this.iframeElement.style.cssText = `
      position: absolute;
      inset: 0;
      margin: auto;
      display: block;
      border: 0;
      width: 100%;
      height: 100%;
      background: transparent;
      animation: nstl-login-modal-enter 0.18s ease-out;
    `

    overlay.appendChild(this.iframeElement)
    document.body.appendChild(overlay)
    document.body.style.overflow = 'hidden'
    this.ensureModalAnimationStyle()
    this.modalElement = overlay
  }

  createIframe() {
    const iframe = document.createElement('iframe')
    iframe.src = this.config.iframeUrl
    iframe.setAttribute('sandbox', this.config.sandbox)

    const loadTimeout = window.setTimeout(() => {
      console.warn('[LoginModal] Iframe load timeout:', this.config.iframeUrl)
    }, 10000)

    iframe.onload = () => {
      window.clearTimeout(loadTimeout)
      console.log('[LoginModal] Iframe loaded successfully')
    }
    iframe.onerror = () => {
      window.clearTimeout(loadTimeout)
      console.error('[LoginModal] Failed to load iframe:', this.config.iframeUrl)
    }

    return iframe
  }

  destroyModal() {
    this.modalElement?.remove()
    this.modalElement = null
    this.iframeElement = null
    document.body.style.overflow = ''
    this.clearTimeout()
  }

  startTimeout() {
    this.clearTimeout()
    this.timeoutTimer = window.setTimeout(() => {
      console.warn('[LoginModal] Login timeout after', this.config.timeout, 'ms')
      this.callbacks.onTimeout?.()
      this.hide()
    }, this.config.timeout)
  }

  clearTimeout() {
    if (this.timeoutTimer) {
      window.clearTimeout(this.timeoutTimer)
      this.timeoutTimer = 0
    }
  }

  ensureModalAnimationStyle() {
    if (document.getElementById('nstl-login-modal-style')) {
      return
    }

    const style = document.createElement('style')
    style.id = 'nstl-login-modal-style'
    style.textContent = `
      @keyframes nstl-login-modal-enter {
        from {
          opacity: 0;
          transform: translateY(12px) scale(0.98);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }
    `
    document.head.appendChild(style)
  }
}

class PostMessageHandler {
  constructor(config) {
    this.config = config
    this.callbacks = {
      onLoginSuccess: null,
      onLoginFailure: null,
      onLoginClosed: null,
    }
    this.isListening = false
    this.boundHandler = this.handleMessage.bind(this)
  }

  startListening() {
    if (this.isListening) {
      return
    }
    window.addEventListener('message', this.boundHandler)
    this.isListening = true
  }

  stopListening() {
    if (!this.isListening) {
      return
    }
    window.removeEventListener('message', this.boundHandler)
    this.isListening = false
  }

  onLoginSuccess(callback) {
    this.callbacks.onLoginSuccess = callback
  }

  onLoginFailure(callback) {
    this.callbacks.onLoginFailure = callback
  }

  onLoginClosed(callback) {
    this.callbacks.onLoginClosed = callback
  }

  handleMessage(event) {
    if (!this.config.trustedOrigins.includes(event.origin)) {
      console.warn('[PostMessageHandler] Ignored message from untrusted origin:', event.origin)
      return
    }

    const data = event.data
    if (!this.validateMessage(data)) {
      console.warn('[PostMessageHandler] Invalid message format:', data)
      return
    }

    if (typeof data === 'string') {
      const normalized = data.toLowerCase()
      if (normalized === 'success') {
        this.callbacks.onLoginSuccess?.({ message: 'SUCCESS' })
      } else if (normalized === 'close' || normalized === 'closelogin') {
        this.callbacks.onLoginClosed?.()
      } else if (normalized === 'failure') {
        this.callbacks.onLoginFailure?.({ message: 'Login failed' })
      }
      return
    }

    if (data.type === 'loginSuccess') {
      this.callbacks.onLoginSuccess?.(data.data || {})
    } else if (data.type === 'loginFailure') {
      this.callbacks.onLoginFailure?.(data.error)
    } else if (data.type === 'loginClosed') {
      this.callbacks.onLoginClosed?.()
    }
  }

  validateMessage(data) {
    if (typeof data === 'string') {
      return ['success', 'close', 'closelogin', 'failure'].includes(data.toLowerCase())
    }

    if (!data || typeof data !== 'object') {
      return false
    }

    const message = data
    if (typeof message.type !== 'string') {
      return false
    }
    if (message.timestamp !== undefined && typeof message.timestamp !== 'number') {
      return false
    }
    return true
  }
}

class NstlAuthManager {
  constructor(config = {}) {
    this.config = {
      ...DEFAULT_CONFIG,
      ...config,
      modalConfig: {
        ...DEFAULT_CONFIG.modalConfig,
        ...(config.modalConfig || {}),
      },
    }
    this.sessionManager = new SessionManager(this.config)
    this.postMessageHandler = new PostMessageHandler(this.config)
    this.postMessageHandler.onLoginSuccess((data) => this.handleLoginSuccess(data))
    this.postMessageHandler.onLoginFailure((error) => this.handleLoginFailure(error))
    this.postMessageHandler.onLoginClosed(() => this.handleLoginClosed())
    this.postMessageHandler.startListening()
    this.loginModal = null
    this.loginPollTimer = 0
    this.loginPollStartedAt = 0
    this.loginPollInFlight = false
    this.loginCompleting = false
    this.loginSyncStartedAt = 0
    this.boundWxRegist = this.handleWxRegist.bind(this)
    this.boundInitWeichatLogin = this.handleInitWeichatLogin.bind(this)
    this.installNstlParentCallbacks()
  }

  async fetchUserInfo() {
    const urls = this.getUserInfoRequestUrls()
    let lastError = null

    for (const url of urls) {
      const controller = new AbortController()
      const timeoutTimer = window.setTimeout(() => controller.abort(), this.config.userInfoRequestTimeout)

      try {
        const response = await fetch(url, {
          method: 'GET',
          headers: {
            'X-Requested-With': 'XMLHttpRequest',
            'Cache-Control': 'no-cache',
            Pragma: 'no-cache',
          },
          credentials: 'include',
          mode: url.startsWith('http') ? 'cors' : 'same-origin',
          signal: controller.signal,
        })

        if (!response.ok) {
          this.log('warn', `NSTL API returned ${response.status}`, { url })
          continue
        }

        const result = await response.json()
        const userInfo = normalizeNstlUserInfoResult(result)
        if (userInfo && isRealUserId(userInfo.userId)) {
          return userInfo
        }

        this.log('warn', 'NSTL API returned non-login status', { url, result })
      } catch (error) {
        lastError = error
        this.log('warn', 'Failed NSTL user info request', { url, error })
      } finally {
        window.clearTimeout(timeoutTimer)
      }
    }

    if (lastError) {
      this.log('error', 'Failed to fetch NSTL user info', lastError)
    }

    return null
  }

  getUserInfoRequestUrls() {
    const endpoint = this.config.userInfoEndpoint
    const urls = []
    const addUrl = (candidate) => {
      if (candidate && !urls.includes(candidate)) {
        urls.push(candidate)
      }
    }
    const withTimestamp = (candidate) => {
      const separator = candidate.includes('?') ? '&' : '?'
      return `${candidate}${separator}t=${Date.now()}`
    }

    addUrl(withTimestamp(endpoint))

    if (endpoint.startsWith('/')) {
      const currentHost = window.location.hostname
      const currentOrigin = window.location.origin
      const configuredOrigin = normalizeOrigin(this.config.userInfoOrigin)

      if (configuredOrigin) {
        addUrl(withTimestamp(`${configuredOrigin}${endpoint}`))
      }

      // When we are already running under an NSTL domain, prefer the current
      // origin only. This keeps local HTTPS dev on :18300/:18301 from probing
      // https://www.nstl.gov.cn or https://nstl.gov.cn on 443 and producing
      // noisy connection/certificate errors during login polling.
      if (currentHost && currentHost.endsWith('nstl.gov.cn')) {
        addUrl(withTimestamp(`${currentOrigin}${endpoint}`))
        return urls
      }

      addUrl(withTimestamp(`https://www.nstl.gov.cn${endpoint}`))
      addUrl(withTimestamp(`https://nstl.gov.cn${endpoint}`))
    }

    return urls
  }

  getUserInfo() {
    return this.sessionManager.getSession()?.user || null
  }

  isAuthenticated() {
    const session = this.sessionManager.getSession()
    if (!session || !normalizeSessionUserId(session.user?.userId)) {
      return false
    }
    return this.sessionManager.validateSession()
  }

  async checkAuth() {
    try {
      let session = this.sessionManager.getSession()
      if (session && this.sessionManager.isSessionExpired(session)) {
        this.sessionManager.clearSession()
        session = null
      }

      if (session) {
        if (normalizeSessionUserId(session.user?.userId) && !isRealUserId(session.user?.userId)) {
          return this.sessionManager.validateSession()
        }

        try {
          const userInfo = await this.fetchUserInfo()
          if (!userInfo || !isRealUserId(userInfo.userId)) {
            this.sessionManager.clearSession()
            this.handleMainSystemLogout()
            return false
          }

          this.updateSessionWithUserInfo(userInfo, session)
          return true
        } catch (error) {
          this.log('warn', 'Unable to verify NSTL login status, clearing local session', error)
          this.sessionManager.clearSession()
          this.handleMainSystemLogout()
          return false
        }
      }

      try {
        const userInfo = await this.fetchUserInfo()
        if (userInfo && isRealUserId(userInfo.userId)) {
          this.updateSessionWithUserInfo(userInfo)
          return true
        }
      } catch (error) {
        this.log('warn', 'Unable to reach NSTL login status and no local session exists', error)
      }

      return false
    } catch (error) {
      this.log('warn', 'Unable to verify NSTL login status', error)
      return false
    }
  }

  showLoginModal() {
    if (this.loginModal?.isVisible()) {
      return
    }

    this.loginCompleting = false
    this.loginSyncStartedAt = 0
    this.loginModal = new LoginModal(this.config.modalConfig)
    this.loginModal.onClose(() => this.handleLoginClosed())
    this.loginModal.onTimeout(() => this.handleLoginTimeout())
    this.loginModal.show()
    this.loginModal.iframe?.addEventListener('load', () => {
      void this.pollLoginStatus()
    })
    this.startLoginPolling()
    this.log('info', 'Login modal displayed')
  }

  hideLoginModal() {
    this.stopLoginPolling()
    this.loginModal?.hide()
    this.loginModal = null
  }

  clearLocalSession() {
    this.sessionManager.clearSession()
    this.hideLoginModal()
  }

  async logout() {
    const userInfo = this.getUserInfo()
    this.clearLocalSession()
    localStorage.setItem(NSTL_UNIFIED_LOGOUT_EVENT_KEY, String(Date.now()))
    this.log('info', 'User logged out', { userId: userInfo?.userId })
    window.dispatchEvent(new CustomEvent('authLogout', {
      detail: { message: 'User logged out locally' },
    }))
    const service = encodeURIComponent(window.location.href)
    window.location.href = `https://login.nstl.gov.cn/logout?service=${service}`
  }

  destroy() {
    this.postMessageHandler.stopListening()
    this.removeNstlParentCallbacks()
    this.hideLoginModal()
  }

  async handleLoginSuccess(data) {
    if (this.loginCompleting) {
      return
    }
    this.loginCompleting = true

    try {
      let userInfo = null
      try {
        userInfo = await this.fetchUserInfo()
      } catch (error) {
        this.log('warn', 'Failed to fetch NSTL user info after login callback, waiting for sync', error)
      }

      if (userInfo && isRealUserId(userInfo.userId)) {
        this.completeLoginWithUserInfo(userInfo, data.token)
        return
      }

      this.showMessage('登录已确认，正在同步 NSTL 用户信息...', 'info')
      userInfo = await this.waitForMainSystemUserInfo(this.config.fallbackSyncMaxAttempts)
      if (userInfo && isRealUserId(userInfo.userId)) {
        this.completeLoginWithUserInfo(userInfo, data.token)
        return
      }

      const fallbackUserInfo = this.createFallbackUserInfo(data)
      if (fallbackUserInfo) {
        this.log('warn', 'NSTL user info unavailable after sync wait, creating fallback local session', data)
        this.showMessage('主站登录已完成，正在继续建立本地兜底会话...', 'warning')
        this.completeLoginWithUserInfo(fallbackUserInfo, data.token)
        return
      }

      this.loginCompleting = false
      this.log('warn', 'NSTL login callback returned without usable user info', data)
      this.showMessage('登录已返回，但未获取到用户信息，请稍后重试', 'warning')
    } catch (error) {
      this.loginCompleting = false
      this.log('error', 'Failed to handle login success', error)
      this.showMessage('登录处理失败，请重试', 'error')
    }
  }

  handleLoginFailure(error) {
    this.log('error', 'Login failed', error)
    this.showMessage('登录失败，请重试', 'error')
  }

  handleLoginClosed() {
    this.hideLoginModal()
    window.dispatchEvent(new CustomEvent('authLoginClosed', {
      detail: { message: 'Login modal closed by user' },
    }))
  }

  handleLoginTimeout() {
    this.stopLoginPolling()
    this.loginModal = null
    this.showMessage('登录超时，请重新尝试', 'error')
    window.setTimeout(() => this.showLoginModal(), 1000)
  }

  startLoginPolling() {
    this.stopLoginPolling()
    this.loginPollStartedAt = Date.now()
    this.loginPollTimer = window.setInterval(() => {
      void this.pollLoginStatus()
    }, this.config.loginPollInterval)
    void this.pollLoginStatus()
  }

  stopLoginPolling() {
    if (this.loginPollTimer) {
      window.clearInterval(this.loginPollTimer)
      this.loginPollTimer = 0
    }
    this.loginPollStartedAt = 0
    this.loginPollInFlight = false
  }

  async pollLoginStatus() {
    if (this.loginPollInFlight || this.loginCompleting || !this.loginModal?.isVisible()) {
      return
    }

    const now = Date.now()

    if (this.loginSyncStartedAt && now - this.loginSyncStartedAt > this.config.loginStatusSyncTimeout) {
      this.loginSyncStartedAt = 0
      this.showMessage('已完成登录操作，但尚未同步到 NSTL 主站登录态，请稍后重试', 'warning')
      return
    }

    if (now - this.loginPollStartedAt > this.config.loginPollTimeout) {
      this.stopLoginPolling()
      this.showMessage('未获取到 NSTL 登录态，请确认微信扫码或手机验证码登录已完成', 'warning')
      return
    }

    this.loginPollInFlight = true
    try {
      const userInfo = await this.fetchUserInfo()
      if (userInfo && isRealUserId(userInfo.userId)) {
        this.completeLoginWithUserInfo(userInfo)
      }
    } catch (error) {
      this.log('warn', 'Polling NSTL login status failed', error)
    } finally {
      this.loginPollInFlight = false
    }
  }

  completeLoginWithUserInfo(userInfo, token) {
    this.loginCompleting = true
    this.sessionManager.createSession({
      userId: userInfo.userId,
      username: userInfo.username,
      email: userInfo.email,
      nickname: userInfo.nickname,
      phone: userInfo.phone,
      account: userInfo.account,
      nstlUnionId: userInfo.nstlUnionId,
      ssoId: userInfo.ssoId,
      unitName: userInfo.unitName,
      sessionToken: token || this.generateNonce(),
      nonce: this.generateNonce(),
      loginTime: Date.now(),
    })
    this.hideLoginModal()
    this.loginSyncStartedAt = 0
    window.dispatchEvent(new CustomEvent('authSuccess', {
      detail: { userInfo: this.getUserInfo() },
    }))
    this.loginCompleting = false
  }

  updateSessionWithUserInfo(userInfo, session) {
    this.sessionManager.createSession({
      userId: userInfo.userId,
      username: userInfo.username,
      email: userInfo.email,
      nickname: userInfo.nickname,
      phone: userInfo.phone,
      account: userInfo.account,
      nstlUnionId: userInfo.nstlUnionId,
      ssoId: userInfo.ssoId,
      unitName: userInfo.unitName,
      sessionToken: session?.session?.token || this.generateNonce(),
      nonce: session?.session?.nonce || this.generateNonce(),
      loginTime: session?.session?.loginTime || Date.now(),
    })
  }

  createFallbackUserInfo(data) {
    const postMessageUserId = normalizeSessionUserId(data?.userId)
    if (postMessageUserId) {
      return {
        userId: postMessageUserId,
        username: data?.username || postMessageUserId,
        email: data?.email || '',
        nickname: data?.nickname || data?.username || postMessageUserId,
        phone: data?.phone || '',
        account: data?.account || '',
        nstlUnionId: data?.nstlUnionId || '',
        ssoId: data?.ssoId || '',
        unitName: data?.unitName || '',
      }
    }

    if (String(data?.message || '').toUpperCase() === 'SUCCESS') {
      const userId = `nstl_user_${Date.now()}`
      return {
        userId,
        username: data?.username || 'NSTL用户',
        email: data?.email || '',
        nickname: data?.nickname || data?.username || 'NSTL用户',
        phone: data?.phone || '',
        account: data?.account || '',
        nstlUnionId: data?.nstlUnionId || '',
        ssoId: data?.ssoId || '',
        unitName: data?.unitName || '',
      }
    }

    return null
  }

  async waitForMainSystemUserInfo(maxAttempts) {
    let attempt = 0

    while (attempt < maxAttempts) {
      attempt += 1
      try {
        const userInfo = await this.fetchUserInfo()
        if (userInfo && isRealUserId(userInfo.userId)) {
          return userInfo
        }
      } catch (error) {
        this.log('warn', 'Waiting for NSTL user info failed', error)
      }

      if (attempt < maxAttempts) {
        await delay(1000)
      }
    }

    return null
  }

  installNstlParentCallbacks() {
    window.wxRegist = this.boundWxRegist
    window.WxRegist = this.boundWxRegist
    window.initWeichatLogin = this.boundInitWeichatLogin
  }

  removeNstlParentCallbacks() {
    if (window.wxRegist === this.boundWxRegist) {
      delete window.wxRegist
    }
    if (window.WxRegist === this.boundWxRegist) {
      delete window.WxRegist
    }
    if (window.initWeichatLogin === this.boundInitWeichatLogin) {
      delete window.initWeichatLogin
    }
  }

  handleInitWeichatLogin() {
    this.log('info', 'NSTL login iframe requested WeChat QR re-initialization')
    this.loginCompleting = false
    this.loginSyncStartedAt = 0
    const iframe = this.loginModal?.iframe
    if (!iframe) {
      return
    }
    iframe.src = this.config.modalConfig.iframeUrl
    this.startLoginPolling()
  }

  handleWxRegist(response) {
    const code = response?.code == null ? '' : String(response.code)
    this.log('info', 'NSTL WeChat login callback received', response)

    if (code === '1' || code === '') {
      this.showMessage('微信账号未绑定 NSTL 账号，请使用手机验证码登录或先完成绑定', 'warning')
      this.handleInitWeichatLogin()
      return
    }

    if (code === '2') {
      this.showMessage(response?.msg || '微信登录失败，请重新扫码', 'error')
      this.handleInitWeichatLogin()
      return
    }

    this.loginSyncStartedAt = Date.now()
    this.showMessage('微信扫码已确认，正在同步 NSTL 登录态...', 'info')
    this.loginCompleting = false
    this.startLoginPolling()
  }

  handleMainSystemLogout() {
    window.dispatchEvent(new CustomEvent('authLogout', {
      detail: { message: 'Main system logout detected' },
    }))
  }

  generateNonce() {
    if (globalThis.crypto?.getRandomValues) {
      return Array.from(globalThis.crypto.getRandomValues(new Uint8Array(16)))
        .map((value) => value.toString(16).padStart(2, '0'))
        .join('')
    }
    return `${Date.now()}_${Math.random().toString(36).slice(2)}`
  }

  showMessage(message, type) {
    const colors = {
      info: '#3b82f6',
      warning: '#f59e0b',
      error: '#ef4444',
      success: '#10b981',
    }
    const messageEl = document.createElement('div')
    messageEl.className = `auth-message auth-message-${type}`
    messageEl.textContent = message
    messageEl.style.cssText = `
      position: fixed;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      padding: 12px 24px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      z-index: 10001;
      background: ${colors[type]};
      color: #ffffff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      max-width: min(520px, calc(100vw - 32px));
      text-align: center;
    `
    document.body.appendChild(messageEl)
    window.setTimeout(() => messageEl.remove(), 3000)
  }

  log(level, message, details = null) {
    const prefix = '[AuthManager]'
    if (level === 'error') {
      console.error(prefix, message, details)
    } else if (level === 'warn') {
      console.warn(prefix, message, details)
    } else if (level === 'info') {
      console.log(prefix, message, details)
    } else {
      console.debug(prefix, message, details)
    }
  }
}

function normalizeNstlUserInfoResult(result) {
  if (!result || typeof result !== 'object') {
    return null
  }

  const response = result
  if (response.code !== 0) {
    return null
  }

  const data = pickObject(response.data)
  const nestedUser = pickObject(data?.userInfo) || pickObject(data?.user) || pickObject(response.userInfo) || pickObject(response.user)
  const payload = nestedUser || data
  if (!payload) {
    return null
  }

  const phone = firstStringValue(payload, ['phone', 'mobile', 'mobilePhone', 'mobile_phone', 'tel']) || ''
  const account = firstStringValue(payload, ['account', 'loginId', 'loginName', 'userid', 'userId', 'user_id']) || ''
  const userId = firstStringValue(payload, ['userId', 'userid', 'user_id', 'id', 'uid', 'loginId', 'loginName', 'account', 'mobile', 'phone'])
  if (!isRealUserId(userId)) {
    return null
  }

  const username = formatDisplayName(firstStringValue(payload, ['userName', 'username', 'name', 'realName', 'nickName', 'displayName', 'account', 'loginName'])) || phone || account || userId
  const nickname = formatDisplayName(firstStringValue(payload, ['nickname', 'nickName', 'realName', 'displayName', 'userName', 'username'])) || phone || username
  const email = firstStringValue(payload, ['email', 'mail']) || ''
  const nstlUnionId = firstStringValue(payload, ['nstlUnionId', 'nstl_union_id', 'unionId', 'unionid', 'union_id']) || ''
  const ssoId = firstStringValue(payload, ['ssoId', 'sso_id', 'ssoid']) || ''
  const unitName = firstStringValue(payload, ['unitName', 'unit', 'orgName', 'organizationName']) || ''
  return { userId, username, email, nickname, phone, nstlUnionId, ssoId, unitName, account }
}

function isRealUserId(userId) {
  return Boolean(normalizeRealUserId(userId))
}

function normalizeSessionUserId(userId) {
  if (!userId) {
    return ''
  }

  const normalized = String(userId).trim()
  const lower = normalized.toLowerCase()
  const blocked = new Set(['default', 'unknown', 'mock_user_id', 'test_user', 'test_account'])
  if (blocked.has(lower)) {
    return ''
  }

  if (lower.startsWith('mock_') || lower.startsWith('fake_') || lower.startsWith('test_')) {
    return ''
  }

  return normalized
}

function normalizeRealUserId(userId) {
  if (!userId) {
    return ''
  }

  const normalized = String(userId).trim()
  const lower = normalized.toLowerCase()
  const blocked = new Set(['default', 'unknown', 'mock_user_id', 'test_user', 'test_account'])
  if (blocked.has(lower)) {
    return ''
  }

  if (
    lower.startsWith('nstl_user_')
    || lower.startsWith('mock_')
    || lower.startsWith('fake_')
    || lower.startsWith('test_')
  ) {
    return ''
  }

  return normalized
}

function formatDisplayName(value) {
  const raw = String(value || '').trim()
  if (!raw) {
    return ''
  }

  const unicodeDecoded = raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(Number.parseInt(hex, 16)))
  try {
    return decodeURIComponent(unicodeDecoded)
  } catch {
    return unicodeDecoded
  }
}

function normalizeOrigin(value) {
  const raw = String(value || '').trim()
  if (!raw) {
    return ''
  }

  try {
    return new URL(raw).origin
  } catch (error) {
    console.warn('[AuthManager] Ignored invalid userInfoOrigin:', raw, error)
    return ''
  }
}

function pickObject(value) {
  return value && typeof value === 'object' ? value : null
}

function firstStringValue(payload, keys) {
  if (!payload || typeof payload !== 'object') {
    return ''
  }

  const normalizedPayload = Object.keys(payload).reduce((result, key) => {
    result[key.toLowerCase()] = payload[key]
    return result
  }, {})

  for (const key of keys) {
    const value = payload[key] ?? normalizedPayload[String(key).toLowerCase()]
    if (typeof value === 'string' && value.trim()) {
      return value.trim()
    }
    if (typeof value === 'number' && Number.isFinite(value)) {
      return String(value)
    }
  }
  return ''
}

function delay(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

let authManager = null

export function createNstlAuthManager(config = {}) {
  if (!authManager) {
    authManager = new NstlAuthManager(config)
    window.nstlAuthManager = authManager
  }
  return authManager
}

export function getNstlAuthManager() {
  return authManager || createNstlAuthManager()
}

