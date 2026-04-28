package com.com4energy.processor.model.measure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "medidaqh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedidaQHEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medidaQH")
    private Long id;

    @Column(name = "id_cliente", nullable = false)
    private Long clienteId;

    @Column(name = "tipomed")
    private Integer tipoMed;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "bandera_inv_ver")
    private Integer banderaInvVer;

    @Column(name = "actent")
    private Integer actent;

    @Column(name = "qactent")
    private Integer qactent;

    @Column(name = "actsal")
    private Integer actsal;

    @Column(name = "qactsal")
    private Integer qactsal;

    @Column(name = "r_q1")
    private Integer rq1;

    @Column(name = "qr_q1")
    private Integer qrq1;

    @Column(name = "r_q2")
    private Integer rq2;

    @Column(name = "qr_q2")
    private Integer qrq2;

    @Column(name = "r_q3")
    private Integer rq3;

    @Column(name = "qr_q3")
    private Integer qrq3;

    @Column(name = "r_q4")
    private Integer rq4;

    @Column(name = "qr_q4")
    private Integer qrq4;

    @Column(name = "medres1")
    private Integer medres1;

    @Column(name = "qmedres1")
    private Integer qmedres1;

    @Column(name = "medres2")
    private Integer medres2;

    @Column(name = "qmedres2")
    private Integer qmedres2;

    @Column(name = "metod_obt")
    private Integer metodObt;

    @Column(name = "origen")
    private String origen;

    @Column(name = "temporal")
    private Integer temporal;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;
}

