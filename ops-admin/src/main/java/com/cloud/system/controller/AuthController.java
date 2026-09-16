package com.cloud.system.controller;

import cn.hutool.core.util.StrUtil;
import com.cloud.system.common.constant.SecurityConstants;
import com.cloud.system.common.exception.BusinessException;
import com.cloud.system.common.result.Result;
import com.cloud.system.common.util.LoginMaskUtils;
import com.cloud.system.common.util.RequestUtils;
import com.cloud.system.model.dto.CaptchaResult;
import com.cloud.system.model.dto.LoginResult;
import com.cloud.system.security.JwtTokenManager;
import com.cloud.system.security.captcha.EasyCaptchaService;
import com.cloud.system.security.userdetails.SysUserDetails;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenManager jwtTokenManager;
    private final EasyCaptchaService easyCaptchaService;
    private final RedisTemplate redisTemplate;

    @Operation(summary = "登录")
    @PostMapping("/login")
    @McpTool(name = "login", description = "登录")
    public Result<LoginResult> login(
            @Parameter(description = "用户名", example = "admin") @RequestParam String username,
            @Parameter(description = "密码", example = "123456") @RequestParam String password,
            HttpServletRequest request
    ) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                username.toLowerCase().trim(),
                password
        );
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 校验登录掩码(IP白名单)
        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        String clientIp = RequestUtils.getClientIp(request);
        if (!LoginMaskUtils.matches(clientIp, userDetails.getLoginMask())) {
            throw new BusinessException("当前登录IP[" + clientIp + "]不在登录掩码允许范围内，禁止登录");
        }
        log.info("用户[{}]登录成功 登录IP{}", userDetails.getUsername(), clientIp);

        // 生成token
        String accessToken = jwtTokenManager.createToken(authentication);
        LoginResult loginResult = LoginResult.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .build();
        return Result.success(loginResult);
    }

    @Operation(summary = "注销", security = {@SecurityRequirement(name = SecurityConstants.TOKEN_KEY)})
    @DeleteMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = RequestUtils.resolveToken(request);
        if (StrUtil.isNotBlank(token)) {
            Claims claims = jwtTokenManager.getTokenClaims(token);
            String jti = claims.get("jti", String.class);

            Date expiration = claims.getExpiration();
            if (expiration != null) {
                // 有过期时间，在token有效时间内存入黑名单，超出时间移除黑名单节省内存占用
                long ttl = (expiration.getTime() - System.currentTimeMillis());
                redisTemplate.opsForValue().set(SecurityConstants.BLACK_TOKEN_CACHE_PREFIX + jti, null, ttl, TimeUnit.MILLISECONDS);
            } else {
                // 无过期时间，永久加入黑名单
                redisTemplate.opsForValue().set(SecurityConstants.BLACK_TOKEN_CACHE_PREFIX + jti, null);
            }
        }
        SecurityContextHolder.clearContext();
        return Result.success("注销成功");
    }

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result getCaptcha() {
        CaptchaResult captcha = easyCaptchaService.getCaptcha();
        return Result.success(captcha);
    }

}
