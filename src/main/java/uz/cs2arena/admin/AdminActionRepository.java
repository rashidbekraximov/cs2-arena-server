package uz.cs2arena.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {

    Page<AdminAction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
