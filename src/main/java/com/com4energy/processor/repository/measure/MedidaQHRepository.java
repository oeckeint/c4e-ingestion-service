package com.com4energy.processor.repository.measure;

import com.com4energy.processor.model.measure.MedidaQHEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedidaQHRepository extends JpaRepository<MedidaQHEntity, Long> {

    boolean existsByOrigen(String origen);
}

