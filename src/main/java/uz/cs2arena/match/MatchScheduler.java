package uz.cs2arena.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;

/** Fon vazifalari: veto vaqti, server navbati, eskirgan lobbilar. */
@Component
public class MatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchScheduler.class);
    private static final Duration STALE_LOBBY = Duration.ofHours(3);

    private final MatchRepository matches;
    private final MatchVetoService veto;
    private final MatchServerService serverService;
    private final MatchLobbyService lobby;
    private final TransactionTemplate tx;

    public MatchScheduler(MatchRepository matches, MatchVetoService veto, MatchServerService serverService,
                          MatchLobbyService lobby, PlatformTransactionManager txManager) {
        this.matches = matches;
        this.veto = veto;
        this.serverService = serverService;
        this.lobby = lobby;
        this.tx = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedDelay = 2_000)
    public void expiredVetoes() {
        for (Match m : matches.findByStatusAndVetoDeadlineBefore(MatchStatus.VETO, Instant.now())) {
            safely(() -> veto.autoBan(m.getId()));
        }
    }

    @Scheduled(fixedDelay = 15_000)
    public void waitingForServer() {
        for (Match m : matches.findByStatusOrderByCreatedAtAsc(MatchStatus.WAITING_SERVER)) {
            safely(() -> tx.executeWithoutResult(st ->
                    matches.lockById(m.getId()).ifPresent(x -> serverService.tryAssign(x, false))));
        }
    }

    @Scheduled(fixedDelay = 300_000)
    public void staleLobbies() {
        for (Match m : matches.findByStatusAndCreatedAtBefore(MatchStatus.LOBBY, Instant.now().minus(STALE_LOBBY))) {
            safely(() -> lobby.cancel(m.getId(), "lobbi juda uzoq faol bo'lmadi"));
        }
    }

    private static void safely(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            log.warn("Fon vazifasida xato: {}", e.getMessage());
        }
    }
}
