<template>
  <t-dialog
    v-model:visible="visible"
    header="接口配置"
    :width="900"
    :confirm-btn="{ content: '保存', loading: loading }"

    @confirm="handleSubmit"
    @close="handleClose"
  >
    <div class="sandbox-test-container">
      <t-form :data="formData" :rules="rules" ref="formRef" label-width="100px">
        <!-- 基本配置区域 -->
        <div class="request-section">
          <div class="url-bar">
            <t-select v-model="formData.apiMethod" class="method-select">
              <t-option value="GET">GET</t-option>
              <t-option value="POST">POST</t-option>
              <t-option value="PUT">PUT</t-option>
              <t-option value="DELETE">DELETE</t-option>
            </t-select>
            <t-input
              v-model="formData.apiPath"
              class="url-input"
              placeholder="例如: /api/doc/search"
              @paste="handleApiPathPaste"
              @change="handleApiPathChange"
            />
            <t-button
              variant="outline"
              :loading="testLoading"
              :disabled="!formData.mockoonServiceId && !isAbsoluteApiUrl"
              @click="handleTest"
            >
              <template #icon><play-icon /></template>
              测试执行
            </t-button>
          </div>

          <t-row :gutter="16">
            <t-col :span="12">
              <t-form-item label="Mockoon服务" name="mockoonServiceId">
                <t-select v-model="formData.mockoonServiceId" clearable placeholder="选择关联的模拟服务">
                  <t-option
                    v-for="service in serviceList"
                    :key="service.id"
                    :value="service.id"
                    :label="service.name"
                  />
                </t-select>
              </t-form-item>
            </t-col>
          </t-row>


          <!-- 请求配置 Tabs -->
          <t-tabs v-model="activeTab" class="config-tabs">
            <t-tab-panel value="params" :label="`Params (${validParamCount})`">
              <div class="params-section">
                <div class="param-list">
                  <div v-for="(item, index) in paramList" :key="index" class="param-row">
                    <t-input v-model="item.key" placeholder="参数名" class="param-key" />
                    <t-input v-model="item.value" placeholder="参数值" class="param-value" />
                    <t-button variant="text" theme="danger" @click="removeItem('param', index)">
                      <delete-icon />
                    </t-button>
                  </div>
                </div>
                <t-button variant="text" theme="primary" @click="addItem('param')">
                  <template #icon><add-icon /></template>
                  添加参数
                </t-button>
              </div>
            </t-tab-panel>

            <t-tab-panel value="headers" label="Headers">
              <div class="headers-section">
                <div class="header-list">
                  <div v-for="(item, index) in headerList" :key="index" class="header-row">
                    <t-input v-model="item.key" placeholder="Header名" class="header-key" />
                    <t-input v-model="item.value" placeholder="Header值" class="header-value" />
                    <t-button variant="text" theme="danger" @click="removeItem('header', index)">
                      <delete-icon />
                    </t-button>
                  </div>
                </div>
                <t-button variant="text" theme="primary" @click="addItem('header')">
                  <template #icon><add-icon /></template>
                  添加 Header
                </t-button>
              </div>
            </t-tab-panel>

            <t-tab-panel value="body" label="Body">
              <div class="body-section">
                <t-radio-group v-model="formData.bodyType" class="body-type-selector">
                  <t-radio-button value="none">none</t-radio-button>
                  <t-radio-button value="form-data">form-data</t-radio-button>
                  <t-radio-button value="x-www-form-urlencoded">x-www-form-urlencoded</t-radio-button>
                  <t-radio-button value="raw">raw</t-radio-button>
                </t-radio-group>

                <div v-if="formData.bodyType === 'form-data' || formData.bodyType === 'x-www-form-urlencoded'" class="form-data-editor">
                  <div v-for="(item, index) in bodyFormData" :key="index" class="form-data-row">
                    <t-input v-model="item.key" placeholder="字段名" class="form-data-key" />
                    <t-input v-model="item.value" placeholder="字段值" class="form-data-value" />
                    <t-button variant="text" theme="danger" @click="removeItem('body', index)">
                      <delete-icon />
                    </t-button>
                  </div>
                  <t-button variant="text" theme="primary" @click="addItem('body')">
                    <template #icon><add-icon /></template>
                    添加字段
                  </t-button>
                </div>

                <div v-if="formData.bodyType === 'raw'" class="raw-editor">
                  <t-textarea v-model="formData.bodyTemplate" placeholder="请输入请求体 JSON 模板" :rows="10" />
                </div>
              </div>
            </t-tab-panel>

          </t-tabs>
        </div>

        <t-form-item label="接口说明" name="description" style="margin-top: 16px">
          <t-textarea v-model="formData.description" placeholder="接口功能说明" :rows="2" />
        </t-form-item>
      </t-form>

      <t-divider v-if="testResponse || curlCommand" />

      <!-- 响应区域 -->
      <div v-if="testResponse" class="response-section">
        <div class="response-header">
          <h4>响应结果</h4>
          <div class="response-status">
            <t-tag :theme="testResponse.code === 200 ? 'success' : 'danger'" size="small">
              {{ testResponse.code !== undefined ? testResponse.code : 200 }}
            </t-tag>
            <span class="response-time" v-if="testTime">{{ testTime }}ms</span>
          </div>
        </div>

        <t-tabs v-model="activeResponseTab">
          <t-tab-panel value="body" label="Body">
            <div class="response-body">
              <div class="response-actions" v-if="testResponse.data">
                <t-button variant="text" theme="primary" size="small" @click="copyBody(testResponse.data)">
                  <template #icon><file-copy-icon /></template>
                  复制 Body
                </t-button>
              </div>
              <pre v-if="testResponse.data" class="json-preview"><code>{{ formatJson(testResponse.data) }}</code></pre>
              <t-empty v-else description="暂无响应数据" />
            </div>
          </t-tab-panel>

          <t-tab-panel value="headers" label="Headers">
            <div class="response-headers">
              <div v-if="testResponse.headers && Object.keys(testResponse.headers).length > 0" class="headers-table">
                <div v-for="(value, key) in testResponse.headers" :key="key" class="header-item">
                  <span class="header-name">{{ key }}:</span>
                  <span class="header-value">{{ value }}</span>
                </div>
              </div>
              <t-empty v-else description="暂无响应头" />
            </div>
          </t-tab-panel>
        </t-tabs>
      </div>


      <!-- curl 命令展示 -->
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

  </t-dialog>
