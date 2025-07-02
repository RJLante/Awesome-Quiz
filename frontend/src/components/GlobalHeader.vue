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
          <a-button type="primary" @click="router.push(`/user/info`)"
            >{{ loginUserStore.loginUser.userName ?? "无名" }}
          </a-button>
          <a-button type="outline" @click="handleLogout">退出</a-button>
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
const visibleRoutes = computed<RouteRecordRaw[]>(() =>
  router.getRoutes().filter((item) => {
    // 只展示顶级、已命名、未隐藏的路由
    if (!item.name || item.children?.length || item.meta?.hideInMenu) {
      return false;
    }
    // 权限控制
    return checkAccess(loginUserStore.loginUser, item.meta?.access as string);
  })
);

const doMenuClick = (key: string) => {
  router.push(key);
};

const authStore = useAuthStore();
const handleLogout = () => {
  authStore.logout();
  router.replace("/user/login");
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
