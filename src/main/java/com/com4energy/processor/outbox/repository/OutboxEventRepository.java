package com.com4energy.processor.outbox.repository;

import java.util.List;

import com.com4energy.processor.outbox.domain.OutboxEvent;
import com.com4energy.processor.outbox.domain.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from OutboxEvent e
            where e.status = :status
              and e.lockedAt is null
            order by e.createdAt asc
            """)
    List<OutboxEvent> findPendingForUpdate(@Param("status") OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}

