<template>
  <div id="userInfoPage">
    <a-form :model="form" :style="{ maxWidth: '600px' }" @submit="handleSubmit">
      <h2 class="title">个人中心</h2>
      <a-form-item field="userName" label="昵称">
        <a-input v-model="form.userName" placeholder="请输入昵称" />
      </a-form-item>

      <a-form-item field="userProfile" label="简介">
        <a-textarea v-model="form.userProfile" placeholder="一句话介绍自己" />
      </a-form-item>

      <a-form-item field="userAvatar" label="头像">
        <PictureUploader
          biz="user_avatar"
          :value="form.userAvatar"
          :onChange="(value) => (form.userAvatar = value)"
        />
      </a-form-item>

      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit">保存</a-button>
          <a-button @click="reset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { Message } from "@arco-design/web-vue";
import { useAuthStore } from "@/store/auth";
import { useLoginUserStore } from "@/store/userStore";
import { updateMyUserUsingPost } from "@/api/userController";
import API from "@/api";
import router from "@/router";
import PictureUploader from "@/components/PictureUploader.vue";

const authStore = useAuthStore();
const loginUserStore = useLoginUserStore();

// 初始数据
const origin = authStore.userInfo ?? {};
const form = reactive<API.UserUpdateMyRequest>({
  userName: origin.userName,
  userProfile: origin.userProfile,
  userAvatar: origin.userAvatar,
});

// 提交
const handleSubmit = async () => {
  const { data } = await updateMyUserUsingPost(form);
  if (data.code === 0) {
    Message.success("保存成功");
    await authStore.fetchMe(); // 刷新 Pinia
    await loginUserStore.fetchLoginUser();
    router.push({
      path: "/",
      replace: true,
    });
  } else {
    Message.error(data.message || "保存失败");
  }
};

// 重置
const reset = () => {
  Object.assign(form, origin);
};
</script>
