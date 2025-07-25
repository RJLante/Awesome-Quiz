<template>
  <a-form
    :model="formSearchParams"
    :style="{ marginBottom: '20px' }"
    layout="inline"
    @submit="doSearch"
  >
    <a-form-item field="resultName" label="结果名称">
      <a-input
        v-model="formSearchParams.resultName"
        placeholder="请输入结果名称"
        allow-clear
      />
    </a-form-item>
    <a-form-item field="resultDesc" label="结果描述">
      <a-input
        v-model="formSearchParams.resultDesc"
        placeholder="请输入结果描述"
        allow-clear
      />
    </a-form-item>
    <a-form-item>
      <a-button type="primary" html-type="submit" style="width: 100px">
        搜索
      </a-button>
    </a-form-item>
  </a-form>
  <a-table
    :columns="columns"
    :data="dataList"
    :pagination="{
      showTotal: true,
      pageSize: searchParams.pageSize,
      current: searchParams.current,
      total,
    }"
    @page-change="onPageChange"
  >
    <template #resultName="{ record }">
      <template v-if="editingId === record.id">
        <a-input v-model="editingRecord.resultName" />
      </template>
      <template v-else>
        {{ record.resultName }}
      </template>
    </template>
    <template #resultDesc="{ record }">
      <template v-if="editingId === record.id">
        <a-input v-model="editingRecord.resultDesc" />
      </template>
      <template v-else>
        {{ record.resultDesc }}
      </template>
    </template>
    <template #resultPicture="{ record }">
      <template v-if="editingId === record.id">
        <a-input v-model="editingRecord.resultPicture" />
      </template>
      <template v-else>
        <a-image width="64" :src="record.resultPicture" />
      </template>
    </template>
    <template #resultProp="{ record }">
      <template v-if="editingId === record.id">
        <a-input-tag
          v-model="editingRecord.resultProp"
          :style="{ width: '150px' }"
        />
      </template>
      <template v-else>
        {{ record.resultProp?.join(",") }}
      </template>
    </template>
    <template #resultScoreRange="{ record }">
      <template v-if="editingId === record.id">
        <a-input-number v-model="editingRecord.resultScoreRange" />
      </template>
      <template v-else>
        {{ record.resultScoreRange }}
      </template>
    </template>
    <template #createTime="{ record }">
      {{ dayjs(record.createTime).format("YYYY-MM-DD HH:mm:ss") }}
    </template>
    <template #updateTime="{ record }">
      {{ dayjs(record.updateTime).format("YYYY-MM-DD HH:mm:ss") }}
    </template>
    <template #optional="{ record }">
      <a-space>
        <template v-if="editingId === record.id">
          <a-button type="primary" @click="saveEdit">保存</a-button>
          <a-button @click="cancelEdit">取消</a-button>
        </template>
        <template v-else>
          <a-button status="success" @click="startEdit(record)">修改</a-button>
          <a-button status="danger" @click="doDelete(record)">删除</a-button>
        </template>
      </a-space>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { defineExpose, defineProps, ref, watchEffect, withDefaults } from "vue";
import {
  deleteScoringResultUsingPost,
  listScoringResultVoByPageUsingPost,
  editScoringResultUsingPost,
} from "@/api/scoringResultController";
import API from "@/api";
import message from "@arco-design/web-vue/es/message";
import { dayjs } from "@arco-design/web-vue/es/_utils/date";
import PictureUploader from "@/components/PictureUploader.vue";

interface Props {
  appId: string;
  doUpdate: (scoringResult: API.ScoringResultVO) => void;
}

const props = withDefaults(defineProps<Props>(), {
  appId: () => {
    return "";
  },
});

const formSearchParams = ref<API.ScoringResultQueryRequest>({});

// 初始化搜索条件（不应该被修改）
const initSearchParams = {
  current: 1,
  pageSize: 10,
  sortField: "createTime",
  sortOrder: "descend",
};

const searchParams = ref<API.ScoringResultQueryRequest>({
  ...initSearchParams,
});
const dataList = ref<API.ScoringResultVO[]>([]);
const total = ref<number>(0);
const editingId = ref<number | null>(null);
const editingRecord = ref<API.ScoringResultVO>({} as API.ScoringResultVO);

/**
 * 加载数据
 */
const loadData = async () => {
  if (!props.appId) {
    return;
  }
  const params = {
    appId: props.appId as any,
    ...searchParams.value,
  };
  const res = await listScoringResultVoByPageUsingPost(params);
  if (res.data.code === 0) {
    dataList.value = res.data.data?.records || [];
    total.value = res.data.data?.total || 0;
  } else {
    message.error("获取数据失败，" + res.data.message);
  }
};

// 暴露函数给父组件
defineExpose({
  loadData,
});

/**
 * 执行搜索
 */
const doSearch = () => {
  searchParams.value = {
    ...initSearchParams,
    ...formSearchParams.value,
  };
};

/**
 * 当分页变化时，改变搜索条件，触发数据加载
 * @param page
 */
const onPageChange = (page: number) => {
  searchParams.value = {
    ...searchParams.value,
    current: page,
  };
};

/**
 * 删除
 * @param record
 */
const doDelete = async (record: API.ScoringResult) => {
  if (!record.id) {
    return;
  }

  const res = await deleteScoringResultUsingPost({
    id: record.id,
  });
  if (res.data.code === 0) {
    loadData();
  } else {
    message.error("删除失败，" + res.data.message);
  }
};

const startEdit = (record: API.ScoringResultVO) => {
  editingId.value = record.id ?? null;
  editingRecord.value = { ...record } as API.ScoringResultVO;
};

const cancelEdit = () => {
  editingId.value = null;
};

const saveEdit = async () => {
  if (!editingRecord.value.id) return;
  const res = await editScoringResultUsingPost({
    ...editingRecord.value,
  } as API.ScoringResultEditRequest);
  if (res.data.code === 0) {
    message.success("修改成功");
    editingId.value = null;
    loadData();
  } else {
    message.error("修改失败，" + res.data.message);
  }
};

/**
 * 监听 searchParams 变量，改变时触发数据的重新加载
 */
watchEffect(() => {
  loadData();
});

// 表格列配置
const columns = [
  {
    title: "id",
    dataIndex: "id",
  },
  {
    title: "名称",
    dataIndex: "resultName",
    slotName: "resultName",
  },
  {
    title: "描述",
    dataIndex: "resultDesc",
    slotName: "resultDesc",
  },
  {
    title: "图片",
    dataIndex: "resultPicture",
    slotName: "resultPicture",
  },
  {
    title: "结果属性",
    dataIndex: "resultProp",
    slotName: "resultProp",
  },
  {
    title: "评分范围",
    dataIndex: "resultScoreRange",
    slotName: "resultScoreRange",
  },
  {
    title: "创建时间",
    dataIndex: "createTime",
    slotName: "createTime",
  },
  {
    title: "更新时间",
    dataIndex: "updateTime",
    slotName: "updateTime",
  },
  {
    title: "操作",
    slotName: "optional",
  },
];
</script>
