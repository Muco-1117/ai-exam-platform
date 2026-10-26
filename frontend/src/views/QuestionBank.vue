<template>
  <div class="page-card">
    <h3 class="page-title">题库刷题</h3>

    <div class="page-toolbar">
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable filterable style="width: 180px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.type" placeholder="全部题型" clearable style="width: 140px">
        <el-option label="单选题" value="SINGLE" />
        <el-option label="多选题" value="MULTI" />
        <el-option label="判断题" value="JUDGE" />
        <el-option label="填空题" value="FILL" />
      </el-select>
      <el-select v-model="query.difficulty" placeholder="全部难度" clearable style="width: 140px">
        <el-option label="简单" :value="1" />
        <el-option label="中等" :value="2" />
        <el-option label="困难" :value="3" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="按题干关键词搜索"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      <div class="flex-spacer"></div>
      <el-button :icon="Star" @click="router.push('/favorites')">我的收藏</el-button>
    </div>

    <p class="list-hint">题库仅展示审核通过的题目；序号按当前列表连续显示，题目ID是固定编号。</p>
    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column type="index" label="序号" width="65" :index="indexMethod" />
      <el-table-column prop="id" label="题目ID" width="85" />
      <el-table-column prop="content" label="题干" min-width="320" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="题型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="90">
        <template #default="{ row }">{{ difficultyText(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="80" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="router.push(`/questions/${row.id}`)">去作答</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无题目" :image-size="80" />
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
import { Search, Refresh, Star } from '@element-plus/icons-vue'
import { getCategoryList } from '@/api/category'
import { getQuestionPage } from '@/api/question'
import type { Category, Question } from '@/api/types'
import { typeText, typeTag, difficultyText } from '@/utils/format'

const router = useRouter()

const categories = ref<Category[]>([])
const records = ref<Question[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  categoryId: undefined as number | undefined,
  type: undefined as string | undefined,
  difficulty: undefined as number | undefined,
  keyword: '',
  page: 1,
  size: 10
})

function indexMethod(index: number): number {
  return (query.page - 1) * query.size + index + 1
}

async function loadCategories() {
  try {
    categories.value = (await getCategoryList()) || []
  } catch {
    categories.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getQuestionPage({
      categoryId: query.categoryId,
      type: query.type,
      difficulty: query.difficulty,
      keyword: query.keyword || undefined,
      reviewStatus: 1,
      page: query.page,
      size: query.size
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.categoryId = undefined
  query.type = undefined
  query.difficulty = undefined
  query.keyword = ''
  query.page = 1
  loadData()
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped lang="scss">
.list-hint {
  color: #606266;
  font-size: 12px;
  margin: 0 0 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
