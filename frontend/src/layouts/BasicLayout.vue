<script setup lang="ts">
import GlobalHeader from "@/components/GlobalHeader.vue";
</script>

<template>
  <div id="basic-layout">
    <a-layout style="height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer" target="_blank">
        <a href="https://github.com/RJLante/Awesome-Quiz"
          >Awesome-Quiz by RJLante</a
        >
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#basic-layout {
}

#basic-layout .header {
  margin-bottom: 16px;
  box-shadow: #eee 1px 1px 5px;
}

#basic-layout .content {
  background: linear-gradient(to right, #fefefe, #fff);
  margin-bottom: 28px;
  padding: 20px;
}

.footer {
  background: #efefef;
  padding: 16px;
  text-align: center;
  text-decoration: none;
}
</style>