</template>


<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { AddIcon, DeleteIcon, PlayIcon, FileCopyIcon } from 'tdesign-icons-vue-next'
import { copyWithFeedback } from '@/utils/clipboard'

import {
  saveInterfaceConfig,
  getInterfaceConfigByCategoryId
} from '@/api/interfaceConfig'
import { getActiveServiceList } from '@/api/mockoonService'
import { executeAdminDebug } from '@/api/sandbox'


const props = defineProps({
  modelValue: Boolean,
  categoryId: Number,
  categoryName: String
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(false)
const loading = ref(false)
const testLoading = ref(false)
const formRef = ref(null)
const serviceList = ref([])
const activeTab = ref('params')

// 测试相关状态
const testResponse = ref(null)
const testTime = ref(0)
const activeResponseTab = ref('body')


// 列表形式的参数和请求头
const paramList = ref([{ key: '', value: '' }])
const headerList = ref([{ key: '', value: '' }])
const bodyFormData = ref([{ key: '', value: '' }])
const validParamCount = computed(() => paramList.value.filter(item => String(item.key || '').trim()).length)
const isAbsoluteApiUrl = computed(() => /^https?:\/\//i.test(String(formData.apiPath || '').trim()))

// 表单数据
const formData = reactive({
  categoryId: null,
  apiPath: '',
  apiMethod: 'GET',
  contentType: 'application/json',
  defaultParams: '',
  defaultHeaders: '',
  bodyTemplate: '',
  bodyType: 'none',
  bodyFormData: '',
  mockoonServiceId: null,
  description: ''
})



// 表单验证规则
const rules = {
  apiPath: [{ required: true, message: '请输入接口路径', type: 'error' }],
  apiMethod: [{ required: true, message: '请选择请求方法', type: 'error' }]
}

// 监听visible变化
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.categoryId) {
    formData.categoryId = props.categoryId
    fetchConfig()
  }
})

watch(() => visible.value, (val) => {
  emit('update:modelValue', val)
})

// 获取Mockoon服务列表
const fetchServiceList = async () => {
  try {
    const res = await getActiveServiceList()
    if (res.code === 200) {
      serviceList.value = res.data
    }
  } catch (error) {
    console.error('获取服务列表失败', error)
  }
}

