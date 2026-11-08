package uz.cs2arena.user;

public enum Role {
    PLAYER,
    ADMIN,
    SUPER_ADMIN;

    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
