// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** addQuestion POST /api/question/add */
export async function addQuestionUsingPost(
  body: API.QuestionAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/question/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** aiGenerateQuestion POST /api/question/ai_generate */
export async function aiGenerateQuestionUsingPost(
  body: API.AiGenerateQuestionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListQuestionContentDTO_>(
    "/api/question/ai_generate",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** aiGenerateQuestionAsyncMq POST /api/question/ai_generate/async/mq */
export async function aiGenerateQuestionAsyncMqUsingPost(
  body: API.AiGenerateQuestionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseTaskIdVO_>(
    "/api/question/ai_generate/async/mq",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** aiGenerateQuestionSSE GET /api/question/ai_generate/sse */
export async function aiGenerateQuestionSseUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.aiGenerateQuestionSSEUsingGET1Params,
  options?: { [key: string]: any }
) {
  return request<API.SseEmitter>("/api/question/ai_generate/sse", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** aiGenerateQuestionSSETest GET /api/question/ai_generate/sse/test */
export async function aiGenerateQuestionSseTestUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.aiGenerateQuestionSSETestUsingGET1Params,
  options?: { [key: string]: any }
) {
  return request<API.SseEmitter>("/api/question/ai_generate/sse/test", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** deleteQuestion POST /api/question/delete */
export async function deleteQuestionUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/question/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** editQuestion POST /api/question/edit */
export async function editQuestionUsingPost(
  body: API.QuestionEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/question/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getQuestionVOById GET /api/question/get/vo */
export async function getQuestionVoByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getQuestionVOByIdUsingGET1Params,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseQuestionVO_>("/api/question/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** listByIds POST /api/question/list/ids */
export async function listByIdsUsingPost(
  body: number[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListQuestionVO_>("/api/question/list/ids", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** listQuestionByPage POST /api/question/list/page */
export async function listQuestionByPageUsingPost(
  body: API.QuestionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestion_>("/api/question/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** listQuestionVOByPage POST /api/question/list/page/vo */
export async function listQuestionVoByPageUsingPost(
  body: API.QuestionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestionVO_>(
    "/api/question/list/page/vo",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** listMyQuestionVOByPage POST /api/question/my/list/page/vo */
export async function listMyQuestionVoByPageUsingPost(
  body: API.QuestionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestionVO_>(
    "/api/question/my/list/page/vo",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** updateQuestion POST /api/question/update */
export async function updateQuestionUsingPost(
  body: API.QuestionUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/question/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
