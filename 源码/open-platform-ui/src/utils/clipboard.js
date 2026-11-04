import { MessagePlugin } from 'tdesign-vue-next'

function normalizeClipboardText(value) {
  if (typeof value === 'string') {
    return value
  }
  if (value === null || value === undefined) {
    return ''
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch (error) {
    return String(value)
  }
}

export async function copyWithFeedback(value, successMessage = '已复制到剪贴板') {
  const text = normalizeClipboardText(value)
  try {
    await navigator.clipboard.writeText(text)
    MessagePlugin.success(successMessage)
    return true
  } catch (error) {
    try {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      textarea.style.pointerEvents = 'none'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      MessagePlugin.success(successMessage)
      return true
    } catch (fallbackError) {
      MessagePlugin.error('复制失败，请手动复制')
      return false
    }
  }
}
