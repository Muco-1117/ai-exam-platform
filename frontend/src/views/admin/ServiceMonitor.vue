<template>
  <div class="page-card">
    <div class="page-toolbar"><div><h3 class="page-title">微服务运行监控</h3><p class="hint">四个独立进程通过 HTTP 契约协作，服务状态来自实时健康检查。</p></div><div class="flex-spacer"/><el-button type="primary" :loading="loading" @click="load">刷新状态</el-button></div>
    <div class="topology"><span>浏览器</span><b>→</b><span>统一网关 :8080</span><b>→</b><span>考试业务 :8081</span><b>→</b><div class="workers"><span>判分服务 :8082</span><span>AI服务 :8083</span></div></div>
    <el-table :data="services" border stripe>
      <el-table-column prop="name" label="服务" min-width="140"/>
      <el-table-column prop="service" label="进程标识" min-width="145"/>
      <el-table-column label="健康状态" width="115"><template #default="{row}"><el-tag :type="row.status==='UP'?'success':'danger'">{{row.status==='UP'?'运行中':'不可用'}}</el-tag></template></el-table-column>
      <el-table-column prop="url" label="调用地址" min-width="220"/>
      <el-table-column prop="latencyMs" label="探测耗时ms" width="115"/>
      <el-table-column prop="requests" label="请求计数" width="100"/>
      <el-table-column prop="version" label="版本" width="90"/>
    </el-table>
    <h4>服务职责与数据边界</h4>
    <el-descriptions v-for="s in services" :key="s.service" :column="1" border class="boundary"><el-descriptions-item :label="s.name">{{s.role}}</el-descriptions-item><el-descriptions-item label="数据边界">{{s.storage}}</el-descriptions-item></el-descriptions>
    <el-alert title="调用约定" type="info" :closable="false" description="服务间使用 HTTP / JSON 和内部令牌认证；请求设置超时，下游不可用时明确返回错误。AI候选题存入Redis并保留24小时。当前采用配置地址发现。"/>
    <p class="hint">最近刷新：{{updated}}。请求计数为各进程启动后的累计值。</p>
  </div>
</template>
<script setup lang="ts">
import {ref,onMounted} from 'vue'
import request from '@/api/request'
interface Service {service:string;name:string;status:string;url:string;version:string;requests?:number;latencyMs?:number;role:string;storage:string}
const services=ref<Service[]>([]),loading=ref(false),updated=ref('')
async function load(){loading.value=true;try{const res=await request.get<{services:Service[]}>('/system/topology');services.value=res.services;updated.value=new Date().toLocaleString('zh-CN')}finally{loading.value=false}}
onMounted(load)
</script>
<style scoped>.hint{color:#606266;font-size:13px;line-height:1.8;margin:6px 0 16px}.topology{display:flex;align-items:center;justify-content:center;gap:18px;padding:30px 10px;background:#f0f7ff;margin-bottom:24px;flex-wrap:wrap}.topology span{border:1px solid #a5c6eb;background:white;padding:15px 20px;border-radius:4px}.workers{display:flex;gap:10px;flex-direction:column}.boundary{margin-bottom:12px}h4{margin:26px 0 16px}</style>
