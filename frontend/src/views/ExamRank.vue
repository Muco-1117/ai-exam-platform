<template>
  <div class="page-card">
    <el-page-header class="page-header" @back="router.back()">
      <template #content>
        <span>排行榜{{ examName ? ` - ${examName}` : '' }}</span>
      </template>
    </el-page-header>

    <el-table v-loading="loading" :data="ranks" stripe>
      <el-table-column label="名次" width="100">
        <template #default="{ $index }">
          <span class="rank-no" :class="rankClass($index)">{{ $index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="用户昵称" min-width="200">
        <template #default="{ row }">{{ row.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="得分" width="140">
        <template #default="{ row }">
          <span class="rank-score">{{ row.totalScore ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="交卷时间" width="200" />
      <template #empty>
        <el-empty description="暂无排行数据" :image-size="80" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExamRank } from '@/api/exam'
import type { RankItem } from '@/api/types'

const route = useRoute()
const router = useRouter()

const examId = Number(route.params.examId)
const examName = typeof route.query.name === 'string' ? route.query.name : ''

const ranks = ref<RankItem[]>([])
const loading = ref(false)

function rankClass(index: number): string {
  if (index === 0) return 'first'
  if (index === 1) return 'second'
  if (index === 2) return 'third'
  return ''
}

async function loadData() {
  loading.value = true
  try {
    ranks.value = (await getExamRank(examId)) || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}

.rank-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: 600;
  color: #606266;

  &.first {
    background: #f56c6c;
    color: #fff;
  }

  &.second {
    background: #e6a23c;
    color: #fff;
  }

  &.third {
    background: #409eff;
    color: #fff;
  }
}

.rank-score {
  font-weight: 600;
  color: #409eff;
}
</style>
