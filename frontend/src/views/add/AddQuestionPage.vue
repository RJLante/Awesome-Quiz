<template>
  <div id="addQuestionPage">
    <h2 style="margin-bottom: 32px">设置题目</h2>
    <a-form
      :model="questionContent"
      :style="{ width: '480px' }"
      label-align="left"
      auto-label-width
      @submit="handleSubmit"
    >
      <a-form-item label="应用 id">
        <div style="display: flex; align-items: center">
          <span>{{ appId }}</span>
          <a-progress
            v-if="drawerProgressVisible"
            :percent="drawerProgress"
            style="margin-left: 16px; flex: 1"
          />
        </div>
      </a-form-item>
      <a-form-item label="题目列表" :content-flex="false" :merge-props="false">
        <!-- AI 生成题目抽屉：内部不再渲染进度条 -->
        <!-- 打开 AI 生成抽屉 -->
        <a-button type="primary" @click="drawerVisible = true">
          AI 生成题目
        </a-button>
        <AiGenerateQuesitonDrawer
          v-model:visible="drawerVisible"
          :appId="appId"
          @refresh="mergeQuestions"
          @reload="loadData"
          @progress="
            (val) => {
              drawerProgress = val;
              drawerProgressVisible = true;
            }
          "
          :onSuccess="onAiGenerateSuccess"
          :onSSESuccess="onAiGenerateSuccessSSE"
          :onSSEClose="onSSEClose"
          :onSSEStart="onSSEStart"
          :onSSEError="onSSEError"
        />
        <!-- 遍历每道题目 -->
        <a-progress
          v-if="drawerProgress > 0 && drawerProgress < 100"
          :percent="drawerProgress"
          status="active"
          style="width: 300px; margin: 16px 0; margin-left: 20px"
        />
        <div v-for="(question, index) in questionContent" :key="index">
          <a-space size="large">
            <h3>题目 {{ index + 1 }}</h3>
            <a-button size="small" @click="addQuestion(index + 1)">
              添加题目
            </a-button>
            <a-button
              size="small"
              status="danger"
              @click="deleteQuestion(index)"
            >
              删除题目
            </a-button>
          </a-space>
          <a-form-item field="posts.post1" :label="`题目 ${index + 1} 标题`">
            <a-input v-model="question.title" placeholder="请输入标题" />
          </a-form-item>
          <!--  题目选项 -->
          <a-space size="large">
            <h4>题目 {{ index + 1 }} 选项列表</h4>
            <a-button
              size="small"
              @click="addQuestionOption(question, question.options.length)"
            >
              底部添加选项
            </a-button>
          </a-space>
          <a-form-item
            v-for="(option, optionIndex) in question.options"
            :key="optionIndex"
            :label="`选项 ${optionIndex + 1}`"
            :content-flex="false"
            :merge-props="false"
          >
            <a-form-item label="选项 key">
              <a-input v-model="option.key" placeholder="请输入选项 key" />
            </a-form-item>
            <a-form-item label="选项值">
              <a-input v-model="option.value" placeholder="请输入选项值" />
            </a-form-item>
            <a-form-item label="选项结果">
              <a-input v-model="option.result" placeholder="请输入选项结果" />
            </a-form-item>
            <a-form-item label="选项得分">
              <a-input-number
                v-model="option.score"
                placeholder="请输入选项得分"
              />
            </a-form-item>
            <a-space size="large">
              <a-button
                size="mini"
                @click="addQuestionOption(question, optionIndex + 1)"
              >
                添加选项
              </a-button>
              <a-button
                size="mini"
                status="danger"
                @click="deleteQuestionOption(question, optionIndex as any)"
              >
                删除选项
              </a-button>
            </a-space>
          </a-form-item>
          <!-- 题目选项结尾 -->
        </div>
      </a-form-item>
      <a-form-item>
        <a-space size="medium">
          <a-button type="primary" html-type="submit" style="width: 120px">
            提交
          </a-button>
          <a-button @click="addQuestion(questionContent.length)">
            添加题目
          </a-button>
          <a-button @click="router.push(`/app/detail/${props.appId}`)">
            返回
          </a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { defineProps, ref, watchEffect, withDefaults } from "vue";
import API from "@/api";
import { useRouter } from "vue-router";
import { watch } from "vue";
import {
  addQuestionUsingPost,
  editQuestionUsingPost,
  listQuestionVoByPageUsingPost,
} from "@/api/questionController";
import message from "@arco-design/web-vue/es/message";
import AiGenerateQuesitonDrawer from "@/views/add/components/AiGenerateQuesitonDrawer.vue";

interface Props {
  appId: string;
}

