package com.com4energy.processor.service.processing;

import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileType;
import com.com4energy.processor.outbox.domain.OutboxEventType;

import java.nio.file.Path;
import java.util.List;

public record FileTypeProcessingResult(
        Status status,
        FailureReason failureReason,
        String comment,
        FileType resolvedType,
        List<DeferredOutboxEvent> deferredOutboxEvents
) {

    public FileTypeProcessingResult {
        deferredOutboxEvents = deferredOutboxEvents == null ? List.of() : List.copyOf(deferredOutboxEvents);
    }

    public static FileTypeProcessingResult success() {
        return success(List.of());
    }

    public static FileTypeProcessingResult success(List<DeferredOutboxEvent> deferredOutboxEvents) {
        return new FileTypeProcessingResult(Status.SUCCEEDED, null, null, null, deferredOutboxEvents);
    }

    public static FileTypeProcessingResult success(FileType resolvedType) {
        return new FileTypeProcessingResult(Status.SUCCEEDED, null, null, resolvedType, List.of());
    }

    public static FileTypeProcessingResult failed(FailureReason failureReason, String comment) {
        return failed(failureReason, comment, List.of());
    }

    public static FileTypeProcessingResult failed(
            FailureReason failureReason,
            String comment,
            List<DeferredOutboxEvent> deferredOutboxEvents
    ) {
        return new FileTypeProcessingResult(Status.FAILED, failureReason, comment, null, deferredOutboxEvents);
    }

    public static FileTypeProcessingResult rejected(FailureReason failureReason, String comment) {
        return rejected(failureReason, comment, List.of());
    }

    public static FileTypeProcessingResult rejected(
            FailureReason failureReason,
            String comment,
            List<DeferredOutboxEvent> deferredOutboxEvents
    ) {
        return new FileTypeProcessingResult(Status.REJECTED, failureReason, comment, null, deferredOutboxEvents);
    }

    public enum Status {
        SUCCEEDED,
        FAILED,
        REJECTED
    }

    public record DeferredOutboxEvent(
            OutboxEventType eventType,
            String phase,
            Integer incidentCount,
            Integer failedRecordCount,
            Path reportPath
    ) {
        public static DeferredOutboxEvent defectReportCreated(String phase, int incidentCount, Path reportPath) {
            return new DeferredOutboxEvent(
                    OutboxEventType.FILE_DEFECT_REPORT_CREATED,
                    phase,
                    incidentCount,
                    null,
                    reportPath
            );
        }

        public static DeferredOutboxEvent persistenceQuarantine(int failedRecordCount, Path reportPath) {
            return new DeferredOutboxEvent(
                    OutboxEventType.FILE_PERSISTENCE_QUARANTINE,
                    null,
                    null,
                    failedRecordCount,
                    reportPath
            );
        }
    }
}

