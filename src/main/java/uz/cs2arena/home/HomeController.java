package uz.cs2arena.home;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.common.PageDto;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.match.MatchDtos;
import uz.cs2arena.match.MatchPlayerRepository;
import uz.cs2arena.match.MatchQueryService;
import uz.cs2arena.match.MatchRepository;
import uz.cs2arena.match.MatchStatus;
import uz.cs2arena.news.NewsDto;
import uz.cs2arena.news.NewsRepository;
import uz.cs2arena.server.ServerStatusService;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserDtos;
import uz.cs2arena.user.UserRepository;
import uz.cs2arena.user.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Ochiq sahifalar: sayt ma'lumotlari, bosh sahifa, reyting, profil. */
@RestController
@RequestMapping("/api")
public class HomeController {

    public record SiteDto(String name, AppProperties.Banner banner) {}

    public record HomeDto(int onlinePlayers, long activeMatches, long openLobbies, long registeredPlayers,
                          Integer myElo, Long myRank, List<MatchDtos.MatchCard> liveMatches, List<NewsDto> news) {}

    public record LeaderRow(long rank, UserDtos.UserSummary user, String city, UserDtos.Stats stats, int weekDelta) {}

    public record EloPoint(Instant at, int elo) {}

    public record ProfileDto(UserDtos.UserSummary user, String country, String city, Instant memberSince,
                             long rank, UserDtos.Stats stats, List<MatchDtos.RecentMatchDto> recentMatches,
                             List<EloPoint> eloHistory, int eloChange30d) {}

    private final AppProperties props;
    private final ServerStatusService serverStatus;
    private final MatchRepository matches;
    private final MatchPlayerRepository matchPlayers;
    private final MatchQueryService matchQuery;
    private final NewsRepository news;
    private final UserRepository users;
    private final UserService userService;

    public HomeController(AppProperties props, ServerStatusService serverStatus, MatchRepository matches,
                          MatchPlayerRepository matchPlayers, MatchQueryService matchQuery, NewsRepository news,
                          UserRepository users, UserService userService) {
        this.props = props;
        this.serverStatus = serverStatus;
        this.matches = matches;
        this.matchPlayers = matchPlayers;
        this.matchQuery = matchQuery;
        this.news = news;
        this.users = users;
        this.userService = userService;
    }

    @GetMapping("/site")
    public SiteDto site() {
        return new SiteDto(props.name(), props.banner());
    }

    @GetMapping("/home")
    @Transactional(readOnly = true)
    public HomeDto home(@AuthenticationPrincipal AuthUser me) {
        Integer myElo = null;
        Long myRank = null;
        if (me != null) {
            User u = userService.get(me.id());
            myElo = u.getElo();
            myRank = userService.rankOf(u);
        }
        return new HomeDto(
                serverStatus.onlinePlayers(),
                matches.countByStatus(MatchStatus.LIVE),
                matches.countByStatus(MatchStatus.LOBBY),
                users.count(),
                myElo, myRank,
                matchQuery.live(),
                news.findTop3ByPublishedTrueOrderByCreatedAtDesc().stream().map(NewsDto::of).toList());
    }

    @GetMapping("/servers")
    public List<ServerStatusService.ServerStatus> servers() {
        return serverStatus.all();
    }

    @GetMapping("/leaderboard")
    @Transactional(readOnly = true)
    public PageDto<LeaderRow> leaderboard(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "25") int size) {
        int s = Math.min(Math.max(size, 1), 100);
        int p = Math.max(page, 0);
        var result = users.leaderboard(PageRequest.of(p, s));
        Map<Long, Integer> weekly = weeklyDelta(result.getContent().stream().map(User::getId).toList());

        List<LeaderRow> rows = new ArrayList<>();
        long rank = (long) p * s + 1;
        for (User u : result.getContent()) {
            rows.add(new LeaderRow(rank++, UserDtos.UserSummary.of(u), u.getCity(), UserDtos.Stats.of(u),
                    weekly.getOrDefault(u.getId(), 0)));
        }
        return new PageDto<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/users/{id}/profile")
    @Transactional(readOnly = true)
    public ProfileDto profile(@PathVariable Long id) {
        User u = userService.get(id);
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<EloPoint> history = new ArrayList<>();
        int change30 = 0;
        for (Object[] row : matchPlayers.eloHistory(id, since)) {
            Instant at = (Instant) row[0];
            int before = ((Number) row[1]).intValue();
            int change = ((Number) row[2]).intValue();
            history.add(new EloPoint(at, before + change));
            change30 += change;
        }
        return new ProfileDto(UserDtos.UserSummary.of(u), u.getCountry(), u.getCity(), u.getCreatedAt(),
                userService.rankOf(u), UserDtos.Stats.of(u), matchQuery.recentForUser(id, 10), history, change30);
    }

    private Map<Long, Integer> weeklyDelta(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : matchPlayers.eloDeltaSince(userIds, Instant.now().minus(7, ChronoUnit.DAYS))) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return map;
    }
}
