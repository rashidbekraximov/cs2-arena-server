package uz.cs2arena.match;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.config.AppProperties;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xarita tanlovi (BO1): jamoalar navbat bilan ban qiladi (A, B, A, B…),
 * oxirgi qolgan xarita o'ynaladi. Har bir navbatga 30 soniya — vaqt tugasa tasodifiy ban.
 */
@Service
public class MatchVetoService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MatchRepository matches;
    private final MatchPlayerRepository players;
    private final MatchVetoRepository vetoes;
    private final MatchLobbyService lobby;
    private final MatchServerService serverService;
    private final AppProperties props;

    public MatchVetoService(MatchRepository matches, MatchPlayerRepository players, MatchVetoRepository vetoes,
                            MatchLobbyService lobby, MatchServerService serverService, AppProperties props) {
        this.matches = matches;
        this.players = players;
        this.vetoes = vetoes;
        this.lobby = lobby;
        this.serverService = serverService;
        this.props = props;
    }

    @Transactional
    public void ban(Long userId, Long matchId, String map) {
        Match m = matches.lockById(matchId).orElseThrow(() -> ApiException.notFound("Match topilmadi"));
        m.requireStatus(MatchStatus.VETO, "Xarita tanlovi hozir o'tkazilmayapti");

        List<MatchPlayer> list = players.findByMatchIdOrderByJoinedAtAsc(matchId);
        Long captain = MatchQueryService.captain(list, m.getVetoTurn());
        if (!userId.equals(captain)) throw ApiException.forbidden("Hozir sizning navbatingiz emas");

        applyBan(m, map);
    }

    /** Navbat vaqti tugagan matchlar uchun tasodifiy ban (scheduler chaqiradi). */
    @Transactional
    public void autoBan(Long matchId) {
        Match m = matches.lockById(matchId).orElse(null);
        if (m == null || m.getStatus() != MatchStatus.VETO || m.getVetoDeadline() == null
                || m.getVetoDeadline().isAfter(Instant.now())) {
            return;
        }
        List<String> available = available(m);
        applyBan(m, available.get(RANDOM.nextInt(available.size())));
    }

    private void applyBan(Match m, String map) {
        List<String> available = available(m);
        if (!available.contains(map)) throw ApiException.badRequest("MAP", "Bu xaritani ban qilib bo'lmaydi");

        int position = props.mapPool().size() - available.size();
        vetoes.save(new MatchVeto(m.getId(), map, m.getVetoTurn(), MatchVeto.Action.BAN, position));
        lobby.system(m.getId(), m.teamName(m.getVetoTurn()) + " " + MapNames.pretty(map) + " xaritasini ban qildi");
        available.remove(map);

        if (available.size() == 1) {
            String decider = available.get(0);
            vetoes.save(new MatchVeto(m.getId(), decider, null, MatchVeto.Action.PICK, position + 1));
            m.mapDecided(decider, null, null);
            lobby.system(m.getId(), "O'ynaladigan xarita: " + MapNames.pretty(decider));
            serverService.tryAssign(m);
        } else {
            m.nextVetoTurn(Instant.now().plusSeconds(MatchLobbyService.VETO_TURN_SECONDS));
        }
        lobby.changed(m.getId());
    }

    private List<String> available(Match m) {
        Set<String> banned = vetoes.findByMatchIdOrderByPositionAsc(m.getId()).stream()
                .map(MatchVeto::getMap).collect(Collectors.toSet());
        return props.mapPool().stream().filter(x -> !banned.contains(x)).collect(Collectors.toList());
    }
}
