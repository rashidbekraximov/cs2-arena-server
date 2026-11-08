package uz.cs2arena.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    private final SteamClient steam;
    private final UserService users;
    private final SecurityContextRepository contextRepository;

    public AuthController(SteamClient steam, UserService users, SecurityContextRepository contextRepository) {
        this.steam = steam;
        this.users = users;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/auth/steam")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(steam.loginRedirectUrl());
    }

    @GetMapping("/auth/steam/return")
    public void loginReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("openid.") && v.length > 0) params.put(k, v[0]);
        });

        Optional<String> steamId = steam.verify(params);
        if (steamId.isEmpty()) {
            response.sendRedirect("/?login=error");
            return;
        }

        User user = users.loginWithSteam(steamId.get());
        if (user.isBannedNow()) {
            response.sendRedirect("/?login=banned");
            return;
        }

        request.getSession(true);
        request.changeSessionId(); // session fixation'dan himoya
        signIn(user, request, response);
        response.sendRedirect("/");
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        AuthUser principal = new AuthUser(user.getId(), user.getSteamId(), user.getRole());
        var auth = new UsernamePasswordAuthenticationToken(principal, null, AuthorityMapper.authorities(user.getRole()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
