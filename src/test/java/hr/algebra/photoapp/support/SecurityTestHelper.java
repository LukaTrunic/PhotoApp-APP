package hr.algebra.photoapp.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class SecurityTestHelper {

    private SecurityTestHelper() {
    }

    public static void loginAs(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clearLogin() {
        SecurityContextHolder.clearContext();
    }
}
