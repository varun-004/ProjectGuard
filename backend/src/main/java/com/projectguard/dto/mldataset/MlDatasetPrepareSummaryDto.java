package com.projectguard.dto.mldataset;

import java.util.ArrayList;
import java.util.List;

public class MlDatasetPrepareSummaryDto {
    private long totalDatabaseRecords;
    private long completeRecords;
    private long incompleteRecords;
    private long eligibleRecords;
    private long excludedRecords;
    private long rejectedRecords;
    private long pendingOutcomeRecords;
    private long verifiedFeasibleRecords;
    private long verifiedNotFeasibleRecords;
    private long duplicateRecords;
    private List<ExcludedRecordDetail> exclusionDetails = new ArrayList<>();

    public static class ExcludedRecordDetail {
        private Long recordId;
        private Long studentProfileId;
        private Long projectId;
        private String reason;

        public ExcludedRecordDetail() {}

        public ExcludedRecordDetail(Long recordId, Long studentProfileId, Long projectId, String reason) {
            this.recordId = recordId;
            this.studentProfileId = studentProfileId;
            this.projectId = projectId;
            this.reason = reason;
        }

        public Long getRecordId() { return recordId; }
        public void setRecordId(Long recordId) { this.recordId = recordId; }

        public Long getStudentProfileId() { return studentProfileId; }
        public void setStudentProfileId(Long studentProfileId) { this.studentProfileId = studentProfileId; }

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public MlDatasetPrepareSummaryDto() {}

    public long getTotalDatabaseRecords() { return totalDatabaseRecords; }
    public void setTotalDatabaseRecords(long totalDatabaseRecords) { this.totalDatabaseRecords = totalDatabaseRecords; }

    public long getCompleteRecords() { return completeRecords; }
    public void setCompleteRecords(long completeRecords) { this.completeRecords = completeRecords; }

    public long getIncompleteRecords() { return incompleteRecords; }
    public void setIncompleteRecords(long incompleteRecords) { this.incompleteRecords = incompleteRecords; }

    public long getEligibleRecords() { return eligibleRecords; }
    public void setEligibleRecords(long eligibleRecords) { this.eligibleRecords = eligibleRecords; }

    public long getExcludedRecords() { return excludedRecords; }
    public void setExcludedRecords(long excludedRecords) { this.excludedRecords = excludedRecords; }

    public long getRejectedRecords() { return rejectedRecords; }
    public void setRejectedRecords(long rejectedRecords) { this.rejectedRecords = rejectedRecords; }

    public long getPendingOutcomeRecords() { return pendingOutcomeRecords; }
    public void setPendingOutcomeRecords(long pendingOutcomeRecords) { this.pendingOutcomeRecords = pendingOutcomeRecords; }

    public long getVerifiedFeasibleRecords() { return verifiedFeasibleRecords; }
    public void setVerifiedFeasibleRecords(long verifiedFeasibleRecords) { this.verifiedFeasibleRecords = verifiedFeasibleRecords; }

    public long getVerifiedNotFeasibleRecords() { return verifiedNotFeasibleRecords; }
    public void setVerifiedNotFeasibleRecords(long verifiedNotFeasibleRecords) { this.verifiedNotFeasibleRecords = verifiedNotFeasibleRecords; }

    public long getDuplicateRecords() { return duplicateRecords; }
    public void setDuplicateRecords(long duplicateRecords) { this.duplicateRecords = duplicateRecords; }

    public List<ExcludedRecordDetail> getExclusionDetails() { return exclusionDetails; }
    public void setExclusionDetails(List<ExcludedRecordDetail> exclusionDetails) { this.exclusionDetails = exclusionDetails; }
}

