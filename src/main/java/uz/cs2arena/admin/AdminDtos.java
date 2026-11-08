package uz.cs2arena.admin;

import jakarta.validation.constraints.*;
import uz.cs2arena.payment.Payment;
import uz.cs2arena.server.GameServer;
import uz.cs2arena.user.Role;
import uz.cs2arena.user.User;

import java.time.Instant;

public final class AdminDtos {

    private AdminDtos() {}

    public record AdminUserDto(Long id, String steamId, String nickname, String avatarUrl, String role, int elo,
                               int matches, long balance, boolean premium, boolean banned, String banReason,
                               Instant bannedUntil, Instant createdAt) {
        public static AdminUserDto of(User u, long balance) {
            return new AdminUserDto(u.getId(), u.getSteamId(), u.getNickname(), u.getAvatarUrl(), u.getRole().name(),
                    u.getElo(), u.getMatchesPlayed(), balance, u.isPremiumActive(), u.isBanned(), u.getBanReason(),
                    u.getBannedUntil(), u.getCreatedAt());
        }
    }

    /** days = null — muddatsiz ban */
    public record BanRequest(@NotBlank @Size(max = 255) String reason, @Positive Integer days) {}

    public record RoleRequest(@NotNull Role role) {}

    public record BalanceRequest(long amount, @NotBlank @Size(max = 200) String note) {}

    public record PaymentDto(Long id, Long userId, String provider, long amount, String status,
                             String providerTransId, Instant createdAt) {
        public static PaymentDto of(Payment p) {
            return new PaymentDto(p.getId(), p.getUserId(), p.getProvider().name(), p.getAmount(), p.getStatus().name(),
                    p.getProviderTransId(), p.getCreatedAt());
        }
    }

    public record ServerDto(Long id, String name, String region, String ip, int port, boolean enabled) {
        public static ServerDto of(GameServer s) {
            return new ServerDto(s.getId(), s.getName(), s.getRegion(), s.getIp(), s.getPort(), s.isEnabled());
        }
    }

    public record ServerRequest(@NotBlank @Size(max = 64) String name,
                                @NotBlank @Size(max = 32) String region,
                                @NotBlank @Size(max = 64) String ip,
                                @Min(1) @Max(65535) int port,
                                /** Bo'sh qoldirilsa — eskisi saqlanadi */
                                @Size(max = 128) String rconPassword,
                                boolean enabled) {}

    public record ActionDto(Long id, Long adminId, String action, String target, String details, Instant createdAt) {
        public static ActionDto of(AdminAction a) {
            return new ActionDto(a.getId(), a.getAdminId(), a.getAction(), a.getTarget(), a.getDetails(),
                    a.getCreatedAt());
        }
    }

    public record CancelRequest(@Size(max = 200) String reason) {}
}
