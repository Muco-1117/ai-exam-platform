<template>
  <div class="page-card">
    <h3 class="page-title">考试管理</h3>

    <div class="page-toolbar">
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <div class="flex-spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建考试(组卷)</el-button>
    </div>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="考试名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="所属分类" width="120" />
      <el-table-column label="时长(分钟)" width="100">
        <template #default="{ row }">{{ row.duration ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="总分" width="80">
        <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="及格分" width="80">
        <template #default="{ row }">{{ row.passScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="题数" width="70">
        <template #default="{ row }">{{ row.questionCount ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="examStatusTag(row.status)" size="small">{{ examStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="success" link @click="handlePublish(row as ExamItem)">发布</el-button>
          <el-button type="warning" link @click="goRank(row as ExamItem)">排行榜</el-button>
          <el-button type="danger" link @click="handleDelete(row as ExamItem)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无考试" :image-size="80" />
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

    <!-- 组卷弹窗 -->
    <el-dialog v-model="createVisible" title="新建考试(组卷)" width="860px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="考试名称" prop="name">
              <el-input v-model="createForm.name" placeholder="请输入考试名称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="createForm.categoryId" placeholder="请选择分类" filterable style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分)" prop="duration">
              <el-input-number v-model="createForm.duration" :min="5" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="及格分" prop="passScore">
              <el-input-number v-model="createForm.passScore" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider content-position="left">选题(勾选题目并设置分值)</el-divider>

      <div class="picker-toolbar">
        <el-select v-model="pickerQuery.categoryId" placeholder="全部分类" clearable filterable style="width: 160px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="pickerQuery.type" placeholder="全部题型" clearable style="width: 120px">
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTI" />
          <el-option label="判断题" value="JUDGE" />
          <el-option label="填空题" value="FILL" />
        </el-select>
        <el-button type="primary" :icon="Search" size="small" @click="loadPicker">筛选</el-button>
        <span class="picked-summary">已选 <b>{{ selected.length }}</b> 题,合计
          <b :class="{ over: totalPickedScore > createForm.passScore }">{{ totalPickedScore }}</b> 分</span>
      </div>

      <el-table
        :data="pickerRecords"
        stripe
        row-key="id"
        max-height="300"
        @selection-change="onPickChange"
      >
        <el-table-column type="selection" width="45" reserve-selection />
        <el-table-column prop="content" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }">{{ difficultyText(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="建议分值" width="100">
          <template #default="{ row }">{{ row.score }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无可选题目" :image-size="60" />
        </template>
      </el-table>

      <div class="picker-pagination">
        <el-pagination
          v-model:current-page="pickerQuery.page"
          v-model:page-size="pickerQuery.size"
          :total="pickerTotal"
          layout="total, prev, pager, next"
          @current-change="loadPicker"
        />
      </div>

      <div v-if="selected.length" class="picked-list">
        <div v-for="(item, idx) in selected" :key="item.q.id" class="picked-item">
          <span class="picked-no">{{ idx + 1 }}.</span>
          <span class="picked-content">{{ item.q.content }}</span>
          <el-input-number v-model="item.score" :min="1" :max="100" size="small" style="width: 110px" />
          <span class="picked-unit">分</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Plus, Search } from '@element-plus/icons-vue'
import { getCategoryList } from '@/api/category'
import { createExam, deleteExam, getExamPage, publishExam } from '@/api/exam'
import { getQuestionPage } from '@/api/question'
import type { Category, ExamItem, Question } from '@/api/types'
import { typeText, typeTag, difficultyText, examStatusText, type TagType } from '@/utils/format'

const router = useRouter()

const categories = ref<Category[]>([])
const records = ref<ExamItem[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ page: 1, size: 10 })

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
    const res = await getExamPage({ page: query.page, size: query.size })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function examStatusTag(status?: number): TagType {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

async function handlePublish(row: ExamItem) {
  await ElMessageBox.confirm(`确定发布考试「${row.name}」吗?发布后学生即可参加。`, '发布确认', {
    confirmButtonText: '确定发布',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await publishExam(row.id)
  ElMessage.success('发布成功')
  loadData()
}

async function handleDelete(row: ExamItem) {
  await ElMessageBox.confirm(`确定删除考试「${row.name}」吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteExam(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function goRank(row: ExamItem) {
  router.push(`/exams/${row.id}/rank?name=${encodeURIComponent(row.name)}`)
}

// ---------------- 组卷 ----------------

const createVisible = ref(false)
const saving = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = reactive({
  name: '',
  categoryId: undefined as number | undefined,
  duration: 60,
  passScore: 60
})

const createRules: FormRules = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  duration: [{ required: true, message: '请设置考试时长', trigger: 'change' }],
  passScore: [{ required: true, message: '请设置及格分', trigger: 'change' }]
}

const pickerQuery = reactive({
  categoryId: undefined as number | undefined,
  type: undefined as string | undefined,
  page: 1,
  size: 10
})

const pickerRecords = ref<Question[]>([])
const pickerTotal = ref(0)

/** 已选题目及其分值 */
const selected = ref<{ q: Question; score: number }[]>([])

const totalPickedScore = computed(() => selected.value.reduce((sum, s) => sum + (s.score || 0), 0))

async function loadPicker() {
  const res = await getQuestionPage({
    categoryId: pickerQuery.categoryId,
    type: pickerQuery.type,
    reviewStatus: 1,
    page: pickerQuery.page,
    size: pickerQuery.size
  })
  pickerRecords.value = res.records || []
  pickerTotal.value = res.total || 0
}

function onPickChange(rows: Question[]) {
  const prev = new Map(selected.value.map((s) => [s.q.id, s.score]))
  selected.value = rows.map((q) => ({ q, score: prev.get(q.id) ?? q.score }))
}

function openCreate() {
  createForm.name = ''
  createForm.categoryId = undefined
  createForm.duration = 60
  createForm.passScore = 60
  selected.value = []
  pickerQuery.categoryId = undefined
  pickerQuery.type = undefined
  pickerQuery.page = 1
  createVisible.value = true
  loadPicker()
}

async function handleCreate() {
  await createFormRef.value?.validate()

  if (selected.value.length === 0) {
    ElMessage.warning('请至少勾选一道题目')
    return
  }

  saving.value = true
  try {
    await createExam({
      name: createForm.name,
      categoryId: createForm.categoryId!,
      duration: createForm.duration,
      passScore: createForm.passScore,
      questions: selected.value.map((s) => ({ questionId: s.q.id, score: s.score }))
    })
    ElMessage.success('创建成功,可在列表中发布')
    createVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped lang="scss">
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.picker-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;

  .picked-summary {
    margin-left: auto;
    font-size: 13px;
    color: #606266;

    b {
      color: #409eff;

      &.over {
        color: #f56c6c;
      }
    }
  }
}

.picker-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.picked-list {
  margin-top: 12px;
  max-height: 220px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;

  .picked-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0;
    font-size: 13px;

    .picked-no {
      color: #909399;
      flex-shrink: 0;
    }

    .picked-content {
      flex: 1;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .picked-unit {
      color: #909399;
    }
  }
}
</style>
