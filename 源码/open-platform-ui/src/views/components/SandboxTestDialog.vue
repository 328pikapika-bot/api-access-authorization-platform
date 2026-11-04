<template>
  <t-dialog
    v-model:visible="visible"
    header="沙箱测试"
    :width="900"
    :close-btn="true"
    :close-on-overlay-click="false"
    :confirm-btn="null"
    :cancel-btn="{ content: '关闭', variant: 'outline' }"
    @close="handleClose"
  >
    <div class="sandbox-test-container">
      <div class="request-section">
        <div class="url-bar">
          <t-select v-model="requestMethod" class="method-select" :disabled="true">
            <t-option value="GET">GET</t-option>
            <t-option value="POST">POST</t-option>
            <t-option value="PUT">PUT</t-option>
            <t-option value="DELETE">DELETE</t-option>
          </t-select>
          <t-input v-model="requestUrl" class="url-input" disabled placeholder="接口地址" />
          <t-button theme="primary" :loading="loading" @click="sendRequest">
            <template #icon><send-icon /></template>
            发送请求
          </t-button>
        </div>

        <t-tabs v-model="activeRequestTab">
          <t-tab-panel value="params" label="Params">
            <div class="params-section">
              <div class="param-list">
                <div v-for="(param, index) in paramList" :key="index" class="param-row">
                  <t-input v-model="param.key" placeholder="参数名" class="param-key" />
                  <t-input v-model="param.value" placeholder="参数值" class="param-value" />
                  <t-button variant="text" theme="danger" @click="removeParam(index)">
                    <delete-icon />
                  </t-button>
                </div>
              </div>
              <t-button variant="text" theme="primary" @click="addParam">
                <template #icon><add-icon /></template>
                添加参数
              </t-button>
            </div>
          </t-tab-panel>

          <t-tab-panel value="headers" label="Headers">
            <div class="headers-section">
              <div class="header-list">
                <div v-for="(header, index) in headerList" :key="index" class="header-row">
                  <t-input v-model="header.key" placeholder="Header 名" class="header-key" />
                  <t-input v-model="header.value" placeholder="Header 值" class="header-value" />
                  <t-button variant="text" theme="danger" @click="removeHeader(index)">
                    <delete-icon />
                  </t-button>
                </div>
              </div>
              <t-button variant="text" theme="primary" @click="addHeader">
                <template #icon><add-icon /></template>
                添加 Header
              </t-button>
            </div>
          </t-tab-panel>

          <t-tab-panel v-if="['POST', 'PUT'].includes(requestMethod)" value="body" label="Body">
            <div class="body-section">
              <t-radio-group v-model="bodyType" class="body-type-selector">
                <t-radio-button value="none">none</t-radio-button>
                <t-radio-button value="form-data">form-data</t-radio-button>
                <t-radio-button value="x-www-form-urlencoded">x-www-form-urlencoded</t-radio-button>
                <t-radio-button value="raw">raw</t-radio-button>
              </t-radio-group>

              <div v-if="bodyType === 'form-data' || bodyType === 'x-www-form-urlencoded'" class="form-data-editor">
                <div v-for="(item, index) in bodyFormData" :key="index" class="form-data-row">
                  <t-input v-model="item.key" placeholder="字段名" class="form-data-key" />
                  <t-input v-model="item.value" placeholder="字段值" class="form-data-value" />
                  <t-button variant="text" theme="danger" @click="removeBodyItem(index)">
                    <delete-icon />
                  </t-button>
                </div>
                <t-button variant="text" theme="primary" @click="addBodyItem">
                  <template #icon><add-icon /></template>
                  添加字段
                </t-button>
              </div>

              <div v-if="bodyType === 'raw'" class="raw-editor">
                <t-textarea v-model="rawBody" placeholder="请输入请求体 JSON" :rows="10" />
              </div>
            </div>
          </t-tab-panel>
        </t-tabs>
      </div>

      <t-divider />

      <div class="response-section">
        <div class="response-header">
          <h4>响应结果</h4>
          <div v-if="responseStatus !== null" class="response-status">
            <t-tag :theme="getStatusTheme(responseStatus)" size="small">
              {{ responseStatus }}
            </t-tag>
            <span class="response-time">{{ responseTime }}ms</span>
          </div>
        </div>

        <t-tabs v-model="activeResponseTab">
          <t-tab-panel value="body" label="Body">
            <div class="response-body">
              <div v-if="responseBody" class="response-actions">
                <t-button variant="text" theme="primary" size="small" @click="copyBody(responseBody)">
                  <template #icon><file-copy-icon /></template>
                  复制 Body
                </t-button>
              </div>
              <pre v-if="responseBody" class="json-preview"><code>{{ formatJson(responseBody) }}</code></pre>
              <t-empty v-else description="暂无响应数据" />
            </div>
          </t-tab-panel>

          <t-tab-panel value="headers" label="Headers">
            <div class="response-headers">
              <div v-if="responseHeaders && Object.keys(responseHeaders).length > 0" class="headers-table">
                <div v-for="(value, key) in responseHeaders" :key="key" class="header-item">
                  <span class="header-name">{{ key }}:</span>
                  <span class="header-value">{{ value }}</span>
                </div>
              </div>
              <t-empty v-else description="暂无响应头" />
            </div>
          </t-tab-panel>
        </t-tabs>
      </div>

      <t-divider />

      <div class="curl-section">
        <div class="curl-header">
          <h4>cURL 命令</h4>
          <t-button variant="text" theme="primary" size="small" @click="copyCurl">
            <template #icon><file-copy-icon /></template>
            复制
          </t-button>
        </div>
        <div class="curl-content">
          <pre class="curl-command">{{ curlCommand }}</pre>
        </div>
      </div>
    </div>

    <t-dialog v-model:visible="apiKeyDialogVisible" header="请输入 API Key" :width="400" @confirm="confirmApiKey">
      <t-input v-model="apiKey" placeholder="请输入您的 X-API-KEY" type="password" />
      <p class="api-key-hint">可在“我的授权接口”中获取 API Key</p>
    </t-dialog>
  </t-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { SendIcon, AddIcon, DeleteIcon, FileCopyIcon } from 'tdesign-icons-vue-next'
