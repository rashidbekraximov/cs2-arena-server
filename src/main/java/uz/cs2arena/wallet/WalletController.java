package uz.cs2arena.wallet;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.cs2arena.auth.AuthUser;
import uz.cs2arena.common.PageDto;

import java.time.Instant;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    public record TransactionDto(Long id, long amount, String type, String description, long balanceAfter,
                                 Instant createdAt) {
        static TransactionDto of(Transaction t) {
            return new TransactionDto(t.getId(), t.getAmount(), t.getType().name(), t.getDescription(),
                    t.getBalanceAfter(), t.getCreatedAt());
        }
    }

    private final WalletService wallets;
    private final TransactionRepository transactions;

    public WalletController(WalletService wallets, TransactionRepository transactions) {
        this.wallets = wallets;
        this.transactions = transactions;
    }

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public PageDto<TransactionDto> transactions(@AuthenticationPrincipal AuthUser me,
                                                @RequestParam(defaultValue = "0") int page) {
        var p = transactions.findByUserIdOrderByCreatedAtDesc(me.id(), PageRequest.of(Math.max(page, 0), 20));
        return PageDto.of(p, TransactionDto::of);
    }
}
