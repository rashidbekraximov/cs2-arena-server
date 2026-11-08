package uz.cs2arena.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.user.User;

/** Balans bilan ishlashning yagona joyi. Har bir o'zgarish tranzaksiya sifatida yoziladi. */
@Service
public class WalletService {

    private final WalletRepository wallets;
    private final TransactionRepository transactions;

    public WalletService(WalletRepository wallets, TransactionRepository transactions) {
        this.wallets = wallets;
        this.transactions = transactions;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createFor(User user) {
        wallets.save(new Wallet(user.getId()));
    }

    @Transactional(readOnly = true)
    public long balanceOf(Long userId) {
        return wallets.findById(userId).map(Wallet::getBalance).orElse(0L);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Transaction credit(Long userId, long amount, Transaction.Type type, String description, String reference) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0 bo'lishi kerak");
        return apply(userId, amount, type, description, reference);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Transaction debit(Long userId, long amount, Transaction.Type type, String description, String reference) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0 bo'lishi kerak");
        Wallet w = lock(userId);
        if (w.getBalance() < amount) {
            throw ApiException.badRequest("INSUFFICIENT_FUNDS", "Balansda mablag' yetarli emas");
        }
        return apply(userId, -amount, type, description, reference);
    }

    /** Admin tomonidan qo'lda o'zgartirish (musbat yoki manfiy). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Transaction adjust(Long userId, long delta, String description) {
        if (delta == 0) throw ApiException.badRequest("ZERO", "Summa 0 bo'lmasligi kerak");
        Wallet w = lock(userId);
        if (w.getBalance() + delta < 0) {
            throw ApiException.badRequest("INSUFFICIENT_FUNDS", "Balans manfiy bo'lib qoladi");
        }
        return apply(userId, delta, Transaction.Type.ADMIN_ADJUST, description, null);
    }

    private Transaction apply(Long userId, long delta, Transaction.Type type, String description, String reference) {
        Wallet w = lock(userId);
        w.change(delta);
        return transactions.save(new Transaction(userId, delta, type, description, reference, w.getBalance()));
    }

    private Wallet lock(Long userId) {
        return wallets.lockByUserId(userId).orElseGet(() -> wallets.save(new Wallet(userId)));
    }
}