import { executeSandboxDebug } from '@/api/sandbox'
import { getInterfaceConfigByCategoryId } from '@/api/interfaceConfig'
import { copyWithFeedback } from '@/utils/clipboard'

const props = defineProps({
  modelValue: Boolean,
  categoryId: Number,
  apiKey: String
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const apiKeyDialogVisible = ref(false)
const apiKey = ref(props.apiKey || '')
const requestMethod = ref('GET')
const requestUrl = ref('')
const activeRequestTab = ref('params')
const paramList = reactive([])
const headerList = reactive([{ key: 'Content-Type', value: 'application/json' }])
const bodyType = ref('none')
const bodyFormData = reactive([])
const rawBody = ref('')
const responseStatus = ref(null)
const responseTime = ref(0)
const responseBody = ref(null)
const responseHeaders = ref({})
const activeResponseTab = ref('body')
const mockoonServiceUrl = ref('')

watch(() => props.apiKey, (val) => {
  if (val) apiKey.value = val
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.categoryId) {
    loadInterfaceConfig()
  }
})

watch(() => visible.value, (val) => emit('update:modelValue', val))

const loadInterfaceConfig = async () => {
  try {
    const res = await getInterfaceConfigByCategoryId(props.categoryId)
    if (res.code === 200) {
      const config = res.data || {}
      requestMethod.value = config.apiMethod || 'GET'
      requestUrl.value = config.apiPath || ''
      mockoonServiceUrl.value = config.mockoonServiceUrl || ''
      resetCollections()

      if (config.defaultParams) {
        try {
          const params = JSON.parse(config.defaultParams)
          Object.keys(params || {}).forEach((key) => paramList.push({ key, value: String(params[key]) }))
        } catch (error) {
          console.error('解析默认参数失败', error)
        }
      }

      if (config.defaultHeaders) {
        try {
          const headers = JSON.parse(config.defaultHeaders)
          Object.keys(headers || {}).forEach((key) => headerList.push({ key, value: String(headers[key]) }))
        } catch (error) {
          console.error('解析默认请求头失败', error)
        }
      }

      if (config.bodyTemplate) {
        bodyType.value = 'raw'
        rawBody.value = config.bodyTemplate
      }
    }
  } catch (error) {
    console.error('加载接口配置失败', error)
    resetRequestData()
  }
}

const resetCollections = () => {
  paramList.length = 0
  headerList.length = 0
  headerList.push({ key: 'Content-Type', value: 'application/json' })
  bodyFormData.length = 0
  bodyType.value = 'none'
  rawBody.value = ''
}

const resetRequestData = () => {
  requestMethod.value = 'GET'
  requestUrl.value = ''
  mockoonServiceUrl.value = ''
  resetCollections()
}

const addParam = () => paramList.push({ key: '', value: '' })
const removeParam = (index) => paramList.splice(index, 1)
const addHeader = () => headerList.push({ key: '', value: '' })
const removeHeader = (index) => headerList.splice(index, 1)
const addBodyItem = () => bodyFormData.push({ key: '', value: '' })
const removeBodyItem = (index) => bodyFormData.splice(index, 1)

const curlCommand = computed(() => {
  const paramsObj = {}
  paramList.forEach((item) => {
    if (item.key) {
      paramsObj[item.key] = item.value
    }
  })

  let baseUrl = mockoonServiceUrl.value
  if (baseUrl && baseUrl.endsWith('/')) {
    baseUrl = baseUrl.slice(0, -1)
  }

  let path = requestUrl.value
  if (path && !path.startsWith('/')) {
    path = '/' + path
  }

  let url = baseUrl + path
  if (Object.keys(paramsObj).length > 0) {
    const searchParams = new URLSearchParams(paramsObj)
    url += (url.includes('?') ? '&' : '?') + searchParams.toString()
  }

  let curl = `curl -X ${requestMethod.value} "${url}"`
  headerList.forEach((header) => {
    if (header.key && header.value) {
      curl += ` -H "${header.key}: ${header.value}"`
    }
  })

  if (['POST', 'PUT'].includes(requestMethod.value) && bodyType.value !== 'none') {
    let bodyData = ''
    if (bodyType.value === 'raw') {
      bodyData = rawBody.value
    } else {
      const params = new URLSearchParams()
      bodyFormData.forEach((item) => {
        if (item.key && item.value) {
          params.append(item.key, item.value)
        }
      })
      bodyData = params.toString()
    }
    if (bodyData) {
      curl += ` -d '${bodyData}'`
    }
  }

  return curl
})

const copyCurl = () => copyWithFeedback(curlCommand.value, '已复制 cURL 命令')
const copyBody = (data) => copyWithFeedback(data, '已复制响应 Body')

const sendRequest = async () => {
  if (!apiKey.value) {
    apiKeyDialogVisible.value = true
    return
  }

  loading.value = true
  const startTime = Date.now()
  try {
    const params = {}
    paramList.forEach((item) => {
      if (item.key) {
        params[item.key] = item.value
      }
    })

    const headers = {}
    headerList.forEach((item) => {
      if (item.key) {
        headers[item.key] = item.value
      }
    })

    let body = null
    const bodyFormDataMap = {}
    if (['POST', 'PUT'].includes(requestMethod.value) && bodyType.value !== 'none') {
      if (bodyType.value === 'raw') {
        try {
          body = JSON.parse(rawBody.value)
        } catch (error) {
          body = rawBody.value
        }
      } else {
        bodyFormData.forEach((item) => {
          if (item.key) {
            bodyFormDataMap[item.key] = item.value
          }
        })
        body = bodyFormDataMap
      }
    }

    const requestData = {
      categoryId: props.categoryId,
      contentPath: requestUrl.value,
      method: requestMethod.value,
      params,
      headers,
      body,
      bodyType: bodyType.value,
      bodyFormData: bodyFormDataMap,
      mockoonServiceId: null
    }

    const res = await executeSandboxDebug(requestData, apiKey.value)
    responseTime.value = Date.now() - startTime
    const sandboxRes = res.data || {}
    responseStatus.value = sandboxRes.httpStatus !== undefined ? sandboxRes.httpStatus : 200
    responseBody.value = sandboxRes.body
    responseHeaders.value = sandboxRes.headers || { 'Content-Type': 'application/json' }
  } catch (error) {
    responseTime.value = Date.now() - startTime
    responseStatus.value = error.response?.status || 500
    responseBody.value = { error: error.message || '请求失败' }
  } finally {
    loading.value = false
  }
}

const confirmApiKey = () => {
  if (!apiKey.value.trim()) {
    return
  }
  apiKeyDialogVisible.value = false
  sendRequest()
}

const formatJson = (data) => {
  try {
    return JSON.stringify(data, null, 2)
  } catch (error) {
    return String(data)
  }
}

const getStatusTheme = (status) => (status === 200 ? 'success' : 'danger')

const handleClose = () => {
  responseStatus.value = null
  responseBody.value = null
  responseHeaders.value = {}
}
</script>

<style scoped>
.sandbox-test-container { display: flex; flex-direction: column; max-height: 65vh; overflow-y: auto; padding-right: 8px; }
.url-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.method-select { width: 100px; }
.url-input { flex: 1; }
.params-section, .headers-section, .body-section { padding: 16px 0; }
.param-list, .header-list { margin-bottom: 12px; }
.param-row, .header-row, .form-data-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.param-key, .header-key, .form-data-key { width: 180px; }
.param-value, .header-value, .form-data-value { flex: 1; }
.body-type-selector { margin-bottom: 16px; }
.response-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.response-header h4 { margin: 0; }
.response-status { display: flex; align-items: center; gap: 12px; }
.response-time { color: #666; font-size: 12px; }
.response-body { position: relative; }
.response-actions { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.json-preview { background: #f5f5f5; padding: 16px; border-radius: 4px; overflow-x: auto; margin: 0; font-family: 'Courier New', monospace; font-size: 13px; line-height: 1.5; }
.headers-table { background: #f5f5f5; padding: 12px; border-radius: 4px; }
.header-item { display: flex; margin-bottom: 8px; font-size: 13px; }
.header-name { color: #666; min-width: 150px; font-weight: 500; }
.header-value { color: #333; word-break: break-all; }
.curl-section { margin-top: 8px; }
.curl-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.curl-header h4 { margin: 0; }
.curl-content { background: #1e1e1e; padding: 12px; border-radius: 4px; }
.curl-command { color: #4ec9b0; font-family: 'Courier New', monospace; font-size: 12px; line-height: 1.6; margin: 0; white-space: pre-wrap; word-break: break-all; }
.api-key-hint { margin-top: 8px; color: #666; font-size: 12px; }
.raw-editor { margin-top: 12px; }
</style>
