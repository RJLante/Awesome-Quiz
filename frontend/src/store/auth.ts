import { defineStore } from "pinia";
import { loginUsingPost, registerUsingPost } from "@/api/authController";
import { getLoginUserUsingGet } from "@/api/userController";
import { updateMyUserUsingPost } from "@/api/userController";
import UserVO = API.UserVO;

const stripBearer = (raw: string) => raw.replace(/^Bearer\s+/i, "");
export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    userInfo: JSON.parse(localStorage.getItem("userInfo") || "null"),
    user: null as UserVO | null,
  }),
  actions: {
    async login(payload: API.UserLoginRequest) {
      try {
        const { data } = await loginUsingPost(payload);
        if (data.code === 0) {
          // @ts-ignore
          this.token = stripBearer(data.data!.token);
          // @ts-ignore
          this.userInfo = data.data!.userInfo;
          localStorage.setItem("token", this.token);
          await this.fetchMe();
          return true;
        }
        return false;
      } catch (e) {
        return false; // 出错也返回 false
      }
    },

    async register(payload: API.UserRegisterRequest) {
      try {
        const { data } = await registerUsingPost(payload);
        if (data.code === 0) {
          // @ts-ignore
          this.token = data.data!.token;
          // @ts-ignore
          this.userInfo = data.data!.userInfo;
          localStorage.setItem("token", this.token);
          await this.fetchMe();
          return true;
        }
      } catch (e) {
        return false; // 出错也返回 false
      }
    },

    logout() {
      this.token = "";
      this.user = null;
      localStorage.removeItem("token");
      window.location.replace("/user/login");
    },

    async fetchMe() {
      const { data } = await getLoginUserUsingGet();
      this.user = data.data ?? null;
      this.userInfo = data.data ?? null;
      localStorage.setItem("userInfo", JSON.stringify(this.userInfo));
      return this.user;
    },

    async updateMe(payload: API.UserUpdateMyRequest) {
      const { data } = await updateMyUserUsingPost(payload);
      if (data.code === 0) {
        await this.fetchMe();
        return true;
      }
      return false;
    },
  },
});