// 获取已有配置
const fetchConfig = async () => {
  try {
    const res = await getInterfaceConfigByCategoryId(props.categoryId)
    if (res.code === 200 && res.data !== null) {
      const data = res.data
      formData.apiPath = data.apiPath || ''
      formData.apiMethod = data.apiMethod || 'GET'
      formData.contentType = data.contentType || 'application/json'
      formData.defaultParams = data.defaultParams || ''
      formData.defaultHeaders = data.defaultHeaders || ''
      formData.bodyTemplate = data.bodyTemplate || ''
      formData.bodyType = data.bodyType || 'none'
      formData.bodyFormData = data.bodyFormData || ''
      formData.mockoonServiceId = data.mockoonServiceId

      formData.description = data.description || ''

      // 解析 JSON 到列表
      parseJsonToList()
    } else {
      resetForm()
    }
  } catch (error) {
    console.error('获取配置失败', error)
    resetForm()
  }
}

// 将 JSON 字符串解析为列表
const parseJsonToList = () => {
  if (formData.defaultParams) {
    try {
      const obj = JSON.parse(formData.defaultParams)
      paramList.value = Object.entries(obj).map(([key, value]) => ({ key, value: String(value) }))
    } catch (e) {
      paramList.value = [{ key: '', value: '' }]
    }
  } else {
    paramList.value = [{ key: '', value: '' }]
  }

  if (formData.defaultHeaders) {
    try {
      const obj = JSON.parse(formData.defaultHeaders)
      headerList.value = Object.entries(obj).map(([key, value]) => ({ key, value: String(value) }))
    } catch (e) {
      headerList.value = [{ key: '', value: '' }]
    }
  } else {
    headerList.value = [{ key: '', value: '' }]
  }

  if (formData.bodyFormData) {
    try {
      const obj = JSON.parse(formData.bodyFormData)
      bodyFormData.value = Object.entries(obj).map(([key, value]) => ({ key, value: String(value) }))
    } catch (e) {
      bodyFormData.value = [{ key: '', value: '' }]
    }
  } else {
    bodyFormData.value = [{ key: '', value: '' }]
  }
}


// 将列表转换为 JSON 对象
const convertListToJson = () => {
  const paramsObj = {}
  paramList.value.forEach(item => {
    if (item.key.trim()) paramsObj[item.key.trim()] = item.value
  })
  formData.defaultParams = Object.keys(paramsObj).length > 0 ? JSON.stringify(paramsObj) : null

  const headersObj = {}
  headerList.value.forEach(item => {
    if (item.key.trim()) headersObj[item.key.trim()] = item.value
  })
  formData.defaultHeaders = Object.keys(headersObj).length > 0 ? JSON.stringify(headersObj) : null

  const bodyFormDataObj = {}
  bodyFormData.value.forEach(item => {
    if (item.key.trim()) bodyFormDataObj[item.key.trim()] = item.value
  })
  formData.bodyFormData = Object.keys(bodyFormDataObj).length > 0 ? JSON.stringify(bodyFormDataObj) : null
}


// 添加/删除项目
const addItem = (type) => {
  if (type === 'param') paramList.value.push({ key: '', value: '' })
  else if (type === 'header') headerList.value.push({ key: '', value: '' })
  else bodyFormData.value.push({ key: '', value: '' })
}

const removeItem = (type, index) => {
  if (type === 'param') paramList.value.splice(index, 1)
  else if (type === 'header') headerList.value.splice(index, 1)
  else bodyFormData.value.splice(index, 1)
}

const createEmptyFieldRow = () => ({ key: '', value: '' })

const ensureFieldRows = (items) => (items.length > 0 ? items : [createEmptyFieldRow()])

const stripWrappingQuotes = (value = '') => {
  const trimmedValue = value.trim()
  if (trimmedValue.length < 2) return trimmedValue
  const firstChar = trimmedValue[0]
  const lastChar = trimmedValue[trimmedValue.length - 1]
  if ((firstChar === '"' && lastChar === '"') || (firstChar === '\'' && lastChar === '\'')) {
    return trimmedValue.slice(1, -1)
  }
  return trimmedValue
}

