package uz.cs2arena.match;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :id")
    Optional<Match> lockById(@Param("id") Long id);

    Optional<Match> findByInviteCode(String inviteCode);

    @Query("""
            select m.id from Match m, MatchPlayer mp
            where mp.matchId = m.id and mp.userId = :userId and m.status in :statuses
            """)
    List<Long> findMatchIdsForUser(@Param("userId") Long userId,
                                   @Param("statuses") Collection<MatchStatus> statuses);

    List<Match> findTop10ByStatusOrderByStartedAtDesc(MatchStatus status);

    List<Match> findTop20ByStatusAndOpenLobbyTrueOrderByCreatedAtDesc(MatchStatus status);

    long countByStatus(MatchStatus status);

    List<Match> findByStatusAndVetoDeadlineBefore(MatchStatus status, Instant now);

    List<Match> findByStatusOrderByCreatedAtAsc(MatchStatus status);

    List<Match> findByStatusAndCreatedAtBefore(MatchStatus status, Instant before);

    Page<Match> findByStatusOrderByCreatedAtDesc(MatchStatus status, Pageable pageable);

    Page<Match> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
