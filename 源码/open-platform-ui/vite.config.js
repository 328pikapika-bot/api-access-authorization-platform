import fs from 'node:fs'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const LOCAL_NSTL_PRIMARY_HOST = 'www.nstl.gov.cn'
const LOCAL_NSTL_HOSTS = [LOCAL_NSTL_PRIMARY_HOST, 'nstl.gov.cn']
const DEFAULT_PUBLIC_BASE = '/'
const PRODUCTION_PUBLIC_BASE = '/open-platform-web/'
const DEFAULT_MAIN_SITE_TARGET = 'https://127.0.0.1'
const LOCAL_CERT_PATH = resolve(__dirname, '../certs/www.nstl.gov.cn.pfx')
const LOCAL_CERT_PASSPHRASE = 'interface-platform-local'

function normalizePublicBase(value = DEFAULT_PUBLIC_BASE) {
  const trimmed = String(value || '').trim()
  if (!trimmed || trimmed === '/') {
    return '/'
  }
  return `/${trimmed.replace(/^\/+|\/+$/g, '')}/`
}

function isAssetRequest(pathname, publicBase) {
  if (
    pathname.startsWith('/api/') ||
    pathname.startsWith('/auth/') ||
    pathname.startsWith('/contents/') ||
    pathname.startsWith('/sandbox/') ||
    pathname.startsWith('/doc.html') ||
    pathname.startsWith('/webjars/') ||
    pathname.startsWith('/swagger-resources/') ||
    pathname.startsWith('/v2/api-docs') ||
    pathname.startsWith('/v3/api-docs')
  ) {
    return true
  }

  if (pathname === publicBase || pathname === `${publicBase}index.html`) {
    return true
  }

  const suffix = pathname.startsWith(publicBase)
    ? pathname.slice(publicBase.length - 1)
    : pathname

  return (
    suffix.startsWith('/@') ||
    suffix.startsWith('/node_modules/') ||
    suffix.startsWith('/src/') ||
    suffix.startsWith('/images/') ||
    suffix.startsWith('/fonts/') ||
    suffix.startsWith('/css/') ||
    suffix.startsWith('/js/') ||
    /\.[a-zA-Z0-9]+$/.test(suffix)
  )
}

function createSpaFallbackPlugin(publicBase) {
  return {
    name: 'nstl-base-spa-fallback',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.method !== 'GET' || !req.url) {
          return next()
        }

        const url = new URL(req.url, 'http://127.0.0.1')
        if (!url.pathname.startsWith(publicBase) || isAssetRequest(url.pathname, publicBase)) {
          return next()
        }

        req.url = `${publicBase}index.html`
        next()
      })
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '')
  const useNstlLocalHost = mode === 'nstl-local'
  const defaultPublicBase = mode === 'production' ? PRODUCTION_PUBLIC_BASE : DEFAULT_PUBLIC_BASE
  const publicBase = normalizePublicBase(env.VITE_PUBLIC_BASE || defaultPublicBase)
  const backendTarget = env.VITE_PROXY_BACKEND || 'http://127.0.0.1:17300'
  const mainSiteTarget = env.VITE_NSTL_MAIN_SITE_TARGET || DEFAULT_MAIN_SITE_TARGET
  const localHttps = useNstlLocalHost
    ? (() => {
        if (!fs.existsSync(LOCAL_CERT_PATH)) {
          throw new Error(`Missing local HTTPS certificate: ${LOCAL_CERT_PATH}`)
        }
        return {
          pfx: fs.readFileSync(LOCAL_CERT_PATH),
          passphrase: LOCAL_CERT_PASSPHRASE,
        }
      })()
    : undefined

  return {
    base: publicBase,
    plugins: [vue(), createSpaFallbackPlugin(publicBase)],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
        '@shared': resolve(__dirname, '../shared'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 18300,
      strictPort: true,
      https: localHttps,
      hmr: useNstlLocalHost
        ? {
            host: LOCAL_NSTL_PRIMARY_HOST,
          }
        : undefined,
      allowedHosts: [...LOCAL_NSTL_HOSTS, '127.0.0.1', 'localhost'],
      proxy: {
        '/api/v1/service/open_platform': {
          target: backendTarget,
          changeOrigin: true,
        },
        '/api/service/nstl/web/': {
          target: mainSiteTarget,
          changeOrigin: true,
          secure: false,
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              proxyReq.setHeader('Host', LOCAL_NSTL_PRIMARY_HOST)
            })
          },
        },
      },
    },
  }
})
