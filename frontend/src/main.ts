import { createApp } from "vue";
import App from "./App.vue";
import ArcoVue from "@arco-design/web-vue";
import { createPinia } from "pinia";
import "@arco-design/web-vue/dist/arco.css";
import router from "./router";
import "@/access";
import { useAuthStore } from "@/store/auth";

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);

const authStore = useAuthStore();

/**
 * 每次路由跳转前，若 pinia 里没用户信息但本地有 token，
 * 就尝试拉一次 /user/get/login
 */
router.beforeEach(async (to, _from, next) => {
  if (!authStore.user && authStore.token) {
    try {
      await authStore.fetchMe();
    } catch {
      /* 忽略错误，fetchMe 已在 request.ts 中 401 时自动登出 */
    }
  }
  next();
});

app.use(ArcoVue).use(router).mount("#app");
