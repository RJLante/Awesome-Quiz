<template>
  <div id="app">
    <template v-if="isUserLayout">
      <router-view />
    </template>
    <template v-else>
      <BasicLayout />
    </template>
  </div>
</template>

<script setup lang="ts">
import BasicLayout from "@/layouts/BasicLayout.vue";
import { useRoute } from "vue-router";
import { onMounted, computed } from "vue";

const route = useRoute();
const userLayoutPaths = ["/user/login", "/user/register"];
const isUserLayout = computed(() =>
  userLayoutPaths.some((path) => route.path.startsWith(path))
);

/**
 * 全局初始化函数，有全局单次调用的代码，都可以写到这里
 */
const doInit = () => {
  console.log("hello 欢迎来到我的项目");
};

onMounted(() => {
  doInit();
});
</script>

<style scoped></style>
