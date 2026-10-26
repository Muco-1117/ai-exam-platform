<template>
  <div class="page-card">
    <h3 class="page-title">错题本</h3>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column type="index" label="#" width="60" :index="indexMethod" />
      <el-table-column label="题干" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">{{ getQuestion(row as WrongItem)?.content || row.content || '-' }}</template>
      </el-table-column>
      <el-table-column label="题型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(getQuestion(row as WrongItem)?.type)" size="small">{{ typeText(getQuestion(row as WrongItem)?.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="90">
        <template #default="{ row }">{{ difficultyText(getQuestion(row as WrongItem)?.difficulty) }}</template>
      </el-table-column>
      <el-table-column label="错误次数" width="100">
        <template #default="{ row }">
          <el-tag type="danger" size="small" effect="plain">{{ row.wrongCount ?? 1 }} 次</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastWrongAt" label="最近错误时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            @click="router.push(`/questions/${row.questionId || getQuestion(row as WrongItem)?.id}`)"
          >
            重新作答
          </el-button>
          <el-button type="success" link @click="handleMaster(row as WrongItem)">标记已掌握</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="错题本是空的,继续保持!" :image-size="80" />
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
import { ElMessage } from 'element-plus'
import { getWrongPage, masterWrong } from '@/api/wrong'
import type { Question, WrongItem } from '@/api/types'
import { typeText, typeTag, difficultyText } from '@/utils/format'

const router = useRouter()

const records = ref<WrongItem[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = ref(10)

function indexMethod(index: number): number {
  return (page.value - 1) * size.value + index + 1
}

function getQuestion(row: WrongItem): Question | null | undefined {
  return row.question || (row as { questionInfo?: Question }).questionInfo || null
}

async function loadData() {
  loading.value = true
  try {
    const res = await getWrongPage({ page: page.value, size: size.value })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function handleMaster(row: WrongItem) {
  await masterWrong(row.questionId)
  ElMessage.success('已标记掌握,移出错题本')
  loadData()
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
