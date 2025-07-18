<template>
  <div id="userInfoPage">
    <h2 style="margin-bottom: 16px">个人中心</h2>

    <a-form
      :model="form"
      :style="{ maxWidth: '480px', margin: '0 auto' }"
      @submit="handleSubmit"
    >
      <a-form-item field="userName" label="昵称">
        <a-input v-model="form.userName" placeholder="请输入昵称" />
      </a-form-item>

      <a-form-item field="userProfile" label="简介">
        <a-textarea v-model="form.userProfile" placeholder="一句话介绍自己" />
      </a-form-item>

      <a-form-item field="userAvatar" label="头像">
        <a-upload
          :action="uploadUrl"
          :data="{ biz: 'user_avatar' }"
          :headers="{ Authorization: `Bearer ${authStore.token}` }"
          :file-list="fileList"
          accept="image/*"
          list-type="picture-card"
          @success="handleUploadSuccess"
        >
          <template #default>
            <icon-plus />
          </template>
        </a-upload>
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

const authStore = useAuthStore();
const loginUserStore = useLoginUserStore();

// 初始数据
const origin = authStore.userInfo ?? {};
const form = reactive<API.UserUpdateMyRequest>({
  userName: origin.userName,
  userProfile: origin.userProfile,
  userAvatar: origin.userAvatar,
});

// 头像上传
const uploadUrl = `${
  process.env.VUE_APP_API_BASE || "http://localhost:8101"
}/api/file/upload`;
const fileList = ref(
  form.userAvatar
    ? [{ url: form.userAvatar, name: "avatar", status: "done" }]
    : []
);
const handleUploadSuccess = (response: API.BaseResponseString_) => {
  if (response.code === 0) {
    form.userAvatar = response.data;
    Message.success("上传成功");
  } else {
    Message.error(response.message || "上传失败");
  }
};

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
