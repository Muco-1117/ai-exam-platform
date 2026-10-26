<template>
  <div v-loading="loading" class="question-detail">
    <el-page-header class="page-header" @back="router.back()">
      <template #content>
        <span>题目作答</span>
      </template>
    </el-page-header>

    <el-card v-if="question" shadow="never" class="question-card">
      <QuestionMeta :question="question" show-source />

      <div class="question-no">第 {{ question.id }} 题</div>
      <div class="question-content">{{ question.content }}</div>

      <div class="options-preview" v-if="hasOptions">
        <div v-for="opt in question.options" :key="opt.key" class="option-line">
          <b>{{ opt.key }}.</b> {{ opt.text }}
        </div>
      </div>

      <el-divider>我的作答</el-divider>

      <QuestionAnswer
        v-model="userAnswer"
        :type="question.type"
        :options="question.options"
        :disabled="resultShow"
      />

      <div class="submit-bar" v-if="!resultShow">
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">提交答案</el-button>
      </div>

      <el-alert
        v-if="resultShow && result"
        :title="result.isCorrect ? '回答正确!' : '回答错误'"
        :type="result.isCorrect ? 'success' : 'error'"
        :closable="false"
        show-icon
        class="result-alert"
      >
        <div class="result-body">
          <p><b>得分:</b>{{ result.score }}</p>
          <p><b>正确答案:</b>{{ answerDisplay }}</p>
          <p v-if="result.analysis"><b>解析:</b>{{ result.analysis }}</p>
        </div>
      </el-alert>
    </el-card>

    <el-card v-if="question" shadow="never" class="action-card">
      <div class="action-bar">
        <div class="left">
          <el-button :type="thumbMine ? 'warning' : 'default'" :icon="CaretTop" @click="handleThumb">
            点赞 {{ thumbCount }}
          </el-button>
          <el-button :type="favored ? 'danger' : 'default'" :icon="Star" @click="handleFavor">
            {{ favored ? '取消收藏' : '收藏' }}
          </el-button>
        </div>
        <div class="right">
          <el-button @click="router.push('/wrong')">错题本</el-button>
          <el-button @click="router.push('/practice')">练习记录</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CaretTop, Star } from '@element-plus/icons-vue'
import QuestionAnswer from '@/components/QuestionAnswer.vue'
import QuestionMeta from '@/components/QuestionMeta.vue'
import { getQuestionDetail } from '@/api/question'
import { submitPractice } from '@/api/practice'
import { addFavor, getFavorPage, removeFavor, addThumb, getThumbCount, getMyThumbs } from '@/api/favor'
import type { JudgeResult, Question } from '@/api/types'
import { answerText } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const questionId = Number(route.params.id)
const question = ref<Question | null>(null)
const loading = ref(false)
const userAnswer = ref('')
const submitting = ref(false)
const result = ref<JudgeResult | null>(null)
const resultShow = ref(false)

const thumbCount = ref(0)
const thumbMine = ref(false)
const favored = ref(false)

const hasOptions = computed(() => {
  return !!question.value?.options && question.value.options.length > 0
})

const answerDisplay = computed(() =>
  answerText(question.value?.type, result.value?.correctAnswer)
)

async function loadQuestion() {
  loading.value = true
  try {
    question.value = await getQuestionDetail(questionId)
  } finally {
    loading.value = false
  }
}

async function loadSocialState() {
  try {
    const [count, mine, favors] = await Promise.all([
      getThumbCount(questionId),
      getMyThumbs(),
      getFavorPage({ size: 999 })
    ])
    thumbCount.value = count ?? 0
    thumbMine.value = (mine || []).includes(questionId)
    favored.value = (favors.records || []).some((f) => f.questionId === questionId)
  } catch {
    // 忽略,不影响作答
  }
}

async function handleSubmit() {
  if (!userAnswer.value) {
    ElMessage.warning('请先作答再提交')
    return
  }
  submitting.value = true
  try {
    const res = await submitPractice({ questionId, userAnswer: userAnswer.value })
    result.value = res
    resultShow.value = true
    if (res.isCorrect) {
      ElMessage.success('回答正确!')
    } else {
      ElMessage.error('回答错误,已加入错题本')
    }
  } finally {
    submitting.value = false
  }
}

async function handleThumb() {
  await addThumb(questionId)
  if (thumbMine.value) {
    ElMessage.info('已点过赞,幂等处理')
  } else {
    ElMessage.success('点赞成功')
    thumbMine.value = true
  }
  thumbCount.value = await getThumbCount(questionId)
}

async function handleFavor() {
  if (favored.value) {
    await removeFavor(questionId)
    favored.value = false
    ElMessage.success('已取消收藏')
  } else {
    await addFavor(questionId)
    favored.value = true
    ElMessage.success('收藏成功')
  }
}

onMounted(() => {
  loadQuestion()
  loadSocialState()
})
</script>

<style scoped lang="scss">
.question-detail {
  max-width: 900px;
  margin: 0 auto;

  .page-header {
    margin-bottom: 16px;
  }

  .question-card {
    margin-bottom: 16px;

    .question-no {
      margin-top: 12px;
      color: #909399;
      font-size: 13px;
    }

    .question-content {
      font-size: 16px;
      line-height: 1.7;
      color: #303133;
      margin: 8px 0 16px;
    }

    .options-preview {
      background: #f8f9fb;
      border-radius: 4px;
      padding: 12px 16px;
      margin-bottom: 8px;

      .option-line {
        line-height: 1.9;
        color: #606266;
      }
    }

    .submit-bar {
      margin-top: 20px;
      text-align: center;
    }

    .result-alert {
      margin-top: 20px;

      .result-body p {
        margin: 4px 0;
      }
    }
  }

  .action-card {
    .action-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }
  }
}
</style>
