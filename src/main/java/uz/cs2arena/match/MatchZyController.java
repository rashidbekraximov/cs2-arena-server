package uz.cs2arena.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** MatchZy plugin bilan aloqa. Ikkala endpoint ham maxfiy kalit bilan himoyalangan. */
@RestController
@RequestMapping("/api/matchzy")
public class MatchZyController {

    private final MatchRepository matches;
    private final MatchPlayerRepository players;
    private final UserRepository users;
    private final MatchResultService results;
    private final AppProperties props;
    private final ObjectMapper json;

    public MatchZyController(MatchRepository matches, MatchPlayerRepository players, UserRepository users,
                             MatchResultService results, AppProperties props, ObjectMapper json) {
        this.matches = matches;
        this.players = players;
        this.users = users;
        this.results = results;
        this.props = props;
        this.json = json;
    }

    /** matchzy_loadmatch_url shu manzildan o'yin sozlamasini yuklaydi. */
    @GetMapping("/{matchId}/config")
    @Transactional(readOnly = true)
    public Map<String, Object> config(@PathVariable Long matchId, @RequestParam String key) {
        checkKey(key);
        Match m = matches.findById(matchId).orElseThrow(() -> ApiException.notFound("Match topilmadi"));
        List<MatchPlayer> list = players.findByMatchIdOrderByJoinedAtAsc(matchId);
        Map<Long, User> userMap = users.findAllById(list.stream().map(MatchPlayer::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("matchid", m.getId());
        cfg.put("num_maps", 1);
        cfg.put("maplist", List.of(m.getMap()));
        cfg.put("skip_veto", true);
        cfg.put("side_type", "knife");
        cfg.put("players_per_team", m.getTeamSize());
        cfg.put("min_players_to_ready", m.getTeamSize());
        cfg.put("wingman", m.getMode() == MatchMode.WINGMAN);
        cfg.put("team1", team(m.getTeamAName(), list, userMap, Team.A));
        cfg.put("team2", team(m.getTeamBName(), list, userMap, Team.B));
        cfg.put("cvars", Map.of(
                "mp_overtime_enable", m.isOvertime() ? "1" : "0",
                "tv_enable", m.isSpectators() ? "1" : "0"));
        return cfg;
    }

    /** matchzy_remote_log_url — MatchZy hodisalarni shu yerga yuboradi. */
    @PostMapping("/events")
    public ResponseEntity<Void> events(@RequestHeader(value = "X-Arena-Key", required = false) String key,
                                       @RequestBody String body) throws Exception {
        checkKey(key);
        JsonNode event = json.readTree(body);
        results.handle(event);
        return ResponseEntity.ok().build();
    }

    private static Map<String, Object> team(String name, List<MatchPlayer> list, Map<Long, User> users, Team t) {
        Map<String, String> ps = new LinkedHashMap<>();
        list.stream().filter(p -> p.getTeam() == t).forEach(p -> {
            User u = users.get(p.getUserId());
            ps.put(u.getSteamId(), u.getNickname());
        });
        Map<String, Object> team = new LinkedHashMap<>();
        team.put("name", name);
        team.put("players", ps);
        return team;
    }

    private void checkKey(String key) {
        String secret = props.matchzy().secret();
        if (key == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.forbidden("Kalit noto'g'ri");
        }
    }
}
