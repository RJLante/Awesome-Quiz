import ACCESS_ENUM from "./accessEnum";

/**
 * 权限检查
 */

const checkAccess = (loginUser: any, needAccess = ACCESS_ENUM.NOT_LOGIN) => {
  // 获取当前登录用户具有的权限（如果没有 loginUser，则表示未登录）
  const loginUserAccess = loginUser?.userRole ?? ACCESS_ENUM.NOT_LOGIN;

  if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
    return true;
  }

  // 如果用户要登录才能访问
  if (needAccess === ACCESS_ENUM.USER) {
    // 如果当前用户是未登录状态，那么表示无权限
    if (loginUserAccess === ACCESS_ENUM.USER) {
      return false;
    }
  }

  // 如果管理员才能访问
  if (needAccess === ACCESS_ENUM.ADMIN) {
    // 如果不是管理员，表示无权限
    if (loginUserAccess !== ACCESS_ENUM.ADMIN) {
      return false;
    }
  }
  return true;
};

export default checkAccess;
