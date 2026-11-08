package uz.cs2arena.premium;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.user.User;
import uz.cs2arena.user.UserService;
import uz.cs2arena.wallet.Transaction;
import uz.cs2arena.wallet.WalletService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PremiumService {

    public enum Period { MONTHLY, YEARLY }

    public record PlanDto(String code, String name, long monthlyPrice, long yearlyPrice) {}

    private final AppProperties props;
    private final WalletService wallets;
    private final UserService users;

    public PremiumService(AppProperties props, WalletService wallets, UserService users) {
        this.props = props;
        this.wallets = wallets;
        this.users = users;
    }

    public List<PlanDto> plans() {
        return props.premium().plans().stream()
                .map(p -> new PlanDto(p.code(), p.name(), p.monthlyPrice(), yearlyPrice(p.monthlyPrice())))
                .toList();
    }

    /** Balansdan to'lab Premium sotib olish. Faol obuna bo'lsa muddati uzaytiriladi. */
    @Transactional
    public User buy(Long userId, String planCode, Period period) {
        var plan = props.premium().plans().stream()
                .filter(p -> p.code().equals(planCode))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("PLAN", "Bunday tarif yo'q"));

        long price = period == Period.YEARLY ? yearlyPrice(plan.monthlyPrice()) : plan.monthlyPrice();
        Duration duration = period == Period.YEARLY ? Duration.ofDays(365) : Duration.ofDays(30);

        User user = users.get(userId);
        if (user.isPremiumActive() && user.getPremiumPlan() != null && !user.getPremiumPlan().equals(plan.code())) {
            throw ApiException.conflict("PLAN_ACTIVE",
                    "Sizda boshqa tarif faol. Uning muddati tugagach yangi tarifni oling.");
        }

        wallets.debit(userId, price, Transaction.Type.PREMIUM,
                plan.name() + (period == Period.YEARLY ? " (1 yil)" : " (1 oy)"), "premium:" + plan.code());

        Instant from = user.isPremiumActive() ? user.getPremiumUntil() : Instant.now();
        user.setPremiumPlan(plan.code());
        user.setPremiumUntil(from.plus(duration));
        return user;
    }

    private long yearlyPrice(long monthly) {
        return Math.round(monthly * 12 * (100 - props.premium().yearlyDiscountPercent()) / 100.0);
    }
}
