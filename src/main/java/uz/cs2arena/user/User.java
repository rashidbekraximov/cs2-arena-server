package uz.cs2arena.user;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String steamId;

    @Column(nullable = false)
    private String nickname;

    private String avatarUrl;
    private String country = "O'zbekiston";
    private String city;
    private String language = "uz";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.PLAYER;

    private int elo = 1000;
    private int matchesPlayed;
    private int wins;
    private int kills;
    private int deaths;
    private int assists;
    private int headshots;
    private long damage;
    private int roundsPlayed;

    private String premiumPlan;
    private Instant premiumUntil;

    private boolean banned;
    private String banReason;
    private Instant bannedUntil;

    private boolean notifyMatchSound = true;
    private boolean notifyFriendRequests = true;
    private boolean notifyTournaments = true;
    private boolean notifyEmail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    public User(String steamId, String nickname) {
        this.steamId = steamId;
        this.nickname = nickname;
    }

    // ---------- Biznes metodlar ----------

    /** Ban hozir amalda bo'lsa true. Muddati o'tgan ban avtomatik yechiladi. */
    public boolean isBannedNow() {
        if (!banned) return false;
        if (bannedUntil != null && bannedUntil.isBefore(Instant.now())) {
            unban();
            return false;
        }
        return true;
    }

    public void ban(String reason, Instant until) {
        this.banned = true;
        this.banReason = reason;
        this.bannedUntil = until;
    }

    public void unban() {
        this.banned = false;
        this.banReason = null;
        this.bannedUntil = null;
    }

    public boolean isPremiumActive() {
        return premiumUntil != null && premiumUntil.isAfter(Instant.now());
    }

    public void applyMatchResult(boolean won, int kills, int deaths, int assists, int headshots, int damage,
                                 int rounds, int eloChange) {
        this.matchesPlayed++;
        if (won) this.wins++;
        this.kills += kills;
        this.deaths += deaths;
        this.assists += assists;
        this.headshots += headshots;
        this.damage += damage;
        this.roundsPlayed += rounds;
        this.elo = Math.max(0, this.elo + eloChange);
    }

    // ---------- Getter / setter ----------

    public Long getId() { return id; }
    public String getSteamId() { return steamId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public int getElo() { return elo; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public int getWins() { return wins; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getAssists() { return assists; }
    public int getHeadshots() { return headshots; }
    public long getDamage() { return damage; }
    public int getRoundsPlayed() { return roundsPlayed; }
    public String getPremiumPlan() { return premiumPlan; }
    public void setPremiumPlan(String premiumPlan) { this.premiumPlan = premiumPlan; }
    public Instant getPremiumUntil() { return premiumUntil; }
    public void setPremiumUntil(Instant premiumUntil) { this.premiumUntil = premiumUntil; }
    public boolean isBanned() { return banned; }
    public String getBanReason() { return banReason; }
    public Instant getBannedUntil() { return bannedUntil; }
    public boolean isNotifyMatchSound() { return notifyMatchSound; }
    public void setNotifyMatchSound(boolean v) { this.notifyMatchSound = v; }
    public boolean isNotifyFriendRequests() { return notifyFriendRequests; }
    public void setNotifyFriendRequests(boolean v) { this.notifyFriendRequests = v; }
    public boolean isNotifyTournaments() { return notifyTournaments; }
    public void setNotifyTournaments(boolean v) { this.notifyTournaments = v; }
    public boolean isNotifyEmail() { return notifyEmail; }
    public void setNotifyEmail(boolean v) { this.notifyEmail = v; }
    public Instant getCreatedAt() { return createdAt; }
}
