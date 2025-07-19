<template>
  <a-row id="globalHeader" align="center" :wrap="false">
    <a-col flex="auto">
      <a-menu
        mode="horizontal"
        :selected-keys="selectedKeys"
        @menu-item-click="doMenuClick"
      >
        <a-menu-item
          key="0"
          :style="{ padding: 0, marginRight: '38px' }"
          disabled
        >
          <div class="titleBar">
            <img class="logo" src="../assets/test.png" />
            <div class="title">Awesome-Quiz</div>
          </div>
        </a-menu-item>
        <a-menu-item v-for="item in visibleRoutes" :key="item.path">
          {{ item.name }}
        </a-menu-item>
      </a-menu>
    </a-col>
    <a-col flex="100px">
      <div v-if="loginUserStore.loginUser.id">
        <a-space>
          <a-avatar>
            <img alt="avatar" :src="loginUserStore.loginUser.userAvatar" />
          </a-avatar>
          <a-dropdown @select="handleSelect">
            <!-- 触发按钮：只负责展开下拉，不再直接路由跳转 -->
            <a-button style="margin-right: 20px">
              {{ loginUserStore.loginUser.userName || "无名" }}
              <icon-down class="icon-down" />
            </a-button>

            <!-- 下拉内容 -->
            <template #content>
              <a-doption :value="'info'">个人中心</a-doption>
              <a-doption :value="'logout'">退出登录</a-doption>
            </template>
          </a-dropdown>
        </a-space>
      </div>
      <div v-else>
        <a-button type="primary" href="/user/login">登录</a-button>
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { useRouter, type RouteRecordRaw } from "vue-router";
import { computed, ref } from "vue";
import { useLoginUserStore } from "@/store/userStore";
import checkAccess from "@/access/checkAccess";
import { useAuthStore } from "@/store/auth";

const router = useRouter();
const loginUserStore = useLoginUserStore();

/** 当前选中的菜单 key */
const selectedKeys = ref([router.currentRoute.value.path]);

router.afterEach((to) => {
  selectedKeys.value = [to.path];
});

/** 顶部可见菜单（运行时读取，彻底避开循环依赖） */
const visibleRoutes = computed<RouteRecordRaw[]>(() => {
  const list = router.getRoutes().filter((item) => {
    if (!item.name || item.children?.length || item.meta?.hideInMenu) {
      return false;
    }
    return checkAccess(loginUserStore.loginUser, item.meta?.access as string);
  });
  // 如果找到了 path === "/"，就 splice 出来，然后 unshift 到最前
  const idx = list.findIndex((r) => r.path === "/");
  if (idx > 0) {
    const [home] = list.splice(idx, 1);
    list.unshift(home);
  }
  return list;
});

const doMenuClick = (key: string) => {
  router.push(key);
};

const authStore = useAuthStore();

const handleSelect = (key: string) => {
  switch (key) {
    case "info":
      router.push("/user/info");
      break;
    case "logout":
      authStore.logout();
      router.push({
        name: "个人中心",
        params: { id: loginUserStore.loginUser.id },
      });
      break;
  }
};
</script>

<style scoped>
#globalHeader {
}

.titleBar {
  display: flex;
  align-items: center;
}

.title {
  margin-left: 16px;
  color: black;
}

.logo {
  height: 48px;
}
</style>
