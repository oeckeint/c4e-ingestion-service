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
@Table(name = "medida_h")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedidaHEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medida_h")
    private Long id;

    @Column(name = "id_cliente", nullable = false)
    private Long clienteId;

    @Column(name = "tipo_medida")
    private Integer tipoMedida;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "bandera_inv_ver")
    private Double banderaInvVer;

    @Column(name = "actent")
    private Double actent;

    @Column(name = "qactent")
    private Double qactent;

    @Column(name = "actsal")
    private Double actsal;

    @Column(name = "qactsal")
    private Double qactsal;

    @Column(name = "r_q1")
    private Double rq1;

    @Column(name = "qr_q1")
    private Double qrq1;

    @Column(name = "r_q2")
    private Double rq2;

    @Column(name = "qr_q2")
    private Double qrq2;

    @Column(name = "r_q3")
    private Double rq3;

    @Column(name = "qr_q3")
    private Double qrq3;

    @Column(name = "r_q4")
    private Double rq4;

    @Column(name = "qr_q4")
    private Double qrq4;

    @Column(name = "medres1")
    private Double medres1;

    @Column(name = "qmedres1")
    private Double qmedres1;

    @Column(name = "medres2")
    private Double medres2;

    @Column(name = "qmedres2")
    private Double qmedres2;

    @Column(name = "metod_obt")
    private Integer metodObt;

    @Column(name = "temporal")
    private Integer temporal;

    @Column(name = "origen")
    private String origen;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;
}

