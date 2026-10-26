<template>
  <div class="page-card">
    <h3 class="page-title">用户管理</h3>

    <div class="page-toolbar">
      <el-input
        v-model="query.username"
        placeholder="按用户名搜索"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="140" />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag :type="isAdminRole(row as UserAdminItem) ? 'danger' : 'primary'" size="small">{{ roleText(row as UserAdminItem) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'danger' : 'success'" size="small">
            {{ row.status === 0 ? '已禁用' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <template v-if="isSelf(row as UserAdminItem)">
            <span class="self-tip">当前登录用户</span>
          </template>
          <template v-else>
            <el-button :type="row.status === 0 ? 'success' : 'warning'" link @click="handleToggleStatus(row as UserAdminItem)">
              {{ row.status === 0 ? '启用' : '禁用' }}
            </el-button>
            <el-button v-if="!isAdminRole(row as UserAdminItem)" type="danger" link @click="handleSetRole(row as UserAdminItem, 1)">设为管理员</el-button>
            <el-button v-else type="primary" link @click="handleSetRole(row as UserAdminItem, 2)">设为学生</el-button>
          </template>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户" :image-size="80" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getUserPage, setUserRole, setUserStatus } from '@/api/user'
import type { UserAdminItem } from '@/api/types'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const records = ref<UserAdminItem[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  username: '',
  page: 1,
  size: 10
})

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage({
      username: query.username || undefined,
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
  query.username = ''
  query.page = 1
  loadData()
}

function isSelf(row: UserAdminItem): boolean {
  return userStore.userInfo?.id === row.id
}

function isAdminRole(row: UserAdminItem): boolean {
  if (row.roleId != null) return row.roleId === 1
  return !!row.roles?.some((r) => r.code === 'ADMIN' || r.name === 'ADMIN')
}

function roleText(row: UserAdminItem): string {
  if (row.roleName) return row.roleName
  if (row.roleId != null) return row.roleId === 1 ? '管理员' : '学生'
  const names = (row.roles || []).map((r) => r.name || r.code).filter(Boolean)
  return names.length ? names.join(',') : '-'
}

async function handleToggleStatus(row: UserAdminItem) {
  const target = row.status === 0 ? 1 : 0
  const action = target === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗?`, '操作确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await setUserStatus(row.id, target)
  ElMessage.success(`${action}成功`)
  loadData()
}

async function handleSetRole(row: UserAdminItem, roleId: number) {
  const roleName = roleId === 1 ? '管理员' : '学生'
  await ElMessageBox.confirm(`确定将用户「${row.username}」的角色调整为${roleName}吗?`, '操作确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await setUserRole(row.id, roleId)
  ElMessage.success('角色调整成功')
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

.self-tip {
  font-size: 12px;
  color: #909399;
}
</style>
