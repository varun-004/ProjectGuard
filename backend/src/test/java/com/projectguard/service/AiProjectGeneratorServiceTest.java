package com.projectguard.service;

import com.projectguard.dto.recommendation.GenerateFreshProjectsResponse;
import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.exception.BadRequestException;
import com.projectguard.exception.ResourceNotFoundException;
import com.projectguard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiProjectGeneratorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private AssessmentResultRepository assessmentResultRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private EngineeringBranchRepository branchRepository;

    @Mock
    private RecommendationEngineService recommendationEngineService;

    @Mock
    private StudentRecommendationHistoryRepository historyRepository;

    @Mock
    private StudentProjectRecommendationRepository recommendationRepository;

    private AiProjectGeneratorService service;

    private User testUser;
    private StudentProfile testProfile;
    private EngineeringBranch testBranch;
    private ProjectDomain testDomain;
    private Technology testTech;
    private Skill testSkill;

    @BeforeEach
    void setUp() {
        service = new AiProjectGeneratorService(
                userRepository,
                studentProfileRepository,
                assessmentResultRepository,
                projectRepository,
                skillRepository,
                technologyRepository,
                branchRepository,
                recommendationEngineService,
                historyRepository,
                recommendationRepository
        );

        ReflectionTestUtils.setField(service, "geminiApiKey", "test-api-key-12345");
        ReflectionTestUtils.setField(service, "geminiModel", "gemini-3.5-flash");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 60);
        ReflectionTestUtils.setField(service, "maxGenerateCount", 5);

        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("teststudent");
        testUser.setEmail("student@test.com");

        testBranch = new EngineeringBranch();
        testBranch.setId(1L);
        testBranch.setName("Computer Science & Engineering");

        testDomain = new ProjectDomain();
        testDomain.setId(2L);
        testDomain.setName("AI / Machine Learning");

        testTech = new Technology();
        testTech.setId(3L);
        testTech.setName("Python");

        testSkill = new Skill();
        testSkill.setId(4L);
        testSkill.setName("Machine Learning");

        testProfile = new StudentProfile();
        testProfile.setId(10L);
        testProfile.setUser(testUser);
        testProfile.setBranch(testBranch);
        testProfile.setDomain(testDomain);
        testProfile.setTechnology(testTech);
        testProfile.setAcademicLevel("UNDERGRADUATE");
        testProfile.setInterests("Deep learning, computer vision");
        testProfile.setTeamSize(3);
        testProfile.setAvailableTimeWeeks(16);

        StudentSkill studentSkill = new StudentSkill();
        studentSkill.setSkill(testSkill);
        studentSkill.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        studentSkill.setYearsOfExperience(2.0);
        testProfile.setStudentSkills(List.of(studentSkill));
    }

    @Test
    @DisplayName("isGeminiConfigured should return true when API key is set and false when blank")
    void testIsGeminiConfigured() {
        assertTrue(service.isGeminiConfigured());

        ReflectionTestUtils.setField(service, "geminiApiKey", "");
        assertFalse(service.isGeminiConfigured());

        ReflectionTestUtils.setField(service, "geminiApiKey", null);
        assertFalse(service.isGeminiConfigured());
    }

    @Test
    @DisplayName("generateForUser should throw ResourceNotFoundException when user is not found")
    void testGenerateForUser_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.generateForUser("unknown@test.com", 3));
    }

    @Test
    @DisplayName("generateForUser should throw BadRequestException when student profile is missing")
    void testGenerateForUser_ProfileNotFound_ThrowsException() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.generateForUser(testUser.getEmail(), 3));

        assertTrue(ex.getMessage().contains("complete your student profile"));
    }

    @Test
    @DisplayName("generateForUser should return friendly message when Gemini is not configured")
    void testGenerateForUser_GeminiNotConfigured_ReturnsSafeResponse() {
        ReflectionTestUtils.setField(service, "geminiApiKey", "");

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        GenerateFreshProjectsResponse response = service.generateForUser(testUser.getEmail(), 3);

        assertNotNull(response);
        assertFalse(response.isGeminiContributed());
        assertEquals(0, response.getGeneratedCount());
        assertEquals(0, response.getSavedCount());
        assertTrue(response.getMessage().contains("GEMINI_API_KEY"));
        assertTrue(response.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("generateForUser should successfully parse, deduplicate, persist, and score projects")
    void testGenerateForUser_Success_ParsesDeduplicatesPersistsAndScores() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        // Setup branches, technologies, skills
        when(branchRepository.findAll()).thenReturn(List.of(testBranch));
        when(technologyRepository.findAll()).thenReturn(List.of(testTech));
        when(skillRepository.findAll()).thenReturn(List.of(testSkill));

        // Mock duplicate check: "Existing Project" already exists, "Novel AI Sepsis Detector" is fresh
        when(projectRepository.existsByTitleIgnoreCase("Existing Project")).thenReturn(true);
        when(projectRepository.existsByTitleIgnoreCase("Novel AI Sepsis Detector")).thenReturn(false);

        // Mock project saving
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(999L);
            }
            return p;
        });

        // Mock recommendationEngineService scoring
        ProjectRecommendationResponse mockRecommendation = new ProjectRecommendationResponse();
        mockRecommendation.setProjectId(999L);
        mockRecommendation.setTitle("Novel AI Sepsis Detector");
        mockRecommendation.setMatchScore(88.5);
        when(recommendationEngineService.scoreProject(any(Project.class), eq(testProfile), anyMap(), anyMap()))
                .thenReturn(mockRecommendation);

        // Spy the service to stub callGeminiApi without real HTTP requests
        AiProjectGeneratorService spyService = spy(service);

        String mockGeminiJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n[\\n  {\\n    \\"title\\": \\"Existing Project\\",\\n    \\"problemStatement\\": \\"Hospital acquired infections risk assessment.\\",\\n    \\"proposedSolution\\": \\"Real-time vital analytics pipeline.\\",\\n    \\"targetBranches\\": [\\"Computer Science & Engineering\\"],\\n    \\"technologies\\": [\\"Python\\"],\\n    \\"requiredSkills\\": [\\"Machine Learning\\"],\\n    \\"complexityLevel\\": \\"INTERMEDIATE\\"\\n  },\\n  {\\n    \\"title\\": \\"Novel AI Sepsis Detector\\",\\n    \\"problemStatement\\": \\"Sepsis claims 11 million lives annually with delayed ICU detection.\\",\\n    \\"proposedSolution\\": \\"Gradient boosted multi-channel time-series telemetry monitoring.\\",\\n    \\"targetBranches\\": [\\"Computer Science & Engineering\\"],\\n    \\"technologies\\": [\\"Python\\"],\\n    \\"requiredSkills\\": [\\"Machine Learning\\"],\\n    \\"mvpScope\\": \\"Stream ingestion and hourly risk probability dashboard.\\",\\n    \\"personalizationRationale\\": \\"Tailored to student's background in Python and ML.\\",\\n    \\"complexityLevel\\": \\"ADVANCED\\",\\n    \\"estimatedWeeks\\": 14,\\n    \\"minTeamSize\\": 2,\\n    \\"maxTeamSize\\": 4\\n  }\\n]\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        doReturn(mockGeminiJson).when(spyService).callGeminiApi(anyString());

        GenerateFreshProjectsResponse response = spyService.generateForUser(testUser.getEmail(), 2);

        assertNotNull(response);
        assertTrue(response.isGeminiContributed());
        assertEquals(2, response.getGeneratedCount(), "Raw ideas count from Gemini should be 2");
        assertEquals(1, response.getSavedCount(), "Only 1 non-duplicate project should be saved");
        assertEquals(1, response.getRecommendations().size());

        ProjectRecommendationResponse rec = response.getRecommendations().getFirst();
        assertEquals("Novel AI Sepsis Detector", rec.getTitle());
        assertTrue(rec.getIsGeneratedIdea(), "Generated project flag must be true");
        assertTrue(rec.getIsFreshAlternative(), "Fresh alternative flag must be true");
        assertEquals(1, rec.getRankOrder());

        // Verify duplicate was never saved with isCatalog = false
        verify(projectRepository, times(2)).save(argThat(p ->
                p.getTitle().equals("Novel AI Sepsis Detector") && !p.getIsCatalog() && p.getQualityApproved()
        ));
    }

    @Test
    @DisplayName("generateForUser should handle Gemini HTTP 429 quota limit cleanly without throwing 500")
    void testGenerateForUser_QuotaExceeded_HandledGracefully() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        AiProjectGeneratorService spyService = spy(service);
        doThrow(new AiProjectGeneratorService.GeminiQuotaException("Gemini API quota exceeded (HTTP 429)"))
                .when(spyService).callGeminiApi(anyString());

        GenerateFreshProjectsResponse response = spyService.generateForUser(testUser.getEmail(), 3);

        assertNotNull(response);
        assertFalse(response.isGeminiContributed());
        assertEquals(0, response.getSavedCount());
        assertTrue(response.getMessage().contains("quota"));
        assertNotNull(response.getErrorDetail());
        assertTrue(response.getErrorDetail().contains("429"));
    }

    @Test
    @DisplayName("generateForUser should handle Gemini timeout cleanly without throwing 500")
    void testGenerateForUser_Timeout_HandledGracefully() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        AiProjectGeneratorService spyService = spy(service);
        doThrow(new AiProjectGeneratorService.GeminiTimeoutException("Gemini timeout after 60s"))
                .when(spyService).callGeminiApi(anyString());

        GenerateFreshProjectsResponse response = spyService.generateForUser(testUser.getEmail(), 3);

        assertNotNull(response);
        assertFalse(response.isGeminiContributed());
        assertEquals(0, response.getSavedCount());
        assertTrue(response.getMessage().contains("timeout window"));
        assertNotNull(response.getErrorDetail());
        assertTrue(response.getErrorDetail().toLowerCase().contains("timeout"));
    }

    @Test
    @DisplayName("generateForUser should handle generic Gemini provider error cleanly")
    void testGenerateForUser_ProviderError_HandledGracefully() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        AiProjectGeneratorService spyService = spy(service);
        doThrow(new AiProjectGeneratorService.GeminiProviderException("Internal provider crash"))
                .when(spyService).callGeminiApi(anyString());

        GenerateFreshProjectsResponse response = spyService.generateForUser(testUser.getEmail(), 3);

        assertNotNull(response);
        assertFalse(response.isGeminiContributed());
        assertEquals(0, response.getSavedCount());
        assertTrue(response.getMessage().contains("provider returned an error"));
        assertTrue(response.getErrorDetail().contains("Internal provider crash"));
    }

    @Test
    @DisplayName("generateForUser should handle malformed Gemini JSON without crashing")
    void testGenerateForUser_MalformedJson_HandledGracefully() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        AiProjectGeneratorService spyService = spy(service);
        String malformedJson = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Sorry, I cannot generate projects.\"}]}}]}";
        doReturn(malformedJson).when(spyService).callGeminiApi(anyString());

        GenerateFreshProjectsResponse response = spyService.generateForUser(testUser.getEmail(), 3);

        assertNotNull(response);
        assertFalse(response.isGeminiContributed());
        assertEquals(0, response.getGeneratedCount());
        assertEquals(0, response.getSavedCount());
        assertTrue(response.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("generateForUser should clamp requested count within bounds (1 to 5)")
    void testGenerateForUser_ClampsCount() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        ReflectionTestUtils.setField(service, "geminiApiKey", "");

        // Request 10 — should be clamped to 5
        GenerateFreshProjectsResponse r1 = service.generateForUser(testUser.getEmail(), 10);
        assertEquals(5, r1.getRequestedCount());

        // Request -2 — should be clamped to 1
        GenerateFreshProjectsResponse r2 = service.generateForUser(testUser.getEmail(), -2);
        assertEquals(1, r2.getRequestedCount());
    }

    @Test
    @DisplayName("RecommendationEngineService should yield different match scores for different assessment result sets")
    void testPersonalizationWithTwoAssessmentSets_ReflectedInRecommendationEngine() {
        // Build real RecommendationEngineService without mocks to verify actual scoring formula
        RecommendationEngineService realEngine = new RecommendationEngineService(
                projectRepository, assessmentResultRepository, new ProjectQualityGateService()
        );

        Project project = new Project();
        project.setId(501L);
        project.setTitle("Autonomous AI Sepsis Prediction System");
        project.setDomain(testDomain);
        project.setComplexityLevel("INTERMEDIATE");
        project.setEstimatedWeeks(16);
        project.setMinTeamSize(2);
        project.setMaxTeamSize(4);
        project.setQualityApproved(true);
        project.setQualityScore(85.0);
        project.setTargetBranches(List.of(testBranch));
        project.setRequiredSkills(List.of(testSkill));

        ProjectTechnology pt = new ProjectTechnology();
        pt.setProject(project);
        pt.setTechnology(testTech);
        project.setProjectTechnologies(List.of(pt));

        Map<Long, StudentSkill> declaredSkills = Map.of(testSkill.getId(), testProfile.getStudentSkills().getFirst());

        // Assessment Set A: High proficiency in Machine Learning (Score: 9, 90.0%, ADVANCED)
        AssessmentResult highResult = new AssessmentResult();
        highResult.setSkill(testSkill);
        highResult.setScore(9);
        highResult.setPercentage(90.0);
        highResult.setLevel(ProficiencyLevel.ADVANCED);
        Map<Long, AssessmentResult> highAssessments = Map.of(testSkill.getId(), highResult);

        ProjectRecommendationResponse highScored = realEngine.scoreProject(
                project, testProfile, highAssessments, declaredSkills
        );

        // Assessment Set B: Low proficiency in Machine Learning (Score: 2, 20.0%, BEGINNER)
        AssessmentResult lowResult = new AssessmentResult();
        lowResult.setSkill(testSkill);
        lowResult.setScore(2);
        lowResult.setPercentage(20.0);
        lowResult.setLevel(ProficiencyLevel.BEGINNER);
        Map<Long, AssessmentResult> lowAssessments = Map.of(testSkill.getId(), lowResult);

        ProjectRecommendationResponse lowScored = realEngine.scoreProject(
                project, testProfile, lowAssessments, declaredSkills
        );

        assertNotNull(highScored, "High proficiency student should receive valid recommendation");
        assertNotNull(lowScored, "Low proficiency student should still be evaluated");

        // The match score for High proficiency MUST be strictly greater than for Low proficiency
        assertTrue(highScored.getMatchScore() > lowScored.getMatchScore(),
                String.format("High proficiency match score (%.1f) must exceed low proficiency score (%.1f)",
                        highScored.getMatchScore(), lowScored.getMatchScore()));

        // Verify that High proficiency verified competency and Low proficiency triggered penalty or lower score
        assertTrue(highScored.getScoreBreakdown().getSkillFitScore() > 20.0, "High assessed skill should get >20.0 skill fit");
        assertTrue(lowScored.getScoreBreakdown().getSkillFitScore() < 10.0, "Low assessed skill should get reduced skill fit (<10.0)");
        assertTrue(highScored.getScoreBreakdown().getProficiencyScore() > lowScored.getScoreBreakdown().getProficiencyScore(),
                "Assessed mastery score must be higher for ADVANCED than BEGINNER");
    }

    @Test
    @DisplayName("generateForUser should support each project count from 1 to 5")
    void testGenerateForUser_SupportedCounts1To5() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());

        ReflectionTestUtils.setField(service, "geminiApiKey", "");

        for (int c = 1; c <= 5; c++) {
            GenerateFreshProjectsResponse resp = service.generateForUser(testUser.getEmail(), c);
            assertEquals(c, resp.getRequestedCount(), "Requested count " + c + " should be preserved");
        }
    }

    @Test
    @DisplayName("generateForUser should record recommendation history and update active recommendations")
    void testGenerateForUser_RecordsHistoryAndUpdatesActiveRecommendations() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(testProfile.getId()))
                .thenReturn(Collections.emptyList());
        when(projectRepository.findByStudentProfileId(testProfile.getId())).thenReturn(Collections.emptyList());

        when(branchRepository.findAll()).thenReturn(List.of(testBranch));
        when(technologyRepository.findAll()).thenReturn(List.of(testTech));
        when(skillRepository.findAll()).thenReturn(List.of(testSkill));
        when(projectRepository.existsByTitleIgnoreCase(anyString())).thenReturn(false);

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            if (p.getId() == null) p.setId(888L);
            return p;
        });

        ProjectRecommendationResponse scored = new ProjectRecommendationResponse();
        scored.setProjectId(888L);
        scored.setTitle("History Tracked Project");
        scored.setMatchScore(91.0);
        scored.setRankOrder(1);
        when(recommendationEngineService.scoreProject(any(Project.class), eq(testProfile), anyMap(), anyMap()))
                .thenReturn(scored);

        AiProjectGeneratorService spyService = spy(service);
        String mockGeminiJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "[{\\"title\\": \\"History Tracked Project\\", \\"problemStatement\\": \\"Problem X\\", \\"proposedSolution\\": \\"Solution Y\\"}]"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        doReturn(mockGeminiJson).when(spyService).callGeminiApi(anyString());

        GenerateFreshProjectsResponse response = spyService.generateForUser(testUser.getEmail(), 1);

        assertNotNull(response);
        assertEquals(1, response.getSavedCount());
        // Verify history repository save was called
        verify(historyRepository, times(1)).save(any(StudentRecommendationHistory.class));
        // Verify active recommendation repository delete and save were called
        verify(recommendationRepository, times(1)).deleteByStudentProfileId(testProfile.getId());
        verify(recommendationRepository, times(1)).save(any(StudentProjectRecommendation.class));
    }

    @Test
    @DisplayName("getGenerationHistoryForUser should return list of past recommendations")
    void testGetGenerationHistoryForUser_ReturnsList() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));

        Project p = new Project();
        p.setId(777L);
        p.setTitle("Prior Sepsis Alert");
        p.setIsCatalog(false);
        p.setComplexityLevel("INTERMEDIATE");
        p.setDomain(testDomain);
        p.setProblemStatement("ICU clinical deterioration.");

        StudentRecommendationHistory hist = new StudentRecommendationHistory(testProfile, p, 84.5, 1);
        hist.setId(101L);

        when(historyRepository.findByStudentProfileIdOrderByRecommendedAtDesc(testProfile.getId()))
                .thenReturn(List.of(hist));

        List<com.projectguard.dto.recommendation.RecommendationHistoryDto> history =
                service.getGenerationHistoryForUser(testUser.getEmail());

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Prior Sepsis Alert", history.getFirst().getProjectTitle());
        assertEquals(84.5, history.getFirst().getMatchScore());
        assertTrue(history.getFirst().isGeneratedIdea());
    }
}
