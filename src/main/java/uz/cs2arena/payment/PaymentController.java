package uz.cs2arena.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.config.AppProperties;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Balansni to'ldirish: buyurtma yaratadi va to'lov sahifasiga havola qaytaradi. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    public record TopupRequest(@NotNull Payment.Provider provider, @Positive long amount) {}

    public record TopupResponse(Long paymentId, String checkoutUrl) {}

    private final PaymentRepository payments;
    private final AppProperties props;

    public PaymentController(PaymentRepository payments, AppProperties props) {
        this.payments = payments;
        this.props = props;
    }

    @GetMapping("/methods")
    public Map<String, Object> methods() {
        return Map.of(
                "payme", props.payme().enabled(),
                "click", props.click().enabled(),
                "min", props.topup().minAmount(),
                "max", props.topup().maxAmount());
    }

    @PostMapping("/topup")
    @Transactional
    public TopupResponse topup(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TopupRequest req) {
        if (req.amount() < props.topup().minAmount() || req.amount() > props.topup().maxAmount()) {
            throw ApiException.badRequest("AMOUNT", "Summa " + props.topup().minAmount() + " va "
                    + props.topup().maxAmount() + " so'm oralig'ida bo'lishi kerak");
        }
        Payment p = payments.save(new Payment(me.id(), req.provider(), req.amount()));
        String returnUrl = props.baseUrlClean() + "/?topup=" + p.getId();
        String url = switch (req.provider()) {
            case PAYME -> paymeUrl(p, returnUrl);
            case CLICK -> clickUrl(p, returnUrl);
        };
        return new TopupResponse(p.getId(), url);
    }

    private String paymeUrl(Payment p, String returnUrl) {
        var cfg = props.payme();
        if (!cfg.enabled() || blank(cfg.merchantId())) {
            throw ApiException.badRequest("PAYME_DISABLED", "Payme hali sozlanmagan");
        }
        String params = "m=" + cfg.merchantId()
                + ";ac." + cfg.accountField() + "=" + p.getId()
                + ";a=" + (p.getAmount() * 100)   // tiyinda
                + ";c=" + returnUrl;
        String host = cfg.testMode() ? "https://test.paycom.uz/" : "https://checkout.paycom.uz/";
        return host + Base64.getEncoder().encodeToString(params.getBytes(StandardCharsets.UTF_8));
    }

    private String clickUrl(Payment p, String returnUrl) {
        var cfg = props.click();
        if (!cfg.enabled() || blank(cfg.serviceId()) || blank(cfg.merchantId())) {
            throw ApiException.badRequest("CLICK_DISABLED", "Click hali sozlanmagan");
        }
        return "https://my.click.uz/services/pay?service_id=" + cfg.serviceId()
                + "&merchant_id=" + cfg.merchantId()
                + "&amount=" + p.getAmount()
                + "&transaction_param=" + p.getId()
                + "&return_url=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
