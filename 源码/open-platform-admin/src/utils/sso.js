const resolveSsoCallbackOrigin = () => {
  const configuredOrigin = import.meta.env.VITE_SSO_CALLBACK_ORIGIN?.trim()
  if (!configuredOrigin) {
    return window.location.origin
  }

  try {
    return new URL(configuredOrigin).origin
  } catch (error) {
    console.warn('Invalid VITE_SSO_CALLBACK_ORIGIN, fallback to current origin:', configuredOrigin, error)
    return window.location.origin
  }
}

export const buildSsoCallbackUrl = () => new URL('/callback', resolveSsoCallbackOrigin()).toString()

export const redirectToSsoLogin = (redirect = '/dashboard') => {
  const callbackUrl = buildSsoCallbackUrl()
  sessionStorage.setItem('admin_sso_redirect_uri', callbackUrl)
  sessionStorage.setItem('admin_sso_redirect', redirect || '/dashboard')
  const state = `${Date.now()}_${Math.random().toString(36).slice(2)}`
  sessionStorage.setItem('admin_sso_state', state)

  const loginUrl = new URL(import.meta.env.VITE_SSO_AUTHORIZATION_URL || 'https://login.nstl.gov.cn/oauth/authorize')
  loginUrl.searchParams.set('client_id', import.meta.env.VITE_SSO_CLIENT_ID || 'rehm1m79tm4q01cd')
  loginUrl.searchParams.set('redirect_uri', callbackUrl)
  loginUrl.searchParams.set('response_type', 'code')
  loginUrl.searchParams.set('state', state)
  window.location.href = loginUrl.toString()
}
