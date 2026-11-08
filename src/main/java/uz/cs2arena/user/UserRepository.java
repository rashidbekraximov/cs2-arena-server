package uz.cs2arena.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySteamId(String steamId);

    @Query("select u from User u where u.banned = false and u.matchesPlayed > 0 order by u.elo desc, u.id asc")
    Page<User> leaderboard(Pageable pageable);

    @Query("select count(u) from User u where u.banned = false and u.matchesPlayed > 0 and u.elo > :elo")
    long countAboveElo(@Param("elo") int elo);

    @Query("""
            select u from User u
            where lower(u.nickname) like lower(concat('%', :q, '%')) or u.steamId = :q
            order by u.id desc
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}
