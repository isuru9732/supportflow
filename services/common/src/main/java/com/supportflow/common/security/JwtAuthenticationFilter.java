package com.supportflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Validates the Bearer JWT on every request and, if valid, sets the
 * authenticated user's ID as the Spring Security principal. Does NOT
 * resolve org context — that's a separate, per-service concern (see
 * OrgContextFilter in services that need it), since not every service
 * requires an org-scoped request (e.g. widget-facing endpoints use API
 * keys, not JWTs, per Doc 05).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtVerifier jwtVerifier;

    public JwtAuthenticationFilter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtVerifier.verify(token);
                String userId = claims.getSubject();
                var auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // Leave SecurityContext empty — request falls through to
                // Spring Security's normal "authenticated()" rejection below
            }
        }

        chain.doFilter(request, response);
    }
}