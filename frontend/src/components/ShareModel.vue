<template>
  <a-modal v-model:visible="visible" :footer="false">
    <template>
      {{ title }}
    </template>
    <h4 style="margin-top: 0">复制分享链接</h4>
    <a-typography-paragraph copyable>{{ link }}</a-typography-paragraph>
    <h4>手机扫码查看</h4>

    <img :src="codeImg" />
  </a-modal>
</template>

<script setup lang="ts">
// @ts-ignore
import QRCode from "qrcode";
import { defineExpose, defineProps, ref, withDefaults } from "vue";
import message from "@arco-design/web-vue/es/message";

const codeImg = ref(true);

interface Props {
  link: string;
  title: string;
}

const props = withDefaults(defineProps<Props>(), {
  link: "https://github.com/RJLante/Awesome-Quiz",
  title: "分享",
});

const visible = ref(false);

// 打开弹窗
const openModel = () => {
  visible.value = true;
};

// 关闭弹窗
const closeModel = () => {
  visible.value = false;
};

defineExpose({
  openModel,
});
// With promises
QRCode.toDataURL(props.link)
  .then((url: any) => {
    console.log(url);
    codeImg.value = url;
  })
  .catch((err: any) => {
    console.error(err);
    message.error("生成二维码失败，" + err.message);
  });
</script>