const decodeCurlToken = (value = '') => {
  const unwrappedValue = stripWrappingQuotes(value)
  return unwrappedValue
    .replace(/\\'/g, '\'')
    .replace(/\\"/g, '"')
    .replace(/\\\\/g, '\\')
}

const toKeyValueList = (searchParams) => {
  const items = []
  searchParams.forEach((value, key) => {
    items.push({ key, value })
  })
  return items
}

const parseUrlLikeText = (inputText = '') => {
  let trimmedText = stripWrappingQuotes(String(inputText || '').trim())
  if (!trimmedText) return null

  const requestLineMatch = trimmedText.match(/^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s+(\S+)(?:\s+HTTP\/\d(?:\.\d)?)?$/i)
  if (requestLineMatch) {
    trimmedText = requestLineMatch[2]
  }

  if (/\s/.test(trimmedText)) return null
  const originalRequestUrl = trimmedText
  const parseableText = trimmedText.replace(/&amp;/gi, '&')

  const candidates = []
  if (/^https?:\/\//i.test(parseableText)) {
    candidates.push(parseableText)
  } else if (/^(?:localhost|(?:[a-z0-9-]+\.)+[a-z]{2,})(?::\d+)?(?:\/|\?)/i.test(parseableText)) {
    candidates.push(`http://${parseableText}`)
  } else if (parseableText.startsWith('/')) {
    candidates.push(`http://placeholder.local${parseableText}`)
  } else if (parseableText.startsWith('?')) {
    candidates.push(`http://placeholder.local/${parseableText}`)
  } else if (parseableText.includes('?')) {
    candidates.push(`http://placeholder.local/${parseableText}`)
  }

  for (const candidate of candidates) {
    try {
      const url = new URL(candidate)
      return {
        source: 'url',
        apiPath: originalRequestUrl,
        params: toKeyValueList(url.searchParams)
      }
    } catch (error) {
      continue
    }
  }

  return null
}

const tokenizeCurlCommand = (commandText = '') => {
  const tokens = commandText.match(/"(?:\\.|[^"])*"|'(?:\\.|[^'])*'|\S+/g)
  return tokens || []
}

const formatRawBody = (bodyText = '') => {
  const trimmedBody = bodyText.trim()
  if (!trimmedBody) return ''
  try {
    return JSON.stringify(JSON.parse(trimmedBody), null, 2)
  } catch (error) {
    return trimmedBody
  }
}

const parseBodyRows = (bodyText = '') => {
  const searchParams = new URLSearchParams(bodyText)
  const rows = []
  searchParams.forEach((value, key) => {
    rows.push({ key, value })
  })
  return rows
}

