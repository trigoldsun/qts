<template>
  <div id="app">
    <TradingPanel />
    <RiskStatusPanel />
  </div>
</template>

<script setup lang="ts">
import TradingPanel from './views/TradingPanel.vue';
import RiskStatusPanel from './views/RiskStatusPanel.vue';
</script>

<style>
#app {
  min-height: 100vh;
}
</style>