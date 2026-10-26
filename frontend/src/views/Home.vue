<template>
  <div class="home-page">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome">
        <div>
          <h2 class="hello">你好,{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}!</h2>
          <p class="sub">欢迎回来,坚持刷题,稳步提升。</p>
        </div>
        <div class="quick-entry">
          <el-button type="primary" :icon="EditPen" @click="router.push('/questions')">去刷题</el-button>
          <el-button type="warning" :icon="WarningFilled" @click="router.push('/wrong')">错题本</el-button>
          <el-button type="success" :icon="Histogram" @click="router.push('/exams')">考试中心</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">累计刷题</div>
            <div class="stat-value">{{ stats?.totalCount ?? 0 }}</div>
            <div class="stat-unit">道</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">答对题数</div>
            <div class="stat-value success">{{ stats?.correctCount ?? 0 }}</div>
            <div class="stat-unit">道</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">正确率</div>
            <div class="stat-value primary">{{ correctRateText }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="by-type-card">
      <template #header>
        <span>各题型作答情况</span>
      </template>
      <el-table v-if="stats && stats.byType && stats.byType.length" :data="stats.byType" stripe>
        <el-table-column label="题型" width="140">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="total" label="作答总数" width="120" />
        <el-table-column prop="correct" label="答对数" width="120" />
        <el-table-column label="正确率">
          <template #default="{ row }">
            <el-progress
              :percentage="row.total ? Math.round((row.correct / row.total) * 100) : 0"
              :stroke-width="14"
            />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无刷题记录,快去题库刷题吧" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { EditPen, WarningFilled, Histogram } from '@element-plus/icons-vue'
import { getPracticeStats } from '@/api/practice'
import type { PracticeStats } from '@/api/types'
import { useUserStore } from '@/store/user'
import { typeText } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const stats = ref<PracticeStats | null>(null)

const correctRateText = computed(() => {
  const rate = stats.value?.correctRate
  if (rate == null) return '0%'
  return rate <= 1 ? `${Math.round(rate * 100)}%` : `${Math.round(rate)}%`
})

onMounted(async () => {
  try {
    stats.value = await getPracticeStats()
  } catch {
    // 错误已由拦截器统一提示
  }
})
</script>

<style scoped lang="scss">
.welcome-card {
  margin-bottom: 16px;

  .welcome {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;

    .hello {
      margin: 0 0 6px;
      font-size: 20px;
      color: #303133;
    }

    .sub {
      margin: 0;
      color: #909399;
      font-size: 14px;
    }
  }
}

.stat-row {
  margin-bottom: 16px;

  .stat-item {
    text-align: center;

    .stat-label {
      color: #909399;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 32px;
      font-weight: 600;
      color: #303133;

      &.success {
        color: #67c23a;
      }

      &.primary {
        color: #409eff;
      }
    }

    .stat-unit {
      color: #909399;
      font-size: 12px;
    }
  }
}
</style>
