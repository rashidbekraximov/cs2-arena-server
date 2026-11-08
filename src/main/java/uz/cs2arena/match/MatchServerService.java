package uz.cs2arena.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.server.GameServer;
import uz.cs2arena.server.GameServerRepository;
import uz.cs2arena.server.RconClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

/**
 * Bo'sh serverni matchga biriktiradi va MatchZy plugin'iga o'yin sozlamasini yuklatadi (RCON orqali).
 * Server MatchZy'dan sozlamani /api/matchzy/{id}/config dan o'zi yuklab oladi,
 * natijalarni esa /api/matchzy/events ga yuboradi.
 */
@Service
public class MatchServerService {

    private static final Logger log = LoggerFactory.getLogger(MatchServerService.class);

    public record ServerAssigned(Long matchId, Long serverId) {}

    private final GameServerRepository servers;
    private final MatchRepository matches;
    private final ChatMessageRepository chat;
    private final AppProperties props;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate newTx;

    public MatchServerService(GameServerRepository servers, MatchRepository matches, ChatMessageRepository chat,
                              AppProperties props, ApplicationEventPublisher events,
                              PlatformTransactionManager txManager) {
        this.servers = servers;
        this.matches = matches;
        this.chat = chat;
        this.props = props;
        this.events = events;
        this.newTx = new TransactionTemplate(txManager);
        this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Match WAITING_SERVER holatida bo'lishi kerak. Server bo'lmasa — keyinroq scheduler qayta urinadi. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryAssign(Match m) {
        return tryAssign(m, true);
    }

    /** notifyIfNone=false — scheduler qayta urinayotganda chatni to'ldirib yubormaslik uchun. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryAssign(Match m, boolean notifyIfNone) {
        if (m.getStatus() != MatchStatus.WAITING_SERVER) return false;
        List<GameServer> free = servers.findFree(m.getRegion(), EnumSet.of(MatchStatus.LIVE));
        if (free.isEmpty()) {
            if (notifyIfNone) {
                chat.save(ChatMessage.system(m.getId(),
                        "Hozir bo'sh server yo'q — navbatdasiz, server bo'shashi bilan boshlanadi"));
            }
            return false;
        }
        GameServer s = free.get(0);
        m.goLive(s.getId());
        chat.save(ChatMessage.system(m.getId(), "Server tayyor: " + s.getName() + ". Ulanish tugmasini bosing!"));
        events.publishEvent(new ServerAssigned(m.getId(), s.getId()));
        events.publishEvent(new MatchEvents.Changed(m.getId()));
        return true;
    }

    /** Tranzaksiya saqlangandan keyin serverga buyruq yuboriladi (tarmoq so'rovi tranzaksiya ichida bo'lmasin). */
    @TransactionalEventListener
    public void onServerAssigned(ServerAssigned e) {
        GameServer s = servers.findById(e.serverId()).orElse(null);
        Match m = matches.findById(e.matchId()).orElse(null);
        if (s == null || m == null) return;

        String base = props.baseUrlClean();
        String secret = props.matchzy().secret();
        String configUrl = base + "/api/matchzy/" + m.getId() + "/config?key="
                + URLEncoder.encode(secret, StandardCharsets.UTF_8);
        String commands = String.join("; ",
                "sv_password \"" + (m.getPassword() == null ? "" : m.getPassword()) + "\"",
                "matchzy_remote_log_url \"" + base + "/api/matchzy/events\"",
                "matchzy_remote_log_header_key \"X-Arena-Key\"",
                "matchzy_remote_log_header_value \"" + secret + "\"",
                "matchzy_loadmatch_url \"" + configUrl + "\"");
        try {
            RconClient.execute(s.getIp(), s.getPort(), s.getRconPassword(), commands);
            log.info("Match #{} serverga yuklandi: {}", m.getId(), s.address());
        } catch (Exception ex) {
            log.error("Match #{}: serverga RCON orqali ulanib bo'lmadi ({}): {}", m.getId(), s.address(), ex.getMessage());
            newTx.executeWithoutResult(st -> chat.save(ChatMessage.system(m.getId(),
                    "Server bilan aloqa xatosi. Admin bilan bog'laning yoki keyinroq qayta urinib ko'ring.")));
        }
    }
}
