package com.com4energy.processor.repository;

import com.com4energy.processor.model.ClienteEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {

    @Query("""
            select c.id as id, c.tarifa as tarifa
            from ClienteEntity c
            where length(:cups) >= 20
              and c.cups like concat(substring(:cups, 1, 20), '%')
            """)
    List<ClienteLookupView> findLookupByCups(@Param("cups") String cups, Pageable pageable);

    interface ClienteLookupView {
        Long getId();
        String getTarifa();
    }

}
