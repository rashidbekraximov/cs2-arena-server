package uz.cs2arena.admin;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.common.PageDto;
import uz.cs2arena.match.MatchDtos;
import uz.cs2arena.match.MatchQueryService;
import uz.cs2arena.match.MatchRepository;
import uz.cs2arena.match.MatchStatus;
import uz.cs2arena.news.NewsDto;
import uz.cs2arena.news.NewsRepository;
import uz.cs2arena.payment.PaymentRepository;
import uz.cs2arena.user.UserRepository;
import uz.cs2arena.user.UserService;
import uz.cs2arena.wallet.WalletService;

/** ADMIN va SUPER_ADMIN uchun (SecurityConfig: /api/admin/** → ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final UserService userService;
    private final UserRepository users;
    private final WalletService wallets;
    private final NewsRepository news;
    private final MatchRepository matches;
    private final MatchQueryService matchQuery;
    private final PaymentRepository payments;
    private final AdminActionRepository actions;

    public AdminController(AdminService admin, UserService userService, UserRepository users, WalletService wallets,
                           NewsRepository news, MatchRepository matches, MatchQueryService matchQuery,
                           PaymentRepository payments, AdminActionRepository actions) {
        this.admin = admin;
        this.userService = userService;
        this.users = users;
        this.wallets = wallets;
        this.news = news;
        this.matches = matches;
        this.matchQuery = matchQuery;
        this.payments = payments;
        this.actions = actions;
    }

    // ---------- Foydalanuvchilar ----------

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public PageDto<AdminDtos.AdminUserDto> users(@RequestParam(defaultValue = "") String q,
                                                 @RequestParam(defaultValue = "0") int page) {
        var pageable = PageRequest.of(Math.max(page, 0), 20);
        var result = q.isBlank() ? users.findAll(pageable) : users.search(q.trim(), pageable);
        return PageDto.of(result, u -> AdminDtos.AdminUserDto.of(u, wallets.balanceOf(u.getId())));
    }

    @PostMapping("/users/{id}/ban")
    public AdminDtos.AdminUserDto ban(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                      @Valid @RequestBody AdminDtos.BanRequest req) {
        var u = admin.ban(userService.get(me.id()), id, req);
        return AdminDtos.AdminUserDto.of(u, wallets.balanceOf(id));
    }

    @PostMapping("/users/{id}/unban")
    public AdminDtos.AdminUserDto unban(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        var u = admin.unban(userService.get(me.id()), id);
        return AdminDtos.AdminUserDto.of(u, wallets.balanceOf(id));
    }

    // ---------- Yangiliklar ----------

    @GetMapping("/news")
    public PageDto<NewsDto> news(@RequestParam(defaultValue = "0") int page) {
        return PageDto.of(news.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), 20)), NewsDto::of);
    }

    @PostMapping("/news")
    public NewsDto createNews(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody NewsDto.Request req) {
        return NewsDto.of(admin.createNews(userService.get(me.id()), req));
    }

    @PutMapping("/news/{id}")
    public NewsDto updateNews(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                              @Valid @RequestBody NewsDto.Request req) {
        return NewsDto.of(admin.updateNews(userService.get(me.id()), id, req));
    }

    @DeleteMapping("/news/{id}")
    public ResponseEntity<Void> deleteNews(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        admin.deleteNews(userService.get(me.id()), id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Matchlar ----------

    @GetMapping("/matches")
    @Transactional(readOnly = true)
    public PageDto<MatchDtos.MatchCard> matches(@RequestParam(required = false) MatchStatus status,
                                                @RequestParam(defaultValue = "0") int page) {
        var pageable = PageRequest.of(Math.max(page, 0), 20);
        var result = status == null ? matches.findAllByOrderByCreatedAtDesc(pageable)
                : matches.findByStatusOrderByCreatedAtDesc(status, pageable);
        var cards = matchQuery.cards(result.getContent());
        return new PageDto<>(cards, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @PostMapping("/matches/{id}/cancel")
    public ResponseEntity<Void> cancelMatch(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                            @Valid @RequestBody(required = false) AdminDtos.CancelRequest req) {
        admin.cancelMatch(userService.get(me.id()), id, req == null ? null : req.reason());
        return ResponseEntity.noContent().build();
    }

    // ---------- To'lovlar va jurnal ----------

    @GetMapping("/payments")
    public PageDto<AdminDtos.PaymentDto> payments(@RequestParam(defaultValue = "0") int page) {
        return PageDto.of(payments.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), 20)),
                AdminDtos.PaymentDto::of);
    }

    @GetMapping("/actions")
    public PageDto<AdminDtos.ActionDto> actions(@RequestParam(defaultValue = "0") int page) {
        return PageDto.of(actions.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), 30)),
                AdminDtos.ActionDto::of);
    }
}
