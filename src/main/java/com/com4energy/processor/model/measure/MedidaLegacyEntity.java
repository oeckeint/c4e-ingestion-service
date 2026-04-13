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
@Table(name = "medida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedidaLegacyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medida")
    private Long id;

    @Column(name = "id_cliente", nullable = false)
    private Long clienteId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "bandera_inv_ver")
    private Integer banderaInvVer;

    @Column(name = "ae1")
    private Integer ae1;

    @Column(name = "as1")
    private Integer as1;

    @Column(name = "r_q1")
    private Integer rq1;

    @Column(name = "r_q2")
    private Integer rq2;

    @Column(name = "r_q3")
    private Integer rq3;

    @Column(name = "r_q4")
    private Integer rq4;

    @Column(name = "metod_obt")
    private Integer metodObt;

    @Column(name = "indic_firmez")
    private Integer indicFirmez;

    @Column(name = "codigo_factura")
    private String codigoFactura;

    @Column(name = "medida_col")
    private String medidaCol;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;
}

