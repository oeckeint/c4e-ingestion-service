package com.com4energy.processor.repository.measure;

import com.com4energy.processor.model.measure.MedidaCCHEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedidaCCHRepository extends JpaRepository<MedidaCCHEntity, Long> {
}

