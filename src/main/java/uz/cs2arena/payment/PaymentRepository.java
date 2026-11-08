package uz.cs2arena.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.provider = :provider and p.providerTransId = :transId")
    Optional<Payment> lockByProviderTrans(@Param("provider") Payment.Provider provider,
                                          @Param("transId") String transId);

    @Query("""
            select p from Payment p
            where p.provider = :provider and p.createTime between :from and :to
            order by p.createTime
            """)
    List<Payment> findForStatement(@Param("provider") Payment.Provider provider,
                                   @Param("from") long from, @Param("to") long to);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
