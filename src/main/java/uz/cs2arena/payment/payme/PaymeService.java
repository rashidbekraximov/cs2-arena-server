package uz.cs2arena.payment.payme;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.payment.Payment;
import uz.cs2arena.payment.PaymentRepository;
import uz.cs2arena.wallet.Transaction;
import uz.cs2arena.wallet.WalletService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payme Merchant API metodlari.
 * Holatlar: 1 — yaratilgan, 2 — bajarilgan, -1 — yaratilganda bekor, -2 — bajarilgandan keyin bekor.
 */
@Service
public class PaymeService {

    private static final long TIMEOUT_MS = 12 * 60 * 60 * 1000L; // 12 soat

    private final PaymentRepository payments;
    private final WalletService wallets;
    private final AppProperties props;

    public PaymeService(PaymentRepository payments, WalletService wallets, AppProperties props) {
        this.payments = payments;
        this.wallets = wallets;
        this.props = props;
    }

    @Transactional
    public Map<String, Object> checkPerformTransaction(JsonNode params) {
        findOrder(params, params.path("amount").asLong());
        return Map.of("allow", true);
    }

    @Transactional(noRollbackFor = PaymeException.class)
    public Map<String, Object> createTransaction(JsonNode params) {
        String transId = params.path("id").asText();
        long time = params.path("time").asLong();

        var existing = payments.lockByProviderTrans(Payment.Provider.PAYME, transId);
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (p.getProviderState() != 1) {
                throw new PaymeException(PaymeException.CANNOT_PERFORM, "Tranzaksiya holati noto'g'ri");
            }
            if (System.currentTimeMillis() - p.getCreateTime() > TIMEOUT_MS) {
                p.markCancelled(-1, System.currentTimeMillis(), 4, false);
                throw new PaymeException(PaymeException.CANNOT_PERFORM, "Tranzaksiya muddati o'tgan");
            }
            return createResult(p);
        }

        Payment order = findOrder(params, params.path("amount").asLong());
        if (order.getStatus() != Payment.Status.PENDING) {
            throw new PaymeException(PaymeException.ORDER_BUSY, "Buyurtma boshqa tranzaksiya bilan band",
                    props.payme().accountField());
        }
        order.markProcessing(transId, 1, time);
        return createResult(order);
    }

    @Transactional(noRollbackFor = PaymeException.class)
    public Map<String, Object> performTransaction(JsonNode params) {
        Payment p = lockTransaction(params.path("id").asText());
        if (p.getProviderState() == 2) return performResult(p);
        if (p.getProviderState() != 1) {
            throw new PaymeException(PaymeException.CANNOT_PERFORM, "Tranzaksiyani bajarib bo'lmaydi");
        }
        if (System.currentTimeMillis() - p.getCreateTime() > TIMEOUT_MS) {
            p.markCancelled(-1, System.currentTimeMillis(), 4, false);
            throw new PaymeException(PaymeException.CANNOT_PERFORM, "Tranzaksiya muddati o'tgan");
        }
        wallets.credit(p.getUserId(), p.getAmount(), Transaction.Type.TOPUP,
                "Payme orqali to'ldirish", "payme:" + p.getProviderTransId());
        p.markPaid(2, System.currentTimeMillis());
        return performResult(p);
    }

    @Transactional
    public Map<String, Object> cancelTransaction(JsonNode params) {
        Payment p = lockTransaction(params.path("id").asText());
        int reason = params.path("reason").asInt();
        Integer state = p.getProviderState();

        if (state == 1) {
            p.markCancelled(-1, System.currentTimeMillis(), reason, false);
        } else if (state == 2) {
            // Pul allaqachon balansga tushgan — qaytarib olamiz (yetmasa, bekor qilib bo'lmaydi)
            if (wallets.balanceOf(p.getUserId()) < p.getAmount()) {
                throw new PaymeException(PaymeException.CANNOT_CANCEL, "Mablag' ishlatilgan, bekor qilib bo'lmaydi");
            }
            wallets.debit(p.getUserId(), p.getAmount(), Transaction.Type.TOPUP_REFUND,
                    "Payme to'lovi bekor qilindi", "payme:" + p.getProviderTransId());
            p.markCancelled(-2, System.currentTimeMillis(), reason, true);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("transaction", String.valueOf(p.getId()));
        r.put("cancel_time", p.getCancelTime());
        r.put("state", p.getProviderState());
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkTransaction(JsonNode params) {
        Payment p = payments.lockByProviderTrans(Payment.Provider.PAYME, params.path("id").asText())
                .orElseThrow(() -> new PaymeException(PaymeException.TRANSACTION_NOT_FOUND, "Tranzaksiya topilmadi"));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("create_time", nz(p.getCreateTime()));
        r.put("perform_time", nz(p.getPerformTime()));
        r.put("cancel_time", nz(p.getCancelTime()));
        r.put("transaction", String.valueOf(p.getId()));
        r.put("state", p.getProviderState());
        r.put("reason", p.getCancelReason());
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatement(JsonNode params) {
        List<Payment> list = payments.findForStatement(Payment.Provider.PAYME,
                params.path("from").asLong(), params.path("to").asLong());
        List<Map<String, Object>> items = list.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getProviderTransId());
            m.put("time", p.getCreateTime());
            m.put("amount", p.getAmount() * 100);
            m.put("account", Map.of(props.payme().accountField(), String.valueOf(p.getId())));
            m.put("create_time", nz(p.getCreateTime()));
            m.put("perform_time", nz(p.getPerformTime()));
            m.put("cancel_time", nz(p.getCancelTime()));
            m.put("transaction", String.valueOf(p.getId()));
            m.put("state", p.getProviderState());
            m.put("reason", p.getCancelReason());
            return m;
        }).toList();
        return Map.of("transactions", items);
    }

    // ---------- yordamchilar ----------

    private Payment findOrder(JsonNode params, long amountTiyin) {
        String field = props.payme().accountField();
        String orderId = params.path("account").path(field).asText("");
        long id;
        try {
            id = Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new PaymeException(PaymeException.ORDER_NOT_FOUND, "Buyurtma topilmadi", field);
        }
        Payment p = payments.lockById(id)
                .filter(x -> x.getProvider() == Payment.Provider.PAYME)
                .orElseThrow(() -> new PaymeException(PaymeException.ORDER_NOT_FOUND, "Buyurtma topilmadi", field));
        if (p.getAmount() * 100 != amountTiyin) {
            throw new PaymeException(PaymeException.INVALID_AMOUNT, "Summa noto'g'ri");
        }
        if (p.getStatus() == Payment.Status.PAID || p.getStatus() == Payment.Status.CANCELLED
                || p.getStatus() == Payment.Status.REFUNDED) {
            throw new PaymeException(PaymeException.ORDER_BUSY, "Buyurtma yopilgan", field);
        }
        return p;
    }

    private Payment lockTransaction(String transId) {
        return payments.lockByProviderTrans(Payment.Provider.PAYME, transId)
                .orElseThrow(() -> new PaymeException(PaymeException.TRANSACTION_NOT_FOUND, "Tranzaksiya topilmadi"));
    }

    private static Map<String, Object> createResult(Payment p) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("create_time", p.getCreateTime());
        r.put("transaction", String.valueOf(p.getId()));
        r.put("state", p.getProviderState());
        return r;
    }

    private static Map<String, Object> performResult(Payment p) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("transaction", String.valueOf(p.getId()));
        r.put("perform_time", p.getPerformTime());
        r.put("state", p.getProviderState());
        return r;
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
