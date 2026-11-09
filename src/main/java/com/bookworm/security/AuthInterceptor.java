package com.bookworm.security;

import com.bookworm.exception.ApiException;
import com.bookworm.model.User;
import com.bookworm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Resolves "Authorization: Bearer &lt;token&gt;" into request attributes "user" and "token".
 * Guests (no/invalid token) pass through unless the endpoint is annotated with @RequireAuth / @RequireAdmin.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String header = req.getHeader("Authorization");
        User user = null;
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            user = authService.resolve(token);
            if (user != null) {
                req.setAttribute("user", user);
                req.setAttribute("token", token);
            }
        }
        if (handler instanceof HandlerMethod hm) {
            boolean admin = has(hm, RequireAdmin.class);
            boolean auth = admin || has(hm, RequireAuth.class);
            if (auth && user == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            if (admin && !"ADMIN".equals(user.getRole())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Admin role required");
            }
        }
        return true;
    }

    private boolean has(HandlerMethod hm, Class<? extends java.lang.annotation.Annotation> a) {
        return AnnotatedElementUtils.hasAnnotation(hm.getMethod(), a)
                || AnnotatedElementUtils.hasAnnotation(hm.getBeanType(), a);
    }
}
