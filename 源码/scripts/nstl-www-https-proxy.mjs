import dns from 'node:dns'
import fs from 'node:fs'
import http from 'node:http'
import https from 'node:https'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..')
const certPath = path.join(repoRoot, 'certs', 'www.nstl.gov.cn.pfx')

const listenHost = process.env.NSTL_PROXY_LISTEN_HOST || '0.0.0.0'
const listenPort = Number(process.env.NSTL_PROXY_LISTEN_PORT || 443)
const targetHost = process.env.NSTL_PROXY_TARGET_HOST || 'www.nstl.gov.cn'
const targetIp = process.env.NSTL_PROXY_TARGET_IP || '127.0.0.1'
const portalPublicPath = normalizePath(process.env.NSTL_PROXY_PORTAL_PUBLIC_PATH || '/interface_platform_main')
const adminPublicPath = normalizePath(process.env.NSTL_PROXY_ADMIN_PUBLIC_PATH || '/interface_platform_admin')
const portalFrontendPort = Number(process.env.NSTL_PROXY_LOCAL_PORTAL_PORT || 18300)
const adminFrontendPort = Number(process.env.NSTL_PROXY_LOCAL_ADMIN_PORT || 18301)
const localFrontendHost = process.env.NSTL_PROXY_LOCAL_FRONTEND_HOST || '127.0.0.1'
const backendHost = process.env.NSTL_PROXY_LOCAL_BACKEND_HOST || '127.0.0.1'
const backendPort = Number(process.env.NSTL_PROXY_LOCAL_BACKEND_PORT || 17300)
const passphrase = process.env.NSTL_PROXY_CERT_PASSPHRASE || 'interface-platform-local'

if (!fs.existsSync(certPath)) {
  console.error(`[nstl-www-proxy] Missing certificate: ${certPath}`)
  process.exit(1)
}

const server = https.createServer(
  {
    pfx: fs.readFileSync(certPath),
    passphrase,
  },
  (clientReq, clientRes) => {
    if (shouldProxyToPortal(clientReq.url)) {
      proxyToLocalHttp(clientReq, clientRes, portalFrontendPort, 'portal')
      return
    }

    if (shouldProxyToAdmin(clientReq.url)) {
      proxyToLocalHttp(clientReq, clientRes, adminFrontendPort, 'admin')
      return
    }

    if (shouldProxyToBackend(clientReq.url)) {
      proxyToLocalHttp(clientReq, clientRes, backendPort, 'backend')
      return
    }

    proxyToRemoteNstl(clientReq, clientRes)
  },
)

function proxyToLocalHttp(clientReq, clientRes, port, label) {
  const headers = {
    ...clientReq.headers,
    host: `127.0.0.1:${port}`,
  }

  const upstreamReq = http.request(
    {
      host: localFrontendHost,
      port,
      method: clientReq.method,
      path: clientReq.url,
      headers,
    },
    (upstreamRes) => {
      clientRes.writeHead(upstreamRes.statusCode || 502, upstreamRes.statusMessage, upstreamRes.headers)
      upstreamRes.pipe(clientRes)
    },
  )

  upstreamReq.on('error', (error) => {
    console.error(`[nstl-www-proxy] local ${label} error:`, error.message)
    if (!clientRes.headersSent) {
      clientRes.writeHead(502, { 'content-type': 'text/plain; charset=utf-8' })
    }
    clientRes.end(`Local ${label} proxy error: ${error.message}`)
  })

  clientReq.pipe(upstreamReq)
}

function proxyToRemoteNstl(clientReq, clientRes) {
  const headers = {
    ...clientReq.headers,
    host: targetHost,
  }

  const upstreamReq = https.request(
    {
      host: targetHost,
      servername: targetHost,
      method: clientReq.method,
      path: clientReq.url,
      headers,
      rejectUnauthorized: false,
      lookup(hostname, options, callback) {
        if (hostname === targetHost) {
          callback(null, targetIp, 4)
          return
        }
        dns.lookup(hostname, options, callback)
      },
    },
    (upstreamRes) => {
      const responseHeaders = { ...upstreamRes.headers }
      delete responseHeaders['content-security-policy']
      delete responseHeaders['content-security-policy-report-only']
      delete responseHeaders['x-frame-options']
      clientRes.writeHead(upstreamRes.statusCode || 502, upstreamRes.statusMessage, responseHeaders)
      upstreamRes.pipe(clientRes)
    },
  )

  upstreamReq.on('error', (error) => {
    console.error('[nstl-www-proxy] upstream error:', error.message)
    if (!clientRes.headersSent) {
      clientRes.writeHead(502, { 'content-type': 'text/plain; charset=utf-8' })
    }
    clientRes.end(`NSTL proxy upstream error: ${error.message}`)
  })

  clientReq.pipe(upstreamReq)
}

function shouldProxyToPortal(value) {
  const pathname = getPathname(value)
  return pathname === portalPublicPath || pathname.startsWith(`${portalPublicPath}/`)
}

function shouldProxyToAdmin(value) {
  const pathname = getPathname(value)
  return pathname === adminPublicPath || pathname.startsWith(`${adminPublicPath}/`)
}

function shouldProxyToBackend(value) {
  const pathname = getPathname(value)
  return (
    pathname.startsWith('/api/v1/service/open_platform/')
    || pathname === '/doc.html'
    || pathname.startsWith('/webjars/')
    || pathname.startsWith('/swagger-resources/')
    || pathname.startsWith('/v2/api-docs')
    || pathname.startsWith('/v3/api-docs')
  )
}

function getPathname(value) {
  try {
    return new URL(value, 'https://www.nstl.gov.cn').pathname
  } catch {
    return '/'
  }
}

function normalizePath(value) {
  const normalized = `/${String(value || '').replace(/^\/+|\/+$/g, '')}`
  return normalized === '/' ? '/interface_platform_main' : normalized
}

server.on('error', (error) => {
  console.error('[nstl-www-proxy] server error:', error.message)
  if (error.code === 'EACCES') {
    console.error('[nstl-www-proxy] Port 443 may require Administrator privileges on Windows.')
  }
  process.exit(1)
})

server.listen(listenPort, listenHost, () => {
  console.log(`[nstl-www-proxy] https://www.nstl.gov.cn -> portal:${portalFrontendPort}, admin:${adminFrontendPort}, backend:${backendPort}`)
})
