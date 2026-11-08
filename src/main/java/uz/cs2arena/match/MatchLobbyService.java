package uz.cs2arena.match;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Lobbi: yaratish, qo'shilish, jamoa almashtirish, sozlamalar, chat, boshlash. */
@Service
public class MatchLobbyService {

    static final int VETO_TURN_SECONDS = 30;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MatchRepository matches;
    private final MatchPlayerRepository players;
    private final ChatMessageRepository chat;
    private final UserRepository users;
    private final MatchQueryService query;
    private final MatchServerService serverService;
    private final AppProperties props;
    private final ApplicationEventPublisher events;

    public MatchLobbyService(MatchRepository matches, MatchPlayerRepository players, ChatMessageRepository chat,
                             UserRepository users, MatchQueryService query, MatchServerService serverService,
                             AppProperties props, ApplicationEventPublisher events) {
        this.matches = matches;
        this.players = players;
        this.chat = chat;
        this.users = users;
        this.query = query;
        this.serverService = serverService;
        this.props = props;
        this.events = events;
    }

    @Transactional
    public Long create(Long userId, MatchDtos.SettingsRequest req) {
        ensureNotInActiveMatch(userId);
        Match m = new Match(userId, newInviteCode());
        applySettings(m, req);
        matches.save(m);
        players.save(new MatchPlayer(m.getId(), userId, Team.A));
        system(m.getId(), nick(userId) + " matchni yaratdi");
        return m.getId();
    }

    /** Taklif havolasi orqali: kod bo'yicha matchni topadi. */
    @Transactional(readOnly = true)
    public Long resolveInvite(String code) {
        return matches.findByInviteCode(code.toUpperCase())
                .map(Match::getId)
                .orElseThrow(() -> ApiException.notFound("Taklif havolasi noto'g'ri yoki eskirgan"));
    }

    @Transactional
    public void join(Long userId, Long matchId, MatchDtos.JoinRequest req) {
        Match m = lock(matchId);
        m.requireStatus(MatchStatus.LOBBY, "Bu matchga endi qo'shilib bo'lmaydi");
        if (players.findByMatchIdAndUserId(matchId, userId).isPresent()) {
            if (req.team() != null) switchTeam(userId, matchId, req.team());
            return;
        }
        ensureNotInActiveMatch(userId);

        boolean invited = req.inviteCode() != null && req.inviteCode().equalsIgnoreCase(m.getInviteCode());
        if (!m.isOpenLobby() && !invited) {
            throw ApiException.forbidden("Bu yopiq lobbi — faqat taklif havolasi orqali kirish mumkin");
        }
        if (m.getPassword() != null && !invited && !m.getPassword().equals(req.password())) {
            throw ApiException.forbidden("Parol noto'g'ri");
        }

        Team team = req.team() != null ? req.team() : pickTeam(m);
        ensureSlot(m, team);
        players.save(new MatchPlayer(matchId, userId, team));
        system(matchId, nick(userId) + " lobbiga qo'shildi");
        changed(matchId);
    }