const parseCurlCommand = (inputText = '') => {
  const trimmedText = inputText.trim()
  if (!/^curl(\s|$)/i.test(trimmedText)) return null

  const tokens = tokenizeCurlCommand(trimmedText)
  if (tokens.length === 0) return null

  const headers = []
  const headerMap = new Map()
  const bodyParts = []
  const formParts = []
  let requestMethod = ''
  let requestUrl = ''

  const assignHeader = (headerText) => {
    const separatorIndex = headerText.indexOf(':')
    if (separatorIndex <= 0) return
    const key = headerText.slice(0, separatorIndex).trim()
    const value = headerText.slice(separatorIndex + 1).trim()
    if (!key) return
    headerMap.set(key.toLowerCase(), value)
    const existingIndex = headers.findIndex(item => item.key.toLowerCase() === key.toLowerCase())
    if (existingIndex >= 0) {
      headers[existingIndex] = { key, value }
    } else {
      headers.push({ key, value })
    }
  }

  for (let index = 1; index < tokens.length; index++) {
    const token = tokens[index]

    if ((token === '-X' || token === '--request') && tokens[index + 1]) {
      requestMethod = decodeCurlToken(tokens[++index]).toUpperCase()
      continue
    }

    if (token.startsWith('--request=')) {
      requestMethod = decodeCurlToken(token.slice('--request='.length)).toUpperCase()
      continue
    }

    if ((token === '-H' || token === '--header') && tokens[index + 1]) {
      assignHeader(decodeCurlToken(tokens[++index]))
      continue
    }

    if (token.startsWith('--header=')) {
      assignHeader(decodeCurlToken(token.slice('--header='.length)))
      continue
    }

    if (
      (token === '-d' || token === '--data' || token === '--data-raw' || token === '--data-binary' || token === '--data-urlencode') &&
      tokens[index + 1]
    ) {
      bodyParts.push(decodeCurlToken(tokens[++index]))
      continue
    }

    if (
      token.startsWith('--data=') ||
      token.startsWith('--data-raw=') ||
      token.startsWith('--data-binary=') ||
      token.startsWith('--data-urlencode=')
    ) {
      const [, rawValue = ''] = token.split(/=(.*)/, 2)
      bodyParts.push(decodeCurlToken(rawValue))
      continue
    }

    if ((token === '-F' || token === '--form') && tokens[index + 1]) {
      formParts.push(decodeCurlToken(tokens[++index]))
      continue
    }

    if (token.startsWith('--form=')) {
      formParts.push(decodeCurlToken(token.slice('--form='.length)))
      continue
    }

    if (token === '--url' && tokens[index + 1]) {
      requestUrl = decodeCurlToken(tokens[++index])
      continue
    }

    if (token.startsWith('--url=')) {
      requestUrl = decodeCurlToken(token.slice('--url='.length))
      continue
    }

    if (!token.startsWith('-') && !requestUrl) {
      requestUrl = decodeCurlToken(token)
    }
  }

  if (!requestUrl) return null

  const parsedUrl = parseUrlLikeText(requestUrl)
  if (!parsedUrl) return null

  const contentType = headerMap.get('content-type') || ''
  const bodyText = bodyParts.join('&')
  const formText = formParts.join('&')
  let bodyType = 'none'
  let bodyTemplate = ''
  let parsedBodyFormData = []

  if (formText) {
    bodyType = 'form-data'
    parsedBodyFormData = parseBodyRows(formText)
  } else if (bodyText) {
    if (contentType.includes('application/x-www-form-urlencoded')) {
      bodyType = 'x-www-form-urlencoded'
      parsedBodyFormData = parseBodyRows(bodyText)
    } else if (contentType.includes('multipart/form-data')) {
      bodyType = 'form-data'
      parsedBodyFormData = parseBodyRows(bodyText)
    } else if (!contentType && bodyText.includes('=') && !bodyText.trim().startsWith('{')) {
      bodyType = 'x-www-form-urlencoded'
      parsedBodyFormData = parseBodyRows(bodyText)
    } else {
      bodyType = 'raw'
      bodyTemplate = formatRawBody(bodyText)
    }
  }

  return {
    source: 'curl',
    apiPath: parsedUrl.apiPath,
    params: parsedUrl.params,
    apiMethod: requestMethod || (bodyText || formText ? 'POST' : 'GET'),
    headers,
    contentType: contentType || formData.contentType,
    bodyType,
    bodyTemplate,
    bodyFormData: parsedBodyFormData
  }
}

const applyParsedRequest = (parsedRequest) => {
  if (!parsedRequest) return

  formData.apiPath = parsedRequest.apiPath || formData.apiPath

  if (parsedRequest.source === 'curl') {
    formData.apiMethod = parsedRequest.apiMethod || formData.apiMethod
    formData.contentType = parsedRequest.contentType || formData.contentType
    headerList.value = ensureFieldRows(parsedRequest.headers || [])
    formData.bodyType = parsedRequest.bodyType || 'none'
    formData.bodyTemplate = parsedRequest.bodyTemplate || ''
    bodyFormData.value = ensureFieldRows(parsedRequest.bodyFormData || [])

    if (parsedRequest.bodyType === 'none') {
      formData.bodyTemplate = ''
      bodyFormData.value = [createEmptyFieldRow()]
    }
  }

  if (Array.isArray(parsedRequest.params)) {
    paramList.value = ensureFieldRows(parsedRequest.params)
  }

  if (parsedRequest.params?.length > 0) {
    activeTab.value = 'params'
  }
}

const handleApiPathPaste = (event) => {
  const clipboardData = event.clipboardData || window.clipboardData
  if (!clipboardData) return
  const pastedText = clipboardData.getData('text')
  if (!pastedText) return

  const parsedCurlRequest = parseCurlCommand(pastedText)
  if (parsedCurlRequest) {
    event.preventDefault()
    applyParsedRequest(parsedCurlRequest)
    return
  }

  const parsedUrlRequest = parseUrlLikeText(pastedText)
  if (!parsedUrlRequest) return

  event.preventDefault()
  applyParsedRequest(parsedUrlRequest)
}

const handleApiPathChange = (value) => {
  const inputText = String(value || '').trim()
  const isRequestLine = /^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s+/i.test(inputText)
  if (!inputText.includes('?') && !/^https?:\/\//i.test(inputText) && !isRequestLine) {
    return
  }

  const parsedRequest = parseUrlLikeText(inputText)
  if (parsedRequest) {
    applyParsedRequest(parsedRequest)
  }
}

