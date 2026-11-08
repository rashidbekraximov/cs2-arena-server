package uz.cs2arena.match;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "match_players")
public class MatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long matchId;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Team team;

    private int kills;
    private int deaths;
    private int assists;
    private int headshots;
    private int damage;
    private Integer eloBefore;
    private Integer eloChange;
    private Instant joinedAt = Instant.now();

    protected MatchPlayer() {}

    public MatchPlayer(Long matchId, Long userId, Team team) {
        this.matchId = matchId;
        this.userId = userId;
        this.team = team;
    }

    public void moveTo(Team team) {
        this.team = team;
        this.joinedAt = Instant.now(); // yangi jamoada oxirgi bo'ladi (kapitanlik uchun)
    }

    public void recordStats(int kills, int deaths, int assists, int headshots, int damage) {
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.headshots = headshots;
        this.damage = damage;
    }

    public void recordElo(int before, int change) {
        this.eloBefore = before;
        this.eloChange = change;
    }

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public Long getUserId() { return userId; }
    public Team getTeam() { return team; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getAssists() { return assists; }
    public int getHeadshots() { return headshots; }
    public int getDamage() { return damage; }
    public Integer getEloBefore() { return eloBefore; }
    public Integer getEloChange() { return eloChange; }
    public Instant getJoinedAt() { return joinedAt; }
}
