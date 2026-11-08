package uz.cs2arena.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.match.MatchLobbyService;
import uz.cs2arena.news.News;
import uz.cs2arena.news.NewsDto;
import uz.cs2arena.news.NewsRepository;
import uz.cs2arena.server.GameServer;
import uz.cs2arena.server.GameServerRepository;
import uz.cs2arena.user.Role;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserService;
import uz.cs2arena.wallet.WalletService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Admin amallari. Har bir amal jurnalga yoziladi. */
@Service
public class AdminService {

    private final UserService users;
    private final WalletService wallets;
    private final NewsRepository news;
    private final GameServerRepository servers;
    private final MatchLobbyService lobby;
    private final AdminActionRepository log;

    public AdminService(UserService users, WalletService wallets, NewsRepository news, GameServerRepository servers,
                        MatchLobbyService lobby, AdminActionRepository log) {
        this.users = users;
        this.wallets = wallets;
        this.news = news;
        this.servers = servers;
        this.lobby = lobby;
        this.log = log;
    }

    // ---------- ADMIN ----------

    @Transactional
    public User ban(User actor, Long targetId, AdminDtos.BanRequest req) {
        User target = users.get(targetId);
        checkCanModerate(actor, target);
        Instant until = req.days() == null ? null : Instant.now().plus(req.days(), ChronoUnit.DAYS);
        target.ban(req.reason(), until);
        record(actor, "BAN", "user:" + targetId,
                req.reason() + (req.days() == null ? " (muddatsiz)" : " (" + req.days() + " kun)"));
        return target;
    }

    @Transactional
    public User unban(User actor, Long targetId) {
        User target = users.get(targetId);
        checkCanModerate(actor, target);
        target.unban();
        record(actor, "UNBAN", "user:" + targetId, null);
        return target;
    }

    @Transactional
    public News createNews(User actor, NewsDto.Request req) {
        News n = news.save(new News(req.category(), req.title(), req.body(), req.published(), actor.getId()));
        record(actor, "NEWS_CREATE", "news:" + n.getId(), req.title());
        return n;
    }

    @Transactional
    public News updateNews(User actor, Long id, NewsDto.Request req) {
        News n = news.findById(id).orElseThrow(() -> ApiException.notFound("Yangilik topilmadi"));
        n.update(req.category(), req.title(), req.body(), req.published());
        record(actor, "NEWS_UPDATE", "news:" + id, req.title());
        return n;
    }

    @Transactional
    public void deleteNews(User actor, Long id) {
        News n = news.findById(id).orElseThrow(() -> ApiException.notFound("Yangilik topilmadi"));
        news.delete(n);
        record(actor, "NEWS_DELETE", "news:" + id, n.getTitle());
    }

    @Transactional
    public void cancelMatch(User actor, Long matchId, String reason) {
        lobby.cancel(matchId, reason == null || reason.isBlank() ? "admin tomonidan" : reason);
        record(actor, "MATCH_CANCEL", "match:" + matchId, reason);
    }

    // ---------- SUPER_ADMIN ----------

    @Transactional
    public User changeRole(User actor, Long targetId, Role role) {
        if (actor.getId().equals(targetId)) {
            throw ApiException.badRequest("SELF", "O'z rolingizni o'zgartira olmaysiz");
        }
        User target = users.get(targetId);
        Role old = target.getRole();
        target.setRole(role);
        record(actor, "ROLE_CHANGE", "user:" + targetId, old + " → " + role);
        return target;
    }

    @Transactional
    public long adjustBalance(User actor, Long targetId, long amount, String note) {
        users.get(targetId);
        var tx = wallets.adjust(targetId, amount, "Admin: " + note);
        record(actor, "BALANCE_ADJUST", "user:" + targetId, (amount > 0 ? "+" : "") + amount + " so'm — " + note);
        return tx.getBalanceAfter();
    }

    @Transactional
    public GameServer saveServer(User actor, Long id, AdminDtos.ServerRequest req) {
        GameServer s;
        if (id == null) {
            if (req.rconPassword() == null || req.rconPassword().isBlank()) {
                throw ApiException.badRequest("RCON", "Yangi server uchun RCON parol kerak");
            }
            s = servers.save(new GameServer(req.name(), req.region(), req.ip(), req.port(), req.rconPassword()));
            if (!req.enabled()) s.update(req.name(), req.region(), req.ip(), req.port(), null, false);
            record(actor, "SERVER_CREATE", "server:" + s.getId(), s.address());
        } else {
            s = servers.findById(id).orElseThrow(() -> ApiException.notFound("Server topilmadi"));
            s.update(req.name(), req.region(), req.ip(), req.port(), req.rconPassword(), req.enabled());
            record(actor, "SERVER_UPDATE", "server:" + id, s.address());
        }
        return s;
    }

    // ---------- yordamchilar ----------

    /** ADMIN faqat PLAYER'ni, SUPER_ADMIN esa ADMIN'ni ham boshqara oladi. SUPER_ADMIN'ga tegib bo'lmaydi. */
    private static void checkCanModerate(User actor, User target) {
        if (actor.getId().equals(target.getId())) {
            throw ApiException.badRequest("SELF", "O'zingizga nisbatan bu amalni bajara olmaysiz");
        }
        if (target.getRole() == Role.SUPER_ADMIN) {
            throw ApiException.forbidden("Super adminni ban qilib bo'lmaydi");
        }
        if (target.getRole() == Role.ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw ApiException.forbidden("Adminni faqat super admin ban qila oladi");
        }
    }

    private void record(User actor, String action, String target, String details) {
        log.save(new AdminAction(actor.getId(), action, target, details));
    }
}
