<template>
  <div v-loading="loading" class="exam-taking">
    <el-card v-if="startData" shadow="never" class="exam-header-card">
      <div class="exam-header">
        <div class="left">
          <h3 class="exam-name">{{ startData.examName }}</h3>
          <div class="exam-info">
            <el-tag type="primary" effect="plain">总分 {{ startData.totalScore }} 分</el-tag>
            <el-tag type="warning" effect="plain">及格 {{ startData.passScore }} 分</el-tag>
            <el-tag type="info" effect="plain">共 {{ questions.length }} 题</el-tag>
          </div>
        </div>
        <div class="right">
          <div class="countdown" :class="{ danger: remainingSeconds <= 60 }">
            <el-progress
              type="circle"
              :percentage="countdownPercent"
              :width="90"
              :stroke-width="8"
              :color="countdownColor"
              :indeterminate="false"
            >
              <template #default>
                <div class="countdown-inner">
                  <div class="countdown-time">{{ formatDuration(remainingSeconds) }}</div>
                  <div class="countdown-label">剩余时间</div>
                </div>
              </template>
            </el-progress>
          </div>
          <el-button type="danger" size="large" :loading="submitting" @click="handleSubmit">交 卷</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="startData" shadow="never" class="answer-card-card">
      <template #header>
        <div class="card-header-row">
          <span>答题卡</span>
          <span class="answered-info">
            已答 <b :style="{ color: answeredCount > 0 ? '#409eff' : '#909399' }">{{ answeredCount }}</b> / {{ questions.length }}
          </span>
        </div>
      </template>
      <div class="answer-sheet">
        <div
          v-for="(q, idx) in questions"
          :key="q.id"
          class="sheet-item"
          :class="{ answered: !!answers[q.id], current: idx === currentIndex }"
          @click="scrollToQuestion(idx)"
        >
          {{ idx + 1 }}
        </div>
      </div>
    </el-card>

    <el-card v-for="(q, idx) in questions" :key="q.id" shadow="never" class="question-card" :ref="setQuestionRef">
      <div class="question-head">
        <span class="question-index">{{ idx + 1 }}</span>
        <el-tag :type="typeTag(q.type)" size="small">{{ typeText(q.type) }}</el-tag>
        <el-tag type="info" size="small" effect="plain">{{ q.score }} 分</el-tag>
        <span class="category" v-if="q.categoryName">{{ q.categoryName }}</span>
      </div>
      <div class="question-content">{{ q.content }}</div>
      <QuestionAnswer
        v-model="answers[q.id]"
        :type="q.type"
        :options="q.options"
        @update:model-value="onAnswerChange(q)"
      />
    </el-card>

    <div v-if="!loading && startData && questions.length === 0" class="page-card">
      <el-empty description="该考试暂无试题" />
    </div>

    <div v-if="startData" class="submit-bottom">
      <el-button type="danger" size="large" :loading="submitting" @click="handleSubmit">交 卷</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import QuestionAnswer from '@/components/QuestionAnswer.vue'