// 重置表单

const resetForm = () => {
  formData.apiPath = ''
  formData.apiMethod = 'GET'
  formData.contentType = 'application/json'
  formData.defaultParams = ''
  formData.defaultHeaders = ''
  formData.bodyTemplate = ''
  formData.bodyType = 'none'
  formData.bodyFormData = ''
  formData.mockoonServiceId = null
  formData.description = props.categoryName ? `${props.categoryName}接口` : ''
  paramList.value = [{ key: '', value: '' }]
  headerList.value = [{ key: '', value: '' }]
  bodyFormData.value = [{ key: '', value: '' }]
  testResponse.value = null
  testTime.value = 0
}



// 提交表单
const handleSubmit = async () => {
  const validateResult = await formRef.value.validate()
  if (validateResult !== true) return

  // 转换列表到 JSON
  convertListToJson()

  // 验证 Body JSON 格式
  try {
    if (formData.bodyType === 'raw' && formData.bodyTemplate && formData.bodyTemplate.trim()) {
      JSON.parse(formData.bodyTemplate)
    }
  } catch (e) {
    MessagePlugin.error('请求体模板不是有效的 JSON 格式')
    return
  }


  loading.value = true
  try {
    const submitData = { ...formData }
    if (!submitData.bodyTemplate || submitData.bodyTemplate.trim() === '') {
      submitData.bodyTemplate = null
    }
    
    const res = await saveInterfaceConfig(submitData)
    if (res.code === 200) {
      MessagePlugin.success('保存成功')
      emit('success')
      visible.value = false
    } else {
      MessagePlugin.error(res.message || '保存失败')
    }
  } catch (error) {
    MessagePlugin.error('保存失败')
  } finally {
    loading.value = false
  }
}

// 关闭弹窗
const handleClose = () => {
  resetForm()
}

// 执行测试
const handleTest = async () => {
  if (!formData.apiPath) {
    MessagePlugin.warning('请输入接口路径后再进行测试')
    return
  }

  testLoading.value = true
  testResponse.value = null
  const startTime = Date.now()

  try {
    // 构造请求参数
    const params = {}
    paramList.value.forEach(item => {
      if (item.key.trim()) params[item.key.trim()] = item.value
    })

    const headers = {}
    headerList.value.forEach(item => {
      if (item.key.trim()) headers[item.key.trim()] = item.value
    })

    let body = null
    const bodyFormDataMap = {}
    
    if (formData.bodyType === 'raw') {
      if (formData.bodyTemplate && formData.bodyTemplate.trim()) {
        try {
          body = JSON.parse(formData.bodyTemplate)
        } catch (e) {
          body = formData.bodyTemplate
        }
      }
    } else if (['form-data', 'x-www-form-urlencoded'].includes(formData.bodyType)) {
      bodyFormData.value.forEach(item => {
        if (item.key.trim()) bodyFormDataMap[item.key.trim()] = item.value
      })
    }

    const testData = {
      categoryId: props.categoryId,
      contentPath: formData.apiPath,
      method: formData.apiMethod,
      params, // 恢复传递参数对象
      headers,
      body,
      bodyType: formData.bodyType,
      bodyFormData: bodyFormDataMap,
      mockoonServiceId: formData.mockoonServiceId
    }

    const res = await executeAdminDebug(testData)
    testTime.value = Date.now() - startTime
    
    // 统一展示后端返回的响应内容
    // res.data 结构: { body: ..., headers: ..., httpStatus: ... }
    const sandboxRes = res.data || {}
    testResponse.value = {
      code: sandboxRes.httpStatus !== undefined ? sandboxRes.httpStatus : (sandboxRes.code !== undefined ? sandboxRes.code : 200),
      data: sandboxRes.body,
      headers: sandboxRes.headers || { 'Content-Type': 'application/json' },
      cookies: []
    }

  } catch (error) {
    testTime.value = Date.now() - startTime
    testResponse.value = {
      code: error.response?.status || 500,
      message: error.message || '测试请求失败',
      data: error.response?.data
    }
  } finally {
    testLoading.value = false
  }
}

// 通用复制到剪贴板工具函数
const copyToClipboard = async (text) => {
  return copyWithFeedback(text)
}

