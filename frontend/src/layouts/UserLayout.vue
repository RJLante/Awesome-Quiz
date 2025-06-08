<script setup lang="ts"></script>

<template>
  <div id="user-layout">
    <a-layout style="height: 100vh">
      <a-layout-header class="header">
        <a-space>
          <img class="logo" src="../assets/test.png" />
          <div>Awesome Quiz AI 答题应用平台</div>
        </a-space>
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
#user-layout {
  text-align: center;
  background: url("https://gw.alipayobjects.com/zos/rmsportal/FfdJeJRQWjEeGTpqgBKj.png")
    0% 0% / 100% 100%;
}

#user-layout .logo {
  height: 48px;
  width: 48px;
}

#user-layout .header {
  margin-top: 16px;
  font-size: 21px;
}

#user-layout .content {
  margin-bottom: 16px;
  padding: 20px;
}

.footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 16px;
  text-align: center;
}
</style>
