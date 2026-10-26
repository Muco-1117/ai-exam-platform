<template>
  <span class="question-meta">
    <el-tag :type="typeTag(question.type)" size="small">{{ typeText(question.type) }}</el-tag>
    <el-tag :type="difficultyTag" size="small" effect="plain">{{ difficultyText(question.difficulty) }}</el-tag>
    <el-tag v-if="showSource && question.source" size="small" effect="plain" type="info">
      {{ sourceText(question.source) }}
    </el-tag>
    <el-tag v-if="showReview" :type="question.reviewStatus === 1 ? 'success' : 'danger'" size="small" effect="plain">
      {{ reviewText(question.reviewStatus) }}
    </el-tag>
    <span v-if="question.categoryName" class="category">分类:{{ question.categoryName }}</span>
    <span class="score">分值:{{ question.score }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Question } from '@/api/types'
import { typeText, typeTag, difficultyText, sourceText, reviewText } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    /** 题目或答题卡格式(答题卡格式结构兼容 Question,可选字段缺失时按默认展示) */
    question: Question
    showSource?: boolean
    showReview?: boolean
  }>(),
  { showSource: false, showReview: false }
)

const difficultyTag = computed(() => {
  const d = props.question.difficulty
  if (d === 1) return 'success'
  if (d === 3) return 'danger'
  return 'warning'
})
</script>

<style scoped lang="scss">
.question-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #909399;

  .category,
  .score {
    color: #909399;
  }
}
</style>
