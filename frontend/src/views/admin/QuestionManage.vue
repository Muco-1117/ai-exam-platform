<template>
  <div class="page-card">
    <h3 class="page-title">题目管理</h3>

    <div class="page-toolbar">
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable filterable style="width: 170px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.type" placeholder="全部题型" clearable style="width: 130px">
        <el-option label="单选题" value="SINGLE" />
        <el-option label="多选题" value="MULTI" />
        <el-option label="判断题" value="JUDGE" />
        <el-option label="填空题" value="FILL" />
      </el-select>
      <el-select v-model="query.difficulty" placeholder="全部难度" clearable style="width: 120px">
        <el-option label="简单" :value="1" />
        <el-option label="中等" :value="2" />
        <el-option label="困难" :value="3" />
      </el-select>
      <el-select v-model="query.reviewStatus" placeholder="审核状态" clearable style="width: 130px">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="按题干关键词搜索"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      <div class="flex-spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增题目</el-button>
      <el-button type="success" :icon="MagicStick" @click="openAiDialog">AI出题</el-button>
    </div>

    <p class="list-hint">管理端包含待审核和已通过的题目；审核通过后才会出现在题库。题目ID允许不连续。</p>
    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column type="index" label="序号" width="65" :index="indexMethod" />
      <el-table-column prop="id" label="题目ID" width="85" />
      <el-table-column prop="content" label="题干" min-width="300" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="题型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="80">
        <template #default="{ row }">{{ difficultyText(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="70" />
      <el-table-column label="来源" width="90">
        <template #default="{ row }">{{ sourceText(row.source) }}</template>
      </el-table-column>
      <el-table-column label="审核状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.reviewStatus === 1 ? 'success' : 'danger'" size="small">
            {{ reviewText(row.reviewStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row as Question)">编辑</el-button>
          <el-button
            v-if="row.reviewStatus !== 1"
            type="success"
            link
            @click="handleReview(row as Question, 1)"
          >审核通过</el-button>
          <el-button v-else type="warning" link @click="handleReview(row as Question, 0)">撤回审核</el-button>
          <el-button type="danger" link @click="handleDelete(row as Question)">删除</el-button>
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

    <!-- 新增/编辑题目 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑题目' : '新增题目'" width="640px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" filterable style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" prop="type">
          <el-select v-model="editForm.type" style="width: 100%" @change="onTypeChange">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="填空题" value="FILL" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" prop="content">
          <el-input v-model="editForm.content" type="textarea" :rows="3" placeholder="请输入题干" />
        </el-form-item>

        <el-form-item v-if="isChoiceType" label="选项" required>
          <div class="option-editor">
            <div v-for="(opt, idx) in editForm.options" :key="idx" class="option-row">
              <el-input v-model="opt.key" class="option-key" maxlength="1" placeholder="A" />
              <el-input v-model="opt.text" placeholder="选项内容" />
              <el-button
                type="danger"
                link
                :disabled="editForm.options.length <= 2"
                @click="editForm.options.splice(idx, 1)"
              >删除</el-button>
            </div>
            <el-button type="primary" link :icon="Plus" @click="addOption">添加选项</el-button>
          </div>
        </el-form-item>

        <el-form-item label="正确答案" prop="answer">
          <el-select v-if="editForm.type === 'SINGLE'" v-model="editForm.answer" placeholder="请选择正确选项" style="width: 100%">
            <el-option v-for="opt in editForm.options" :key="opt.key" :label="`${opt.key}. ${opt.text}`" :value="opt.key" />
          </el-select>
          <el-select
            v-else-if="editForm.type === 'MULTI'"
            v-model="multiAnswer"
            multiple
            placeholder="请选择正确选项(可多选)"
            style="width: 100%"
          >
            <el-option v-for="opt in editForm.options" :key="opt.key" :label="`${opt.key}. ${opt.text}`" :value="opt.key" />
          </el-select>
          <el-select v-else-if="editForm.type === 'JUDGE'" v-model="editForm.answer" placeholder="请选择" style="width: 100%">
            <el-option label="正确" value="true" />
            <el-option label="错误" value="false" />
          </el-select>
          <el-input v-else v-model="editForm.answer" placeholder="请输入正确答案" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="editForm.analysis" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="editForm.difficulty" style="width: 100%">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="分值" prop="score">
          <el-input-number v-model="editForm.score" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI 出题 -->
    <el-dialog v-model="aiVisible" title="AI 智能出题" width="560px" destroy-on-close @closed="resetAiState">
      <el-form :model="aiForm" label-width="90px">
        <el-form-item label="所属分类" required>
          <el-select v-model="aiForm.categoryId" placeholder="请选择分类" filterable style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="aiForm.type" style="width: 100%">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="填空题" value="FILL" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" required>
          <el-select v-model="aiForm.difficulty" style="width: 100%">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="出题数量" required>
          <el-input-number v-model="aiForm.count" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="aiForm.knowledge" placeholder="选填,如:Java 集合框架" clearable />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="aiPhase === 'loading'"
        type="info"
        :closable="false"
        show-icon
        title="正在生成题目,请耐心等待..."
      />
      <el-alert
        v-else-if="aiPhase === 'done'"
        type="success"
        :closable="false"
        show-icon
        title="生成成功!请先确认入库，再到待审核列表逐题审核。"
        class="ai-result"
      >
        <div class="ai-actions">
          <el-button type="primary" size="small" :loading="confirming" @click="handleConfirm(true)">全部入库(待审核)</el-button>
          <el-button type="danger" size="small" :loading="confirming" @click="handleConfirm(false)">全部丢弃</el-button>
        </div>
      </el-alert>
      <el-alert
        v-else-if="aiPhase === 'failed'"
        type="error"
        :closable="false"
        show-icon
        :title="`生成失败:${aiErrorMsg || '未知错误'}`"
        class="ai-result"
      />

      <template #footer>
        <el-button @click="aiVisible = false">关闭</el-button>
        <el-button type="success" :loading="aiPhase === 'loading'" :disabled="aiPhase === 'loading'" @click="handleGenerate">
          {{ aiPhase === 'loading' ? '生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, MagicStick } from '@element-plus/icons-vue'
import { getCategoryList } from '@/api/category'
import { addQuestion, deleteQuestion, getQuestionPage, reviewQuestion, updateQuestion } from '@/api/question'
import { confirmAiGenerate, generateQuestions, getAiLogDetail } from '@/api/ai'
import type { AiLog, Category, Question, QuestionOption } from '@/api/types'
import { typeText, typeTag, difficultyText, sourceText, reviewText, sortMultiAnswer } from '@/utils/format'

const route = useRoute()

const categories = ref<Category[]>([])
const records = ref<Question[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  categoryId: undefined as number | undefined,
  type: undefined as string | undefined,
  difficulty: undefined as number | undefined,
  reviewStatus: route.query.reviewStatus != null ? Number(route.query.reviewStatus) : (undefined as number | undefined),
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
      reviewStatus: query.reviewStatus,
      keyword: query.keyword || undefined,
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
  query.reviewStatus = undefined
  query.keyword = ''
  query.page = 1
  loadData()
}

// ---------------- 审核 / 删除 ----------------

async function handleReview(row: Question, status: number) {
  await reviewQuestion(row.id, status)
  ElMessage.success(status === 1 ? '已审核通过' : '已撤回审核')
  loadData()
}

async function handleDelete(row: Question) {
  await ElMessageBox.confirm('确定删除该题目吗?删除后不可恢复。', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteQuestion(row.id)
  ElMessage.success('删除成功')
  loadData()
}

// ---------------- 新增 / 编辑 ----------------

const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref<FormInstance>()

const editForm = reactive<{
  id?: number
  categoryId?: number
  type: string
  content: string
  options: QuestionOption[]
  answer: string
  analysis: string
  difficulty: number
  score: number
}>({
  id: undefined,
  categoryId: undefined,
  type: 'SINGLE',
  content: '',
  options: [],
  answer: '',
  analysis: '',
  difficulty: 1,
  score: 5
})

const multiAnswer = ref<string[]>([])

const isChoiceType = computed(() => editForm.type === 'SINGLE' || editForm.type === 'MULTI')

const editRules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ required: true, message: '请填写正确答案', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  score: [{ required: true, message: '请填写分值', trigger: 'change' }]
}

function defaultOptions(): QuestionOption[] {
  return ['A', 'B', 'C', 'D'].map((key) => ({ key, text: '' }))
}

function addOption() {
  const keys = editForm.options.map((o) => o.key)
  let next = 'A'
  for (const c of 'ABCDEFGH') {
    if (!keys.includes(c)) {
      next = c
      break
    }
  }
  editForm.options.push({ key: next, text: '' })
}

function onTypeChange() {
  // 切换题型时重置选项与答案
  editForm.answer = ''
  multiAnswer.value = []
  editForm.options = isChoiceType.value ? defaultOptions() : []
}

function openEdit(row?: Question) {
  if (row) {
    editForm.id = row.id
    editForm.categoryId = row.categoryId
    editForm.type = row.type
    editForm.content = row.content
    editForm.options = row.options ? row.options.map((o) => ({ ...o })) : []
    editForm.answer = row.answer || ''
    multiAnswer.value = row.type === 'MULTI' ? (row.answer || '').split(',').filter(Boolean) : []
    editForm.analysis = row.analysis || ''
    editForm.difficulty = row.difficulty
    editForm.score = row.score
  } else {
    editForm.id = undefined
    editForm.categoryId = undefined
    editForm.type = 'SINGLE'
    editForm.content = ''
    editForm.options = defaultOptions()
    editForm.answer = ''
    multiAnswer.value = []
    editForm.analysis = ''
    editForm.difficulty = 1
    editForm.score = 5
  }
  editVisible.value = true
}

async function handleSave() {
  await editFormRef.value?.validate()

  if (isChoiceType.value && editForm.options.some((o) => !o.key || !o.text)) {
    ElMessage.warning('请补全选项内容')
    return
  }

  const payload = {
    categoryId: editForm.categoryId!,
    type: editForm.type,
    content: editForm.content,
    options: isChoiceType.value ? editForm.options.map((o) => ({ ...o })) : null,
    answer: editForm.type === 'MULTI' ? sortMultiAnswer(multiAnswer.value) : editForm.answer,
    analysis: editForm.analysis || undefined,
    difficulty: editForm.difficulty,
    score: editForm.score
  }

  saving.value = true
  try {
    if (editForm.id) {
      await updateQuestion(editForm.id, payload)
      ElMessage.success('修改成功')
    } else {
      await addQuestion(payload)
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

// ---------------- AI 出题 ----------------

const aiVisible = ref(false)
const aiPhase = ref<'idle' | 'loading' | 'done' | 'failed'>('idle')
const confirming = ref(false)
const aiErrorMsg = ref('')
const aiLogId = ref(0)
let pollTimer: number | null = null
let pollCount = 0

const aiForm = reactive({
  categoryId: undefined as number | undefined,
  type: 'SINGLE',
  difficulty: 1,
  count: 5,
  knowledge: ''
})

function openAiDialog() {
  resetAiState()
  aiVisible.value = true
}

function resetAiState() {
  stopPoll()
  aiPhase.value = 'idle'
  aiErrorMsg.value = ''
  aiLogId.value = 0
}

function stopPoll() {
  if (pollTimer != null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleGenerate() {
  if (!aiForm.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  aiPhase.value = 'loading'
  aiErrorMsg.value = ''
  try {
    const res = await generateQuestions({
      categoryId: aiForm.categoryId,
      type: aiForm.type,
      difficulty: aiForm.difficulty,
      count: aiForm.count,
      knowledge: aiForm.knowledge || undefined
    })
    aiLogId.value = res.logId
    pollCount = 0
    pollTimer = window.setInterval(pollAiLog, 2000)
  } catch {
    aiPhase.value = 'failed'
  }
}

async function pollAiLog() {
  if (!aiLogId.value) return
  pollCount += 1
  if (pollCount > 90) {
    stopPoll()
    aiPhase.value = 'failed'
    aiErrorMsg.value = '生成超时,请稍后在 AI 日志中查看结果'
    return
  }
  try {
    const log: AiLog = await getAiLogDetail(aiLogId.value)
    if (log.status === 1) {
      stopPoll()
      aiPhase.value = 'done'
      ElMessage.success('AI 出题成功,请审核生成的题目')
    } else if (log.status != null && log.status !== 0) {
      stopPoll()
      aiPhase.value = 'failed'
      aiErrorMsg.value = log.errorMsg || '生成失败'
    }
  } catch {
    // 单次轮询失败忽略,下次重试
  }
}

async function handleConfirm(confirm: boolean) {
  confirming.value = true
  try {
    await confirmAiGenerate(aiLogId.value, confirm)
    ElMessage.success(confirm ? '已全部入库，请在待审核列表逐题审核' : '已全部丢弃')
    aiVisible.value = false
    // 查看待审核新题
    query.reviewStatus = 0
    query.page = 1
    loadData()
  } finally {
    confirming.value = false
  }
}

onBeforeUnmount(stopPoll)

watch(() => route.query.reviewStatus, value => {
  query.reviewStatus = value != null ? Number(value) : undefined
  query.page = 1
  loadData()
})

watch(() => route.query.ai, value => {
  if (value === '1') openAiDialog()
})

onMounted(() => {
  loadCategories()
  loadData()
  if (route.query.ai === '1') {
    openAiDialog()
  }
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

.option-editor {
  width: 100%;

  .option-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .option-key {
      width: 70px;
      flex-shrink: 0;
    }
  }
}

.ai-result {
  margin-top: 4px;

  .ai-actions {
    margin-top: 8px;
    display: flex;
    gap: 8px;
  }
}
</style>
