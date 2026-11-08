package com.projectguard.dto.mldataset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MlOutcomeUpdateRequest {

    @NotBlank(message = "Actual outcome is required")
    @Pattern(regexp = "^(FEASIBLE|NOT_FEASIBLE)$", message = "Actual outcome must be FEASIBLE or NOT_FEASIBLE")
    private String actualOutcome;

    private String labelEvidence;
    private String labelNotes;
    private Boolean validationConfirmed;

    public MlOutcomeUpdateRequest() {}

    public String getActualOutcome() { return actualOutcome; }
    public void setActualOutcome(String actualOutcome) { this.actualOutcome = actualOutcome; }

    public String getLabelEvidence() { return labelEvidence; }
    public void setLabelEvidence(String labelEvidence) { this.labelEvidence = labelEvidence; }

    public String getLabelNotes() { return labelNotes; }
    public void setLabelNotes(String labelNotes) { this.labelNotes = labelNotes; }

    public Boolean getValidationConfirmed() { return validationConfirmed; }
    public void setValidationConfirmed(Boolean validationConfirmed) { this.validationConfirmed = validationConfirmed; }
}