const drawerProgress = ref(0);
const drawerProgressVisible = ref(false);
const drawerVisible = ref(false);
const mergeQuestions = (newList: API.QuestionContentDTO[]): void => {
  questionContent.value = [...questionContent.value, ...newList];
};

const props = withDefaults(defineProps<Props>(), {
  appId: () => {
    return "";
  },
});

const router = useRouter();

// 题目内容结构（理解为题目列表）
const questionContent = ref<API.QuestionContentDTO[]>([]);
watch(drawerVisible, (open) => {
  if (!open) {
    drawerProgressVisible.value = false;
    drawerProgress.value = 0;
  }
});
/**
 * 添加题目
 * @param index
 */
const addQuestion = (index: number) => {
  questionContent.value.splice(index, 0, {
    title: "",
    options: [],
  });
};

/**
 * 删除题目
 * @param index
 */
const deleteQuestion = (index: number) => {
  questionContent.value.splice(index, 1);
};

/**
 * 添加题目选项
 * @param question
 * @param index
 */
const addQuestionOption = (question: API.QuestionContentDTO, index: number) => {
  if (!question.options) {
    question.options = [];
  }
  question.options.splice(index, 0, {
    key: "",
    value: "",
  });
};

/**
 * 删除题目选项
 * @param question
 * @param index
 */
const deleteQuestionOption = (
  question: API.QuestionContentDTO,
  index: number
) => {
  if (!question.options) {
    question.options = [];
  }
  question.options.splice(index, 1);
};

const oldQuestion = ref<API.QuestionVO>();

/**
 * 加载数据
 */
// const loadData = async () => {
//   if (!props.appId) {
//     return;
//   }
//   const res = await listQuestionVoByPageUsingPost({
//     appId: props.appId as any,
//     current: 1,
//     pageSize: 1,
//     sortField: "createTime",
//     sortOrder: "descend",
//   });
//   if (res.data.code === 0 && res.data.data?.records) {
//     oldQuestion.value = res.data.data?.records[0];
//     if (oldQuestion.value) {
//       questionContent.value = oldQuestion.value.questionContent ?? [];
//     }
//   } else {
//     message.error("获取数据失败，" + res.data.message);
//   }
// };
const loadData = async () => {
  if (!props.appId) return;
  // 多拉几条，确保能拿到全部“散装”记录
  const res = await listQuestionVoByPageUsingPost({
    appId: props.appId as any,
    current: 1,
    pageSize: 20,
    sortField: "createTime",
    sortOrder: "ascend",
  });
  if (res.data.code === 0 && res.data.data?.records) {
    const allQvos = res.data.data.records;
    // 扁平化：把每条记录的 questionContent 数组都放到一个大数组里
    questionContent.value = allQvos.flatMap((qvo) => qvo.questionContent ?? []);
    // 如果后续还要用某个 id 来判断“编辑”之类的逻辑，
    // 可以取第一个 qvo.id 存下来
    oldQuestion.value = allQvos[0];
  } else {
    message.error("获取数据失败，" + res.data.message);
  }
};
// 获取旧数据
watchEffect(() => {
  loadData();
});

/**
 * 提交
 */
const handleSubmit = async () => {
  if (!props.appId || !questionContent.value) {
    return;
  }
  let res: any;
  // 如果是修改
  if (oldQuestion.value?.id) {
    res = await editQuestionUsingPost({
      id: oldQuestion.value.id,
      questionContent: questionContent.value,
    });
  } else {
    // 创建
    res = await addQuestionUsingPost({
      appId: props.appId as any,
      questionContent: questionContent.value,
    });
  }
  if (res.data.code === 0) {
    message.success("操作成功，即将跳转到应用详情页");
    setTimeout(() => {
      router.push(`/app/detail/${props.appId}`);
    }, 1000);
  } else {
    message.error("操作失败，" + res.data.message);
  }
};

/**
 * AI 生成题目成功后执行
 */
const onAiGenerateSuccess = (result: API.QuestionContentDTO[]) => {
  questionContent.value = [...questionContent.value, ...result];
};

/**
 * AI 生成题目成功后执行（SSE）
 */
const onAiGenerateSuccessSSE = (result: API.QuestionContentDTO) => {
  questionContent.value = [...questionContent.value, result];
};
const onSSEStart = (event: any) => {
  message.success(`正在生成题目，请稍后...`);
};
const onSSEClose = (event: any) => {
  message.success(`生成完毕`);
};
const onSSEError = (event: any) => {
  message.error(`生成题目失败，${event.data}`);
};
</script>

<style scoped>
.question-item {
  margin-bottom: 20px;
}
</style>
