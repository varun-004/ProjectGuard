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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class Phase6RecommendationEndToEndTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationEngineService recommendationEngine;

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
    private StudentProjectRecommendationRepository recommendationRepository;

    @Test
    void testCatalogSeededAndQualityGatePassed() {
        List<Project> catalog = projectRepository.findByIsCatalogTrue();
        assertFalse(catalog.isEmpty(), "Project catalog should be seeded and non-empty");
        assertTrue(catalog.size() >= 20, "Catalog should have at least 20 diverse curated projects; found " + catalog.size());

        for (Project p : catalog) {
            assertTrue(p.getQualityApproved(), "Catalog project '" + p.getTitle() + "' must pass Quality Gate");
            assertTrue(p.getQualityScore() >= 70.0, "Catalog project quality score must be >= 70");
            assertNotNull(p.getProblemStatement(), "Project must document problem statement");
            assertNotNull(p.getTargetStakeholders(), "Project must document stakeholders");
            assertNotNull(p.getProposedSolution(), "Project must document proposed solution");
            assertNotNull(p.getExpectedOutcomeKpis(), "Project must document measurable KPIs");
            assertNotNull(p.getTestingValidation(), "Project must document testing/validation");
        }
    }

    @Test
    void testRecommendationsForCSEStudent_WithVerifiedAssessments() {
        // Create or load test user
        User user = getOrCreateUser("test_cse_rec@example.com", "test_cse_rec");
        EngineeringBranch cse = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Computer Science"))
                .findFirst().orElseThrow();
        ProjectDomain ai = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("AI") || d.getName().contains("Machine Learning"))
                .findFirst().orElseThrow();

        StudentProfile profile = getOrCreateProfile(user, cse, ai, "UG", 3, 16);

        // Add verified Phase 5 assessment for Python
        Skill python = skillRepository.findByName("Python").orElse(null);
        if (python != null) {
            AssessmentResult ar = new AssessmentResult();
            ar.setStudentProfile(profile);
            ar.setSkill(python);
            ar.setScore(10);
            ar.setPercentage(100.0);
            ar.setLevel(ProficiencyLevel.ADVANCED);
            assessmentResultRepository.save(ar);
        }

        // Generate recommendations
        RecommendationListResponse response =
                recommendationService.getRecommendationsForUser(user.getEmail(), true);

        assertNotNull(response);
        assertEquals(user.getUsername(), response.getStudentName());
        assertFalse(response.getRecommendations().isEmpty(), "Should return recommendations for CSE student");

        ProjectRecommendationResponse top = response.getRecommendations().get(0);
        assertEquals(1, top.getRankOrder());
        assertTrue(top.getMatchScore() >= 60.0, "Top recommendation match score should be high for matching domain");
        assertNotNull(top.getScoreBreakdown());
        assertNotNull(top.getScoreExplanation());
        assertFalse(top.getRecommendationReasons().isEmpty());

        // Verify persistence in database
        List<StudentProjectRecommendation> saved =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profile.getId());
        assertFalse(saved.isEmpty(), "Recommendations must persist in database");
        assertEquals(response.getRecommendations().size(), saved.size());

        // Test reload from database (without refresh)
        RecommendationListResponse reloaded =
                recommendationService.getRecommendationsForUser(user.getEmail(), false);
        assertEquals(response.getRecommendations().size(), reloaded.getRecommendations().size());
        assertEquals(top.getProjectId(), reloaded.getRecommendations().get(0).getProjectId());
    }

    @Test
    void testRecommendationsForCivilStudent_DoesNotIncludeSoftwareOnlyProjects() {
        User user = getOrCreateUser("test_civil_rec@example.com", "test_civil_rec");
        EngineeringBranch civil = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Civil"))
                .findFirst().orElseThrow();
        ProjectDomain struct = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("Structural"))
                .findFirst().orElseThrow();

        StudentProfile profile = getOrCreateProfile(user, civil, struct, "UG", 3, 16);

        RecommendationListResponse response =
                recommendationService.getRecommendationsForUser(user.getEmail(), true);

        assertNotNull(response);
        for (ProjectRecommendationResponse rec : response.getRecommendations()) {
            // Must not recommend pure software like Sepsis or Payment Settlement
            assertFalse(rec.getTitle().contains("Sepsis"), "Civil student must not receive Clinical Sepsis project");
            assertFalse(rec.getTitle().contains("Payment Dispute"), "Civil student must not receive Payment Dispute project");
        }
    }

    @Test
    void testRecommendationsForEEEStudent_MatchesPowerAndRenewableProjects() {
        User user = getOrCreateUser("test_eee_rec@example.com", "test_eee_rec");
        EngineeringBranch eee = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Electrical and Electronics"))
                .findFirst().orElseThrow();
        ProjectDomain power = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("Power") || d.getName().contains("Renewable"))
                .findFirst().orElseThrow();

        StudentProfile profile = getOrCreateProfile(user, eee, power, "UG", 2, 14);

        RecommendationListResponse response =
                recommendationService.getRecommendationsForUser(user.getEmail(), true);

        assertNotNull(response);
        assertFalse(response.getRecommendations().isEmpty(), "EEE student should receive relevant engineering recommendations");
        ProjectRecommendationResponse top = response.getRecommendations().get(0);
        assertTrue(top.getTargetBranches().contains("Electrical and Electronics Engineering"));
    }

    @Test
    void testStudentWithNoAssessments_DisclosesGapsWithoutFabrication() {
        User user = getOrCreateUser("test_no_assess@example.com", "test_no_assess");
        EngineeringBranch cse = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Computer Science"))
                .findFirst().orElseThrow();
        ProjectDomain ai = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("AI") || d.getName().contains("Machine Learning"))
                .findFirst().orElseThrow();

        StudentProfile profile = getOrCreateProfile(user, cse, ai, "UG", 2, 12);

        RecommendationListResponse response =
                recommendationService.getRecommendationsForUser(user.getEmail(), true);

        assertNotNull(response);
        if (!response.getRecommendations().isEmpty()) {
            ProjectRecommendationResponse first = response.getRecommendations().get(0);
            assertEquals(0.0, first.getScoreBreakdown().getSkillFitScore(),
                    "Skills score must be 0 when student has 0 assessments and 0 declared skills (no fabrication)");
            assertFalse(first.getIdentifiedGaps().isEmpty(),
                    "Must disclose unassessed skills as gaps");
        }
    }

    @Test
    void testRecommendationsAcrossAllTenPrograms() {
        String[] branchesToTest = {
                "Computer Science and Engineering",
                "Information Science and Engineering",
                "Electronics and Communication Engineering",
                "Electrical and Electronics Engineering",
                "Mechanical Engineering",
                "Civil Engineering",
                "Bachelor of Computer Applications (BCA)",
                "Master of Computer Applications (MCA)",
                "B.Sc Computer Science",
                "M.Sc Computer Science"
        };

        for (int i = 0; i < branchesToTest.length; i++) {
            String branchName = branchesToTest[i];
            EngineeringBranch branch = branchRepository.findAll().stream()
                    .filter(b -> b.getName().equalsIgnoreCase(branchName))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Branch not found in DB: " + branchName));

            // Select a matching domain available for this branch or fallback to any domain in DB
            ProjectDomain domain = domainRepository.findAll().stream()
                    .filter(d -> branch.getDomains() != null && branch.getDomains().contains(d))
                    .findFirst()
                    .orElseGet(() -> domainRepository.findAll().get(0));

            String email = "student_branch_" + i + "@example.com";
            String username = "student_branch_" + i;
            String level = (branchName.contains("MCA") || branchName.contains("M.Sc")) ? "PG" : "UG";

            User user = getOrCreateUser(email, username);
            StudentProfile profile = getOrCreateProfile(user, branch, domain, level, 2, 16);

            RecommendationListResponse response =
                    recommendationService.getRecommendationsForUser(user.getEmail(), true);

            assertNotNull(response, "Response must not be null for branch: " + branchName);
            assertFalse(response.getRecommendations().isEmpty(),
                    "Should return valid recommendations for branch: " + branchName);

            for (ProjectRecommendationResponse rec : response.getRecommendations()) {
                assertTrue(rec.getMatchScore() >= 35.0, "All recommendations must satisfy minimum match threshold (35.0)");
                assertNotNull(rec.getScoreBreakdown(), "Score breakdown must be present");
                assertNotNull(rec.getScoreExplanation(), "Score explanation must be present");
                assertNotNull(rec.getTargetBranches(), "Target branches list must be populated");
                assertTrue(rec.getTargetBranches().contains(branch.getName()),
                        "Project target branches must contain student branch: " + branchName);
            }
        }
    }

    @Test
    void testNoMatchCase_SevereTimeAndScopeDeficit_ReturnsZeroRecommendations() {
        User user = getOrCreateUser("test_no_match@example.com", "test_no_match");
        EngineeringBranch civil = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Civil"))
                .findFirst().orElseThrow();
        // Disconnected domain
        ProjectDomain vlsi = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("VLSI"))
                .findFirst().orElseThrow();

        // 1 week time (deficit for all 12+ week projects), team size 10 (capacity mismatch)
        StudentProfile profile = getOrCreateProfile(user, civil, vlsi, "UG", 10, 1);

        RecommendationListResponse response =
                recommendationService.getRecommendationsForUser(user.getEmail(), true);

        assertNotNull(response);
        assertTrue(response.getRecommendations().isEmpty(),
                "Incompatible branch/domain with severe time and team deficits must return 0 recommendations");
        assertTrue(response.getMessage().contains("No matching projects found"),
                "Must return informative guidance message when no match is found");
    }

    @Test
    void testRefreshIdempotencyAndNoDuplicates() {
        User user = getOrCreateUser("test_refresh_idempotent@example.com", "test_refresh_user");
        EngineeringBranch cse = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Computer Science"))
                .findFirst().orElseThrow();
        ProjectDomain ai = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("AI") || d.getName().contains("Machine Learning"))
                .findFirst().orElseThrow();

        StudentProfile profile = getOrCreateProfile(user, cse, ai, "UG", 3, 16);

        // Initial generation with refresh = false
        RecommendationListResponse firstCall =
                recommendationService.getRecommendationsForUser(user.getEmail(), false);
        assertNotNull(firstCall);
        int initialCount = firstCall.getRecommendations().size();
        assertTrue(initialCount > 0);

        List<StudentProjectRecommendation> initialSaved =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profile.getId());
        assertEquals(initialCount, initialSaved.size());
        List<Long> initialIds = initialSaved.stream().map(StudentProjectRecommendation::getId).toList();

        // Second call with refresh = false -> must return persisted records without duplicate rows
        RecommendationListResponse secondCall =
                recommendationService.getRecommendationsForUser(user.getEmail(), false);
        assertEquals(initialCount, secondCall.getRecommendations().size());

        List<StudentProjectRecommendation> secondSaved =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profile.getId());
        assertEquals(initialCount, secondSaved.size(), "Row count must remain identical (no duplicates)");
        List<Long> secondIds = secondSaved.stream().map(StudentProjectRecommendation::getId).toList();
        assertEquals(initialIds, secondIds, "Persisted recommendation IDs must match exactly");

        // Third call with refresh = true -> must replace rankings cleanly
        RecommendationListResponse thirdCall =
                recommendationService.getRecommendationsForUser(user.getEmail(), true);
        assertEquals(initialCount, thirdCall.getRecommendations().size());

        List<StudentProjectRecommendation> thirdSaved =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profile.getId());
        assertEquals(initialCount, thirdSaved.size(), "Row count must remain exactly matched to recommendation list");
    }

    @Test
    void testStudentOwnershipAndIsolation() {
        User userA = getOrCreateUser("user_a_owner@example.com", "user_a_owner");
        User userB = getOrCreateUser("user_b_owner@example.com", "user_b_owner");

        EngineeringBranch cse = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Computer Science"))
                .findFirst().orElseThrow();
        EngineeringBranch mech = branchRepository.findAll().stream()
                .filter(b -> b.getName().contains("Mechanical"))
                .findFirst().orElseThrow();

        ProjectDomain ai = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("AI") || d.getName().contains("Machine Learning"))
                .findFirst().orElseThrow();
        ProjectDomain cad = domainRepository.findAll().stream()
                .filter(d -> d.getName().contains("CAD") || d.getName().contains("Product"))
                .findFirst().orElseThrow();

        StudentProfile profileA = getOrCreateProfile(userA, cse, ai, "UG", 2, 16);
        StudentProfile profileB = getOrCreateProfile(userB, mech, cad, "UG", 2, 16);

        // Generate recommendations for User A
        RecommendationListResponse respA = recommendationService.getRecommendationsForUser(userA.getEmail(), true);
        assertNotNull(respA);

        // Verify User B's persisted recommendations table is initially empty
        List<StudentProjectRecommendation> recsBBefore =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profileB.getId());
        assertTrue(recsBBefore.isEmpty(), "User B must have no persisted recommendations prior to calculation");

        // Generate recommendations for User B
        RecommendationListResponse respB = recommendationService.getRecommendationsForUser(userB.getEmail(), true);
        assertNotNull(respB);

        // Verify isolation in database
        List<StudentProjectRecommendation> recsA =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profileA.getId());
        List<StudentProjectRecommendation> recsB =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profileB.getId());

        assertFalse(recsA.isEmpty());
        assertFalse(recsB.isEmpty());

        for (StudentProjectRecommendation r : recsA) {
            assertEquals(profileA.getId(), r.getStudentProfile().getId(), "Rec A must belong exclusively to Profile A");
            assertNotEquals(profileB.getId(), r.getStudentProfile().getId(), "Rec A must never belong to Profile B");
        }

        for (StudentProjectRecommendation r : recsB) {
            assertEquals(profileB.getId(), r.getStudentProfile().getId(), "Rec B must belong exclusively to Profile B");
            assertNotEquals(profileA.getId(), r.getStudentProfile().getId(), "Rec B must never belong to Profile A");
        }
    }

    @Test
    void testSafetyConstraintsForCriticalDomains() {
        List<Project> catalog = projectRepository.findByIsCatalogTrue();

        // 1. Healthcare projects
        List<Project> healthProjects = catalog.stream()
                .filter(p -> p.getTitle().contains("Sepsis") || p.getTitle().contains("Maternal"))
                .toList();
        assertFalse(healthProjects.isEmpty(), "Healthcare projects must exist in catalog");
        for (Project p : healthProjects) {
            assertNotNull(p.getSecurityPrivacySafety(), "Healthcare project must have security/privacy/safety constraints");
            String safety = p.getSecurityPrivacySafety().toLowerCase();
            assertTrue(safety.contains("hipaa") || safety.contains("encrypt") || safety.contains("failsafe") || safety.contains("lock"),
                    "Healthcare project safety constraint must specify privacy/failsafe protocols: " + p.getTitle());
        }

        // 2. Automotive projects
        List<Project> automotiveProjects = catalog.stream()
                .filter(p -> p.getTitle().contains("CAN Bus") || p.getTitle().contains("Brake-by-Wire"))
                .toList();
        assertFalse(automotiveProjects.isEmpty(), "Automotive projects must exist in catalog");
        for (Project p : automotiveProjects) {
            assertNotNull(p.getSecurityPrivacySafety(), "Automotive project must have safety constraints");
            String safety = p.getSecurityPrivacySafety().toLowerCase();
            assertTrue(safety.contains("fail-safe") || safety.contains("can") || safety.contains("redundancy"),
                    "Automotive project must specify fail-safe / redundancy / bus safety: " + p.getTitle());
        }

        // 3. Robotics projects
        List<Project> roboticsProjects = catalog.stream()
                .filter(p -> p.getTitle().contains("Robot") || p.getTitle().contains("SLAM") || p.getTitle().contains("Exoskeleton"))
                .toList();
        assertFalse(roboticsProjects.isEmpty(), "Robotics projects must exist in catalog");
        for (Project p : roboticsProjects) {
            assertNotNull(p.getSecurityPrivacySafety(), "Robotics project must have safety constraints");
            String safety = p.getSecurityPrivacySafety().toLowerCase();
            assertTrue(safety.contains("stop") || safety.contains("e-stop") || safety.contains("limit") || safety.contains("ergonomic"),
                    "Robotics project must specify emergency stop, hardware limits or ergonomics: " + p.getTitle());
        }

        // 4. Electrical projects
        List<Project> electricalProjects = catalog.stream()
                .filter(p -> p.getTitle().contains("Substation") || p.getTitle().contains("Solar") || p.getTitle().contains("Motor Drive"))
                .toList();
        assertFalse(electricalProjects.isEmpty(), "Electrical projects must exist in catalog");
        for (Project p : electricalProjects) {
            assertNotNull(p.getSecurityPrivacySafety(), "Electrical project must have safety constraints");
            String safety = p.getSecurityPrivacySafety().toLowerCase();
            assertTrue(safety.contains("isolation") || safety.contains("protection") || safety.contains("interlock") || safety.contains("dead-time"),
                    "Electrical project must specify electrical isolation / protection / interlocks: " + p.getTitle());
        }
    }

    private User getOrCreateUser(String email, String username) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setUsername(username);
            u.setPassword("$2a$10$abcdefghijklmnopqrstuv");
            u.setRole(Role.STUDENT);
            u.setAuthProvider(AuthProvider.LOCAL);
            return userRepository.save(u);
        });
    }

    private StudentProfile getOrCreateProfile(User user, EngineeringBranch branch,
                                              ProjectDomain domain, String level,
                                              int teamSize, int weeks) {
        return studentProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            StudentProfile p = new StudentProfile();
            p.setUser(user);
            p.setBranch(branch);
            p.setDomain(domain);
            p.setAcademicLevel(level);
            p.setTeamSize(teamSize);
            p.setAvailableTimeWeeks(weeks);
            p.setInterests("Engineering, real-world systems, capstone research");
            return studentProfileRepository.save(p);
        });
    }
}