// 复制 Body
const copyBody = (data) => {
  const text = typeof data === 'string' ? data : JSON.stringify(data, null, 2)
  copyToClipboard(text)
}

const formatJson = (data) => {
  if (!data) return ''
  try {
    return JSON.stringify(data, null, 2)
  } catch (e) {
    return String(data)
  }
}

// 生成 curl 命令
const curlCommand = computed(() => {
  const paramsObj = {}
  paramList.value.forEach(item => {
    if (item.key.trim()) paramsObj[item.key.trim()] = item.value
  })

  // 获取服务地址
  let baseUrl = ''
  if (formData.mockoonServiceId) {
    const service = serviceList.value.find(s => s.id === formData.mockoonServiceId)
    if (service && service.serviceUrl) {
      baseUrl = service.serviceUrl
      if (baseUrl.endsWith('/')) {
        baseUrl = baseUrl.substring(0, baseUrl.length - 1)
      }
    }
  }

  const originalApiPath = String(formData.apiPath || '').trim()
  const isAbsoluteUrl = /^https?:\/\//i.test(originalApiPath)
  let path = originalApiPath
  if (!isAbsoluteUrl && path && !path.startsWith('/')) {
    path = '/' + path
  }

  let url = isAbsoluteUrl ? originalApiPath : baseUrl + path
  if (!url.includes('?') && Object.keys(paramsObj).length > 0) {
    const searchParams = new URLSearchParams(paramsObj)
    url += '?' + searchParams.toString()
  }

  let curl = `curl -X ${formData.apiMethod} "${url}"`

  // 添加 headers
  headerList.value.forEach(header => {
    if (header.key && header.value) {
      curl += ` -H "${header.key}: ${header.value}"`
    }
  })

  // 添加 body
  if (['POST', 'PUT', 'PATCH'].includes(formData.apiMethod)) {
    if (formData.bodyType === 'raw' && formData.bodyTemplate) {
      const escapedBody = formData.bodyTemplate.replace(/'/g, "'\\''")
      curl += ` -d '${escapedBody}'`
    } else if (formData.bodyType === 'form-data') {
      bodyFormData.value.forEach(item => {
        if (item.key && item.value) {
          curl += ` -F "${item.key}=${item.value}"`
        }
      })
    } else if (formData.bodyType === 'x-www-form-urlencoded') {
      bodyFormData.value.forEach(item => {
        if (item.key && item.value) {
          curl += ` -d "${item.key}=${encodeURIComponent(item.value)}"`
        }
      })
    }
  }

  return curl
})


// 复制 curl
const copyCurl = () => {
  copyToClipboard(curlCommand.value)
}

onMounted(() => {


  fetchServiceList()
})
</script>

<style scoped>
.sandbox-test-container {
  display: flex;
  flex-direction: column;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
}

.url-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.method-select {
  width: 120px;
}

.url-input {
  flex: 1;
}

.params-section,
.headers-section,
.body-section {
  padding: 16px 0;
}

.param-list,
.header-list {
  margin-bottom: 12px;
}

.param-row,
.header-row,
.form-data-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.param-key,
.header-key,
.form-data-key {
  width: 200px;
}

.param-value,
.header-value,
.form-data-value {
  flex: 1;
}

.body-type-selector {
  margin-bottom: 16px;
}

.form-data-editor {
  margin-top: 12px;
}

.raw-editor {
  margin-top: 12px;
}


.response-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.response-header h4 {
  margin: 0;
}

.response-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.response-time {
  color: #666;
  font-size: 12px;
}

.response-body {
  position: relative;
}

.response-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.json-preview {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  max-height: 300px;
}

.headers-table,
.cookies-table {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
}

.header-item,
.cookie-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 13px;
}

.header-name,
.cookie-name {
  color: #666;
  min-width: 150px;
  font-weight: 500;
}

.header-value,
.cookie-value {
  color: #333;
  word-break: break-all;
}

.curl-section {

  margin-top: 16px;
}

.curl-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.curl-header h4 {
  margin: 0;
}

.curl-content {
  background: #1e1e1e;
  padding: 12px;
  border-radius: 4px;
}

.curl-command {
  color: #4ec9b0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.config-tabs :deep(.t-tabs__content) {
  padding: 0;
}

:deep(.t-tabs__nav-container) {
  margin-bottom: 0;
}
</style>
