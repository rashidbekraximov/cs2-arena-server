package uz.cs2arena.match;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Umumiy kanal + tizim xabarlari (eng yangisi birinchi). */
    @Query("""
            select c from ChatMessage c
            where c.matchId = :matchId and c.channel in :channels
            order by c.createdAt desc
            """)
    List<ChatMessage> findByChannels(@Param("matchId") Long matchId,
                                     @Param("channels") Collection<ChatMessage.Channel> channels, Pageable pageable);

    @Query("""
            select c from ChatMessage c
            where c.matchId = :matchId and c.channel = :channel and c.team = :team
            order by c.createdAt desc
            """)
    List<ChatMessage> findTeam(@Param("matchId") Long matchId, @Param("channel") ChatMessage.Channel channel,
                               @Param("team") Team team, Pageable pageable);
}
