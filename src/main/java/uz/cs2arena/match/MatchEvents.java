package uz.cs2arena.match;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * WebSocket orqali brauzerlarga xabar berish. Tranzaksiya tugagandan keyin yuboriladi,
 * shunda brauzer yangi holatni so'raganda bazada eski ma'lumot bo'lmaydi.
 *
 * Kanallar:
 *   /topic/match/{id}            — {"type":"STATE"}  (brauzer holatni qayta yuklaydi)
 *   /topic/match/{id}/chat       — umumiy chat xabari
 *   /topic/match/{id}/chat/{A|B} — jamoa chati
 */
@Component
public class MatchEvents {

    public record Changed(Long matchId) {}

    public record ChatPosted(Long matchId, MatchDtos.ChatDto message, Team team) {}

    private final SimpMessagingTemplate ws;

    public MatchEvents(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onChanged(Changed e) {
        ws.convertAndSend("/topic/match/" + e.matchId(), Map.of("type", "STATE"));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onChat(ChatPosted e) {
        String topic = "/topic/match/" + e.matchId() + "/chat" + (e.team() != null ? "/" + e.team() : "");
        ws.convertAndSend(topic, e.message());
    }
}
