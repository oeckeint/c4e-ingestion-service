package com.com4energy.processor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class ClienteEntity {

    @Id
    @Column(name = "id_cliente")
    private Long id;

    @Column(name = "cups")
    private String cups;

    @Column(name = "tarifa")
    private String tarifa;

    @Column(name = "is_deleted")
    private Short isDeleted;

}
