package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.GuestbookLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestbookLikeRepository extends JpaRepository<GuestbookLike, Long> {

    Optional<GuestbookLike> findByEntry_IdAndClientKey(Long entryId, String clientKey);

    boolean existsByEntry_IdAndClientKey(Long entryId, String clientKey);
}
