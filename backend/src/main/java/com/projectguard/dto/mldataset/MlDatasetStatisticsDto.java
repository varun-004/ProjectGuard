package com.projectguard.dto.mldataset;

public class MlDatasetStatisticsDto {
    private long totalRecords;
    private long completeRecords;
    private long incompleteRecords;
    private long pendingOutcomeRecords;
    private long verifiedFeasibleRecords;
    private long verifiedNotFeasibleRecords;
    private long trainingReadyRecords;
    private long duplicateRecords;

    public MlDatasetStatisticsDto() {}

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getCompleteRecords() { return completeRecords; }
    public void setCompleteRecords(long completeRecords) { this.completeRecords = completeRecords; }

    public long getIncompleteRecords() { return incompleteRecords; }
    public void setIncompleteRecords(long incompleteRecords) { this.incompleteRecords = incompleteRecords; }

    public long getPendingOutcomeRecords() { return pendingOutcomeRecords; }
    public void setPendingOutcomeRecords(long pendingOutcomeRecords) { this.pendingOutcomeRecords = pendingOutcomeRecords; }

    public long getVerifiedFeasibleRecords() { return verifiedFeasibleRecords; }
    public void setVerifiedFeasibleRecords(long verifiedFeasibleRecords) { this.verifiedFeasibleRecords = verifiedFeasibleRecords; }

    public long getVerifiedNotFeasibleRecords() { return verifiedNotFeasibleRecords; }
    public void setVerifiedNotFeasibleRecords(long verifiedNotFeasibleRecords) { this.verifiedNotFeasibleRecords = verifiedNotFeasibleRecords; }

    public long getTrainingReadyRecords() { return trainingReadyRecords; }
    public void setTrainingReadyRecords(long trainingReadyRecords) { this.trainingReadyRecords = trainingReadyRecords; }

    public long getDuplicateRecords() { return duplicateRecords; }
    public void setDuplicateRecords(long duplicateRecords) { this.duplicateRecords = duplicateRecords; }
}

