<template>
  <!-- 通用题型作答组件:SINGLE 单选 / MULTI 多选 / JUDGE 判断(正确/错误) / FILL 填空文本框 -->
  <div class="question-answer">
    <template v-if="type === 'SINGLE'">
      <el-radio-group :model-value="modelValue" :disabled="disabled" @update:model-value="onChange">
        <el-radio v-for="opt in options" :key="opt.key" :value="opt.key" class="answer-item">
          {{ opt.key }}. {{ opt.text }}
        </el-radio>
      </el-radio-group>
    </template>

    <template v-else-if="type === 'MULTI'">
      <el-checkbox-group :model-value="multiValue" :disabled="disabled" @update:model-value="onMultiChange">
        <el-checkbox v-for="opt in options" :key="opt.key" :value="opt.key" class="answer-item">
          {{ opt.key }}. {{ opt.text }}
        </el-checkbox>
      </el-checkbox-group>
      <div class="multi-tip">多选题:选出所有正确选项,漏选、错选均不得分</div>
    </template>

    <template v-else-if="type === 'JUDGE'">
      <el-radio-group :model-value="modelValue" :disabled="disabled" @update:model-value="onChange">
        <el-radio value="true" class="answer-item">正确</el-radio>
        <el-radio value="false" class="answer-item">错误</el-radio>
      </el-radio-group>
    </template>

    <template v-else>
      <el-input
        :model-value="modelValue"
        :disabled="disabled"
        placeholder="请输入答案"
        clearable
        @update:model-value="onChange"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QuestionOption } from '@/api/types'

const props = defineProps<{
  type: string
  options?: QuestionOption[] | null
  modelValue: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const optionList = computed(() => props.options || [])

const multiValue = computed<string[]>({
  get: () => (props.modelValue ? props.modelValue.split(',').filter(Boolean) : []),
  set: (val: string[]) => val
})

function onChange(value: string | boolean | number | undefined): void {
  emit('update:modelValue', value == null ? '' : String(value))
}

/** 多选:提交前按字母排序合并为 "A,B" */
function onMultiChange(value: unknown): void {
  const arr = Array.isArray(value) ? value : []
  const sorted = [...(arr as string[])].sort()
  emit('update:modelValue', sorted.join(','))
}
</script>

<style scoped lang="scss">
.question-answer {
  .answer-item {
    display: flex;
    margin-right: 24px;
    padding: 6px 0;
    align-items: flex-start;
  }

  .multi-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
