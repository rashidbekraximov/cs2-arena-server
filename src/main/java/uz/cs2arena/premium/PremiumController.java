package uz.cs2arena.premium;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.user.UserDtos;
import uz.cs2arena.wallet.WalletService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/premium")
public class PremiumController {

    public record BuyRequest(@NotBlank String plan, @NotNull PremiumService.Period period) {}

    private final PremiumService premium;
    private final WalletService wallets;
    private final AppProperties props;

    public PremiumController(PremiumService premium, WalletService wallets, AppProperties props) {
        this.premium = premium;
        this.wallets = wallets;
        this.props = props;
    }

    @GetMapping("/plans")
    public Map<String, Object> plans() {
        List<PremiumService.PlanDto> plans = premium.plans();
        return Map.of("plans", plans, "yearlyDiscountPercent", props.premium().yearlyDiscountPercent());
    }

    @PostMapping("/buy")
    public UserDtos.Me buy(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BuyRequest req) {
        var user = premium.buy(me.id(), req.plan(), req.period());
        return UserDtos.Me.of(user, wallets.balanceOf(me.id()));
    }
}
