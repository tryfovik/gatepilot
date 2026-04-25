<template>
  <div class="page-stack">
    <MetricStrip :items="metrics" />

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>发布与节点概览</h2>
          <p>按控制面、agent、proxy 三层查看当前系统是否能正常推进配置。</p>
        </div>
      </div>
      <div class="overview-grid">
        <div class="summary-block">
          <div class="summary-title">当前发布</div>
          <div class="summary-value">等待接入</div>
          <StatusBadge label="未发布" tone="neutral" />
        </div>
        <div class="summary-block">
          <div class="summary-title">节点状态</div>
          <div class="summary-value">0 / 0</div>
          <StatusBadge label="暂无节点" tone="neutral" />
        </div>
        <div class="summary-block">
          <div class="summary-title">上游健康</div>
          <div class="summary-value">暂无数据</div>
          <StatusBadge label="待上报" tone="info" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import MetricStrip from '../components/MetricStrip.vue';
import StatusBadge from '../components/StatusBadge.vue';

const metrics = [
  { label: '项目数', value: '0', note: 'GatewayProject' },
  { label: '路由数', value: '0', note: 'GatewayRoute' },
  { label: 'Proxy 副本', value: '0', note: 'GatewayNode' },
  { label: '配置分片', value: '0', note: 'configShard' }
];
</script>
