package com.com4energy.processor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "measure_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_measure_file_record_line", columnNames = {"file_record_id", "line_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasureRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fileRecordId;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = false, length = 64)
    private String cups;

    @Column(nullable = false)
    private LocalDateTime measureTimestamp;

    private Integer tipoMedida;
    private Double banderaInvVer;
    private Double actent;
    private Double qactent;
    private Double actsal;
    private Double qactsal;
    private Double rq1;
    private Double qrq1;
    private Double rq2;
    private Double qrq2;
    private Double rq3;
    private Double qrq3;
    private Double rq4;
    private Double qrq4;
    private Double medres1;
    private Double qmedres1;
    private Double medres2;
    private Double qmedres2;
    private Integer metodObt;
    private Integer temporal;
    private Integer indicFirmez;
    private String codigoFactura;
    private String origen;
}

