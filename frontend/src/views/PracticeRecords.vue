<template>
  <div class="page-card">
    <h3 class="page-title">练习记录</h3>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column label="题干" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">{{ row.questionContent || row.content || row.question?.content || '-' }}</template>
      </el-table-column>
      <el-table-column label="我的答案" width="120">
        <template #default="{ row }">{{ answerText(row.type || row.question?.type, row.userAnswer) }}</template>
      </el-table-column>
      <el-table-column label="判分结果" width="100">
        <template #default="{ row }">
          <el-tag :type="isCorrect(row as PracticeRecord) ? 'success' : 'danger'" size="small">
            {{ isCorrect(row as PracticeRecord) ? '正确' : '错误' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="得分" width="80" />
      <el-table-column prop="createTime" label="作答时间" width="180" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.questionId || row.question?.id"
            type="primary"
            link
            @click="router.push(`/questions/${row.questionId || row.question.id}`)"
          >
            查看题目
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无练习记录" :image-size="80" />
      </template>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPracticePage } from '@/api/practice'
import type { PracticeRecord } from '@/api/types'
import { answerText } from '@/utils/format'

const router = useRouter()

const records = ref<PracticeRecord[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = ref(10)

function indexMethod(index: number): number {
  return (page.value - 1) * size.value + index + 1
}

function isCorrect(row: PracticeRecord): boolean {
  return row.isCorrect === true || row.isCorrect === 1
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPracticePage({ page: page.value, size: size.value })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
