<template>
  <div v-loading="loading" class="exam-result">
    <el-page-header class="page-header" @back="router.back()">
      <template #content>
        <span>成绩详情</span>
      </template>
    </el-page-header>

    <el-card v-if="info" shadow="never" class="summary-card">
      <div class="summary">
        <div class="left">
          <h3 class="exam-name">{{ info.examName || info.record?.examName || '考试' }}</h3>
          <div class="summary-tags">
            <el-tag type="primary" effect="plain">得分 {{ info.totalScore ?? '-' }} 分</el-tag>
            <el-tag type="success" effect="plain">答对 {{ info.correctCount ?? '-' }} 题</el-tag>
            <el-tag type="info" effect="plain">共 {{ rows.length }} 题</el-tag>
            <el-tag v-if="info.record?.submitTime" type="warning" effect="plain">
              交卷时间 {{ info.record.submitTime }}
            </el-tag>
          </div>
        </div>
        <div class="right">
          <el-button
            v-if="info.record?.examId"
            type="warning"
            :icon="Trophy"
            @click="goRank"
          >查看排行榜</el-button>
          <el-button :icon="List" @click="router.push('/exams')">返回考试中心</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-for="(row, idx) in rows" :key="row.questionId" shadow="never" class="question-card">
      <div class="question-head">
        <span class="question-index">{{ idx + 1 }}</span>
        <el-tag v-if="row.type" :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
        <el-tag :type="row.isCorrect ? 'success' : 'danger'" size="small" effect="dark">
          {{ row.isCorrect ? '正确' : '错误' }}
        </el-tag>
        <el-tag type="info" size="small" effect="plain">得分 {{ row.score }}</el-tag>
      </div>

      <div v-if="row.content" class="question-content">{{ row.content }}</div>

      <div v-if="row.options && row.options.length" class="options-preview">
        <div v-for="opt in row.options" :key="opt.key" class="option-line">
          <b>{{ opt.key }}.</b> {{ opt.text }}
        </div>
      </div>

      <div class="judge-result">
        <p>
          <b>我的答案:</b>
          <span :class="row.isCorrect ? 'correct' : 'wrong'">{{ row.userAnswerDisplay }}</span>
        </p>
        <p><b>正确答案:</b>{{ row.correctAnswerDisplay }}</p>
        <p v-if="row.analysis"><b>解析:</b>{{ row.analysis }}</p>
      </div>
    </el-card>

    <div v-if="!loading && info && rows.length === 0" class="page-card">
      <el-empty description="暂无判分明细" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Trophy, List } from '@element-plus/icons-vue'
import { getExamRecordDetail } from '@/api/exam'
import type { ExamRecordDetail, ExamSubmitDetail, QuestionCard, QuestionOption } from '@/api/types'
import { typeText, typeTag, answerText } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const recordId = Number(route.params.recordId)

const info = ref<ExamRecordDetail | null>(null)
const loading = ref(false)

interface ResultRow {
  questionId: number
  content: string
  type?: string
  options?: QuestionOption[] | null
  userAnswerDisplay: string
  correctAnswerDisplay: string
  isCorrect: boolean
  score: number
  analysis?: string | null
}

const rows = ref<ResultRow[]>([])

/** isCorrect 后端可能为 boolean/数字,统一转换为布尔 */
function toBool(v: unknown): boolean {
  return v === true || v === 1 || v === 'true'
}

function buildRows(detail: ExamSubmitDetail[], questions: (QuestionCard & { userAnswer?: string })[]) {
  const qMap = new Map<number, QuestionCard & { userAnswer?: string }>()
  for (const q of questions || []) {
    qMap.set(q.id, q)
  }
  rows.value = (detail || []).map((d) => {
    const q = d.questionId !== undefined ? qMap.get(d.questionId) : undefined
    const embedded = (d.question ?? null) as QuestionCard | null
    const question = q ?? embedded
    const type = question?.type ?? (typeof d.type === 'string' ? d.type : undefined)
    const userAnswer = q?.userAnswer
      ?? (typeof d.userAnswer === 'string' ? d.userAnswer : undefined)
    const withMeta = question as (QuestionCard & { answer?: string; analysis?: string | null }) | null
    return {
      questionId: d.questionId ?? question?.id ?? 0,
      content: question?.content ?? (typeof d.content === 'string' ? d.content : ''),
      type,
      options: question?.options ?? null,
      userAnswerDisplay: answerText(type, userAnswer ?? '-'),
      correctAnswerDisplay: answerText(
        type,
        (typeof d.correctAnswer === 'string' && d.correctAnswer)
          ? d.correctAnswer
          : (withMeta?.answer ?? '-')
      ),
      isCorrect: toBool(d.isCorrect),
      score: (typeof d.gotScore === 'number' ? d.gotScore : d.score) ?? 0,
      analysis: withMeta?.analysis ?? d.analysis ?? null
    }
  })
}

function goRank() {
  const examId = info.value?.record?.examId
  if (!examId) return
  const name = info.value?.examName || info.value?.record?.examName || ''
  router.push(`/exams/${examId}/rank?name=${encodeURIComponent(name)}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getExamRecordDetail(recordId)
    info.value = res
    buildRows(res.detail || [], res.questions || [])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.exam-result {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.summary-card {
  margin-bottom: 16px;

  .summary {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .exam-name {
      margin: 0 0 10px;
      font-size: 20px;
      color: #303133;
    }

    .summary-tags {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
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
  }

  .question-content {
    font-size: 15px;
    line-height: 1.7;
    margin-bottom: 12px;
    color: #303133;
  }

  .options-preview {
    background: #f8f9fb;
    border-radius: 4px;
    padding: 10px 16px;
    margin-bottom: 12px;

    .option-line {
      line-height: 1.9;
      color: #606266;
    }
  }

  .judge-result {
    background: #fafafa;
    border-radius: 4px;
    padding: 10px 16px;

    p {
      margin: 4px 0;
      font-size: 14px;
      color: #606266;

      .correct {
        color: #67c23a;
        font-weight: 600;
      }

      .wrong {
        color: #f56c6c;
        font-weight: 600;
      }
    }
  }
}
</style>
