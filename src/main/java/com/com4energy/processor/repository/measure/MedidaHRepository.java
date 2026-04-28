package com.com4energy.processor.repository.measure;

import com.com4energy.processor.model.measure.MedidaHEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedidaHRepository extends JpaRepository<MedidaHEntity, Long> {

    boolean existsByOrigen(String origen);
}

