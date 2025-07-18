// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** getTask GET /api/task/${param0} */
export async function getTaskUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTaskUsingGET1Params,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseQuestionTask_>(`/api/task/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}
