package uz.cs2arena.server;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "game_servers")
public class GameServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String region;
    private String ip;
    private int port;
    private String rconPassword;
    private boolean enabled = true;
    private Instant createdAt = Instant.now();

    protected GameServer() {}

    public GameServer(String name, String region, String ip, int port, String rconPassword) {
        update(name, region, ip, port, rconPassword, true);
    }

    public void update(String name, String region, String ip, int port, String rconPassword, boolean enabled) {
        this.name = name;
        this.region = region;
        this.ip = ip;
        this.port = port;
        if (rconPassword != null && !rconPassword.isBlank()) this.rconPassword = rconPassword;
        this.enabled = enabled;
    }

    public String address() { return ip + ":" + port; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public String getRconPassword() { return rconPassword; }
    public boolean isEnabled() { return enabled; }
}
