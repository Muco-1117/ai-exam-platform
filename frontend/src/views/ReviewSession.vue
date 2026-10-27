<template>
  <div class="review-page">
    <div class="page-toolbar">
      <el-page-header @back="router.back()">
        <template #content><span>今日复习</span></template>
      </el-page-header>
      <span class="flex-spacer" />
      <el-tag v-if="reviewSet" type="info">本次 {{ reviewSet.items.length }} 题</el-tag>
    </div>

    <el-card v-if="loading" v-loading="loading" shadow="never" class="loading-card" />
    <el-empty v-else-if="!current" description="今天没有到期错题，继续保持" :image-size="120">
      <el-button type="primary" @click="router.push('/questions')">去题库练习</el-button>
    </el-empty>

    <template v-else>
      <el-card shadow="never" class="progress-card">
        <div class="progress-row">
          <span>第 {{ index + 1 }} / {{ reviewSet!.items.length }} 题</span>
          <el-progress :percentage="Math.round(((index + 1) / reviewSet!.items.length) * 100)" :show-text="false" />
          <span class="wrong-count">历史错 {{ current.wrongCount || 1 }} 次</span>
        </div>
      </el-card>

      <el-card shadow="never" class="question-card">
        <div class="question-content">{{ current.question.content }}</div>
        <QuestionAnswer
          v-model="answer"
          :type="current.question.type"
          :options="current.question.options"
          :disabled="result !== null"
        />
        <div class="actions">
          <el-button v-if="!result" type="primary" :loading="submitting" @click="submit">提交答案</el-button>
          <el-button v-else type="primary" @click="next">{{ index + 1 === reviewSet!.items.length ? '完成复习' : '下一题' }}</el-button>
        </div>
        <el-alert v-if="result" :type="result.isCorrect ? 'success' : 'error'" :closable="false" show-icon>
          <template #title>{{ result.isCorrect ? '回答正确，已从复习队列移除' : '回答错误，将按间隔再次提醒' }}</template>
          <div class="result-body">
            <p><b>正确答案：</b>{{ answerText(current.question.type, result.correctAnswer) }}</p>
            <p v-if="result.analysis"><b>解析：</b>{{ result.analysis }}</p>
          </div>
        </el-alert>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QuestionAnswer from '@/components/QuestionAnswer.vue'
import { getReviewSet, submitPractice } from '@/api/practice'
import type { JudgeResult, ReviewSet } from '@/api/types'
import { answerText } from '@/utils/format'

const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const reviewSet = ref<ReviewSet | null>(null)
const index = ref(0)
const answer = ref('')
const result = ref<JudgeResult | null>(null)
const current = computed(() => reviewSet.value?.items[index.value] || null)

async function load() {
  loading.value = true
  try {
    reviewSet.value = await getReviewSet(10)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!current.value || !answer.value.trim()) {
    ElMessage.warning('请先作答再提交')
    return
  }
  submitting.value = true
  try {
    result.value = await submitPractice({ questionId: current.value.questionId, userAnswer: answer.value })
  } finally {
    submitting.value = false
  }
}

function next() {
  if (!reviewSet.value || index.value + 1 >= reviewSet.value.items.length) {
    reviewSet.value = { ...reviewSet.value!, items: [] }
    return
  }
  index.value += 1
  answer.value = ''
  result.value = null
}

onMounted(load)
</script>

<style scoped lang="scss">
.review-page { max-width: 900px; margin: 0 auto; }
.loading-card { min-height: 240px; }
.progress-card { margin: 16px 0; }
.progress-row { display: flex; align-items: center; gap: 16px; color: #606266; font-size: 14px; }
.progress-row .el-progress { flex: 1; }
.wrong-count { color: #f56c6c; white-space: nowrap; }
.question-card { margin-top: 16px; }
.question-content { color: #303133; font-size: 18px; line-height: 1.8; margin-bottom: 20px; }
.actions { display: flex; justify-content: center; margin: 24px 0 12px; }
.result-body p { margin: 6px 0; }
@media (max-width: 600px) { .progress-row { gap: 8px; flex-wrap: wrap; } .progress-row .el-progress { flex-basis: 100%; order: 3; } }
</style>
