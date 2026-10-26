<template>
  <div class="page-card">
    <h3 class="page-title">考试中心</h3>

    <el-table v-loading="loading" :data="exams" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="考试名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="所属分类" width="130" />
      <el-table-column label="时长(分钟)" width="110">
        <template #default="{ row }">{{ row.duration ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="总分" width="80">
        <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="及格分" width="90">
        <template #default="{ row }">{{ row.passScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="题数" width="80">
        <template #default="{ row }">{{ row.questionCount ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.recordStatus === 1" type="primary" size="small" @click="router.push('/exam-result/' + row.recordId)">查看成绩</el-button>
          <el-button v-else type="primary" :icon="EditPen" size="small" :loading="startingExamId === row.id" :disabled="startingExamId !== null" @click="handleStart(row as ExamItem)">{{ row.recordStatus === 0 ? '继续考试' : '开始考试' }}</el-button>
          <el-button type="warning" :icon="Trophy" size="small" @click="router.push(`/exams/${row.id}/rank?name=${encodeURIComponent(row.name)}`)">
            排行榜
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无可参加的考试" :image-size="80" />
      </template>
    </el-table>

    <h3 class="page-title section-title">我的考试记录</h3>

    <el-table v-loading="recordLoading" :data="records" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="examName" label="考试名称" min-width="220" show-overflow-tooltip />
      <el-table-column label="得分" width="100">
        <template #default="{ row }">
          <span v-if="row.status === 1" class="record-score">{{ row.totalScore ?? '-' }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
            {{ recordStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="180" />
      <el-table-column prop="submitTime" label="交卷时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            type="primary"
            link
            @click="router.push(`/exam-result/${row.id}`)"
          >查看成绩</el-button>
          <el-button
            v-if="row.examId"
            type="warning"
            link
            @click="router.push(`/exams/${row.examId}/rank?name=${encodeURIComponent(row.examName || '')}`)"
          >排行榜</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无考试记录" :image-size="80" />
      </template>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="recordQuery.page"
        v-model:page-size="recordQuery.size"
        :total="recordTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadRecords"
        @current-change="loadRecords"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen, Trophy } from '@element-plus/icons-vue'
import { getPublishedExamList, getExamRecordPage, startExam } from '@/api/exam'
import type { ExamItem, ExamRecordItem } from '@/api/types'
import { recordStatusText } from '@/utils/format'

const router = useRouter()

const exams = ref<ExamItem[]>([])
const loading = ref(false)
const startingExamId = ref<number | null>(null)

const records = ref<ExamRecordItem[]>([])
const recordTotal = ref(0)
const recordLoading = ref(false)
const recordQuery = reactive({ page: 1, size: 10 })

async function loadData() {
  loading.value = true
  try {
    exams.value = (await getPublishedExamList()) || []
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const res = await getExamRecordPage({ page: recordQuery.page, size: recordQuery.size, scope: 'mine' })
    records.value = res.records || []
    recordTotal.value = res.total || 0
  } finally {
    recordLoading.value = false
  }
}

async function handleStart(row: ExamItem) {
  if (startingExamId.value !== null) return
  startingExamId.value = row.id
  try {
    // start 幂等:进行中重复调用返回原记录,可直接进入答题页
    const res = await startExam(row.id)
    ElMessage.success(`已进入考试:${res.examName}`)
    await router.push(`/exam-taking/${row.id}`)
  } finally {
    startingExamId.value = null
  }
}

onMounted(() => {
  loadData()
  loadRecords()
})
</script>

<style scoped lang="scss">
.section-title {
  margin-top: 24px;
}

.record-score {
  font-weight: 600;
  color: #409eff;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
