package com.smartbank.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Get Authorization header
        String header = request.getHeader("Authorization");

        // Check whether a Bearer token exists
        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            try {

                // Get email from JWT
                String email = jwtService.extractEmail(token);

                // Make sure user is not already authenticated
                if (SecurityContextHolder
                        .getContext()
                        .getAuthentication() == null) {

                    // Load user from database
                    UserDetails user =
                            userDetailsService.loadUserByUsername(email);

                    // Make sure user is active
                    if (user.isEnabled()) {

                        // Create authenticated user
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        user.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        // Store authentication in Spring Security
                        SecurityContextHolder
                                .getContext()
                                .setAuthentication(authentication);
                    }
                }

            } catch (
                    JwtException |
                    IllegalArgumentException |
                    UsernameNotFoundException e) {

                // Invalid JWT.
                // Request remains unauthenticated.
            }
        }

        // Continue request
        filterChain.doFilter(request, response);
    }
}