<template>
  <div class="page-card">
    <h3 class="page-title">我的收藏</h3>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column label="题干" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">{{ getQuestion(row as FavorItem)?.content || row.content || '-' }}</template>
      </el-table-column>
      <el-table-column label="题型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(getQuestion(row as FavorItem)?.type)" size="small">{{ typeText(getQuestion(row as FavorItem)?.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="90">
        <template #default="{ row }">{{ difficultyText(getQuestion(row as FavorItem)?.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="收藏时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            @click="router.push(`/questions/${row.questionId || getQuestion(row as FavorItem)?.id}`)"
          >
            去作答
          </el-button>
          <el-button type="danger" link @click="handleRemove(row as FavorItem)">取消收藏</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无收藏题目" :image-size="80" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getFavorPage, removeFavor } from '@/api/favor'
import type { Question, FavorItem } from '@/api/types'
import { typeText, typeTag, difficultyText } from '@/utils/format'

const router = useRouter()

const records = ref<FavorItem[]>([])
const loading = ref(false)

function getQuestion(row: FavorItem): Question | null | undefined {
  return row.question || (row as { questionInfo?: Question }).questionInfo || null
}

async function loadData() {
  loading.value = true
  try {
    const res = await getFavorPage({ page: 1, size: 100 })
    records.value = res.records || []
  } finally {
    loading.value = false
  }
}

async function handleRemove(row: FavorItem) {
  await removeFavor(row.questionId)
  ElMessage.success('已取消收藏')
  loadData()
}

onMounted(loadData)
</script>
