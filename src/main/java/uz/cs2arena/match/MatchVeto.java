package uz.cs2arena.match;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "match_vetoes")
public class MatchVeto {

    public enum Action { BAN, PICK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long matchId;
    private String map;

    @Enumerated(EnumType.STRING)
    private Team team;

    @Enumerated(EnumType.STRING)
    private Action action;

    private int position;
    private Instant createdAt = Instant.now();

    protected MatchVeto() {}

    public MatchVeto(Long matchId, String map, Team team, Action action, int position) {
        this.matchId = matchId;
        this.map = map;
        this.team = team;
        this.action = action;
        this.position = position;
    }

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public String getMap() { return map; }
    public Team getTeam() { return team; }
    public Action getAction() { return action; }
    public int getPosition() { return position; }
}
