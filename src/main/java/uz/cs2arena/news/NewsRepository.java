package uz.cs2arena.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

    List<News> findTop3ByPublishedTrueOrderByCreatedAtDesc();

    Page<News> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<News> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
