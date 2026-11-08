package uz.cs2arena.auth;

import uz.cs2arena.user.Role;

import java.io.Serializable;

/** Sessiyada saqlanadigan kirgan foydalanuvchi (Spring Security principal). */
public record AuthUser(Long id, String steamId, Role role) implements Serializable {

    public boolean isAdmin() {
        return role.isAdmin();
    }

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }
}
