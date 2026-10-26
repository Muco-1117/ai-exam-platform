<template>
  <el-container class="app-layout">
    <el-header class="app-header" height="60px">
      <div class="brand" @click="router.push('/')">
        <el-icon :size="26" color="#409eff"><Cpu /></el-icon>
        <span class="brand-name">AI智能刷题考试平台</span>
      </div>
      <el-menu
        mode="horizontal"
        router
        :default-active="activeMenu"
        :ellipsis="false"
        class="app-menu"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/questions">题库</el-menu-item>
        <el-menu-item index="/practice">练习记录</el-menu-item>
        <el-menu-item index="/wrong">错题本</el-menu-item>
        <el-menu-item index="/favorites">我的收藏</el-menu-item>
        <el-menu-item index="/exams">考试中心</el-menu-item>
        <el-menu-item index="/exam-records">考试记录</el-menu-item>
        <template v-if="userStore.isAdmin">
          <el-sub-menu index="admin">
            <template #title>管理端</template>
            <el-menu-item index="/admin/questions">题目管理</el-menu-item>
            <el-menu-item index="/admin/questions?reviewStatus=0">待审核题目</el-menu-item>
            <el-menu-item index="/admin/questions?ai=1">AI出题</el-menu-item>
            <el-menu-item index="/admin/exams">考试管理</el-menu-item>
            <el-menu-item index="/admin/categories">分类管理</el-menu-item>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/ai-logs">AI日志</el-menu-item>
            <el-menu-item index="/admin/services">微服务监控</el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon><UserFilled /></el-icon>
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
            <el-tag v-if="userStore.isAdmin" size="small" type="danger" class="role-tag">管理员</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { connectWs, disconnectWs } from '@/utils/websocket'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

onMounted(() => {
  // 登录状态下建立 WebSocket 长连接
  if (userStore.token) {
    connectWs(userStore.token)
  }
})

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    disconnectWs()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100%;
}

.app-header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;

  .brand-name {
    font-size: 17px;
    font-weight: 600;
    color: #303133;
    white-space: nowrap;
  }
}

.app-menu {
  flex: 1;
  border-bottom: none;
}

.header-right {
  flex-shrink: 0;
  cursor: pointer;

  .user-info {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #303133;
    font-size: 14px;
  }

  .role-tag {
    margin-left: 2px;
  }
}

.app-main {
  padding: 20px 24px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
}
</style>
