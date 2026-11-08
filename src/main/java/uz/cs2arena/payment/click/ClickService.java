package uz.cs2arena.payment.click;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.payment.Payment;
import uz.cs2arena.payment.PaymentRepository;
import uz.cs2arena.wallet.Transaction;
import uz.cs2arena.wallet.WalletService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Click SHOP API: Prepare (action=0) va Complete (action=1).
 * Xato kodlari: -1 imzo, -2 summa, -4 allaqachon to'langan, -5 buyurtma yo'q, -6 tranzaksiya yo'q, -9 bekor qilingan.
 */
@Service
public class ClickService {

    private final PaymentRepository payments;
    private final WalletService wallets;
    private final AppProperties props;

    public ClickService(PaymentRepository payments, WalletService wallets, AppProperties props) {
        this.payments = payments;
        this.wallets = wallets;
        this.props = props;
    }

    @Transactional
    public Map<String, Object> prepare(Map<String, String> p) {
        String sign = md5(p.get("click_trans_id") + p.get("service_id") + props.click().secretKey()
                + p.get("merchant_trans_id") + p.get("amount") + p.get("action") + p.get("sign_time"));
        if (!sign.equalsIgnoreCase(p.getOrDefault("sign_string", ""))) return result(p, -1, "SIGN CHECK FAILED", null);
        if (!"0".equals(p.get("action"))) return result(p, -3, "Action not found", null);

        Payment payment = find(p.get("merchant_trans_id"));
        if (payment == null) return result(p, -5, "Order not found", null);
        if (payment.getStatus() == Payment.Status.PAID) return result(p, -4, "Already paid", null);
        if (payment.getStatus() == Payment.Status.CANCELLED) return result(p, -9, "Transaction cancelled", null);
        if (!amountMatches(payment, p.get("amount"))) return result(p, -2, "Incorrect parameter amount", null);

        payment.markProcessing(p.get("click_trans_id"), 0, System.currentTimeMillis());
        return result(p, 0, "Success", payment.getId());
    }

    @Transactional
    public Map<String, Object> complete(Map<String, String> p) {
        String sign = md5(p.get("click_trans_id") + p.get("service_id") + props.click().secretKey()
                + p.get("merchant_trans_id") + p.get("merchant_prepare_id") + p.get("amount")
                + p.get("action") + p.get("sign_time"));
        if (!sign.equalsIgnoreCase(p.getOrDefault("sign_string", ""))) return result(p, -1, "SIGN CHECK FAILED", null);
        if (!"1".equals(p.get("action"))) return result(p, -3, "Action not found", null);

        Payment payment = find(p.get("merchant_prepare_id"));
        if (payment == null || !p.get("click_trans_id").equals(payment.getProviderTransId())) {
            return result(p, -6, "Transaction does not exist", null);
        }
        if (payment.getStatus() == Payment.Status.PAID) return result(p, -4, "Already paid", payment.getId());
        if (payment.getStatus() == Payment.Status.CANCELLED) return result(p, -9, "Transaction cancelled", null);
        if (!amountMatches(payment, p.get("amount"))) return result(p, -2, "Incorrect parameter amount", null);

        int clickError = parseInt(p.get("error"));
        if (clickError < 0) {
            payment.markCancelled(-1, System.currentTimeMillis(), clickError, false);
            return result(p, -9, "Transaction cancelled", null);
        }

        wallets.credit(payment.getUserId(), payment.getAmount(), Transaction.Type.TOPUP,
                "Click orqali to'ldirish", "click:" + payment.getProviderTransId());
        payment.markPaid(1, System.currentTimeMillis());
        return result(p, 0, "Success", payment.getId());
    }

    private Payment find(String id) {
        try {
            return payments.lockById(Long.parseLong(id))
                    .filter(x -> x.getProvider() == Payment.Provider.CLICK)
                    .orElse(null);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    private static boolean amountMatches(Payment payment, String amount) {
        try {
            return new BigDecimal(amount).compareTo(BigDecimal.valueOf(payment.getAmount())) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Map<String, Object> result(Map<String, String> p, int error, String note, Long merchantId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("click_trans_id", p.get("click_trans_id"));
        r.put("merchant_trans_id", p.get("merchant_trans_id"));
        if ("0".equals(p.get("action"))) r.put("merchant_prepare_id", merchantId);
        else r.put("merchant_confirm_id", merchantId);
        r.put("error", error);
        r.put("error_note", note);
        return r;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
