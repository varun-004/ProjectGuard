package com.projectguard.service;

import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.repository.AssessmentResultRepository;
import com.projectguard.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationEngineServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AssessmentResultRepository assessmentResultRepository;

    @Mock
    private ProjectQualityGateService qualityGateService;

    @InjectMocks
    private RecommendationEngineService recommendationEngine;

    private EngineeringBranch cseBranch;
    private EngineeringBranch civilBranch;
    private ProjectDomain aiDomain;
    private ProjectDomain civilDomain;
    private Technology pythonMlTech;
    private Skill pythonSkill;
    private Skill mlSkill;

    @BeforeEach
    void setUp() {
        cseBranch = new EngineeringBranch();
        cseBranch.setId(1L);
        cseBranch.setName("Computer Science and Engineering");

        civilBranch = new EngineeringBranch();
        civilBranch.setId(2L);
        civilBranch.setName("Civil Engineering");

        aiDomain = new ProjectDomain();
        aiDomain.setId(10L);
        aiDomain.setName("AI / Machine Learning");

        civilDomain = new ProjectDomain();
        civilDomain.setId(20L);
        civilDomain.setName("Structural Engineering");

        pythonSkill = new Skill();
        pythonSkill.setId(100L);
        pythonSkill.setName("Python");

        mlSkill = new Skill();
        mlSkill.setId(101L);
        mlSkill.setName("Machine Learning");

        pythonMlTech = new Technology();
        pythonMlTech.setId(200L);
        pythonMlTech.setName("Python ML");
        pythonMlTech.setDomain(aiDomain);
    }

    @Test
    void testMatchingStudent_GetsHighMatchScoreAndBreakdown() {
        StudentProfile profile = new StudentProfile();
        profile.setId(1L);
        profile.setBranch(cseBranch);
        profile.setDomain(aiDomain);
        profile.setTechnology(pythonMlTech);
        profile.setAcademicLevel("UG");
        profile.setTeamSize(3);
        profile.setAvailableTimeWeeks(16);
        profile.setInterests("healthcare AI, clinical models");

        Project project = new Project();
        project.setId(500L);
        project.setTitle("Clinical Sepsis Prediction");
        project.setDomain(aiDomain);
        project.setTargetBranches(List.of(cseBranch));
        project.setRequiredSkills(List.of(pythonSkill, mlSkill));
        project.setQualityApproved(true);
        project.setQualityScore(90.0);
        project.setMinTeamSize(2);
        project.setMaxTeamSize(4);
        project.setEstimatedWeeks(16);
        project.setComplexityLevel("INTERMEDIATE");

        ProjectTechnology pt = new ProjectTechnology();
        pt.setProject(project);
        pt.setTechnology(pythonMlTech);
        project.setProjectTechnologies(List.of(pt));

        when(projectRepository.findAllApprovedCatalogProjectsWithDetails()).thenReturn(List.of(project));

        AssessmentResult ar1 = new AssessmentResult();
        ar1.setSkill(pythonSkill);
        ar1.setScore(10);
        ar1.setPercentage(100.0);
        ar1.setLevel(ProficiencyLevel.ADVANCED);

        AssessmentResult ar2 = new AssessmentResult();
        ar2.setSkill(mlSkill);
        ar2.setScore(8);
        ar2.setPercentage(80.0);
        ar2.setLevel(ProficiencyLevel.ADVANCED);

        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(ar1, ar2));

        List<ProjectRecommendationResponse> recs = recommendationEngine.generateRecommendations(profile);

        assertNotNull(recs);
        assertEquals(1, recs.size());

        ProjectRecommendationResponse rec = recs.get(0);
        assertEquals(500L, rec.getProjectId());
        assertEquals(1, rec.getRankOrder());
        assertTrue(rec.getMatchScore() >= 80.0, "Matching student with verified skills should get >= 80% match");
        assertEquals(25.0, rec.getScoreBreakdown().getDomainFitScore());
        assertEquals(20.0, rec.getScoreBreakdown().getTechFitScore());
        assertEquals(22.5, rec.getScoreBreakdown().getSkillFitScore());
        assertTrue(rec.getRecommendationReasons().size() >= 3);
        assertNotNull(rec.getScoreExplanation());
    }

    @Test
    void testBranchIncompatibleProject_IsFilteredOut() {
        StudentProfile profile = new StudentProfile();
        profile.setId(2L);
        profile.setBranch(cseBranch);
        profile.setDomain(aiDomain);

        Project civilProject = new Project();
        civilProject.setId(600L);
        civilProject.setTitle("RCC Bridge Seismic Retrofit");
        civilProject.setDomain(civilDomain);
        civilProject.setTargetBranches(List.of(civilBranch)); // Only civil
        civilProject.setQualityApproved(true);

        when(projectRepository.findAllApprovedCatalogProjectsWithDetails()).thenReturn(List.of(civilProject));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());

        List<ProjectRecommendationResponse> recs = recommendationEngine.generateRecommendations(profile);

        assertTrue(recs.isEmpty(), "Civil engineering project must not be recommended to CSE student");
    }

    @Test
    void testStudentWithMissingAssessments_DisclosesGapsAndDoesNotFabricate() {
        StudentProfile profile = new StudentProfile();
        profile.setId(3L);
        profile.setBranch(cseBranch);
        profile.setDomain(aiDomain);
        profile.setTechnology(null);
        profile.setTeamSize(2);
        profile.setAvailableTimeWeeks(12);

        Project project = new Project();
        project.setId(700L);
        project.setTitle("Crop Stress Vision Pipeline");
        project.setDomain(aiDomain);
        project.setTargetBranches(List.of(cseBranch));
        project.setRequiredSkills(List.of(pythonSkill, mlSkill));
        project.setQualityApproved(true);
        project.setQualityScore(88.0);
        project.setMinTeamSize(1);
        project.setMaxTeamSize(4);
        project.setEstimatedWeeks(12);
        project.setComplexityLevel("INTERMEDIATE");

        when(projectRepository.findAllApprovedCatalogProjectsWithDetails()).thenReturn(List.of(project));
        // Empty assessments
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(3L)).thenReturn(List.of());

        List<ProjectRecommendationResponse> recs = recommendationEngine.generateRecommendations(profile);

        assertNotNull(recs);
        if (!recs.isEmpty()) {
            ProjectRecommendationResponse rec = recs.get(0);
            assertTrue(rec.getIdentifiedGaps().stream().anyMatch(g -> g.contains("Skill gap") || g.contains("not yet in profile")),
                    "Must disclose unverified skills as identified gaps");
            assertEquals(0.0, rec.getScoreBreakdown().getSkillFitScore(), "Skill score must be 0 for missing skills without fabrication");
        }
    }

    @Test
    void testTimeConstraintDeficit_PenalizesScore() {
        StudentProfile profile = new StudentProfile();
        profile.setId(4L);
        profile.setBranch(cseBranch);
        profile.setDomain(aiDomain);
        profile.setTeamSize(2);
        profile.setAvailableTimeWeeks(4); // Severe deficit: 4 weeks for a 16-week project

        Project project = new Project();
        project.setId(800L);
        project.setTitle("Clinical Sepsis Prediction");
        project.setDomain(aiDomain);
        project.setTargetBranches(List.of(cseBranch));
        project.setRequiredSkills(List.of(pythonSkill));
        project.setQualityApproved(true);
        project.setQualityScore(92.0);
        project.setMinTeamSize(2);
        project.setMaxTeamSize(4);
        project.setEstimatedWeeks(16);

        when(projectRepository.findAllApprovedCatalogProjectsWithDetails()).thenReturn(List.of(project));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(4L)).thenReturn(List.of());

        List<ProjectRecommendationResponse> recs = recommendationEngine.generateRecommendations(profile);

        for (ProjectRecommendationResponse r : recs) {
            assertTrue(r.getScoreBreakdown().getPenaltyScore() >= 10.0, "Severe time deficit should trigger penalty of at least 10 points");
            assertTrue(r.getIdentifiedGaps().stream().anyMatch(g -> g.contains("timeline") || g.contains("Available time")));
        }
    }
}
