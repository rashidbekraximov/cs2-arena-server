package uz.cs2arena.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {}

    /** Ro'yxatlar, lobby va reytingda ko'rinadigan qisqa ma'lumot. */
    public record UserSummary(Long id, String nickname, String avatarUrl, int elo, boolean premium, String role) {
        public static UserSummary of(User u) {
            return new UserSummary(u.getId(), u.getNickname(), u.getAvatarUrl(), u.getElo(),
                    u.isPremiumActive(), u.getRole().name());
        }
    }

    public record Stats(int matches, int wins, double winRate, double kd, double hsPercent, double adr,
                        int kills, int deaths, int assists) {
        public static Stats of(User u) {
            int m = u.getMatchesPlayed();
            double winRate = m == 0 ? 0 : round1(100.0 * u.getWins() / m);
            double kd = u.getDeaths() == 0 ? u.getKills() : round2((double) u.getKills() / u.getDeaths());
            double hs = u.getKills() == 0 ? 0 : round1(100.0 * u.getHeadshots() / u.getKills());
            double adr = u.getRoundsPlayed() == 0 ? 0 : round1((double) u.getDamage() / u.getRoundsPlayed());
            return new Stats(m, u.getWins(), winRate, kd, hs, adr, u.getKills(), u.getDeaths(), u.getAssists());
        }
    }

    /** Joriy foydalanuvchi (/api/me). */
    public record Me(boolean loggedIn, Long id, String steamId, String nickname, String avatarUrl, String role,
                     int elo, long balance, boolean premium, String premiumPlan, Instant premiumUntil,
                     String country, String city, String language,
                     boolean notifyMatchSound, boolean notifyFriendRequests,
                     boolean notifyTournaments, boolean notifyEmail) {

        public static Me anonymous() {
            return new Me(false, null, null, null, null, null, 0, 0, false, null, null,
                    null, null, null, false, false, false, false);
        }

        public static Me of(User u, long balance) {
            return new Me(true, u.getId(), u.getSteamId(), u.getNickname(), u.getAvatarUrl(), u.getRole().name(),
                    u.getElo(), balance, u.isPremiumActive(), u.isPremiumActive() ? u.getPremiumPlan() : null,
                    u.getPremiumUntil(), u.getCountry(), u.getCity(), u.getLanguage(),
                    u.isNotifyMatchSound(), u.isNotifyFriendRequests(), u.isNotifyTournaments(), u.isNotifyEmail());
        }
    }

    public record UpdateSettingsRequest(
            @Size(min = 2, max = 32, message = "2–32 belgi bo'lishi kerak")
            @Pattern(regexp = "^[\\p{L}\\p{N}_.\\- ]+$", message = "faqat harf, raqam, _ . - belgilar")
            String nickname,
            @Size(max = 64) String country,
            @Size(max = 64) String city,
            @Pattern(regexp = "^(uz|ru|en)$") String language,
            Boolean notifyMatchSound,
            Boolean notifyFriendRequests,
            Boolean notifyTournaments,
            Boolean notifyEmail
    ) {}

    static double round1(double v) { return Math.round(v * 10) / 10.0; }
    static double round2(double v) { return Math.round(v * 100) / 100.0; }
}
