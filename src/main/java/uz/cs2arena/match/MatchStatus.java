package uz.cs2arena.match;

import java.util.EnumSet;
import java.util.Set;

/**
 * LOBBY → VETO → (WAITING_SERVER) → LIVE → FINISHED
 *            ↘ CANCELLED (istalgan bosqichda)
 */
public enum MatchStatus {
    LOBBY,
    VETO,
    WAITING_SERVER,
    LIVE,
    FINISHED,
    CANCELLED;

    /** O'yinchi bir vaqtda faqat bitta "faol" matchda bo'la oladi. */
    public static final Set<MatchStatus> ACTIVE = EnumSet.of(LOBBY, VETO, WAITING_SERVER, LIVE);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
