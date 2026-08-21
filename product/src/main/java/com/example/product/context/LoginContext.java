package com.example.product.context;

import com.example.product.common.BizException;
import com.example.product.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 登录上下文（L0 模拟实现）。
 *
 * <p>真实环境中应从 SSO / JWT / Session 中取得当前登录用户 ID 和是否管理员。
 * L0 通过请求头 {@code X-User-Id}（创建人）和 {@code X-Admin}（管理员标识）模拟。</p>
 *
 * <p>对外暴露两个能力：</p>
 * <ul>
 *   <li>{@link #currentCreatorId()}：从登录态取得 creatorId，用于发布接口</li>
 *   <li>{@link #isAdmin()}：判断当前请求是否管理员，用于处罚操作和跨创建人列表</li>
 * </ul>
 */
@Component
public class LoginContext {

    /** 请求头：当前登录用户 ID */
    public static final String HEADER_USER_ID = "X-User-Id";
    /** 请求头：管理员标识（"true" 时为管理员） */
    public static final String HEADER_ADMIN = "X-Admin";

    /**
     * 取得当前登录创建人 ID。
     *
     * <p>找不到登录态时抛 {@link BizException}，避免下游出现 NPE。</p>
     */
    public Long currentCreatorId() {
        HttpServletRequest request = currentRequest();
        String userId = request.getHeader(HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED, "缺少登录上下文：" + HEADER_USER_ID);
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED, HEADER_USER_ID + " 必须为数字");
        }
    }

    /**
     * 判断当前请求是否为管理员。
     */
    public boolean isAdmin() {
        HttpServletRequest request = currentRequest();
        String admin = request.getHeader(HEADER_ADMIN);
        return "true".equalsIgnoreCase(admin);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "无法获取请求上下文");
        }
        return sra.getRequest();
    }
}
