package uz.cs2arena.user;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.wallet.WalletService;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService users;
    private final WalletService wallets;

    public UserController(UserService users, WalletService wallets) {
        this.users = users;
        this.wallets = wallets;
    }

    @GetMapping("/me")
    public UserDtos.Me me(@AuthenticationPrincipal AuthUser me) {
        if (me == null) return UserDtos.Me.anonymous();
        return UserDtos.Me.of(users.get(me.id()), wallets.balanceOf(me.id()));
    }

    @PatchMapping("/me/settings")
    public UserDtos.Me updateSettings(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody UserDtos.UpdateSettingsRequest req) {
        return UserDtos.Me.of(users.updateSettings(me.id(), req), wallets.balanceOf(me.id()));
    }

    @PostMapping("/me/sync-steam")
    public UserDtos.Me syncSteam(@AuthenticationPrincipal AuthUser me) {
        return UserDtos.Me.of(users.syncFromSteam(me.id()), wallets.balanceOf(me.id()));
    }
}
