import { createRouter, createWebHistory } from "vue-router";
import { routes } from "@/router/routes";
import { useAuthStore } from "@/store/auth";

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();

  // 白名单
  if (["/user/login", "/user/register"].includes(to.path)) return true;
  if (to.path === "/" || to.path.startsWith("/app/detail")) return true;

  // ① 没 token：跳登录
  if (!auth.token) return "/user/login";

  // ② token 有，但 userInfo 为空：拉一次
  if (!auth.userInfo) await auth.fetchMe();

  // ③ 拉完仍没有 userInfo（token 已过期）
  if (!auth.userInfo) return "/user/login";

  // ④ 个人中心权限校验：仅本人或管理员可访问
  if (to.path.startsWith("/user/info")) {
    const targetId = String(to.params.id ?? "");
    const loginUser = auth.userInfo;
    if (
      targetId &&
      loginUser &&
      loginUser.userRole !== "admin" &&
      String(loginUser.id) !== targetId
    ) {
      return "/noAuth";
    }
  }
  return true;
});

export default router;
