package uz.cs2arena.admin;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.server.GameServerRepository;
import uz.cs2arena.user.UserService;
import uz.cs2arena.wallet.WalletService;

import java.util.List;
import java.util.Map;

/** Faqat SUPER_ADMIN (SecurityConfig: /api/admin/super/** → ROLE_SUPER_ADMIN). */
@RestController
@RequestMapping("/api/admin/super")
public class SuperAdminController {

    private final AdminService admin;
    private final UserService userService;
    private final WalletService wallets;
    private final GameServerRepository servers;

    public SuperAdminController(AdminService admin, UserService userService, WalletService wallets,
                                GameServerRepository servers) {
        this.admin = admin;
        this.userService = userService;
        this.wallets = wallets;
        this.servers = servers;
    }

    @PutMapping("/users/{id}/role")
    public AdminDtos.AdminUserDto changeRole(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                             @Valid @RequestBody AdminDtos.RoleRequest req) {
        var u = admin.changeRole(userService.get(me.id()), id, req.role());
        return AdminDtos.AdminUserDto.of(u, wallets.balanceOf(id));
    }

    @PostMapping("/users/{id}/balance")
    public Map<String, Long> adjustBalance(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                           @Valid @RequestBody AdminDtos.BalanceRequest req) {
        return Map.of("balance", admin.adjustBalance(userService.get(me.id()), id, req.amount(), req.note()));
    }

    @GetMapping("/servers")
    public List<AdminDtos.ServerDto> servers() {
        return servers.findAll().stream().map(AdminDtos.ServerDto::of).toList();
    }

    @PostMapping("/servers")
    public AdminDtos.ServerDto createServer(@AuthenticationPrincipal AuthUser me,
                                            @Valid @RequestBody AdminDtos.ServerRequest req) {
        return AdminDtos.ServerDto.of(admin.saveServer(userService.get(me.id()), null, req));
    }

    @PutMapping("/servers/{id}")
    public AdminDtos.ServerDto updateServer(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                            @Valid @RequestBody AdminDtos.ServerRequest req) {
        return AdminDtos.ServerDto.of(admin.saveServer(userService.get(me.id()), id, req));
    }
}
