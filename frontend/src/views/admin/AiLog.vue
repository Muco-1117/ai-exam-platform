<template>
  <div class="page-card">
    <h3 class="page-title">AI 出题日志</h3>

    <div class="page-toolbar">
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <el-button type="primary" :icon="List" @click="router.push('/admin/questions')">题目管理</el-button>
      <el-button type="warning" @click="router.push('/admin/questions?reviewStatus=0')">查看待审核题目</el-button>
      <div class="flex-spacer"></div>
      <el-tag type="info" effect="plain">生成的新题目为「待审核」状态,审核通过后学生可见</el-tag>
    </div>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="id" label="日志ID" width="80" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="题型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="90">
        <template #default="{ row }">{{ difficultyText(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="count" label="数量" width="70" />
      <el-table-column prop="knowledge" label="知识点" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="errorMsg" label="错误信息" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="生成时间" width="170" />
      <template #empty>
        <el-empty description="暂无生成记录" :image-size="80" />
      </template>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, List } from '@element-plus/icons-vue'
import { getAiLogPage } from '@/api/ai'
import type { AiLog } from '@/api/types'
import { typeText, typeTag, difficultyText, type TagType } from '@/utils/format'

const router = useRouter()

const records = ref<AiLog[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ page: 1, size: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await getAiLogPage({ page: query.page, size: query.size })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

/** 日志状态:0生成中/1成功/2失败 */
function statusText(status?: number): string {
  if (status === 1) return '成功'
  if (status != null && status !== 0) return '失败'
  return '生成中'
}

function statusTag(status?: number): TagType {
  if (status === 1) return 'success'
  if (status != null && status !== 0) return 'danger'
  return 'warning'
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
