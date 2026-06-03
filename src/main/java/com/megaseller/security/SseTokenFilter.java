package com.megaseller.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * EventSource (SSE) cannot set Authorization headers.
 * This filter promotes a ?token=... query parameter to the Authorization header
 * so SSE endpoints are protected by the same JWT filter chain.
 */
@Component
public class SseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getParameter("token");
        if (token != null && !token.isBlank()
                && request.getHeader("Authorization") == null) {
            filterChain.doFilter(new TokenWrappedRequest(request, "Bearer " + token), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    // Wrapper to inject the Authorization header
    private static class TokenWrappedRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final String authHeader;

        TokenWrappedRequest(HttpServletRequest request, String authHeader) {
            super(request);
            this.authHeader = authHeader;
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) return authHeader;
            return super.getHeader(name);
        }
    }
}
