package uz.cs2arena.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserRepository;

import java.io.IOException;
import java.util.Optional;

/**
 * Har so'rovda foydalanuvchini bazadan yangilaydi:
 * ban berilgan bo'lsa — sessiya yopiladi, rol o'zgargan bo'lsa — darhol kuchga kiradi.
 * Bean emas (SecurityConfig ichida yaratiladi), aks holda servlet filter sifatida ikki marta ishlaydi.
 */
public class UserRefreshFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public UserRefreshFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser current) {
            Optional<User> fresh = users.findById(current.id());
            if (fresh.isEmpty() || fresh.get().isBannedNow()) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
            } else if (fresh.get().getRole() != current.role()) {
                User u = fresh.get();
                AuthUser updated = new AuthUser(u.getId(), u.getSteamId(), u.getRole());
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                        updated, null, AuthorityMapper.authorities(u.getRole())));
                SecurityContextHolder.setContext(ctx);
                HttpSession session = request.getSession(false);
                if (session != null) session.setAttribute("SPRING_SECURITY_CONTEXT", ctx);
            }
        }
        chain.doFilter(request, response);
    }
}
