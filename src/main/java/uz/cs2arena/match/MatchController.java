package uz.cs2arena.match;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.config.AppProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchLobbyService lobby;
    private final MatchVetoService veto;
    private final MatchQueryService query;
    private final AppProperties props;

    public MatchController(MatchLobbyService lobby, MatchVetoService veto, MatchQueryService query,
                           AppProperties props) {
        this.lobby = lobby;
        this.veto = veto;
        this.query = query;
        this.props = props;
    }

    // ---------- O'qish ----------

    @GetMapping("/live")
    public List<MatchDtos.MatchCard> live() {
        return query.live();
    }

    @GetMapping("/open")
    public List<MatchDtos.MatchCard> open() {
        return query.openLobbies();
    }

    /** Xaritalar puli va rejimlar (sozlamalar oynasi uchun). */
    @GetMapping("/options")
    public Map<String, Object> options() {
        return Map.of(
                "maps", props.mapPool(),
                "modes", List.of(MatchMode.values()),
                "regions", List.of("Toshkent"));
    }

    @GetMapping("/current")
    public Map<String, Object> current(@AuthenticationPrincipal AuthUser me) {
        Map<String, Object> r = new HashMap<>();
        r.put("matchId", query.currentMatchId(me.id()).orElse(null));
        return r;
    }

    @GetMapping("/invite/{code}")
    public Map<String, Long> resolveInvite(@PathVariable String code) {
        return Map.of("matchId", lobby.resolveInvite(code));
    }

    @GetMapping("/{id}")
    public MatchDtos.MatchView get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return query.view(id, me.id());
    }

    @GetMapping("/{id}/chat")
    public List<MatchDtos.ChatDto> chat(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return query.chat(id, me.id());
    }

    // ---------- Lobbi amallari ----------

    @PostMapping
    public Map<String, Long> create(@AuthenticationPrincipal AuthUser me,
                                    @Valid @RequestBody MatchDtos.SettingsRequest req) {
        return Map.of("matchId", lobby.create(me.id(), req));
    }

    @PostMapping("/{id}/join")
    public MatchDtos.MatchView join(@PathVariable Long id, @AuthenticationPrincipal AuthUser me,
                                    @RequestBody MatchDtos.JoinRequest req) {
        lobby.join(me.id(), id, req);
        return query.view(id, me.id());
    }

    @PostMapping("/{id}/team")
    public MatchDtos.MatchView switchTeam(@PathVariable Long id, @AuthenticationPrincipal AuthUser me,
                                          @Valid @RequestBody MatchDtos.TeamRequest req) {
        lobby.switchTeam(me.id(), id, req.team());
        return query.view(id, me.id());
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        lobby.leave(me.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/kick/{userId}")
    public MatchDtos.MatchView kick(@PathVariable Long id, @PathVariable Long userId,
                                    @AuthenticationPrincipal AuthUser me) {
        lobby.kick(me.id(), id, userId);
        return query.view(id, me.id());
    }

    @PutMapping("/{id}/settings")
    public MatchDtos.MatchView settings(@PathVariable Long id, @AuthenticationPrincipal AuthUser me,
                                        @Valid @RequestBody MatchDtos.SettingsRequest req) {
        lobby.updateSettings(me.id(), id, req);
        return query.view(id, me.id());
    }

    @PostMapping("/{id}/start")
    public MatchDtos.MatchView start(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        lobby.start(me.id(), id);
        return query.view(id, me.id());
    }

    @PostMapping("/{id}/veto")
    public MatchDtos.MatchView ban(@PathVariable Long id, @AuthenticationPrincipal AuthUser me,
                                   @Valid @RequestBody MatchDtos.VetoRequest req) {
        veto.ban(me.id(), id, req.map());
        return query.view(id, me.id());
    }

    @PostMapping("/{id}/chat")
    public MatchDtos.ChatDto postChat(@PathVariable Long id, @AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody MatchDtos.ChatRequest req) {
        return lobby.postChat(me.id(), id, req);
    }
}