import { startExam, saveExamAnswer, submitExam } from '@/api/exam'
import type { ExamStartResult, QuestionCard } from '@/api/types'
import { typeText, typeTag, formatDuration, parseServerTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const examId = Number(route.params.examId)

const startData = ref<ExamStartResult | null>(null)
const questions = ref<QuestionCard[]>([])
const answers = ref<Record<number, string>>({})
const loading = ref(false)
const submitting = ref(false)
const recordId = ref(0)
const currentIndex = ref(0)

const remainingSeconds = ref(0)
let timer: number | null = null
let submitted = false
let questionRefs: HTMLElement[] = []

function setQuestionRef(el: unknown): void {
  const item = el as { $el?: HTMLElement } | HTMLElement | null
  if (!item) return
  if ('$el' in item && item.$el) {
    questionRefs.push(item.$el)
  } else if (item instanceof HTMLElement) {
    questionRefs.push(item)
  }
}

const answeredCount = computed(() =>
  questions.value.filter((q) => !!answers.value[q.id]).length
)

const countdownPercent = computed(() => {
  if (!startData.value?.duration) return 0
  const totalSec = startData.value.duration * 60
  return Math.max(0, Math.min(100, Math.round((remainingSeconds.value / totalSec) * 100)))
})

const countdownColor = computed(() => {
  if (remainingSeconds.value <= 60) return '#f56c6c'
  if (remainingSeconds.value <= 300) return '#e6a23c'
  return '#409eff'
})

function startCountdown() {
  if (!startData.value) return
  const deadlineMs = parseServerTime(startData.value.deadline)
  if (Number.isNaN(deadlineMs)) return
  const tick = () => {
    const diff = Math.floor((deadlineMs - Date.now()) / 1000)
    remainingSeconds.value = Math.max(0, diff)
    if (remainingSeconds.value <= 0 && !submitted) {
      ElMessage.warning('考试时间已到,自动交卷')
      doSubmit()
    }
  }
  tick()
  timer = window.setInterval(tick, 1000)
}

function scrollToQuestion(idx: number) {
  currentIndex.value = idx
  const el = questionRefs[idx]
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

/** 答案变化时自动保存草稿:按题防抖 + 全局串行,避免并发请求乱序/重复写入 */
const saveTimers: Record<number, number> = {}
const pendingSaves = new Map<number, string>()
let saveChain: Promise<void> = Promise.resolve()

function enqueueSave(questionId: number, userAnswer: string) {
  saveChain = saveChain
    .catch(() => undefined)
    .then(() => {
      return saveExamAnswer({ recordId: recordId.value, questionId, userAnswer }).then(() => {
        if (pendingSaves.get(questionId) === userAnswer) pendingSaves.delete(questionId)
      })
    })
  // 保留待保存答案供交卷重试，同时避免异步事件出现未处理的 Promise 拒绝。
  void saveChain.catch(() => undefined)
}

function onAnswerChange(q: QuestionCard) {
  const answer = answers.value[q.id]
  if (answer === undefined) return
  pendingSaves.set(q.id, answer)
  window.clearTimeout(saveTimers[q.id])
  saveTimers[q.id] = window.setTimeout(() => enqueueSave(q.id, answer), 500)
}

async function flushSaves() {
  // 防抖计时尚未结束时，交卷也必须保存当前答案。
  for (const [questionId, answer] of pendingSaves) {
    window.clearTimeout(saveTimers[questionId])
    enqueueSave(questionId, answer)
  }
  await saveChain
}

async function handleSubmit() {
  const unanswered = questions.value.filter((q) => !answers.value[q.id]).length
  const tip = unanswered > 0 ? `还有 ${unanswered} 题未作答,` : ''
  await ElMessageBox.confirm(`${tip}确定交卷吗?交卷后立即判分。`, '交卷确认', {
    confirmButtonText: '确定交卷',
    cancelButtonText: '继续作答',
    type: 'warning'
  })
  doSubmit()
}

async function doSubmit() {
  if (submitted) return
  submitted = true
  submitting.value = true
  try {
    await flushSaves()
    await submitExam(recordId.value)
    ElMessage.success('交卷成功,正在查看成绩')
    router.push(`/exam-result/${recordId.value}`)
  } catch {
    submitted = false
    ElMessage.error('交卷失败,请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await startExam(examId)
    startData.value = res
    recordId.value = res.recordId
    questions.value = res.questions || []
    answers.value = Object.fromEntries(questions.value.map(q => [q.id, q.savedAnswer || '']))
    remainingSeconds.value = Math.max(0, Math.floor((parseServerTime(res.deadline) - Date.now()) / 1000))
    startCountdown()
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  Object.values(saveTimers).forEach(window.clearTimeout)
  if (timer != null) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped lang="scss">
.exam-taking {
  max-width: 1000px;
  margin: 0 auto;
}

.exam-header-card {
  margin-bottom: 16px;

  .exam-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .exam-name {
      margin: 0 0 8px;
      font-size: 20px;
      color: #303133;
    }

    .exam-info {
      display: flex;
      gap: 8px;
    }

    .right {
      display: flex;
      align-items: center;
      gap: 20px;
    }
  }
}

.countdown-inner {
  .countdown-time {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    font-variant-numeric: tabular-nums;
  }

  .countdown-label {
    font-size: 11px;
    color: #909399;
  }
}

.answer-card-card {
  margin-bottom: 16px;

  .card-header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .answered-info {
      font-size: 13px;
      color: #909399;
    }
  }

  .answer-sheet {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;

    .sheet-item {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      border: 1px solid #dcdfe6;
      color: #606266;
      cursor: pointer;
      user-select: none;
      transition: all 0.2s;

      &:hover {
        border-color: #409eff;
        color: #409eff;
      }

      &.answered {
        background: #409eff;
        border-color: #409eff;
        color: #fff;
      }

      &.current {
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.4);
      }
    }
  }
}

.question-card {
  margin-bottom: 16px;

  .question-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    .question-index {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      background: #409eff;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
    }

    .category {
      color: #909399;
      font-size: 13px;
    }
  }

  .question-content {
    font-size: 15px;
    line-height: 1.7;
    margin-bottom: 14px;
    color: #303133;
  }
}

.submit-bottom {
  text-align: center;
  padding: 12px 0 40px;
}
</style>
