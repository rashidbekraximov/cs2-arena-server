package uz.cs2arena.match;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class MatchDtos {

    private MatchDtos() {}

    // ---------- So'rovlar ----------

    public record SettingsRequest(
            @NotNull MatchMode mode,
            /** "VETO" yoki xarita nomi (de_mirage) */
            @NotBlank String map,
            @NotBlank String region,
            @Size(max = 64) String password,
            boolean antiCheat,
            boolean overtime,
            boolean openLobby,
            boolean spectators
    ) {}

    public record JoinRequest(String inviteCode, String password, Team team) {}

    public record TeamRequest(@NotNull Team team) {}

    public record VetoRequest(@NotBlank String map) {}

    public record ChatRequest(@NotNull ChatMessage.Channel channel, @NotBlank @Size(max = 500) String text) {}

    // ---------- Javoblar ----------

    public record PlayerDto(Long userId, String nickname, String avatarUrl, int elo, boolean premium,
                            boolean host, boolean captain,
                            int kills, int deaths, int assists, int headshots, int damage, Integer eloChange) {}

    public record TeamDto(String name, List<PlayerDto> players, int avgElo, Integer score) {}

    public record VetoMapDto(String map, String state, Team team) {}

    public record VetoDto(Team turn, Instant deadline, List<VetoMapDto> maps, boolean canAct) {}

    public record ServerDto(String name, String region, String address, String connectCommand, String steamUrl) {}

    public record ViewerDto(boolean inMatch, Team team, boolean host, boolean captain) {}

    public record MatchView(
            Long id, MatchStatus status, MatchMode mode, String modeLabel, int teamSize,
            String map, String region, boolean antiCheat, boolean overtime, boolean openLobby,
            boolean spectators, boolean hasPassword,
            /** Faqat match ishtirokchilariga ko'rinadi */
            String inviteCode,
            Long hostId, TeamDto teamA, TeamDto teamB, VetoDto veto, ServerDto server,
            Team winner, ViewerDto viewer, Instant createdAt, Instant startedAt, Instant finishedAt
    ) {}

    /** Ro'yxatlar uchun qisqa ko'rinish (jonli matchlar, ochiq lobbilar). */
    public record MatchCard(Long id, MatchStatus status, String modeLabel, String map, String region,
                            String teamAName, String teamBName, Integer scoreA, Integer scoreB,
                            int players, int maxPlayers, boolean hasPassword, Instant createdAt) {}

    public record ChatDto(Long id, Long userId, String nickname, String avatarUrl, String channel, String text,
                          Instant createdAt) {}

    public record RecentMatchDto(Long matchId, String map, String modeLabel, boolean won, Integer myScore,
                                 Integer enemyScore, int kills, int deaths, int assists, double adr,
                                 Integer eloChange, Instant finishedAt) {}
}
