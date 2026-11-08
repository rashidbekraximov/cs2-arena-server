package uz.cs2arena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import uz.cs2arena.auth.UserRefreshFilter;
import uz.cs2arena.user.UserRepository;

/**
 * Rollar:
 *  - PLAYER       — o'yin, lobby, balans, premium
 *  - ADMIN        — /api/admin/**   (ban, yangiliklar, matchlarni bekor qilish, to'lovlarni ko'rish)
 *  - SUPER_ADMIN  — /api/admin/super/** (rollar, serverlar, balansni qo'lda o'zgartirish)
 *
 * CSRF: sessiya cookie SameSite=Lax — boshqa saytlardan POST so'rovlarda cookie yuborilmaydi.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contextRepository,
                                           UserRepository users) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .securityContext(c -> c.securityContextRepository(contextRepository))
                .addFilterAfter(new UserRefreshFilter(users), SecurityContextHolderFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(a -> a
                        // Tashqi tizimlar (o'z kalitlari bilan tekshiriladi)
                        .requestMatchers("/api/payments/payme", "/api/payments/click/**", "/api/matchzy/**").permitAll()
                        // Rollar
                        .requestMatchers("/api/admin/super/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Ochiq ma'lumotlar
                        .requestMatchers(HttpMethod.GET, "/api/site", "/api/home", "/api/news/**", "/api/leaderboard",
                                "/api/users/*/profile", "/api/premium/plans", "/api/matches/live", "/api/matches/open", "/api/servers",
                                "/api/me").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // React sahifalari, statik fayllar, Steam login, WebSocket
                        .anyRequest().permitAll());
        return http.build();
    }
}
