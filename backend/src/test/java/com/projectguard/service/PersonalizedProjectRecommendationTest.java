package com.projectguard.service;

import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.dto.recommendation.RecommendationListResponse;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.AuthProvider;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.entity.enums.Role;
import com.projectguard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PersonalizedProjectRecommendationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationEngineService recommendationEngine;

    @Autowired
    private ProjectIdeaGeneratorService projectIdeaGeneratorService;

    @Autowired
    private ProjectQualityGateService qualityGateService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EngineeringBranchRepository branchRepository;

    @Autowired
    private ProjectDomainRepository domainRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private TechnologyRepository technologyRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private StudentRecommendationHistoryRepository historyRepository;

    @Autowired
    private StudentProjectRecommendationRepository recommendationRepository;

    private EngineeringBranch cseBranch;
    private EngineeringBranch civilBranch;
    private EngineeringBranch eeeBranch;
    private ProjectDomain aiDomain;
    private ProjectDomain civilDomain;
    private ProjectDomain powerDomain;
    private Skill pythonSkill;
    private Skill javaSkill;
    private Technology pythonMlTech;

    @BeforeEach
    void setUp() {
        cseBranch = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Computer Science"))
                .findFirst().orElseThrow();

        civilBranch = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Civil"))
                .findFirst().orElseThrow();

        eeeBranch = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Electrical"))
                .findFirst().orElseThrow();

        aiDomain = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("AI") || d.getName().contains("Machine Learning"))
                .findFirst().orElseThrow();

        civilDomain = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("Structural"))
                .findFirst().orElseThrow();

        powerDomain = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("Power") || d.getName().contains("Renewable"))
                .findFirst().orElseThrow();

        pythonSkill = skillRepository.findByName("Python").orElseGet(() -> {
            Skill s = new Skill();
            s.setName("Python");
            return skillRepository.save(s);
        });

        javaSkill = skillRepository.findByName("Java").orElseGet(() -> {
            Skill s = new Skill();
            s.setName("Java");
            return skillRepository.save(s);
        });

        pythonMlTech = technologyRepository.findAll().stream()
                .filter(t -> t.getName().toLowerCase().contains("python"))
                .findFirst().orElseGet(() -> {
                    Technology t = new Technology();
                    t.setName("Python ML");
                    t.setDomain(aiDomain);
                    return technologyRepository.save(t);
                });
    }

    /**
     * Requirement 1 & 4:
     * Verify that all four personalization inputs influence recommendation scoring or eligibility:
     * 1. Engineering branch
     * 2. Technologies
     * 3. Skills and proficiency
     * 4. Assessment/quiz scores
     */
    @Test
    void testAllFourInputsInfluenceScoringOrEligibility() {
        // --- INPUT 1: Engineering Branch ---
        User userCivil = createTestUser("civil_stud_inp1@test.com", "civil_stud_inp1");
        StudentProfile civilProfile = createProfile(userCivil, civilBranch, civilDomain, null, "UG", 2, 16);

        List<ProjectRecommendationResponse> civilRecs = recommendationEngine.generateRecommendations(civilProfile);
        assertFalse(civilRecs.isEmpty(), "Civil student should have recommendations");
        for (ProjectRecommendationResponse r : civilRecs) {
            assertTrue(r.getTargetBranches().contains(civilBranch.getName()),
                    "Branch incompatibility filter must exclude projects not intended for student's branch");
        }

        // --- INPUT 2: Technologies ---
        User userTechA = createTestUser("tech_a_inp2@test.com", "tech_a_inp2");
        StudentProfile profileWithTech = createProfile(userTechA, cseBranch, aiDomain, pythonMlTech, "UG", 2, 16);

        User userTechB = createTestUser("tech_b_inp2@test.com", "tech_b_inp2");
        StudentProfile profileWithoutTech = createProfile(userTechB, cseBranch, aiDomain, null, "UG", 2, 16);

        List<ProjectRecommendationResponse> recsWithTech = recommendationEngine.generateRecommendations(profileWithTech);
        List<ProjectRecommendationResponse> recsWithoutTech = recommendationEngine.generateRecommendations(profileWithoutTech);

        assertFalse(recsWithTech.isEmpty());
        assertFalse(recsWithoutTech.isEmpty());
        // For a project utilizing Python ML, profileWithTech must have techFitScore = 20.0
        ProjectRecommendationResponse topWithTech = recsWithTech.stream()
                .filter(r -> r.getTechnologies() != null && r.getTechnologies().stream().anyMatch(t -> t.toLowerCase().contains("python")))
                .findFirst().orElse(null);

        if (topWithTech != null) {
            assertEquals(20.0, topWithTech.getScoreBreakdown().getTechFitScore(),
                    "Selecting matching technology must yield full technology fit score of 20.0");
        }

        // --- INPUT 3: Skills and Proficiency ---
        User userAdvSkill = createTestUser("skill_adv_inp3@test.com", "skill_adv_inp3");
        StudentProfile profileAdv = createProfile(userAdvSkill, cseBranch, aiDomain, null, "UG", 2, 16);
        StudentSkill ssAdv = new StudentSkill();
        ssAdv.setStudentProfile(profileAdv);
        ssAdv.setSkill(pythonSkill);
        ssAdv.setProficiencyLevel(ProficiencyLevel.ADVANCED);
        ssAdv.setYearsOfExperience(3.0);
        profileAdv.getStudentSkills().add(ssAdv);
        studentProfileRepository.save(profileAdv);

        User userBegSkill = createTestUser("skill_beg_inp3@test.com", "skill_beg_inp3");
        StudentProfile profileBeg = createProfile(userBegSkill, cseBranch, aiDomain, null, "UG", 2, 16);
        StudentSkill ssBeg = new StudentSkill();
        ssBeg.setStudentProfile(profileBeg);
        ssBeg.setSkill(pythonSkill);
        ssBeg.setProficiencyLevel(ProficiencyLevel.BEGINNER);
        ssBeg.setYearsOfExperience(0.0);
        profileBeg.getStudentSkills().add(ssBeg);
        studentProfileRepository.save(profileBeg);

        List<ProjectRecommendationResponse> recsAdv = recommendationEngine.generateRecommendations(profileAdv);
        List<ProjectRecommendationResponse> recsBeg = recommendationEngine.generateRecommendations(profileBeg);

        // Find Python-requiring project
        Optional<ProjectRecommendationResponse> pyAdv = recsAdv.stream()
                .filter(r -> r.getRequiredSkills().contains("Python")).findFirst();
        Optional<ProjectRecommendationResponse> pyBeg = recsBeg.stream()
                .filter(r -> r.getRequiredSkills().contains("Python")).findFirst();

        if (pyAdv.isPresent() && pyBeg.isPresent()) {
            assertTrue(pyAdv.get().getScoreBreakdown().getSkillFitScore() > pyBeg.get().getScoreBreakdown().getSkillFitScore(),
                    "ADVANCED proficiency with experience must yield higher skill score than BEGINNER");
            assertTrue(pyAdv.get().getScoreBreakdown().getProficiencyScore() > pyBeg.get().getScoreBreakdown().getProficiencyScore(),
                    "ADVANCED proficiency must yield higher proficiency score than BEGINNER");
        }

        // --- INPUT 4: Assessment / Quiz Scores ---
        User userHighAssess = createTestUser("assess_high_inp4@test.com", "assess_high_inp4");
        StudentProfile profileHighAssess = createProfile(userHighAssess, cseBranch, aiDomain, null, "UG", 2, 16);
        AssessmentResult arHigh = new AssessmentResult();
        arHigh.setStudentProfile(profileHighAssess);
        arHigh.setSkill(pythonSkill);
        arHigh.setScore(10);
        arHigh.setPercentage(100.0);
        arHigh.setLevel(ProficiencyLevel.ADVANCED);
        assessmentResultRepository.save(arHigh);

        User userLowAssess = createTestUser("assess_low_inp4@test.com", "assess_low_inp4");
        StudentProfile profileLowAssess = createProfile(userLowAssess, cseBranch, aiDomain, null, "UG", 2, 16);
        AssessmentResult arLow = new AssessmentResult();
        arLow.setStudentProfile(profileLowAssess);
        arLow.setSkill(pythonSkill);
        arLow.setScore(2);
        arLow.setPercentage(20.0); // Critical shortfall (<35%)
        arLow.setLevel(ProficiencyLevel.BEGINNER);
        assessmentResultRepository.save(arLow);

        List<ProjectRecommendationResponse> recsHigh = recommendationEngine.generateRecommendations(profileHighAssess);
        List<ProjectRecommendationResponse> recsLow = recommendationEngine.generateRecommendations(profileLowAssess);

        Optional<ProjectRecommendationResponse> topHigh = recsHigh.stream()
                .filter(r -> r.getRequiredSkills().contains("Python")).findFirst();
        Optional<ProjectRecommendationResponse> topLow = recsLow.stream()
                .filter(r -> r.getRequiredSkills().contains("Python")).findFirst();

        if (topHigh.isPresent() && topLow.isPresent()) {
            assertTrue(topHigh.get().getMatchScore() > topLow.get().getMatchScore(),
                    "Student with 100% assessment in Python must achieve higher match score than student with 20%");
            assertTrue(topLow.get().getScoreBreakdown().getPenaltyScore() > topHigh.get().getScoreBreakdown().getPenaltyScore(),
                    "Assessment score < 35% in required skill must trigger shortfall penalty");
            assertTrue(topLow.get().getIdentifiedGaps().stream().anyMatch(g -> g.contains("Critical skill shortfall") || g.contains("below 35%")),
                    "Low assessment score must be clearly disclosed as skill gap");
        }
    }

    /**
     * Requirement 2 & 4:
     * Repeated requests avoid unnecessary duplicate recommendations for the same user when suitable alternatives exist.
     * History tracking tracks previously recommended projects and prefers relevant alternatives.
     */
    @Test
    void testRepeatedRequestsAvoidDuplicatesWhenAlternativesExist() {
        User user = createTestUser("repeat_user_fresh@test.com", "repeat_user_fresh");
        StudentProfile profile = createProfile(user, cseBranch, aiDomain, pythonMlTech, "UG", 2, 16);

        // First recommendation request
        RecommendationListResponse firstReq = recommendationService.getRecommendationsForUser(user.getEmail(), true);
        assertNotNull(firstReq);
        assertFalse(firstReq.getRecommendations().isEmpty());

        List<Long> firstBatchIds = firstReq.getRecommendations().stream()
                .map(ProjectRecommendationResponse::getProjectId)
                .toList();

        // Verify history repository tracked these project recommendations
        Set<Long> historyIds = historyRepository.findRecommendedProjectIdsByStudentProfileId(profile.getId());
        assertFalse(historyIds.isEmpty(), "History must record initial recommendation IDs");
        assertTrue(historyIds.containsAll(firstBatchIds), "History must contain all initial recommended project IDs");

        // Second recommendation request (refresh=true)
        RecommendationListResponse secondReq = recommendationService.getRecommendationsForUser(user.getEmail(), true);
        assertNotNull(secondReq);
        assertFalse(secondReq.getRecommendations().isEmpty());

        List<Long> secondBatchIds = secondReq.getRecommendations().stream()
                .map(ProjectRecommendationResponse::getProjectId)
                .toList();

        // Top recommendation of second request must prefer an unseen relevant alternative
        Long topFirst = firstBatchIds.get(0);
        Long topSecond = secondBatchIds.get(0);

        assertNotEquals(topFirst, topSecond,
                "Repeated request must prefer a fresh relevant alternative rather than presenting the exact same top project");

        // Verify fresh alternative flag
        assertTrue(secondReq.getRecommendations().get(0).getIsFreshAlternative(),
                "New alternative in second request must be flagged as fresh alternative");
    }

    /**
     * Requirement 2 & 4:
     * Different student profiles receive different recommendations.
     */
    @Test
    void testDifferentStudentProfilesReceiveDifferentRecommendations() {
        User cseUser = createTestUser("cse_diff@test.com", "cse_diff");
        StudentProfile cseProfile = createProfile(cseUser, cseBranch, aiDomain, pythonMlTech, "UG", 3, 16);

        User civilUser = createTestUser("civil_diff@test.com", "civil_diff");
        StudentProfile civilProfile = createProfile(civilUser, civilBranch, civilDomain, null, "UG", 2, 16);

        User eeeUser = createTestUser("eee_diff@test.com", "eee_diff");
        StudentProfile eeeProfile = createProfile(eeeUser, eeeBranch, powerDomain, null, "UG", 2, 16);

        RecommendationListResponse cseResp = recommendationService.getRecommendationsForUser(cseUser.getEmail(), true);
        RecommendationListResponse civilResp = recommendationService.getRecommendationsForUser(civilUser.getEmail(), true);
        RecommendationListResponse eeeResp = recommendationService.getRecommendationsForUser(eeeUser.getEmail(), true);

        assertFalse(cseResp.getRecommendations().isEmpty());
        assertFalse(civilResp.getRecommendations().isEmpty());
        assertFalse(eeeResp.getRecommendations().isEmpty());

        // Top recommendations must be distinct across programs
        Long topCseProject = cseResp.getRecommendations().get(0).getProjectId();
        Long topCivilProject = civilResp.getRecommendations().get(0).getProjectId();
        Long topEeeProject = eeeResp.getRecommendations().get(0).getProjectId();

        assertNotEquals(topCseProject, topCivilProject, "CSE and Civil profiles must receive different recommendations");
        assertNotEquals(topCivilProject, topEeeProject, "Civil and EEE profiles must receive different recommendations");
        assertNotEquals(topCseProject, topEeeProject, "CSE and EEE profiles must receive different recommendations");

        // Civil recommendations must contain Civil branch
        for (ProjectRecommendationResponse r : civilResp.getRecommendations()) {
            assertTrue(r.getTargetBranches().contains("Civil Engineering"));
        }

        // EEE recommendations must contain Electrical branch
        for (ProjectRecommendationResponse r : eeeResp.getRecommendations()) {
            assertTrue(r.getTargetBranches().stream().anyMatch(b -> b.contains("Electrical")));
        }
    }

    /**
     * Requirement 2 & 4:
     * Generated projects are validated, industry-relevant, and reject generic CRUD applications.
     */
    @Test
    void testGeneratedProjectsAreValidatedAndNotGenericCrud() {
        User user = createTestUser("gen_valid_user@test.com", "gen_valid_user");
        StudentProfile profile = createProfile(user, cseBranch, aiDomain, pythonMlTech, "UG", 3, 16);

        // Generate innovative projects using the generator service
        List<Project> generated = projectIdeaGeneratorService.generateInnovativeProjects(profile, 2);
        assertNotNull(generated);
        assertFalse(generated.isEmpty(), "Should generate innovative industry-level projects for profile");

        for (Project p : generated) {
            // Quality Gate validation
            assertTrue(p.getQualityApproved(), "Generated project must pass Quality Gate");
            assertTrue(p.getQualityScore() >= 70.0, "Generated project quality score must be >= 70");

            // Real-world problem & industry relevance
            assertNotNull(p.getProblemStatement());
            assertTrue(p.getProblemStatement().length() >= 50, "Problem statement must possess realistic engineering depth");
            assertNotNull(p.getTargetStakeholders());
            assertNotNull(p.getProposedSolution());
            assertNotNull(p.getExpectedOutcomeKpis());
            assertNotNull(p.getTestingValidation());

            // Anti-CRUD validation: no generic CRUD applications
            String combined = (p.getTitle() + " " + p.getDescription() + " " + p.getProblemStatement()).toLowerCase();
            assertFalse(combined.contains("student management"), "Must not generate Student Management system");
            assertFalse(combined.contains("library management"), "Must not generate Library Management system");
            assertFalse(combined.contains("crud only"), "Must not generate CRUD-only system");

            // Target branch matches student branch
            assertTrue(p.getTargetBranches().stream().anyMatch(b -> b.getId().equals(cseBranch.getId())));
        }
    }

    /**
     * Requirement 3 & 4:
     * Feasibility and suitability scores are populated and recommendation explanations provided.
     */
    @Test
    void testFeasibilityAndSuitabilityScoresPopulated() {
        User user = createTestUser("scores_user@test.com", "scores_user");
        StudentProfile profile = createProfile(user, cseBranch, aiDomain, pythonMlTech, "UG", 3, 16);

        RecommendationListResponse response = recommendationService.getRecommendationsForUser(user.getEmail(), true);
        assertNotNull(response);
        assertFalse(response.getRecommendations().isEmpty());

        for (ProjectRecommendationResponse r : response.getRecommendations()) {
            assertNotNull(r.getTitle(), "Title must be present");
            assertNotNull(r.getProblemStatement(), "Real-world problem must be present");
            assertNotNull(r.getProposedSolution(), "Proposed solution must be present");
            assertNotNull(r.getIndustryValue(), "Industry use case must be present");
            assertNotNull(r.getSuitabilityScore(), "Suitability score must be populated");
            assertNotNull(r.getFeasibilityScore(), "Feasibility score must be populated");
            assertTrue(r.getSuitabilityScore() >= 35.0, "Suitability score must satisfy threshold");
            assertTrue(r.getFeasibilityScore() > 0.0, "Feasibility score must be positive");
            assertFalse(r.getRecommendationReasons().isEmpty(), "Why the project matches student profile must be provided");
            assertNotNull(r.getScoreExplanation(), "Personalized score explanation must be provided");
        }
    }

    private User createTestUser(String email, String username) {
        User u = new User();
        u.setEmail(email);
        u.setUsername(username);
        u.setPassword("$2a$10$abcdefghijklmnopqrstuv");
        u.setRole(Role.STUDENT);
        u.setAuthProvider(AuthProvider.LOCAL);
        return userRepository.save(u);
    }

    private StudentProfile createProfile(User user, EngineeringBranch branch, ProjectDomain domain,
                                         Technology tech, String level, int teamSize, int weeks) {
        StudentProfile p = new StudentProfile();
        p.setUser(user);
        p.setBranch(branch);
        p.setDomain(domain);
        p.setTechnology(tech);
        p.setAcademicLevel(level);
        p.setTeamSize(teamSize);
        p.setAvailableTimeWeeks(weeks);
        p.setInterests("Deep Learning, Cloud Computing, Edge Robotics");
        p.setPreviousExperience("Undergraduate coursework and laboratory implementations");
        return studentProfileRepository.save(p);
    }
}
