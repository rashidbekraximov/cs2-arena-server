package uz.cs2arena.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchVetoRepository extends JpaRepository<MatchVeto, Long> {

    List<MatchVeto> findByMatchIdOrderByPositionAsc(Long matchId);
}
