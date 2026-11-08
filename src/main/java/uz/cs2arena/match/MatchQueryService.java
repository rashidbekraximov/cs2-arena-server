package uz.cs2arena.match;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.server.GameServerRepository;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Matchlarni o'qish va DTO'ga aylantirish (yozish yo'q). */
@Service
@Transactional(readOnly = true)
public class MatchQueryService {

    private final MatchRepository matches;
    private final MatchPlayerRepository players;
    private final MatchVetoRepository vetoes;
    private final ChatMessageRepository chat;
    private final UserRepository users;
    private final GameServerRepository servers;
    private final AppProperties props;

    public MatchQueryService(MatchRepository matches, MatchPlayerRepository players, MatchVetoRepository vetoes,
                             ChatMessageRepository chat, UserRepository users, GameServerRepository servers,
                             AppProperties props) {
        this.matches = matches;
        this.players = players;
        this.vetoes = vetoes;
        this.chat = chat;
        this.users = users;
        this.servers = servers;
        this.props = props;
    }

    public Match require(Long matchId) {
        return matches.findById(matchId).orElseThrow(() -> ApiException.notFound("Match topilmadi"));
    }

    public Optional<Long> currentMatchId(Long userId) {
        return matches.findMatchIdsForUser(userId, MatchStatus.ACTIVE).stream().findFirst();
    }

    public MatchDtos.MatchView view(Long matchId, Long viewerId) {
        Match m = require(matchId);
        List<MatchPlayer> list = players.findByMatchIdOrderByJoinedAtAsc(matchId);
        Map<Long, User> userMap = usersOf(list);

        MatchPlayer me = viewerId == null ? null
                : list.stream().filter(p -> p.getUserId().equals(viewerId)).findFirst().orElse(null);
        Long captainA = captain(list, Team.A);
        Long captainB = captain(list, Team.B);
        boolean inMatch = me != null;

        var viewer = new MatchDtos.ViewerDto(inMatch, inMatch ? me.getTeam() : null,
                m.getHostId().equals(viewerId),
                inMatch && (me.getUserId().equals(captainA) || me.getUserId().equals(captainB)));

        MatchDtos.VetoDto veto = null;
        if (m.needsVeto() || !vetoes.findByMatchIdOrderByPositionAsc(matchId).isEmpty()) {
            veto = vetoView(m, me, captainA, captainB);
        }

        MatchDtos.ServerDto server = null;
        if (inMatch && m.getServerId() != null && m.getStatus() == MatchStatus.LIVE) {
            server = servers.findById(m.getServerId())
                    .map(s -> new MatchDtos.ServerDto(s.getName(), s.getRegion(), s.address(),
                            "connect " + s.address() + (m.getPassword() != null ? "; password " + m.getPassword() : ""),
                            "steam://connect/" + s.address() + (m.getPassword() != null ? "/" + m.getPassword() : "")))
                    .orElse(null);
        }

        return new MatchDtos.MatchView(
                m.getId(), m.getStatus(), m.getMode(), m.getMode().label(), m.getTeamSize(),
                m.getMap(), m.getRegion(), m.isAntiCheat(), m.isOvertime(), m.isOpenLobby(), m.isSpectators(),
                m.getPassword() != null,
                inMatch ? m.getInviteCode() : null,
                m.getHostId(),
                team(m, Team.A, list, userMap, captainA),
                team(m, Team.B, list, userMap, captainB),
                veto, server, m.getWinner(), viewer,
                m.getCreatedAt(), m.getStartedAt(), m.getFinishedAt());
    }

    public List<MatchDtos.ChatDto> chat(Long matchId, Long viewerId) {
        List<MatchPlayer> list = players.findByMatchIdOrderByJoinedAtAsc(matchId);
        Optional<MatchPlayer> me = list.stream().filter(p -> p.getUserId().equals(viewerId)).findFirst();

        List<ChatMessage> messages = new ArrayList<>(chat.findByChannels(matchId,
                List.of(ChatMessage.Channel.ALL, ChatMessage.Channel.SYSTEM), PageRequest.of(0, 100)));
        me.ifPresent(p -> messages.addAll(
                chat.findTeam(matchId, ChatMessage.Channel.TEAM, p.getTeam(), PageRequest.of(0, 100))));
        messages.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        Set<Long> ids = messages.stream().map(ChatMessage::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return messages.stream().map(c -> toChatDto(c, userMap.get(c.getUserId()))).toList();
    }

    public List<MatchDtos.MatchCard> live() {
        return cards(matches.findTop10ByStatusOrderByStartedAtDesc(MatchStatus.LIVE));
    }

    public List<MatchDtos.MatchCard> openLobbies() {
        return cards(matches.findTop20ByStatusAndOpenLobbyTrueOrderByCreatedAtDesc(MatchStatus.LOBBY));
    }

    public List<MatchDtos.MatchCard> cards(List<Match> list) {
        if (list.isEmpty()) return List.of();
        Map<Long, Long> counts = players.findByMatchIdInOrderByJoinedAtAsc(list.stream().map(Match::getId).toList())
                .stream().collect(Collectors.groupingBy(MatchPlayer::getMatchId, Collectors.counting()));
        return list.stream().map(m -> new MatchDtos.MatchCard(m.getId(), m.getStatus(), m.getMode().label(),
                m.getMap(), m.getRegion(), m.getTeamAName(), m.getTeamBName(), m.getScoreA(), m.getScoreB(),
                counts.getOrDefault(m.getId(), 0L).intValue(), m.getTeamSize() * 2,
                m.getPassword() != null, m.getCreatedAt())).toList();
    }

    public List<MatchDtos.RecentMatchDto> recentForUser(Long userId, int limit) {
        List<MatchPlayer> mine = players.findForUserByStatus(userId, MatchStatus.FINISHED, PageRequest.of(0, limit));
        Map<Long, Match> byId = matches.findAllById(mine.stream().map(MatchPlayer::getMatchId).toList())
                .stream().collect(Collectors.toMap(Match::getId, Function.identity()));
        return mine.stream().map(p -> {
            Match m = byId.get(p.getMatchId());
            boolean isA = p.getTeam() == Team.A;
            Integer my = isA ? m.getScoreA() : m.getScoreB();
            Integer enemy = isA ? m.getScoreB() : m.getScoreA();
            int rounds = (my == null ? 0 : my) + (enemy == null ? 0 : enemy);
            double adr = rounds == 0 ? 0 : Math.round(10.0 * p.getDamage() / rounds) / 10.0;
            return new MatchDtos.RecentMatchDto(m.getId(), m.getMap(), m.getMode().label(),
                    m.getWinner() == p.getTeam(), my, enemy, p.getKills(), p.getDeaths(), p.getAssists(), adr,
                    p.getEloChange(), m.getFinishedAt());
        }).toList();
    }

    public MatchDtos.ChatDto toChatDto(ChatMessage c, User u) {
        return new MatchDtos.ChatDto(c.getId(), c.getUserId(), u == null ? null : u.getNickname(),
                u == null ? null : u.getAvatarUrl(), c.getChannel().name(), c.getText(), c.getCreatedAt());
    }

    // ---------- yordamchilar ----------

    /** Jamoa kapitani — jamoaga birinchi qo'shilgan o'yinchi. */
    static Long captain(List<MatchPlayer> list, Team team) {
        return list.stream().filter(p -> p.getTeam() == team).findFirst().map(MatchPlayer::getUserId).orElse(null);
    }

    private MatchDtos.TeamDto team(Match m, Team t, List<MatchPlayer> list, Map<Long, User> userMap, Long captainId) {
        List<MatchDtos.PlayerDto> ps = list.stream().filter(p -> p.getTeam() == t).map(p -> {
            User u = userMap.get(p.getUserId());
            return new MatchDtos.PlayerDto(u.getId(), u.getNickname(), u.getAvatarUrl(), u.getElo(),
                    u.isPremiumActive(), u.getId().equals(m.getHostId()), u.getId().equals(captainId),
                    p.getKills(), p.getDeaths(), p.getAssists(), p.getHeadshots(), p.getDamage(), p.getEloChange());
        }).toList();
        int avg = ps.isEmpty() ? 0 : (int) Math.round(ps.stream().mapToInt(MatchDtos.PlayerDto::elo).average().orElse(0));
        return new MatchDtos.TeamDto(m.teamName(t), ps, avg, t == Team.A ? m.getScoreA() : m.getScoreB());
    }

    private MatchDtos.VetoDto vetoView(Match m, MatchPlayer me, Long captainA, Long captainB) {
        Map<String, MatchVeto> done = vetoes.findByMatchIdOrderByPositionAsc(m.getId()).stream()
                .collect(Collectors.toMap(MatchVeto::getMap, Function.identity(), (a, b) -> b));
        List<MatchDtos.VetoMapDto> maps = props.mapPool().stream().map(map -> {
            MatchVeto v = done.get(map);
            if (v != null) return new MatchDtos.VetoMapDto(map, v.getAction().name(), v.getTeam());
            if (map.equals(m.getMap()) && m.getStatus() != MatchStatus.VETO) {
                return new MatchDtos.VetoMapDto(map, "PICK", null);
            }
            return new MatchDtos.VetoMapDto(map, "AVAILABLE", null);
        }).toList();

        boolean canAct = m.getStatus() == MatchStatus.VETO && me != null && m.getVetoTurn() != null
                && me.getUserId().equals(m.getVetoTurn() == Team.A ? captainA : captainB);
        return new MatchDtos.VetoDto(m.getVetoTurn(), m.getVetoDeadline(), maps, canAct);
    }

    private Map<Long, User> usersOf(List<MatchPlayer> list) {
        return users.findAllById(list.stream().map(MatchPlayer::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
