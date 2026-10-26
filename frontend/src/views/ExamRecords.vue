<template>
 <div class="page-card"><div class="page-toolbar"><h3 class="page-title">{{user.isAdmin?'考试答卷管理':'我的考试记录'}}</h3><div class="flex-spacer"/><el-button @click="load">刷新</el-button></div>
 <el-table :data="records" v-loading="loading" stripe><el-table-column prop="examName" label="考试名称" min-width="200"/><el-table-column prop="nickname" label="考生" width="130"/><el-table-column prop="startTime" label="开始时间" min-width="170"/><el-table-column prop="submitTime" label="交卷时间" min-width="170"/><el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.status===1?'success':'warning'">{{row.status===1?'已交卷':'进行中'}}</el-tag></template></el-table-column><el-table-column prop="totalScore" label="得分" width="80"/><el-table-column label="操作" width="150"><template #default="{row}"><el-button v-if="row.status===1||user.isAdmin" type="primary" link @click="router.push('/exam-result/'+row.id)">查看答卷</el-button><el-button v-else link @click="router.push('/exam-taking/'+row.examId)">继续作答</el-button></template></el-table-column></el-table>
 <div class="pagination-wrap"><el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="total,prev,pager,next" @current-change="load"/></div></div>
</template>
<script setup lang="ts">
import {ref,onMounted} from 'vue';import {useRouter} from 'vue-router';import {useUserStore} from '@/store/user';import {getExamRecordPage} from '@/api/exam';import type {ExamRecordItem} from '@/api/types';
const router=useRouter(),user=useUserStore(),records=ref<ExamRecordItem[]>([]),page=ref(1),total=ref(0),loading=ref(false);
async function load(){loading.value=true;try{const r=await getExamRecordPage({page:page.value,size:10,scope:user.isAdmin?'all':'mine'});records.value=r.records;total.value=r.total}finally{loading.value=false}}onMounted(load);
</script>
