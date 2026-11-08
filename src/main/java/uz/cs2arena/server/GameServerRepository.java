package uz.cs2arena.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uz.cs2arena.match.MatchStatus;

import java.util.Collection;
import java.util.List;

public interface GameServerRepository extends JpaRepository<GameServer, Long> {

    List<GameServer> findByEnabledTrueOrderByIdAsc();

    /** Faol matchga biriktirilmagan serverlar. */
    @Query("""
            select s from GameServer s
            where s.enabled = true and s.region = :region and s.id not in (
                select m.serverId from Match m
                where m.serverId is not null and m.status in :busyStatuses
            )
            order by s.id
            """)
    List<GameServer> findFree(@Param("region") String region,
                              @Param("busyStatuses") Collection<MatchStatus> busyStatuses);
}
