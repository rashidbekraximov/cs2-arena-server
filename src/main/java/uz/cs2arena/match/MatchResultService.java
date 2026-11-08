package uz.cs2arena.match;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MatchZy hodisalarini qayta ishlaydi (Get5 formatidagi JSON):
 *   round_end  — joriy hisobni yangilash
 *   map_result — match yakuni: hisob, statistika, ELO
 */
@Service
public class MatchResultService {

    private static final Logger log = LoggerFactory.getLogger(MatchResultService.class);

    private final MatchRepository matches;
    private final MatchPlayerRepository players;
    private final UserRepository users;
    private final MatchLobbyService lobby;

    public MatchResultService(MatchRepository matches, MatchPlayerRepository players, UserRepository users,
                              MatchLobbyService lobby) {
        this.matches = matches;
        this.players = players;
        this.users = users;
        this.lobby = lobby;
    }

    @Transactional
    public void handle(JsonNode event) {
        String type = event.path("event").asText();
        Long matchId = parseId(event.path("matchid"));
        if (matchId == null) return;

        switch (type) {
            case "round_end" -> onRoundEnd(matchId, event);
            case "map_result" -> onMapResult(matchId, event);
            default -> log.debug("MatchZy hodisasi e'tiborsiz qoldirildi: {}", type);
        }
    }

    private void onRoundEnd(Long matchId, JsonNode e) {
        Match m = matches.lockById(matchId).orElse(null);
        if (m == null || m.getStatus() != MatchStatus.LIVE) return;
        m.updateScore(e.path("team1").path("score").asInt(), e.path("team2").path("score").asInt());
        lobby.changed(matchId);
    }

    private void onMapResult(Long matchId, JsonNode e) {
        Match m = matches.lockById(matchId).orElse(null);
        if (m == null || m.getStatus() != MatchStatus.LIVE) return;

        int scoreA = e.path("team1").path("score").asInt();
        int scoreB = e.path("team2").path("score").asInt();
        m.finish(scoreA, scoreB);

        List<MatchPlayer> list = players.findByMatchIdOrderByJoinedAtAsc(matchId);
        Map<Long, User> userMap = users.findAllById(list.stream().map(MatchPlayer::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        // Statistika: steamid → stats
        Map<String, JsonNode> stats = new HashMap<>();
        for (String team : List.of("team1", "team2")) {
            for (JsonNode p : e.path(team).path("players")) {
                stats.put(p.path("steamid").asText(), p.path("stats"));
            }
        }

        double avgA = avgElo(list, userMap, Team.A);
        double avgB = avgElo(list, userMap, Team.B);
        double resultA = scoreA > scoreB ? 1 : scoreA < scoreB ? 0 : 0.5;
        int deltaA = EloCalculator.deltaForA(avgA, avgB, resultA);
        int rounds = scoreA + scoreB;

        for (MatchPlayer p : list) {
            User u = userMap.get(p.getUserId());
            JsonNode s = stats.get(u.getSteamId());
            if (s != null) {
                p.recordStats(s.path("kills").asInt(), s.path("deaths").asInt(), s.path("assists").asInt(),
                        s.path("headshot_kills").asInt(), s.path("damage").asInt());
            }
            int delta = p.getTeam() == Team.A ? deltaA : -deltaA;
            p.recordElo(u.getElo(), delta);
            u.applyMatchResult(m.getWinner() == p.getTeam(), p.getKills(), p.getDeaths(), p.getAssists(),
                    p.getHeadshots(), p.getDamage(), rounds, delta);
        }

        lobby.system(matchId, "Match yakunlandi: " + m.getTeamAName() + " " + scoreA + " : " + scoreB + " "
                + m.getTeamBName());
        lobby.changed(matchId);
    }

    private static double avgElo(List<MatchPlayer> list, Map<Long, User> users, Team team) {
        return list.stream().filter(p -> p.getTeam() == team)
                .mapToInt(p -> users.get(p.getUserId()).getElo()).average().orElse(1000);
    }

    private static Long parseId(JsonNode n) {
        try {
            return n.isNumber() ? n.asLong() : Long.parseLong(n.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
