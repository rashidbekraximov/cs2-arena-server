package uz.cs2arena.match;

import jakarta.persistence.*;
import uz.cs2arena.common.ApiException;

import java.time.Instant;

@Entity
@Table(name = "matches")
public class Match {

    /** Xarita oldindan tanlanmagan bo'lsa — veto o'tkaziladi. */
    public static final String MAP_VETO = "VETO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.LOBBY;

    @Enumerated(EnumType.STRING)
    private MatchMode mode;

    private int teamSize;
    private String map;
    private String region;
    private String password;
    private boolean antiCheat;
    private boolean overtime = true;
    private boolean openLobby;
    private boolean spectators = true;
    private String inviteCode;
    private Long hostId;
    private Long serverId;
    @Column(name = "team_a_name")
    private String teamAName = "Team A";

    @Column(name = "team_b_name")
    private String teamBName = "Team B";

    @Column(name = "score_a")
    private Integer scoreA;

    @Column(name = "score_b")
    private Integer scoreB;

    @Enumerated(EnumType.STRING)
    private Team winner;

    @Enumerated(EnumType.STRING)
    private Team vetoTurn;

    private Instant vetoDeadline;
    private Instant createdAt = Instant.now();
    private Instant startedAt;
    private Instant finishedAt;

    protected Match() {}

    public Match(Long hostId, String inviteCode) {
        this.hostId = hostId;
        this.inviteCode = inviteCode;
    }

    // ---------- Holat o'tishlari ----------

    public void requireStatus(MatchStatus expected, String message) {
        if (status != expected) throw ApiException.conflict("MATCH_STATE", message);
    }

    public void applySettings(MatchMode mode, String map, String region, String password, boolean antiCheat,
                              boolean overtime, boolean openLobby, boolean spectators) {
        requireStatus(MatchStatus.LOBBY, "Sozlamalarni faqat lobbi bosqichida o'zgartirish mumkin");
        this.mode = mode;
        this.teamSize = mode.teamSize();
        this.map = map;
        this.region = region;
        this.password = password == null || password.isBlank() ? null : password;
        this.antiCheat = antiCheat;
        this.overtime = overtime;
        this.openLobby = openLobby;
        this.spectators = spectators;
    }

    public boolean needsVeto() {
        return MAP_VETO.equals(map);
    }

    public void startVeto(String teamAName, String teamBName, Instant deadline) {
        requireStatus(MatchStatus.LOBBY, "Match allaqachon boshlangan");
        this.teamAName = teamAName;
        this.teamBName = teamBName;
        this.status = MatchStatus.VETO;
        this.vetoTurn = Team.A;
        this.vetoDeadline = deadline;
        this.startedAt = Instant.now();
    }

    public void nextVetoTurn(Instant deadline) {
        this.vetoTurn = vetoTurn.opposite();
        this.vetoDeadline = deadline;
    }

    /** Xarita aniq bo'ldi — server kutilmoqda. */
    public void mapDecided(String map, String teamAName, String teamBName) {
        this.map = map;
        if (teamAName != null) this.teamAName = teamAName;
        if (teamBName != null) this.teamBName = teamBName;
        if (startedAt == null) startedAt = Instant.now();
        this.vetoTurn = null;
        this.vetoDeadline = null;
        this.status = MatchStatus.WAITING_SERVER;
    }

    public void goLive(Long serverId) {
        this.serverId = serverId;
        this.status = MatchStatus.LIVE;
        this.scoreA = 0;
        this.scoreB = 0;
    }

    public void updateScore(int a, int b) {
        this.scoreA = a;
        this.scoreB = b;
    }

    public void finish(int a, int b) {
        this.scoreA = a;
        this.scoreB = b;
        this.winner = a > b ? Team.A : b > a ? Team.B : null;
        this.status = MatchStatus.FINISHED;
        this.finishedAt = Instant.now();
    }

    public void cancel() {
        if (status == MatchStatus.FINISHED || status == MatchStatus.CANCELLED) {
            throw ApiException.conflict("MATCH_STATE", "Match allaqachon yakunlangan");
        }
        this.status = MatchStatus.CANCELLED;
        this.finishedAt = Instant.now();
        this.vetoTurn = null;
        this.vetoDeadline = null;
    }

    public void transferHost(Long newHostId) {
        this.hostId = newHostId;
    }

    public String teamName(Team t) {
        return t == Team.A ? teamAName : teamBName;
    }

    // ---------- Getterlar ----------

    public Long getId() { return id; }
    public MatchStatus getStatus() { return status; }
    public MatchMode getMode() { return mode; }
    public int getTeamSize() { return teamSize; }
    public String getMap() { return map; }
    public String getRegion() { return region; }
    public String getPassword() { return password; }
    public boolean isAntiCheat() { return antiCheat; }
    public boolean isOvertime() { return overtime; }
    public boolean isOpenLobby() { return openLobby; }
    public boolean isSpectators() { return spectators; }
    public String getInviteCode() { return inviteCode; }
    public Long getHostId() { return hostId; }
    public Long getServerId() { return serverId; }
    public String getTeamAName() { return teamAName; }
    public String getTeamBName() { return teamBName; }
    public Integer getScoreA() { return scoreA; }
    public Integer getScoreB() { return scoreB; }
    public Team getWinner() { return winner; }
    public Team getVetoTurn() { return vetoTurn; }
    public Instant getVetoDeadline() { return vetoDeadline; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
