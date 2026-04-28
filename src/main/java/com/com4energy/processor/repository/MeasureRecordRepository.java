package com.com4energy.processor.repository;

import com.com4energy.processor.model.MeasureRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeasureRecordRepository extends JpaRepository<MeasureRecordEntity, Long> {

    void deleteByFileRecordId(Long fileRecordId);
}