    @Transactional
    public void switchTeam(Long userId, Long matchId, Team team) {
        Match m = lock(matchId);
        m.requireStatus(MatchStatus.LOBBY, "Jamoani faqat lobbi bosqichida almashtirish mumkin");
        MatchPlayer p = players.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> ApiException.forbidden("Siz bu matchda emassiz"));
        if (p.getTeam() == team) return;
        ensureSlot(m, team);
        p.moveTo(team);
        changed(matchId);
    }

    @Transactional
    public void leave(Long userId, Long matchId) {
        Match m = lock(matchId);
        MatchPlayer p = players.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> ApiException.forbidden("Siz bu matchda emassiz"));
        if (m.getStatus() != MatchStatus.LOBBY) {
            throw ApiException.conflict("MATCH_STARTED", "Match boshlangan — chiqib bo'lmaydi");
        }
        players.delete(p);
        players.flush();
        system(matchId, nick(userId) + " lobbidan chiqdi");

        if (m.getHostId().equals(userId)) {
            List<MatchPlayer> rest = players.findByMatchIdOrderByJoinedAtAsc(matchId);
            if (rest.isEmpty()) {
                m.cancel();
            } else {
                m.transferHost(rest.get(0).getUserId());
                system(matchId, nick(rest.get(0).getUserId()) + " endi lobbi egasi");
            }
        }
        changed(matchId);
    }

    @Transactional
    public void kick(Long hostId, Long matchId, Long targetId) {
        Match m = lock(matchId);
        requireHost(m, hostId);
        m.requireStatus(MatchStatus.LOBBY, "Faqat lobbi bosqichida chiqarish mumkin");
        if (hostId.equals(targetId)) throw ApiException.badRequest("SELF", "O'zingizni chiqara olmaysiz");
        MatchPlayer p = players.findByMatchIdAndUserId(matchId, targetId)
                .orElseThrow(() -> ApiException.notFound("O'yinchi lobbida emas"));
        players.delete(p);
        system(matchId, nick(targetId) + " lobbidan chiqarildi");
        changed(matchId);
    }

    @Transactional
    public void updateSettings(Long hostId, Long matchId, MatchDtos.SettingsRequest req) {
        Match m = lock(matchId);
        requireHost(m, hostId);
        int newSize = req.mode().teamSize();
        if (players.countByMatchIdAndTeam(matchId, Team.A) > newSize
                || players.countByMatchIdAndTeam(matchId, Team.B) > newSize) {
            throw ApiException.badRequest("TEAM_TOO_BIG", "Jamoalarda o'yinchi ko'p — avval kamaytiring");
        }
        applySettings(m, req);
        system(matchId, "Match sozlamalari yangilandi");
        changed(matchId);
    }

    /** Ikkala jamoa to'lganda: veto boshlanadi yoki (xarita tanlangan bo'lsa) server beriladi. */
    @Transactional
    public void start(Long hostId, Long matchId) {
        Match m = lock(matchId);
        requireHost(m, hostId);
        m.requireStatus(MatchStatus.LOBBY, "Match allaqachon boshlangan");
        List<MatchPlayer> list = players.findByMatchIdOrderByJoinedAtAsc(matchId);
        long a = list.stream().filter(p -> p.getTeam() == Team.A).count();
        long b = list.stream().filter(p -> p.getTeam() == Team.B).count();
        if (a != m.getTeamSize() || b != m.getTeamSize()) {
            throw ApiException.badRequest("TEAMS_NOT_FULL",
                    "Ikkala jamoada " + m.getTeamSize() + " tadan o'yinchi bo'lishi kerak (" + a + " / " + b + ")");
        }

        String nameA = "Team " + nick(MatchQueryService.captain(list, Team.A));
        String nameB = "Team " + nick(MatchQueryService.captain(list, Team.B));
        if (m.needsVeto()) {
            m.startVeto(nameA, nameB, Instant.now().plusSeconds(VETO_TURN_SECONDS));
            system(matchId, "Xarita tanlovi boshlandi. Navbat: " + nameA);
        } else {
            m.mapDecided(m.getMap(), nameA, nameB);
            system(matchId, "Match boshlandi. Server tayyorlanmoqda…");
            serverService.tryAssign(m);
        }
        changed(matchId);
    }

    @Transactional
    public MatchDtos.ChatDto postChat(Long userId, Long matchId, MatchDtos.ChatRequest req) {
        MatchPlayer p = players.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> ApiException.forbidden("Chatga faqat match ishtirokchilari yoza oladi"));
        if (req.channel() == ChatMessage.Channel.SYSTEM) throw ApiException.badRequest("CHANNEL", "Noto'g'ri kanal");
        Team team = req.channel() == ChatMessage.Channel.TEAM ? p.getTeam() : null;
        ChatMessage msg = chat.save(new ChatMessage(matchId, userId, req.channel(), team, req.text().trim()));
        var dto = query.toChatDto(msg, users.findById(userId).orElse(null));
        events.publishEvent(new MatchEvents.ChatPosted(matchId, dto, team));
        return dto;
    }

    @Transactional
    public void cancel(Long matchId, String reason) {
        Match m = lock(matchId);
        m.cancel();
        system(matchId, "Match bekor qilindi" + (reason == null ? "" : ": " + reason));
        changed(matchId);
    }

    // ---------- yordamchilar ----------

    public void system(Long matchId, String text) {
        ChatMessage msg = chat.save(ChatMessage.system(matchId, text));
        events.publishEvent(new MatchEvents.ChatPosted(matchId, query.toChatDto(msg, null), null));
    }

    public void changed(Long matchId) {
        events.publishEvent(new MatchEvents.Changed(matchId));
    }

    private void applySettings(Match m, MatchDtos.SettingsRequest req) {
        String map = req.map().trim();
        if (!Match.MAP_VETO.equals(map) && !props.mapPool().contains(map)) {
            throw ApiException.badRequest("MAP", "Bunday xarita pulda yo'q");
        }
        m.applySettings(req.mode(), map, req.region().trim(), req.password(), req.antiCheat(), req.overtime(),
                req.openLobby(), req.spectators());
    }

    private Match lock(Long matchId) {
        return matches.lockById(matchId).orElseThrow(() -> ApiException.notFound("Match topilmadi"));
    }

    private void requireHost(Match m, Long userId) {
        if (!Objects.equals(m.getHostId(), userId)) throw ApiException.forbidden("Buni faqat lobbi egasi qila oladi");
    }

    private void ensureNotInActiveMatch(Long userId) {
        if (!matches.findMatchIdsForUser(userId, MatchStatus.ACTIVE).isEmpty()) {
            throw ApiException.conflict("ALREADY_IN_MATCH", "Siz allaqachon boshqa matchdasiz");
        }
    }

    private void ensureSlot(Match m, Team team) {
        if (players.countByMatchIdAndTeam(m.getId(), team) >= m.getTeamSize()) {
            throw ApiException.conflict("TEAM_FULL", "Bu jamoada joy qolmagan");
        }
    }

    private Team pickTeam(Match m) {
        long a = players.countByMatchIdAndTeam(m.getId(), Team.A);
        long b = players.countByMatchIdAndTeam(m.getId(), Team.B);
        return a <= b ? Team.A : Team.B;
    }

    private String nick(Long userId) {
        return users.findById(userId).map(User::getNickname).orElse("O'yinchi");
    }

    private static String newInviteCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }
}
