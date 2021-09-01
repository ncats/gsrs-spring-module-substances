package gsrs.module.substance.approval;

import ix.ginas.models.v1.Substance;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public interface ApprovalService {

    ApprovalResult approve(Substance s) throws ApprovalException;

    @Data
    @Builder
    class ApprovalResult {
        private String generatorName;
        private String approvalId;
        private Substance substance;
        private LocalDateTime approvalDateTime;
        private String approvedBy;

    }

    class ApprovalException extends Exception{
        public ApprovalException(String message) {
            super(message);
        }

        public ApprovalException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
