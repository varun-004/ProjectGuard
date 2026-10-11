package com.projectguard.service;

import com.projectguard.entity.Project;
import com.projectguard.entity.ProjectDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectQualityGateServiceTest {

    private ProjectQualityGateService qualityGateService;

    @BeforeEach
    void setUp() {
        qualityGateService = new ProjectQualityGateService();
    }

    @Test
    void testGenuineRealWorldProject_PassesQualityGate() {
        Project project = new Project();
        project.setTitle("Clinical Decision Support for Early Sepsis Risk Stratification");
        project.setProblemStatement("Sepsis claims over 11 million lives annually worldwide. Mortality increases by 7.6% for every hour antimicrobials are delayed after septic shock onset, with high false alarms in ICU monitors.");
        project.setTargetStakeholders("ICU intensivists, clinical nursing staff, and hospital patient safety officers managing continuous bedside telemetry monitors.");
        project.setProposedSolution("Sliding-window feature extraction on multi-channel vitals, missing-data imputation via forward-fill and spline interpolation, followed by an ensemble gradient boosting classifier.");
        project.setExpectedOutcomeKpis("Area Under ROC Curve (AUC-ROC) >= 0.86; 4-hour lead time prior to clinical sepsis onset; reduction in false alert frequency by >= 35%.");
        project.setFunctionalRequirements("FR1: Ingest hourly physiological telemetry feeds. FR2: Handle irregular sampling and missing labs. FR3: Generate risk probability.");
        project.setEstimatedWeeks(16);
        project.setMinTeamSize(2);
        project.setMaxTeamSize(4);
        project.setTestingValidation("5-fold cross-validation with group-splitting by patient ID to prevent data leakage; calibration curves (Brier score < 0.12).");
        project.setSecurityPrivacySafety("HIPAA-compliant de-identification; encrypted data stores (AES-256); role-based clinician authorization.");

        ProjectDomain domain = new ProjectDomain();
        domain.setName("AI / Machine Learning");
        project.setDomain(domain);

        ProjectQualityGateService.QualityGateResult result = qualityGateService.evaluate(project);

        assertTrue(result.isPassed(), "Genuine real-world project must pass quality gate");
        assertTrue(result.getQualityScore() >= 75.0, "Quality score should be at least 75");
        assertTrue(result.getEvaluationSummary().contains("Passed Quality Gate"));
    }

    @Test
    void testGenericCrudProject_FailsQualityGate() {
        Project project = new Project();
        project.setTitle("Simple Student Management System");
        project.setProblemStatement("Basic management system and CRUD only application for college students.");
        project.setTargetStakeholders("College students.");
        project.setProposedSolution("Simple forms to insert and delete records from MySQL database.");
        project.setExpectedOutcomeKpis("Good system to manage records.");
        project.setFunctionalRequirements("Add student, delete student.");
        project.setEstimatedWeeks(2); // Too low
        project.setTestingValidation("Manual testing.");

        ProjectQualityGateService.QualityGateResult result = qualityGateService.evaluate(project);

        assertFalse(result.isPassed(), "Generic CRUD clone must fail quality gate");
        assertTrue(result.getQualityScore() < 60.0, "Quality score should be below 60");
        assertTrue(result.getFlags().stream().anyMatch(f -> f.contains("Rejected: Generic CRUD") || f.contains("too low")));
    }

    @Test
    void testMissingStakeholderAndKpis_PenalizedAndFails() {
        Project project = new Project();
        project.setTitle("IoT Sensor Project");
        project.setProblemStatement("Sensors collect temperature data across a room.");
        project.setTargetStakeholders("Users"); // Too brief
        project.setProposedSolution("Connect sensors to Arduino and read temperature.");
        project.setExpectedOutcomeKpis("Shows temp"); // Too brief
        project.setFunctionalRequirements("Read sensor.");
        project.setEstimatedWeeks(8);
        project.setTestingValidation("Check readings.");

        ProjectQualityGateService.QualityGateResult result = qualityGateService.evaluate(project);

        assertFalse(result.isPassed(), "Missing stakeholder and KPIs must fail quality gate");
        assertTrue(result.getFlags().stream().anyMatch(f -> f.contains("stakeholders") || f.contains("KPIs")));
    }
}
