package com.com4energy.processor.repository.measure;

import com.com4energy.processor.model.measure.MedidaLegacyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedidaLegacyRepository extends JpaRepository<MedidaLegacyEntity, Long> {
}

