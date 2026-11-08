package uz.cs2arena.match;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    public enum Channel { ALL, TEAM, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long matchId;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private Team team;

    private String text;
    private Instant createdAt = Instant.now();

    protected ChatMessage() {}

    public ChatMessage(Long matchId, Long userId, Channel channel, Team team, String text) {
        this.matchId = matchId;
        this.userId = userId;
        this.channel = channel;
        this.team = team;
        this.text = text;
    }

    public static ChatMessage system(Long matchId, String text) {
        return new ChatMessage(matchId, null, Channel.SYSTEM, null, text);
    }

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public Long getUserId() { return userId; }
    public Channel getChannel() { return channel; }
    public Team getTeam() { return team; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}
