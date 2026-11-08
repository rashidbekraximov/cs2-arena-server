package uz.cs2arena.match;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {

    List<MatchPlayer> findByMatchIdOrderByJoinedAtAsc(Long matchId);

    List<MatchPlayer> findByMatchIdInOrderByJoinedAtAsc(Collection<Long> matchIds);

    Optional<MatchPlayer> findByMatchIdAndUserId(Long matchId, Long userId);

    long countByMatchIdAndTeam(Long matchId, Team team);

    long countByMatchId(Long matchId);

    /** Profil: yakunlangan matchlar (eng yangisi birinchi). */
    @Query("""
            select mp from MatchPlayer mp, Match m
            where mp.matchId = m.id and mp.userId = :userId
              and m.status = :status
            order by m.finishedAt desc
            """)
    List<MatchPlayer> findForUserByStatus(@Param("userId") Long userId, @Param("status") MatchStatus status,
                                          Pageable pageable);

    /** ELO tarixi uchun: [finishedAt, eloBefore, eloChange] */
    @Query("""
            select m.finishedAt, mp.eloBefore, mp.eloChange from MatchPlayer mp, Match m
            where mp.matchId = m.id and mp.userId = :userId and mp.eloChange is not null
              and m.finishedAt >= :since
            order by m.finishedAt asc
            """)
    List<Object[]> eloHistory(@Param("userId") Long userId, @Param("since") Instant since);

    /** Reyting: haftalik ELO o'zgarishi. [userId, sum(eloChange)] */
    @Query("""
            select mp.userId, sum(mp.eloChange) from MatchPlayer mp, Match m
            where mp.matchId = m.id and mp.userId in :userIds and mp.eloChange is not null
              and m.finishedAt >= :since
            group by mp.userId
            """)
    List<Object[]> eloDeltaSince(@Param("userIds") Collection<Long> userIds, @Param("since") Instant since);
}
