// src/request.ts
import axios, {
  AxiosError,
  AxiosHeaders,
  InternalAxiosRequestConfig,
} from "axios";
import { useAuthStore } from "@/store/auth";
import { Message } from "@arco-design/web-vue";

/* ========= 实例 ========= */
const instance = axios.create({
  // 后端端口；如有 .env.development -> VUE_APP_API_BASE，写那里也行
  baseURL: process.env.VUE_APP_API_BASE || "http://localhost:8101",
  timeout: 60_000,
  withCredentials: false,
});

const toBearer = (raw: string) => `Bearer ${raw.replace(/^Bearer\s+/i, "")}`;

/* ========= 请求拦截：自动加 JWT ========= */
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { token } = useAuthStore();
    if (token) {
      const headerToken = toBearer(token);

      // Axios v1 在两种 headers 结构间切换：AxiosHeaders ⇆ 普通对象
      if (config.headers && "set" in config.headers) {
        // TS 认为 headers 可能是 AxiosHeaders
        (config.headers as AxiosHeaders).set("Authorization", headerToken);
      } else {
        // headers 是普通对象或 undefined
        (config.headers ??= {} as any).Authorization = headerToken;
      }
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

/* ========= 响应拦截：统一 401 ========= */
instance.interceptors.response.use(
  (res) => res,
  (err: AxiosError) => {
    const status = err.response?.status;
    const bizCode = (err.response?.data as any)?.code; // 后端自定义

    if (status === 401 && bizCode === 10003 /* TOKEN_EXPIRED */) {
      useAuthStore().logout();
      Message.error("登录失效，请重新登录");
    } else if (status === 401) {
      Message.warning("无权限访问该资源");
    }
    return Promise.reject(err);
  }
);

export default instance;
