<template>
  <t-dialog
    v-model:visible="visible"
    :header="`接口凭证 - ${title}`"
    width="480px"
    :footer="false"
    @close="handleClose"
  >
    <div v-if="credential && Object.keys(credential).length > 0" class="credential-detail">
      <div v-for="(value, label) in credential" :key="label" class="credential-row">
        <div class="credential-label">{{ label }}</div>
        <div class="credential-value-row">
          <t-textarea
            v-if="label === '说明'"
            :model-value="value"
            :readonly="true"
            :autosize="{ minRows: 2, maxRows: 6 }"
            class="credential-textarea"
          />
          <template v-else>
            <div class="credential-value">{{ value }}</div>
            <t-button theme="primary" variant="outline" size="small" class="copy-btn" @click="copyField(value)">
              复制
            </t-button>
          </template>
        </div>
      </div>
    </div>
    <div v-else class="credential-empty">
      <t-empty description="暂无凭证信息" />
    </div>
  </t-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { copyWithFeedback } from '@/utils/clipboard'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '当前接口' },
  credential: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleClose = () => {
  visible.value = false
}

const copyField = (value) => copyWithFeedback(value)
</script>

<style scoped>
.credential-detail { padding: 8px; }
.credential-row { margin-bottom: 16px; }
.credential-row:last-child { margin-bottom: 0; }
.credential-label { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 8px; }
.credential-value-row { display: flex; align-items: center; gap: 8px; }
.credential-value { flex: 1; padding: 8px 12px; background-color: #f5f7fa; border-radius: 4px; font-family: monospace; font-size: 13px; color: #333; word-break: break-all; }
.credential-textarea { flex: 1; }
.credential-textarea :deep(.t-textarea__inner) { background-color: #f5f7fa; font-family: monospace; font-size: 13px; }
.copy-btn { flex-shrink: 0; }
.credential-empty { padding: 32px 0; }
</style>
