package com.rd.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rd.backend.common.BaseResponse;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("认证失败: {}", authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        BaseResponse<?> body = ResultUtils.error(ErrorCode.TOKEN_EXPIRED, "未认证或 Token 已过期");
        MAPPER.writeValue(response.getWriter(), body);
    }
}
