package com.projectguard.service;

import com.projectguard.dto.mldataset.*;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.*;
import com.projectguard.exception.BadRequestException;
import com.projectguard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MlDatasetServiceTest {

    @Mock
    private MlDatasetRepository mlDatasetRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssessmentResultRepository assessmentResultRepository;

    @InjectMocks
    private MlDatasetService mlDatasetService;

    private User studentUser;
    private StudentProfile studentProfile;
    private Project project;
    private Skill pythonSkill;
    private Skill mlSkill;
    private Technology pythonMlTech;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setUsername("teststudent");
        studentUser.setEmail("student@testguard.com");
        studentUser.setRole(Role.STUDENT);

        EngineeringBranch branch = new EngineeringBranch();
        branch.setId(10L);
        branch.setName("Computer Science and Engineering");

        ProjectDomain domain = new ProjectDomain();
        domain.setId(20L);
        domain.setName("AI / Machine Learning");

        pythonSkill = new Skill();
        pythonSkill.setId(101L);
        pythonSkill.setName("Python");

        mlSkill = new Skill();
        mlSkill.setId(102L);
        mlSkill.setName("Machine Learning");

        pythonMlTech = new Technology();
        pythonMlTech.setId(300L);
        pythonMlTech.setName("Python ML");
        pythonMlTech.setSkills(List.of(pythonSkill, mlSkill));

        studentProfile = new StudentProfile();
        studentProfile.setId(5L);
        studentProfile.setUser(studentUser);
        studentProfile.setBranch(branch);
        studentProfile.setDomain(domain);
        studentProfile.setTechnology(pythonMlTech);
        studentProfile.setTeamSize(3);
        studentProfile.setAvailableTimeWeeks(16);

        StudentSkill ss1 = new StudentSkill();
        ss1.setStudentProfile(studentProfile);
        ss1.setSkill(pythonSkill);
        ss1.setProficiencyLevel(ProficiencyLevel.ADVANCED);
        ss1.setYearsOfExperience(2.5);

        StudentSkill ss2 = new StudentSkill();
        ss2.setStudentProfile(studentProfile);
        ss2.setSkill(mlSkill);
        ss2.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        ss2.setYearsOfExperience(1.0);

        studentProfile.setStudentSkills(new ArrayList<>(List.of(ss1, ss2)));

        project = new Project();
        project.setId(500L);
        project.setTitle("Autonomous Drone Obstacle Avoidance");
        project.setDomain(domain);
        project.setComplexityLevel("ADVANCED");
        project.setMinTeamSize(2);
        project.setMaxTeamSize(4);
        project.setEstimatedWeeks(16);
        project.setFunctionalRequirements("Computer vision pipeline, LiDAR SLAM, real-time trajectory planner");
        project.setNonFunctionalRequirements("Sub-50ms inference latency, fail-safe RTL trigger");
        project.setRequiredSkills(List.of(pythonSkill, mlSkill));

        ProjectTechnology pt = new ProjectTechnology();
        pt.setProject(project);
        pt.setTechnology(pythonMlTech);
        project.setProjectTechnologies(List.of(pt));
    }

    @Test
    void testAutomaticRecordSnapshot_DerivesAllFeaturesAndPendingOutcome() {
        when(mlDatasetRepository.findByStudentProfileIdAndProjectId(5L, 500L))
                .thenReturn(Optional.empty());
        when(mlDatasetRepository.save(any(MlDataset.class)))
                .thenAnswer(invocation -> {
                    MlDataset d = invocation.getArgument(0);
                    d.setId(999L);
                    return d;
                });

        MlDataset snapshot = mlDatasetService.recordDatasetSnapshot(
                studentProfile,
                project,
                20.0,
                null,
                null,
                null,
                null,
                "AUTOMATIC_COLLECTION"
        );

        assertNotNull(snapshot);
        assertEquals(999L, snapshot.getId());

        // Feature 1: skill level (ADVANCED=3.0, INTERMEDIATE=2.0 -> avg 2.5)
        assertEquals(2.5, snapshot.getStudentSkillLevel());

        // Feature 2: skill gap (all required skills matched -> gap should be small or 0)
        assertNotNull(snapshot.getSkillGap());
        assertTrue(snapshot.getSkillGap() >= 0.0 && snapshot.getSkillGap() <= 25.0);

        // Feature 3: complexity (ADVANCED -> HARD)
        assertEquals(ProjectComplexity.HARD, snapshot.getProjectComplexity());

        // Feature 4: team size (from profile)
        assertEquals(3, snapshot.getTeamSize());

        // Feature 5: available hours per week
        assertEquals(20.0, snapshot.getAvailableHoursPerWeek());

        // Feature 6: technology familiarity
        assertNotNull(snapshot.getTechnologyFamiliarity());
        assertTrue(snapshot.getTechnologyFamiliarity() >= 1.0 && snapshot.getTechnologyFamiliarity() <= 4.0);

        // Feature 7: number of requirements
        assertTrue(snapshot.getNumberOfRequirements() >= 2);

        // Feature 8: number of features
        assertTrue(snapshot.getNumberOfFeatures() >= 1);

        // Feature 9: previous experience (max 2.5 from skills)
        assertEquals(2.5, snapshot.getPreviousExperience());

        // Feature 10: estimated effort (16 weeks * 20 hrs = 320 hrs)
        assertEquals(320.0, snapshot.getEstimatedEffort());

        // Target: pending outcome, actualOutcome null
        assertEquals(MlOutcomeStatus.PENDING_OUTCOME, snapshot.getOutcomeStatus());
        assertNull(snapshot.getActualOutcome(), "Actual outcome must remain null until verified by evaluator");
    }

    @Test
    void testDuplicatePrevention_UpdatesExistingSnapshot() {
        MlDataset existing = new MlDataset();
        existing.setId(77L);
        existing.setStudentProfile(studentProfile);
        existing.setProject(project);
        existing.setOutcomeStatus(MlOutcomeStatus.PENDING_OUTCOME);

        when(mlDatasetRepository.findByStudentProfileIdAndProjectId(5L, 500L))
                .thenReturn(Optional.of(existing));
        when(mlDatasetRepository.save(any(MlDataset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MlDataset updated = mlDatasetService.recordDatasetSnapshot(
                studentProfile,
                project,
                25.0,
                4,
                null,
                null,
                null,
                "PROJECT_REGISTRATION"
        );

        assertEquals(77L, updated.getId(), "Should update existing snapshot without creating duplicate");
        assertEquals(4, updated.getTeamSize());
        assertEquals(25.0, updated.getAvailableHoursPerWeek());
    }

    @Test
    void testMissingProficiency_DoesNotSilentlyReplaceWithZero() {
        StudentProfile emptyProfile = new StudentProfile();
        emptyProfile.setId(12L);
        emptyProfile.setStudentSkills(Collections.emptyList());

        Project p = new Project();
        p.setId(888L);
        p.setRequiredSkills(Collections.emptyList());
        p.setComplexityLevel("BEGINNER");

        when(mlDatasetRepository.findByStudentProfileIdAndProjectId(12L, 888L))
                .thenReturn(Optional.empty());
        when(mlDatasetRepository.save(any(MlDataset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MlDataset snapshot = mlDatasetService.recordDatasetSnapshot(
                emptyProfile,
                p,
                15.0,
                null,
                null,
                null,
                null,
                "TEST"
        );

        assertNull(snapshot.getStudentSkillLevel(), "Missing skill proficiency must remain null, never silently zero");
        assertNull(snapshot.getPreviousExperience(), "Missing previous experience must remain null, never invented");
    }

    @Test
    void testEvaluatorOutcomeRecording_Success() {
        MlDataset record = new MlDataset();
        record.setId(10L);
        record.setStudentSkillLevel(3.0);
        record.setSkillGap(2.0);
        record.setProjectComplexity(ProjectComplexity.MEDIUM);
        record.setTeamSize(3);
        record.setAvailableHoursPerWeek(20.0);
        record.setTechnologyFamiliarity(3.2);
        record.setNumberOfRequirements(5);
        record.setNumberOfFeatures(4);
        record.setPreviousExperience(1.5);
        record.setEstimatedEffort(240.0);
        record.setOutcomeStatus(MlOutcomeStatus.PENDING_OUTCOME);

        when(mlDatasetRepository.findById(10L)).thenReturn(Optional.of(record));
        when(mlDatasetRepository.save(any(MlDataset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MlOutcomeUpdateRequest req = new MlOutcomeUpdateRequest();
        req.setActualOutcome("FEASIBLE");
        req.setLabelEvidence("Completed full capstone demonstration on time with verified automated testing.");
        req.setLabelNotes("High quality engineering deliverables.");
        req.setValidationConfirmed(true);

        MlDatasetRecordDto result = mlDatasetService.recordActualOutcome(10L, req);

        assertEquals("FEASIBLE", result.getActualOutcome());
        assertEquals("FEASIBLE", result.getOutcomeStatus());
        assertEquals("VALIDATED", result.getValidationStatus());
        assertEquals("Completed full capstone demonstration on time with verified automated testing.", result.getLabelEvidence());
    }

    @Test
    void testEvaluatorOutcomeRecording_InvalidOutcomeThrowsBadRequest() {
        MlDataset record = new MlDataset();
        record.setId(11L);
        when(mlDatasetRepository.findById(11L)).thenReturn(Optional.of(record));

        MlOutcomeUpdateRequest req = new MlOutcomeUpdateRequest();
        req.setActualOutcome("MAYBE_FEASIBLE"); // Invalid

        assertThrows(BadRequestException.class, () -> mlDatasetService.recordActualOutcome(11L, req));
    }

    @Test
    void testDatasetCleaningAndPreparation_ExcludesPendingAndIncomplete() {
        // Record 1: Valid and verified FEASIBLE
        MlDataset r1 = new MlDataset();
        r1.setId(1L);
        r1.setStudentSkillLevel(3.0);
        r1.setSkillGap(4.0);
        r1.setProjectComplexity(ProjectComplexity.MEDIUM);
        r1.setTeamSize(2);
        r1.setAvailableHoursPerWeek(20.0);
        r1.setTechnologyFamiliarity(3.0);
        r1.setNumberOfRequirements(3);
        r1.setNumberOfFeatures(2);
        r1.setPreviousExperience(1.0);
        r1.setEstimatedEffort(240.0);
        r1.setActualOutcome("FEASIBLE");
        r1.setOutcomeStatus(MlOutcomeStatus.FEASIBLE);
        r1.setLabelEvidence("Verified via live capstone demonstration with high fidelity metrics.");

        // Record 2: Pending outcome (must be excluded from training-ready)
        MlDataset r2 = new MlDataset();
        r2.setId(2L);
        r2.setStudentSkillLevel(2.5);
        r2.setSkillGap(6.0);
        r2.setProjectComplexity(ProjectComplexity.EASY);
        r2.setTeamSize(1);
        r2.setAvailableHoursPerWeek(15.0);
        r2.setTechnologyFamiliarity(2.0);
        r2.setNumberOfRequirements(2);
        r2.setNumberOfFeatures(1);
        r2.setPreviousExperience(0.5);
        r2.setEstimatedEffort(180.0);
        r2.setOutcomeStatus(MlOutcomeStatus.PENDING_OUTCOME);

        // Record 3: Verified NOT_FEASIBLE but missing features (incomplete)
        MlDataset r3 = new MlDataset();
        r3.setId(3L);
        r3.setActualOutcome("NOT_FEASIBLE");
        r3.setOutcomeStatus(MlOutcomeStatus.NOT_FEASIBLE);
        r3.setLabelEvidence("Project was abandoned due to unmet requirements.");
        r3.setStudentSkillLevel(null); // Missing!

        when(mlDatasetRepository.findAll()).thenReturn(List.of(r1, r2, r3));

        MlDatasetPrepareSummaryDto summary = mlDatasetService.prepareAndValidateDataset();

        assertEquals(3, summary.getTotalDatabaseRecords());
        assertEquals(1, summary.getEligibleRecords(), "Only complete, verified record r1 should be eligible");
        assertEquals(2, summary.getExcludedRecords(), "Pending r2 and incomplete r3 must be excluded");
        assertEquals(1, summary.getPendingOutcomeRecords());
        assertEquals(1, summary.getVerifiedFeasibleRecords());
        assertEquals(1, summary.getVerifiedNotFeasibleRecords());
    }

    @Test
    void testExportTrainingReadyCsv_ExcludesPendingOutcomesAndIncludesExactHeader() {
        MlDataset r1 = new MlDataset();
        r1.setId(1L);
        r1.setStudentSkillLevel(3.2);
        r1.setSkillGap(3.5);
        r1.setProjectComplexity(ProjectComplexity.HARD);
        r1.setTeamSize(3);
        r1.setAvailableHoursPerWeek(25.0);
        r1.setTechnologyFamiliarity(3.1);
        r1.setNumberOfRequirements(4);
        r1.setNumberOfFeatures(3);
        r1.setPreviousExperience(2.0);
        r1.setEstimatedEffort(400.0);
        r1.setActualOutcome("FEASIBLE");
        r1.setOutcomeStatus(MlOutcomeStatus.FEASIBLE);
        r1.setValidationStatus(MlValidationStatus.VALIDATED);
        r1.setLabelEvidence("Comprehensive automated testing pass confirmed with thesis defense.");

        MlDataset r2 = new MlDataset();
        r2.setId(2L);
        r2.setOutcomeStatus(MlOutcomeStatus.PENDING_OUTCOME); // Pending!

        when(mlDatasetRepository.findAll()).thenReturn(List.of(r1, r2));

        StringWriter sw = new StringWriter();
        mlDatasetService.exportCleanedTrainingCsv(sw);

        String csv = sw.toString();
        String[] lines = csv.trim().split("\\r?\\n");

        assertEquals(2, lines.length, "Must contain header and exactly 1 verified record (pending excluded)");
        assertEquals("student_skill_level,skill_gap,project_complexity,team_size,available_hours_per_week,technology_familiarity,number_of_requirements,number_of_features,previous_experience,estimated_effort,actual_outcome", lines[0]);
        assertTrue(lines[1].contains("3.20,3.50,HARD,3,25.0,3.10,4,3,2.00,400.0,FEASIBLE"));
    }

    @Test
    void testSplitReport_ReportsInsufficientDataGracefullyWithoutFabrication() {
        when(mlDatasetRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> report = mlDatasetService.getSplitReport();

        assertNotNull(report);
        assertEquals(0, report.get("totalTrainingReadyRecords"));
        assertEquals(false, report.get("isReadyForTraining"));
        assertTrue(((String) report.get("message")).contains("insufficient verified real outcome labels"));
    }

    @Test
    void testEvaluatorOutcomeRecording_MissingConfirmation_ThrowsBadRequest() {
        MlDataset record = new MlDataset();
        record.setId(20L);
        when(mlDatasetRepository.findById(20L)).thenReturn(Optional.of(record));

        MlOutcomeUpdateRequest req = new MlOutcomeUpdateRequest();
        req.setActualOutcome("FEASIBLE");
        req.setLabelEvidence("Demonstrated project successfully");
        req.setValidationConfirmed(false); // Unconfirmed

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mlDatasetService.recordActualOutcome(20L, req));
        assertTrue(ex.getMessage().contains("validationConfirmed=true"));
    }

    @Test
    void testEvaluatorOutcomeRecording_MissingOrShortEvidence_ThrowsBadRequest() {
        MlDataset record = new MlDataset();
        record.setId(21L);
        when(mlDatasetRepository.findById(21L)).thenReturn(Optional.of(record));

        MlOutcomeUpdateRequest req = new MlOutcomeUpdateRequest();
        req.setActualOutcome("FEASIBLE");
        req.setValidationConfirmed(true);
        req.setLabelEvidence("short"); // Less than 10 characters

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mlDatasetService.recordActualOutcome(21L, req));
        assertTrue(ex.getMessage().contains("Meaningful label evidence"));
    }

    @Test
    void testEvaluatorOutcomeRecording_SafeCorrectionProcess() {
        MlDataset record = new MlDataset();
        record.setId(22L);
        record.setStudentSkillLevel(3.0);
        record.setSkillGap(2.0);
        record.setProjectComplexity(ProjectComplexity.MEDIUM);
        record.setTeamSize(3);
        record.setAvailableHoursPerWeek(20.0);
        record.setTechnologyFamiliarity(3.2);
        record.setNumberOfRequirements(5);
        record.setNumberOfFeatures(4);
        record.setPreviousExperience(1.5);
        record.setEstimatedEffort(240.0);
        record.setActualOutcome("FEASIBLE");
        record.setOutcomeStatus(MlOutcomeStatus.FEASIBLE);
        record.setLabelEvidence("Initial assessment passed");

        when(mlDatasetRepository.findById(22L)).thenReturn(Optional.of(record));

        // Attempt 1: Overwrite without allowCorrection -> must fail
        MlOutcomeUpdateRequest req = new MlOutcomeUpdateRequest();
        req.setActualOutcome("NOT_FEASIBLE");
        req.setValidationConfirmed(true);
        req.setLabelEvidence("Upon code audit, project failed validation.");
        req.setAllowCorrection(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mlDatasetService.recordActualOutcome(22L, req));
        assertTrue(ex.getMessage().contains("already has a verified outcome"));

        // Attempt 2: Overwrite with allowCorrection=true -> succeeds and appends audit trail
        req.setAllowCorrection(true);
        when(mlDatasetRepository.save(any(MlDataset.class))).thenAnswer(inv -> inv.getArgument(0));

        MlDatasetRecordDto result = mlDatasetService.recordActualOutcome(22L, req);
        assertEquals("NOT_FEASIBLE", result.getActualOutcome());
        assertTrue(result.getLabelNotes().contains("AUDIT: Outcome corrected from FEASIBLE to NOT_FEASIBLE"));
    }

    @Test
    void testStudentProjectRegistration_CannotRegisterAnotherStudentProject() {
        StudentProfile otherProfile = new StudentProfile();
        otherProfile.setId(99L);

        Project customProject = new Project();
        customProject.setId(777L);
        customProject.setIsCatalog(false);
        customProject.setStudentProfile(otherProfile); // Belongs to other student!

        when(userRepository.findByEmail("student@testguard.com")).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));
        when(projectRepository.findById(777L)).thenReturn(Optional.of(customProject));

        MlProjectRegistrationRequest req = new MlProjectRegistrationRequest();
        req.setProjectId(777L);
        req.setAvailableHoursPerWeek(15.0);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mlDatasetService.registerStudentProject("student@testguard.com", req));
        assertTrue(ex.getMessage().contains("belongs to another student"));
    }

    @Test
    void testNoArbitraryFallbackValues_MissingFieldsRemainNullAndRecordIncomplete() {
        StudentProfile sparseProfile = new StudentProfile();
        sparseProfile.setId(33L);
        sparseProfile.setStudentSkills(Collections.emptyList());
        sparseProfile.setTeamSize(null); // No team size!

        Project sparseProject = new Project();
        sparseProject.setId(444L);
        sparseProject.setComplexityLevel("MEDIUM");
        sparseProject.setEstimatedWeeks(null); // No estimated duration!
        sparseProject.setRequirements(Collections.emptyList());
        sparseProject.setMinTeamSize(null);

        when(mlDatasetRepository.findByStudentProfileIdAndProjectId(33L, 444L)).thenReturn(Optional.empty());
        when(mlDatasetRepository.save(any(MlDataset.class))).thenAnswer(inv -> inv.getArgument(0));

        MlDataset snapshot = mlDatasetService.recordDatasetSnapshot(
                sparseProfile,
                sparseProject,
                null, // availableHoursPerWeek NOT provided
                null,
                null,
                null,
                null,
                "AI_GENERATION"
        );

        assertNull(snapshot.getAvailableHoursPerWeek(), "Must not assume 15.0 hours/week");
        assertNull(snapshot.getTeamSize(), "Must not assume 1 team member");
        assertNull(snapshot.getNumberOfRequirements(), "Must not assume 2 requirements");
        assertNull(snapshot.getNumberOfFeatures(), "Must not assume 3 features");
        assertNull(snapshot.getEstimatedEffort(), "Must not assume 12 weeks or effort");
        assertEquals(MlValidationStatus.INCOMPLETE, snapshot.getValidationStatus(), "Incomplete record must be flagged INCOMPLETE");
    }
}

