<template>
  <a-drawer
    :width="340"
    :visible="visible"
    @update:visible="(v) => emit('update:visible', v)"
    @cancel="() => handleCancel"
    unmountOnClose
  >
    <template #title>AI 生成题目</template>
    <div>
      <a-form
        :model="form"
        label-align="left"
        auto-label-width
        @submit="handleSubmit"
      >
        <a-form-item label="应用 id">
          {{ appId }}
        </a-form-item>
        <a-form-item field="questionNumber" label="题目数量">
          <a-input-number
            min="0"
            max="20"
            v-model="form.questionNumber"
            placeholder="请输入题目数量"
          />
        </a-form-item>
        <a-form-item field="optionNumber" label="选项数量">
          <a-input-number
            min="0"
            max="8"
            v-model="form.optionNumber"
            placeholder="请输入选项数量"
          />
        </a-form-item>
        <a-form-item>
          <a-space direction="vertical">
            <a-button
              :loading="submitting"
              type="primary"
              html-type="submit"
              style="width: 120px"
            >
              {{ submitting ? "生成中" : "一键生成" }}
            </a-button>

            <a-button
              :loading="submitting"
              style="width: 120px"
              @click="handleSSESubmit"
            >
              {{ submitting ? "生成中" : "实时生成" }}
            </a-button>

            <a-button
              :loading="submitting"
              style="width: 120px"
              @click="handleAsyncSubmit"
            >
              {{ submitting ? "生成中" : "异步生成" }}
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { defineProps, reactive, ref, withDefaults } from "vue";
import API from "@/api";
import {
  aiGenerateQuestionUsingPost,
  aiGenerateQuestionAsyncMqUsingPost,
  listByIdsUsingPost,
} from "@/api/questionController";
import message from "@arco-design/web-vue/es/message";
import { useAuthStore } from "@/store/auth";
import { getTaskUsingGet } from "@/api/taskController";

interface Props {
  appId: string;
  visible: boolean;
  onSuccess?: (result: API.QuestionContentDTO[]) => void;
  onSSESuccess?: (result: API.QuestionContentDTO) => void;
  onSSEStart?: (event: any) => void;
  onSSEClose?: (event: any) => void;
}

const props = withDefaults(defineProps<Props>(), {
  appId: () => "",
  visible: () => false,
});

const form = reactive<API.AiGenerateQuestionRequest>({
  optionNumber: 2,
  questionNumber: 10,
} as API.AiGenerateQuestionRequest);

const submitting = ref(false);

const progressVisible = ref(false);
const progress = ref(0);
let timer: number | undefined;
/* eslint-disable no-undef */
const emit = defineEmits<{
  (e: "refresh", list: API.QuestionContentDTO[]): void;
  (e: "progress", p: number): void;
}>();

const handleCancel = () => {
  emit("update:visible", false);
};

const handleSubmit = async () => {
  if (!props.appId) {
    return;
  }
  submitting.value = true;
  const res = await aiGenerateQuestionUsingPost({
    appId: props.appId as any,
    ...form,
  });
  if (res.data.code === 0 && res.data.data.length > 0) {
    if (props.onSuccess) {
      props.onSuccess(res.data.data);
    } else {
      message.success("生成题目成功");
    }
    // 关闭抽屉
    handleCancel();
  } else {
    message.error("操作失败，" + res.data.message);
  }
  submitting.value = false;
};

async function handleAsyncSubmit() {
  if (!props.appId) return;
  submitting.value = true;
  progressVisible.value = true;

  try {
    const res = await aiGenerateQuestionAsyncMqUsingPost({
      appId: props.appId as any,
      ...form,
    });
    submitting.value = false;

    if (res.data.code !== 0 || !res.data.data) {
      message.error("提交失败：" + res.data.message);
      progressVisible.value = false;
      return;
    }

    const taskId = res.data.data.taskId;
    message.success(`已提交，任务号 #${taskId}`);
    handleCancel?.();
    progress.value = 0;

    // 启动轮询
    timer = window.setInterval(async () => {
      const r = await getTaskUsingGet({ id: taskId });
      if (r.data.code !== 0 || !r.data.data) return;

      const task = r.data.data;
      if (task.status === "running") {
        const raw = task.progress ?? 0;
        progress.value = raw / 100;
        emit("progress", progress.value);
      } else if (task.status === "succeed") {
        clearInterval(timer!);
        progress.value = 1;
        emit("progress", progress.value);

        // 解析结果 ID 列表
        const ids: number[] = JSON.parse(task.genResult ?? "[]");
        if (ids.length > 0) {
          const qRes = await listByIdsUsingPost(ids);
          if (qRes.data.code === 0 && qRes.data.data) {
            const dtoList: API.QuestionContentDTO[] = qRes.data.data.flatMap(
              (q) => q.questionContent ?? []
            );
            props.onSuccess?.(dtoList);
            emit("reload");
          }
        }
        message.success("题目生成完成！");
        progressVisible.value = false;
      } else if (task.status === "failed") {
        clearInterval(timer!);
        progress.value = 0;
        emit("progress", progress.value);
        message.error("任务失败：" + task.execMessage);
        progressVisible.value = false;
      }
    }, 1500);
  } catch (e: any) {
    // 异常处理
    submitting.value = false;
    progressVisible.value = false;
    message.error("异步生成出错：" + e.message);
  }
}

/**
 * 实时生成
 */
const handleSSESubmit = async () => {
  if (!props.appId) {
    return;
  }
  submitting.value = true;
  handleCancel();
  // 创建 SSE 请求
  const base =
    process.env.VUE_APP_API_BASE ||
    "http://localhost:8101" ||
    window.location.origin;
  const { token } = useAuthStore();
  const eventSource = new EventSource(
    `${base}/api/question/ai_generate/sse` +
      `?token=${encodeURIComponent(token)}` + // 把 JWT 加到查询串
      `&appId=${props.appId}` +
      `&optionNumber=${form.optionNumber}` +
      `&questionNumber=${form.questionNumber}`
  );
  let first = true;
  // 接收消息
  eventSource.onmessage = function (event) {
    if (first) {
      first = false;
      props.onSSEStart?.(event);
      handleCancel();
    }
    props.onSSESuccess?.(JSON.parse(event.data));
  };
  // 报错或连接关闭时触发
  eventSource.onerror = function (event) {
    if (event.eventPhase === EventSource.CLOSED) {
      console.log("SSE 连接已关闭");
      props.onSSEClose()?.(event);
      eventSource.close();
    } else {
      eventSource.close();
    }
  };
  submitting.value = false;
};
</script>

<style scoped></style>
