package com.com4energy.processor.repository;

import com.com4energy.processor.model.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {

	List<ClienteEntity> findByCups(String cups);

}
