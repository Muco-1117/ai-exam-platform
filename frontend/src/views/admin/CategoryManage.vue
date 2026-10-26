<template>
  <div class="page-card">
    <h3 class="page-title">分类管理</h3>

    <div class="page-toolbar">
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <div class="flex-spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增分类</el-button>
    </div>

    <el-table v-loading="loading" :data="categories" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="分类名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="280" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row as Category)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row as Category)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无分类" :image-size="80" />
      </template>
    </el-table>

    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑分类' : '新增分类'" width="480px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入分类名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { addCategory, deleteCategory, getCategoryList, updateCategory } from '@/api/category'
import type { Category } from '@/api/types'

const categories = ref<Category[]>([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    categories.value = (await getCategoryList()) || []
  } finally {
    loading.value = false
  }
}

async function handleDelete(row: Category) {
  await ElMessageBox.confirm(
    `确定删除分类「${row.name}」吗?若该分类下存在题目将无法删除。`,
    '删除确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  loadData()
}

// ---------------- 新增 / 编辑 ----------------

const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref<FormInstance>()

const editForm = reactive<{ id?: number; name: string; description: string }>({
  id: undefined,
  name: '',
  description: ''
})

const editRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

function openEdit(row?: Category) {
  editForm.id = row?.id
  editForm.name = row?.name || ''
  editForm.description = row?.description || ''
  editVisible.value = true
}

async function handleSave() {
  await editFormRef.value?.validate()
  saving.value = true
  try {
    const data = { name: editForm.name, description: editForm.description || undefined }
    if (editForm.id) {
      await updateCategory(editForm.id, data)
      ElMessage.success('修改成功')
    } else {
      await addCategory(data)
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
