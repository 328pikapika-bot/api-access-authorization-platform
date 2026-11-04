import dayjs from 'dayjs'

const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss'
const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/

function formatRangeValue(value, fallbackTime) {
  if (!value) return ''

  if (typeof value === 'string') {
    const normalized = value.trim().replace('T', ' ')
    if (!normalized) return ''
    if (DATE_ONLY_PATTERN.test(normalized)) {
      return `${normalized} ${fallbackTime}`
    }
    const parsed = dayjs(normalized)
    return parsed.isValid() ? parsed.format(DATE_TIME_FORMAT) : normalized.slice(0, 19)
  }

  if (value instanceof Date) {
    const parsed = dayjs(value)
    return parsed.isValid() ? parsed.format(DATE_TIME_FORMAT) : ''
  }

  if (typeof value?.format === 'function') {
    const formatted = value.format(DATE_TIME_FORMAT)
    return formatted === 'Invalid Date' ? '' : formatted
  }

  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format(DATE_TIME_FORMAT) : ''
}

export function normalizeTimeRangeValue(value) {
  if (!value) {
    return []
  }

  if (Array.isArray(value)) {
    return value.filter((item) => item !== null && item !== undefined && item !== '')
  }

  if (Array.isArray(value?.date)) {
    return normalizeTimeRangeValue(value.date)
  }

  if (Array.isArray(value?.value)) {
    return normalizeTimeRangeValue(value.value)
  }

  if (Array.isArray(value?.formattedValue)) {
    return normalizeTimeRangeValue(value.formattedValue)
  }

  if (Array.isArray(value?.dates)) {
    return normalizeTimeRangeValue(value.dates)
  }

  if (Array.isArray(value?.dayjsValue)) {
    return normalizeTimeRangeValue(value.dayjsValue)
  }

  return []
}

export function pickUsableTimeRange(currentRange, cachedRange) {
  const normalizedCurrentRange = normalizeTimeRangeValue(currentRange)
  if (normalizedCurrentRange.length === 2) {
    return normalizedCurrentRange
  }

  const normalizedCachedRange = normalizeTimeRangeValue(cachedRange)
  return normalizedCachedRange.length === 2 ? normalizedCachedRange : normalizedCurrentRange
}

export function mergeTimeRangeInputValue(currentRange, cachedRange, index, value) {
  const baseRange = pickUsableTimeRange(currentRange, cachedRange)
  const nextRange = [
    baseRange[0] || '',
    baseRange[1] || ''
  ]
  nextRange[index] = value || ''
  return nextRange
}

export function buildTimeRangeParams(range) {
  const normalizedRange = normalizeTimeRangeValue(range)
  if (normalizedRange.length !== 2) {
    return {}
  }

  const startTime = formatRangeValue(normalizedRange[0], '00:00:00')
  const endTime = formatRangeValue(normalizedRange[1], '23:59:59')

  if (!startTime || !endTime) {
    return {}
  }

  return { startTime, endTime }
}
