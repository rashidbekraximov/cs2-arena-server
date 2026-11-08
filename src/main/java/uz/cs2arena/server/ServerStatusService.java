package uz.cs2arena.server;

import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Serverlar holatini har 15 soniyada A2S orqali yangilaydi. */
@Service
public class ServerStatusService {

    public record ServerStatus(Long id, String name, String region, String address, boolean online,
                               String map, int players, int maxPlayers, long ping) {}

    private final GameServerRepository servers;
    private final Map<Long, A2sClient.Info> cache = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(8);

    public ServerStatusService(GameServerRepository servers) {
        this.servers = servers;
    }

    @Scheduled(fixedDelay = 15_000, initialDelay = 2_000)
    public void refresh() {
        List<GameServer> list = servers.findByEnabledTrueOrderByIdAsc();
        List<Future<?>> tasks = new ArrayList<>();
        for (GameServer s : list) {
            tasks.add(pool.submit(() -> cache.put(s.getId(), A2sClient.query(s.getIp(), s.getPort(), 1500))));
        }
        for (Future<?> f : tasks) {
            try { f.get(); } catch (Exception ignored) {}
        }
        cache.keySet().removeIf(id -> list.stream().noneMatch(s -> s.getId().equals(id)));
    }

    public List<ServerStatus> all() {
        return servers.findByEnabledTrueOrderByIdAsc().stream().map(s -> {
            A2sClient.Info i = cache.getOrDefault(s.getId(), A2sClient.Info.offline());
            return new ServerStatus(s.getId(), s.getName(), s.getRegion(), s.address(), i.online(),
                    i.map(), i.humans(), i.maxPlayers(), i.ping());
        }).toList();
    }

    public int onlinePlayers() {
        return cache.values().stream().mapToInt(A2sClient.Info::humans).sum();
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdownNow();
    }
}
