package com.rd.backend.controller;

import com.rd.backend.common.BaseResponse;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.common.ResultUtils;
import com.rd.backend.exception.BusinessException;
import com.rd.backend.model.dto.user.UserLoginRequest;
import com.rd.backend.model.dto.user.UserRegisterRequest;
import com.rd.backend.model.vo.LoginTokenVO;
import com.rd.backend.model.vo.LoginUserVO;
import com.rd.backend.security.JwtUtil;
import com.rd.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/register")
    public BaseResponse<LoginTokenVO> register(@RequestBody UserRegisterRequest req) {

        // 0) 基本参数校验
        if (req == null || StringUtils.isAnyBlank(
                req.getUserAccount(), req.getUserPassword(), req.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 1) 调用现有 Service 完成注册（内部已做密码加密）
        userService.userRegister(
                req.getUserAccount(), req.getUserPassword(), req.getCheckPassword());

        // 2) 复用登录流程 → 颁发 token
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUserAccount(), req.getUserPassword()));

        String token = jwtUtil.generateToken(req.getUserAccount());
        LoginUserVO userInfo = userService.getLoginUserVO(req.getUserAccount());

        return ResultUtils.success(new LoginTokenVO(token, userInfo));
    }

    /**
     * JWT 登录（推荐前端以后都用这个接口）
     */
    @PostMapping("/login")
    public BaseResponse<LoginTokenVO> login(@RequestBody UserLoginRequest req) {

        // 1. 交给 Spring Security 做账号密码校验
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUserAccount(), req.getUserPassword()));

        // 2. 生成 JWT
        String token = jwtUtil.generateToken(req.getUserAccount());

        // 3. 取用户信息（沿用你已有的 LoginUserVO 转换逻辑）
        LoginUserVO userInfo = userService.getLoginUserVO(req.getUserAccount());

        // 4. 返回 token + 用户信息
        return ResultUtils.success(new LoginTokenVO(token, userInfo));
    }


}
