package uz.cs2arena.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uz.cs2arena.user.Role;

import java.util.List;

/** Rol ierarxiyasi: SUPER_ADMIN ⊃ ADMIN ⊃ PLAYER. */
public final class AuthorityMapper {

    private AuthorityMapper() {}

    public static List<GrantedAuthority> authorities(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_PLAYER"));
            case ADMIN -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_PLAYER"));
            case PLAYER -> List.of(new SimpleGrantedAuthority("ROLE_PLAYER"));
        };
    }
}
