const ADMIN_LOGGING_OUT_UNTIL_KEY = 'admin_logging_out_until'

export function markAdminLoggingOut(durationMs = 8000) {
  sessionStorage.setItem(ADMIN_LOGGING_OUT_UNTIL_KEY, String(Date.now() + durationMs))
}

export function clearAdminLoggingOut() {
  sessionStorage.removeItem(ADMIN_LOGGING_OUT_UNTIL_KEY)
}

export function isAdminLoggingOut() {
  const until = Number(sessionStorage.getItem(ADMIN_LOGGING_OUT_UNTIL_KEY) || 0)
  return Number.isFinite(until) && Date.now() < until
}

export function isAdminSessionRequestError(error) {
  return Boolean(error?.__adminLoggingOut) || isAdminLoggingOut()
}
