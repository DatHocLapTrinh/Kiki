package com.kiki.backend.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.kiki.backend.config.FirebaseConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private final FirebaseConfig firebaseConfig;

    public FirebaseAuthenticationFilter(FirebaseConfig firebaseConfig) {
        this.firebaseConfig = firebaseConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7).trim();

            if (!idToken.isEmpty()) {
                try {
                    if (firebaseConfig.isInitialized() && !FirebaseApp.getApps().isEmpty()) {
                        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                        UserPrincipal principal = new UserPrincipal(
                            decodedToken.getUid(),
                            decodedToken.getEmail(),
                            decodedToken.getName()
                        );

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.debug("Authenticated Firebase user: {}", principal.email());
                    } else {
                        // LOCAL DEV FALLBACK: Allows developer testing before putting actual service-account file
                        UserPrincipal devPrincipal = new UserPrincipal(
                            "dev-user-001",
                            "dev@kiki.app",
                            "Kiki Dev Adventurer"
                        );
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            devPrincipal,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.debug("Dev fallback authentication granted for token: {}", idToken.substring(0, Math.min(idToken.length(), 10)));
                    }
                } catch (Exception e) {
                    log.warn("Invalid Firebase ID Token: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Mã xác thực JWT không hợp lệ hoặc đã hết hạn: " + e.getMessage() + "\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
