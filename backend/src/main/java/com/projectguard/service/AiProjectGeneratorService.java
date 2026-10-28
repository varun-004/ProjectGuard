package com.projectguard.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectguard.dto.recommendation.GenerateFreshProjectsResponse;
import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.dto.recommendation.RecommendationHistoryDto;
import com.projectguard.dto.recommendation.ScoreBreakdownDto;
import com.projectguard.entity.*;
import com.projectguard.exception.BadRequestException;
import com.projectguard.exception.ResourceNotFoundException;
import com.projectguard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AiProjectGeneratorService — Gemini-powered fresh project generation with score-based personalization.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Resolve the authenticated student's profile using the Security Context (never a client-supplied ID).</li>
 *   <li>Assemble the four student input groups (branch, technology, declared skills, actual assessment results).</li>
 *   <li>Retrieve prior generated project ideas to avoid exact-title and semantic duplicates on repeated requests.</li>
 *   <li>Call Google Gemini REST API securely (using x-goog-api-key header; never logging keys or credentials in URLs).</li>
 *   <li>Apply bounded retries for transient HTTP errors (500, 502, 503, 504) and network glitches.</li>
 *   <li>Robustly parse, extract, and validate structured JSON output.</li>
 *   <li>Check for duplicates against existing project titles (case-insensitive and intra-batch).</li>
 *   <li>Persist non-duplicate projects and link them to database Technologies and Skills.</li>
 *   <li>Score each project using the unmodified {@link RecommendationEngineService} so scores reflect actual student inputs.</li>
 *   <li>Track recommendation history in {@link StudentRecommendationHistoryRepository} to preserve history across quiz changes.</li>
 *   <li>Return transparent scoring, breakdown, and generation metadata reporting partial success honestly.</li>
 * </ol>
 */
@Service
public class AiProjectGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AiProjectGeneratorService.class);

    // ─── Configuration ───────────────────────────────────────────────────────
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-3.5-flash}")
    private String geminiModel;

    @Value("${gemini.api.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${gemini.api.max-generate-count:5}")
    private int maxGenerateCount;

    // ─── Gemini REST endpoint template (URL NEVER contains the API key) ───────
    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    // ─── Dependencies ────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final TechnologyRepository technologyRepository;
    private final EngineeringBranchRepository branchRepository;
    private final RecommendationEngineService recommendationEngineService;
    private final StudentRecommendationHistoryRepository historyRepository;
    private final StudentProjectRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public AiProjectGeneratorService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            AssessmentResultRepository assessmentResultRepository,
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            TechnologyRepository technologyRepository,
            EngineeringBranchRepository branchRepository,
            RecommendationEngineService recommendationEngineService,
            StudentRecommendationHistoryRepository historyRepository,
            StudentProjectRecommendationRepository recommendationRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.technologyRepository = technologyRepository;
        this.branchRepository = branchRepository;
        this.recommendationEngineService = recommendationEngineService;
        this.historyRepository = historyRepository;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = createConfiguredObjectMapper();
    }

    /** Backward-compatible constructor for testing. */
    public AiProjectGeneratorService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            AssessmentResultRepository assessmentResultRepository,
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            TechnologyRepository technologyRepository,
            EngineeringBranchRepository branchRepository,
            RecommendationEngineService recommendationEngineService) {
        this(userRepository, studentProfileRepository, assessmentResultRepository,
             projectRepository, skillRepository, technologyRepository,
             branchRepository, recommendationEngineService, null, null);
    }

    private static ObjectMapper createConfiguredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);
        return mapper;
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Resolves the configured Gemini API key from properties or GEMINI_API_KEY environment variable.
     * Never uses hardcoded keys.
     */
    public String getResolvedApiKey() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return geminiApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return null;
    }

    /** Returns {@code true} when a non-blank Gemini API key is configured. */
    public boolean isGeminiConfigured() {
        return getResolvedApiKey() != null;
    }

    /**
     * Generates fresh, personalised project ideas for the authenticated student
     * using the Gemini API, validates them, persists non-duplicate projects,
     * scores them via the existing {@link RecommendationEngineService}, and
     * records history.
     *
     * @param usernameOrEmail Principal name from the Security Context
     * @param requestedCount  Number of ideas to request (capped at max-generate-count)
     * @return populated {@link GenerateFreshProjectsResponse}
     */
    @Transactional
    public GenerateFreshProjectsResponse generateForUser(String usernameOrEmail, int requestedCount) {

        // ── 1. Resolve user and profile ────────────────────────────────────
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Please complete your student profile before requesting project generation."));

        // ── 2. Enforce count limits ─────────────────────────────────────────
        int clampedCount = Math.max(1, Math.min(requestedCount, maxGenerateCount));

        // ── 3. Build response metadata ──────────────────────────────────────
        GenerateFreshProjectsResponse response = new GenerateFreshProjectsResponse();
        response.setStudentProfileId(profile.getId());
        response.setStudentName(user.getUsername());
        if (profile.getBranch() != null) response.setBranchName(profile.getBranch().getName());
        if (profile.getDomain() != null) response.setDomainName(profile.getDomain().getName());
        if (profile.getTechnology() != null) response.setTechnologyName(profile.getTechnology().getName());
        response.setRequestedCount(clampedCount);
        response.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // ── 4. Fetch assessment results (Input Group 4: actual quiz scores) ─
        List<AssessmentResult> assessmentResults =
                assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(profile.getId());

        // ── 5. Fetch prior generated projects to prevent duplicates on repeated requests ─
        List<Project> priorProjects = projectRepository.findByStudentProfileId(profile.getId());

        // ── 6. Check Gemini configuration ───────────────────────────────────
        if (!isGeminiConfigured()) {
            log.warn("Gemini API key not configured; generate-fresh requires GEMINI_API_KEY environment variable.");
            response.setMessage("Gemini API key is not configured on the server. " +
                    "Please set the GEMINI_API_KEY environment variable to enable fresh project generation.");
            response.setGeminiContributed(false);
            response.setGeneratedCount(0);
            response.setSavedCount(0);
            response.setRecommendations(Collections.emptyList());
            return response;
        }

        // ── 7. Call Gemini API ──────────────────────────────────────────────
        List<Map<String, Object>> rawIdeas;
        try {
            String prompt = buildPrompt(profile, assessmentResults, priorProjects, clampedCount);
            String geminiJson = callGeminiApi(prompt);
            rawIdeas = parseGeminiOutput(geminiJson);
        } catch (GeminiQuotaException e) {
            log.warn("Gemini 429 quota exceeded for student '{}': {}", user.getUsername(), e.getMessage());
            response.setMessage("Gemini API quota has been reached. Please try again later.");
            response.setErrorDetail(e.getMessage());
            response.setGeminiContributed(false);
            response.setGeneratedCount(0);
            response.setSavedCount(0);
            response.setRecommendations(Collections.emptyList());
            return response;
        } catch (GeminiTimeoutException e) {
            log.warn("Gemini request timed out for student '{}': {}", user.getUsername(), e.getMessage());
            response.setMessage("Gemini AI service did not respond within the timeout window (" + timeoutSeconds + "s). Please try again.");
            response.setErrorDetail(e.getMessage());
            response.setGeminiContributed(false);
            response.setGeneratedCount(0);
            response.setSavedCount(0);
            response.setRecommendations(Collections.emptyList());
            return response;
        } catch (GeminiProviderException e) {
            log.error("Gemini provider error for student '{}': {}", user.getUsername(), e.getMessage());
            response.setMessage("Gemini AI provider returned an error. Please try again later.");
            response.setErrorDetail(e.getMessage());
            response.setGeminiContributed(false);
            response.setGeneratedCount(0);
            response.setSavedCount(0);
            response.setRecommendations(Collections.emptyList());
            return response;
        }

        if (rawIdeas == null || rawIdeas.isEmpty()) {
            log.warn("Gemini API returned zero valid project ideas for student '{}'", user.getUsername());
            response.setMessage("Gemini AI returned a response, but no project ideas passed validation. Please try again.");
            response.setErrorDetail("No valid structured project objects found in AI response.");
            response.setGeminiContributed(false);
            response.setGeneratedCount(0);
            response.setSavedCount(0);
            response.setRecommendations(Collections.emptyList());
            return response;
        }

        response.setGeminiContributed(true);
        response.setGeneratedCount(rawIdeas.size());

        // ── 8. Validate, de-duplicate, and persist ──────────────────────────
        List<Project> savedProjects = persistValidatedIdeas(rawIdeas, profile);
        response.setSavedCount(savedProjects.size());

        // ── 9. Score with the existing recommendation engine (unmodified) ───
        List<ProjectRecommendationResponse> scoredResponses = new ArrayList<>();
        Map<Long, AssessmentResult> latestSkillAssessments = buildLatestSkillAssessments(assessmentResults);
        Map<Long, StudentSkill> declaredSkills = buildDeclaredSkillsMap(profile);

        for (Project saved : savedProjects) {
            ProjectRecommendationResponse scored = recommendationEngineService.scoreProject(
                    saved, profile, latestSkillAssessments, declaredSkills);
            if (scored != null) {
                scored.setIsGeneratedIdea(true);
                scored.setIsFreshAlternative(true);
                scoredResponses.add(scored);
            } else {
                // Below threshold: include with transparent minimal response so student
                // can inspect generated idea without inflating score
                scoredResponses.add(buildMinimalResponse(saved));
            }
        }

        // Rank by match score descending (ties broken by quality score descending)
        scoredResponses.sort((a, b) -> {
            int cmp = Double.compare(b.getMatchScore(), a.getMatchScore());
            if (cmp != 0) return cmp;
            return Double.compare(
                    b.getQualityScore() != null ? b.getQualityScore() : 0.0,
                    a.getQualityScore() != null ? a.getQualityScore() : 0.0
            );
        });

        int rank = 1;
        for (ProjectRecommendationResponse r : scoredResponses) {
            r.setRankOrder(rank++);
        }

        response.setRecommendations(scoredResponses);

        // ── 10. Record in recommendation history ─────────────────────────────
        if (historyRepository != null) {
            for (ProjectRecommendationResponse scored : scoredResponses) {
                Project savedProj = savedProjects.stream()
                        .filter(p -> p.getId() != null && p.getId().equals(scored.getProjectId()))
                        .findFirst().orElse(null);
                if (savedProj != null) {
                    try {
                        historyRepository.save(new StudentRecommendationHistory(
                                profile, savedProj, scored.getMatchScore(), scored.getRankOrder()));
                    } catch (Exception e) {
                        log.warn("Failed to record recommendation history for project {}: {}", savedProj.getId(), e.getMessage());
                    }
                }
            }
        }

        // ── 11. Update active student recommendations ────────────────────────
        if (recommendationRepository != null && !scoredResponses.isEmpty()) {
            try {
                recommendationRepository.deleteByStudentProfileId(profile.getId());
                for (ProjectRecommendationResponse scored : scoredResponses) {
                    Project p = savedProjects.stream()
                            .filter(proj -> proj.getId() != null && proj.getId().equals(scored.getProjectId()))
                            .findFirst().orElse(null);
                    if (p != null) {
                        StudentProjectRecommendation spr = new StudentProjectRecommendation();
                        spr.setStudentProfile(profile);
                        spr.setProject(p);
                        spr.setMatchScore(scored.getMatchScore());
                        spr.setRankOrder(scored.getRankOrder());
                        ScoreBreakdownDto sb = scored.getScoreBreakdown();
                        if (sb != null) {
                            spr.setDomainFitScore(sb.getDomainFitScore());
                            spr.setTechFitScore(sb.getTechFitScore());
                            spr.setSkillFitScore(sb.getSkillFitScore());
                            spr.setProficiencyScore(sb.getProficiencyScore());
                            spr.setExperienceFitScore(sb.getExperienceFitScore());
                            spr.setTeamFitScore(sb.getTeamFitScore());
                            spr.setTimeFitScore(sb.getTimeFitScore());
                            spr.setPenaltyScore(sb.getPenaltyScore());
                        }
                        spr.setScoreExplanation(scored.getScoreExplanation());
                        if (scored.getRecommendationReasons() != null) {
                            spr.setRecommendationReasons(String.join("\n", scored.getRecommendationReasons()));
                        }
                        if (scored.getIdentifiedGaps() != null) {
                            spr.setIdentifiedGaps(String.join("\n", scored.getIdentifiedGaps()));
                        }
                        recommendationRepository.save(spr);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to update active student recommendations: {}", e.getMessage());
            }
        }

        // ── 12. Assemble honest status message ───────────────────────────────
        if (savedProjects.isEmpty()) {
            response.setMessage(String.format(
                    "Gemini returned %d idea%s, but all had duplicate titles matching existing projects. Please try generating again.",
                    rawIdeas.size(), rawIdeas.size() == 1 ? "" : "s"));
        } else if (savedProjects.size() < clampedCount) {
            response.setMessage(String.format(
                    "Generated %d project ideas (%d new project%s saved, %d duplicate title%s skipped), scored by your academic profile.",
                    rawIdeas.size(), savedProjects.size(), savedProjects.size() == 1 ? "" : "s",
                    rawIdeas.size() - savedProjects.size(), (rawIdeas.size() - savedProjects.size()) == 1 ? "" : "s"));
        } else {
            response.setMessage(String.format(
                    "Successfully generated %d fresh industry-level project idea%s, scored by your academic profile.",
                    savedProjects.size(), savedProjects.size() == 1 ? "" : "s"));
        }

        return response;
    }

    /**
     * Retrieve the saved recommendation history for a student in descending chronological order.
     */
    @Transactional(readOnly = true)
    public List<RecommendationHistoryDto> getGenerationHistoryForUser(String usernameOrEmail) {
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Student profile not found."));

        if (historyRepository == null) {
            return Collections.emptyList();
        }

        List<StudentRecommendationHistory> historyRecords =
                historyRepository.findByStudentProfileIdOrderByRecommendedAtDesc(profile.getId());

        List<RecommendationHistoryDto> dtos = new ArrayList<>();
        for (StudentRecommendationHistory h : historyRecords) {
            Project p = h.getProject();
            if (p != null) {
                String domainName = p.getDomain() != null ? p.getDomain().getName() : null;
                boolean isGen = (p.getIsCatalog() != null && !p.getIsCatalog());
                dtos.add(new RecommendationHistoryDto(
                        h.getId(),
                        p.getId(),
                        p.getTitle(),
                        h.getMatchScore(),
                        h.getRankOrder(),
                        h.getRecommendedAt(),
                        p.getComplexityLevel(),
                        domainName,
                        p.getProblemStatement(),
                        p.getIndustryValue(),
                        isGen
                ));
            }
        }
        return dtos;
    }

    // ─── Prompt Builder ───────────────────────────────────────────────────────

    /**
     * Constructs the Gemini prompt using all four student input groups and previously generated projects:
     * 1. Engineering branch and academic level
     * 2. Primary technology stack
     * 3. Declared skills and proficiency levels
     * 4. Actual assessment quiz results in original chronological order
     * 5. Prior project ideas to prevent duplicate titles/topics
     */
    private String buildPrompt(StudentProfile profile,
                                List<AssessmentResult> assessmentResults,
                                List<Project> priorProjects,
                                int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert academic project advisor and industry engineering architect. ");
        sb.append("Generate exactly ").append(count)
          .append(" innovative, industry-level project idea").append(count > 1 ? "s" : "");
        sb.append(" for an engineering student with the following comprehensive academic profile. ");
        sb.append("Each project must solve a real-world problem, avoid generic CRUD/tutorial applications, ");
        sb.append("and be suitable for an engineering capstone project.\n\n");

        // Input Group 1: Engineering Branch and Academic Level
        sb.append("=== INPUT GROUP 1: ENGINEERING BRANCH & ACADEMIC LEVEL ===\n");
        if (profile.getBranch() != null) {
            sb.append("Engineering Branch: ").append(profile.getBranch().getName()).append("\n");
        }
        if (profile.getAcademicLevel() != null) {
            sb.append("Academic Level: ").append(profile.getAcademicLevel()).append("\n");
        }
        if (profile.getDomain() != null) {
            sb.append("Preferred Domain: ").append(profile.getDomain().getName()).append("\n");
        }

        // Input Group 2: Technologies
        sb.append("\n=== INPUT GROUP 2: PRIMARY TECHNOLOGY / STACK ===\n");
        if (profile.getTechnology() != null) {
            sb.append("Primary Technology Stack: ").append(profile.getTechnology().getName()).append("\n");
        } else {
            sb.append("Primary Technology: General Engineering Stack\n");
        }

        // Input Group 3: Declared Skills and Proficiency Levels
        sb.append("\n=== INPUT GROUP 3: DECLARED SKILLS & PROFICIENCY ===\n");
        if (profile.getStudentSkills() != null && !profile.getStudentSkills().isEmpty()) {
            for (StudentSkill ss : profile.getStudentSkills()) {
                if (ss.getSkill() != null) {
                    sb.append("  - ").append(ss.getSkill().getName());
                    if (ss.getProficiencyLevel() != null) {
                        sb.append(" (Level: ").append(ss.getProficiencyLevel().name()).append(")");
                    }
                    if (ss.getYearsOfExperience() != null && ss.getYearsOfExperience() > 0) {
                        sb.append(", ").append(ss.getYearsOfExperience()).append(" yr(s) experience");
                    }
                    sb.append("\n");
                }
            }
        } else {
            sb.append("  (No declared skills in profile)\n");
        }

        // Input Group 4: Actual Assessment / Quiz Results in Original Chronological Order
        sb.append("\n=== INPUT GROUP 4: ACTUAL ASSESSMENT QUIZ RESULTS ===\n");
        sb.append("(Preserved in exact chronological order — reflect these real strengths and skill gaps):\n");
        if (!assessmentResults.isEmpty()) {
            for (AssessmentResult ar : assessmentResults) {
                if (ar.getSkill() != null) {
                    sb.append("  - Skill: ").append(ar.getSkill().getName());
                    sb.append(" | Raw Score: ").append(ar.getScore());
                    sb.append(" | Percentage: ").append(String.format("%.1f", ar.getPercentage())).append("%");
                    if (ar.getLevel() != null) {
                        sb.append(" | Assessed Level: ").append(ar.getLevel().name());
                    }
                    sb.append("\n");
                }
            }
        } else {
            sb.append("  (No formal quiz assessments completed yet)\n");
        }

        if (profile.getInterests() != null && !profile.getInterests().isBlank()) {
            sb.append("\nStudent Interests: ").append(profile.getInterests()).append("\n");
        }
        if (profile.getTeamSize() != null) {
            sb.append("Team Size: ").append(profile.getTeamSize()).append(" members\n");
        }
        if (profile.getAvailableTimeWeeks() != null) {
            sb.append("Available Timeline: ").append(profile.getAvailableTimeWeeks()).append(" weeks\n");
        }

        // Prior Ideas to Avoid Duplication
        if (priorProjects != null && !priorProjects.isEmpty()) {
            sb.append("\n=== PREVIOUSLY GENERATED / EXISTING PROJECTS (DO NOT REPEAT) ===\n");
            sb.append("The student has already received the following project ideas. ");
            sb.append("You MUST propose completely distinct, non-duplicate ideas that explore new problems and technologies:\n");
            for (Project prior : priorProjects) {
                if (prior.getTitle() != null && !prior.getTitle().isBlank()) {
                    sb.append("  - ").append(prior.getTitle().trim()).append("\n");
                }
            }
            sb.append("DO NOT repeat, clone, or create minor variations of any existing project titles listed above.\n");
        }

        sb.append("\n=== INSTRUCTIONS & JSON SCHEMA ===\n");
        sb.append("Generate EXACTLY ").append(count).append(" project idea").append(count > 1 ? "s" : "");
        sb.append(". Return ONLY a valid JSON array (no markdown code blocks, no preamble, no commentary). ");
        sb.append("Each JSON object MUST include these exact keys:\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"title\": \"<innovative, specific real-world project title>\",\n");
        sb.append("    \"problemStatement\": \"<clear measurable real-world problem and pain point>\",\n");
        sb.append("    \"proposedSolution\": \"<technical architectural solution and methodology>\",\n");
        sb.append("    \"targetBranches\": [\"").append(profile.getBranch() != null ? profile.getBranch().getName() : "Computer Science and Engineering").append("\"],\n");
        sb.append("    \"technologies\": [\"").append(profile.getTechnology() != null ? profile.getTechnology().getName() : "Python").append("\"],\n");
        sb.append("    \"requiredSkills\": [\"<skill1>\", \"<skill2>\"],\n");
        sb.append("    \"optionalSkills\": [\"<optionalSkill>\"],\n");
        sb.append("    \"mvpScope\": \"<concrete deliverables for MVP within timeline>\",\n");
        sb.append("    \"dataToolsHardware\": \"<datasets, APIs, hardware, or tools required>\",\n");
        sb.append("    \"evaluationMetrics\": \"<quantitative KPIs e.g. latency, accuracy, throughput>\",\n");
        sb.append("    \"risks\": \"<technical bottlenecks and risk mitigation strategies>\",\n");
        sb.append("    \"personalizationRationale\": \"<why this aligns with the student's assessed strengths and addresses their skill gaps>\",\n");
        sb.append("    \"complexityLevel\": \"INTERMEDIATE\",\n");
        sb.append("    \"estimatedWeeks\": ").append(profile.getAvailableTimeWeeks() != null ? profile.getAvailableTimeWeeks() : 12).append(",\n");
        sb.append("    \"minTeamSize\": ").append(profile.getTeamSize() != null ? Math.max(1, profile.getTeamSize() - 1) : 2).append(",\n");
        sb.append("    \"maxTeamSize\": ").append(profile.getTeamSize() != null ? profile.getTeamSize() + 1 : 4).append(",\n");
        sb.append("    \"industryValue\": \"<commercial or societal impact>\",\n");
        sb.append("    \"innovationDifferentiator\": \"<what makes this novel over standard student projects>\"\n");
        sb.append("  }\n");
        sb.append("]\n\n");
        sb.append("Strict Rules:\n");
        sb.append("1. Zero generic CRUD clones (no simple bookstore, hospital record, or student portal CRUDs).\n");
        sb.append("2. In 'technologies', include the student's primary stack where appropriate.\n");
        sb.append("3. In 'requiredSkills', use standard skills from the student's branch and declared/assessed skills so they can be assessed by the scoring engine.\n");
        sb.append("4. All projects must have distinct, creative titles.\n");
        sb.append("5. Output ONLY the raw JSON array starting with '[' and ending with ']'.\n");

        return sb.toString();
    }

    // ─── Gemini HTTP Client ───────────────────────────────────────────────────

    /**
     * Calls the Gemini REST API using Java's built-in HttpClient with bounded retries for transient failures.
     * The API key is sent exclusively in the {@code x-goog-api-key} header.
     * The URL and log entries NEVER contain API credentials.
     */
    String callGeminiApi(String prompt) {
        String apiKey = getResolvedApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiProviderException("Gemini API key is not configured (GEMINI_API_KEY).");
        }

        String url = String.format(GEMINI_URL_TEMPLATE, geminiModel);

        String requestBody;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
            ));
            payload.put("generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 8192,
                    "topP", 0.95
            ));
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new GeminiProviderException("Failed to serialize Gemini request payload: " + e.getMessage());
        }

        int maxAttempts = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = httpResponse.statusCode();
                String responseBody = httpResponse.body() != null ? httpResponse.body() : "";

                if (statusCode == 429) {
                    String errorMsg = extractErrorMessage(responseBody, "Gemini API rate limit or quota exceeded");
                    log.warn("Gemini API quota exceeded (HTTP 429). Model: '{}'. Safe detail: {}", geminiModel, sanitize(errorMsg, apiKey));
                    throw new GeminiQuotaException("Gemini API quota exceeded (HTTP 429): " + sanitize(errorMsg, apiKey));
                }

                // Eligible transient server errors for bounded retry
                if ((statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504) && attempt < maxAttempts) {
                    log.warn("Gemini returned transient status {} on attempt {}. Retrying once...", statusCode, attempt);
                    try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }

                if (statusCode < 200 || statusCode >= 300) {
                    String errorMsg = extractErrorMessage(responseBody, "HTTP " + statusCode);
                    log.error("Gemini API call failed. Status: {}, Model: '{}', Safe detail: {}", statusCode, geminiModel, sanitize(errorMsg, apiKey));
                    throw new GeminiProviderException(String.format("Gemini API returned HTTP %d: %s", statusCode, sanitize(errorMsg, apiKey)));
                }

                if (responseBody.isBlank()) {
                    throw new GeminiProviderException("Gemini API returned an empty response body (HTTP 200).");
                }

                return responseBody;

            } catch (GeminiQuotaException | GeminiProviderException e) {
                throw e;
            } catch (java.net.http.HttpTimeoutException e) {
                log.warn("Gemini request timed out after {}s for model '{}'", timeoutSeconds, geminiModel);
                throw new GeminiTimeoutException("Gemini API timed out after " + timeoutSeconds + "s");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GeminiTimeoutException("Gemini API request was interrupted");
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    log.warn("Gemini request failed on attempt {} ({}). Retrying...", attempt, e.getClass().getSimpleName());
                    try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                String safeMsg = sanitize(e.getMessage(), apiKey);
                log.error("Gemini API connection error: {}", safeMsg);
                throw new GeminiProviderException("Gemini API connection error: " + safeMsg);
            }
        }

        throw new GeminiProviderException("Gemini API call failed after retries: " + (lastException != null ? lastException.getMessage() : "unknown"));
    }

    // ─── Response Parser and Validator ───────────────────────────────────────

    /**
     * Parses the Gemini HTTP response body, inspecting candidates and safety blocks,
     * and extracts the validated JSON array of project ideas.
     */
    private List<Map<String, Object>> parseGeminiOutput(String geminiResponseBody) {
        try {
            JsonNode root = objectMapper.readTree(geminiResponseBody);

            // 1. Check prompt feedback / safety blocks
            JsonNode promptFeedback = root.path("promptFeedback");
            if (!promptFeedback.isMissingNode() && promptFeedback.has("blockReason")) {
                String blockReason = promptFeedback.path("blockReason").asText();
                log.warn("Gemini prompt was blocked by safety filters: {}", blockReason);
                throw new GeminiProviderException("Gemini prompt was blocked by safety filters: " + blockReason);
            }

            // 2. Extract candidates
            JsonNode candidatesNode = root.path("candidates");
            if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
                log.warn("Gemini response contains no candidates. Root response: {}",
                        geminiResponseBody.substring(0, Math.min(300, geminiResponseBody.length())));
                return Collections.emptyList();
            }

            JsonNode candidate = candidatesNode.get(0);
            String finishReason = candidate.path("finishReason").asText("UNKNOWN");

            // 3. Extract text from all content parts (ignoring thoughts)
            StringBuilder fullText = new StringBuilder();
            JsonNode partsNode = candidate.path("content").path("parts");
            if (partsNode.isArray()) {
                for (JsonNode part : partsNode) {
                    if (!part.path("thought").asBoolean(false) && part.has("text")) {
                        fullText.append(part.path("text").asText());
                    }
                }
            }

            if (fullText.isEmpty()) {
                log.warn("Gemini response candidate has no text part. Finish reason: {}", finishReason);
                return Collections.emptyList();
            }

            // 4. Robust JSON extraction
            String rawJson = extractJsonArray(fullText.toString());
            JsonNode arrayNode = objectMapper.readTree(rawJson);

            if (!arrayNode.isArray()) {
                log.warn("Gemini output was not a JSON array. Extracted text snippet: {}",
                        rawJson.substring(0, Math.min(200, rawJson.length())));
                return Collections.emptyList();
            }

            TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : arrayNode) {
                if (isValidProjectNode(item)) {
                    result.add(objectMapper.convertValue(item, mapType));
                } else {
                    log.warn("Skipping Gemini idea — missing required fields (title, problemStatement, or proposedSolution). Title: '{}'",
                            item.path("title").asText("(no title)"));
                }
            }
            return result;

        } catch (GeminiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Robustly extracts a JSON array substring from text, handling markdown fences
     * (````json ... ````) and conversational introductions/trailers.
     */
    private String extractJsonArray(String text) {
        if (text == null) return "";
        String trimmed = text.trim();

        // 1. Strip markdown fences if present
        int codeFenceStart = trimmed.indexOf("```");
        if (codeFenceStart >= 0) {
            int afterFence = trimmed.indexOf('\n', codeFenceStart);
            if (afterFence >= 0) {
                int codeFenceEnd = trimmed.lastIndexOf("```");
                if (codeFenceEnd > afterFence) {
                    trimmed = trimmed.substring(afterFence + 1, codeFenceEnd).trim();
                }
            }
        }

        // 2. Locate outermost JSON array brackets
        int firstBracket = trimmed.indexOf('[');
        int lastBracket = trimmed.lastIndexOf(']');
        if (firstBracket >= 0 && lastBracket > firstBracket) {
            return trimmed.substring(firstBracket, lastBracket + 1).trim();
        }

        return trimmed;
    }

    /**
     * Validates that the three essential fields for a {@link Project} are present and non-blank.
     */
    private boolean isValidProjectNode(JsonNode node) {
        String title = node.path("title").asText("").trim();
        String problem = node.path("problemStatement").asText("").trim();
        String solution = node.path("proposedSolution").asText("").trim();
        return !title.isEmpty() && !problem.isEmpty() && !solution.isEmpty();
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    /**
     * Maps validated Gemini ideas to {@link Project} entities, resolves and links
     * technologies and skills, and saves them within the current {@code @Transactional} boundary.
     * Duplicate titles (case-insensitive and intra-batch) are safely skipped. Catalog projects are untouched.
     */
    private List<Project> persistValidatedIdeas(List<Map<String, Object>> ideas, StudentProfile profile) {
        List<Project> saved = new ArrayList<>();
        Set<String> seenTitlesInBatch = new HashSet<>();

        for (Map<String, Object> idea : ideas) {
            String title = getString(idea, "title");
            if (title == null || title.isBlank()) continue;
            String trimmedTitle = title.trim();
            String lowerTitle = trimmedTitle.toLowerCase();

            // Intra-batch duplicate check
            if (!seenTitlesInBatch.add(lowerTitle)) {
                log.info("Skipping intra-batch duplicate project title: '{}'", trimmedTitle);
                continue;
            }

            // Case-insensitive duplicate check before inserting
            if (projectRepository.existsByTitleIgnoreCase(trimmedTitle)) {
                log.info("Skipping duplicate project title (case-insensitive match): '{}'", trimmedTitle);
                continue;
            }

            try {
                Project project = new Project();
                project.setTitle(trimmedTitle);
                project.setIsCatalog(false);           // Generated idea, not pre-seeded catalog
                project.setStudentProfile(profile);
                project.setQualityApproved(true);
                project.setQualityScore(80.0);          // Baseline for verified Gemini ideas

                // Map Gemini JSON fields to Project entity fields
                project.setProblemStatement(getString(idea, "problemStatement"));
                project.setProposedSolution(getString(idea, "proposedSolution"));
                project.setDescription(buildDescription(idea));
                project.setIndustryValue(getString(idea, "industryValue"));
                project.setInnovationDifferentiator(getString(idea, "innovationDifferentiator"));
                project.setExpectedOutcomeKpis(getString(idea, "evaluationMetrics"));
                project.setDataRequirements(getString(idea, "dataToolsHardware"));
                project.setConstraintsDependencies(getString(idea, "risks"));
                project.setFunctionalRequirements(getString(idea, "mvpScope"));
                project.setDeploymentAcademicDepth(getString(idea, "personalizationRationale"));

                String complexity = getString(idea, "complexityLevel");
                project.setComplexityLevel(
                        (complexity != null && !complexity.isBlank()) ? complexity.toUpperCase() : "INTERMEDIATE");
                project.setAcademicLevelSuitability("UG_AND_PG");
                project.setMinTeamSize(getInt(idea, "minTeamSize", 2));
                project.setMaxTeamSize(getInt(idea, "maxTeamSize", 4));
                project.setEstimatedWeeks(getInt(idea, "estimatedWeeks", 12));

                // Assign student's domain so the scoring engine computes domain fit
                if (profile.getDomain() != null) {
                    project.setDomain(profile.getDomain());
                }

                // Resolve target branches from DB; fall back to student's branch
                List<String> branchNames = getStringList(idea, "targetBranches");
                project.setTargetBranches(resolveBranches(branchNames, profile));

                // Initial save to establish project ID
                Project initialSaved = projectRepository.save(project);

                // Resolve and link technologies
                List<String> techNames = getStringList(idea, "technologies");
                initialSaved.setProjectTechnologies(resolveTechnologies(techNames, initialSaved, profile));

                // Resolve and link required skills
                List<String> skillNames = getStringList(idea, "requiredSkills");
                initialSaved.setRequiredSkills(resolveSkills(skillNames, profile));

                Project finalSaved = projectRepository.save(initialSaved);
                saved.add(finalSaved);
                log.info("Saved Gemini-generated project: '{}' (id={})", finalSaved.getTitle(), finalSaved.getId());

            } catch (Exception e) {
                log.error("Failed to persist Gemini project '{}': {}", getString(idea, "title"), e.getMessage());
            }
        }

        return saved;
    }

    private String buildDescription(Map<String, Object> idea) {
        StringBuilder desc = new StringBuilder();
        String mvp = getString(idea, "mvpScope");
        String personalization = getString(idea, "personalizationRationale");
        if (mvp != null && !mvp.isBlank()) {
            desc.append("MVP Scope: ").append(mvp).append("\n\n");
        }
        if (personalization != null && !personalization.isBlank()) {
            desc.append("Personalization Rationale: ").append(personalization);
        }
        String result = desc.toString().trim();
        return result.isBlank() ? null : result;
    }

    private List<EngineeringBranch> resolveBranches(List<String> names, StudentProfile profile) {
        List<EngineeringBranch> result = new ArrayList<>();
        List<EngineeringBranch> allBranches = branchRepository.findAll();
        if (names != null) {
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                String trimmed = name.trim();
                allBranches.stream()
                        .filter(b -> b.getName().equalsIgnoreCase(trimmed) ||
                                     b.getName().toLowerCase().contains(trimmed.toLowerCase()))
                        .findFirst()
                        .ifPresent(result::add);
            }
        }
        if (result.isEmpty() && profile.getBranch() != null) {
            result.add(profile.getBranch());
        }
        return result;
    }

    /**
     * Resolves technology names to existing DB entities and links them to the project.
     * If no match is found, links the student's primary technology.
     */
    private List<ProjectTechnology> resolveTechnologies(List<String> names, Project project, StudentProfile profile) {
        List<ProjectTechnology> result = new ArrayList<>();
        List<Technology> allTechs = technologyRepository.findAll();
        Set<Long> addedTechIds = new HashSet<>();

        if (names != null) {
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                String trimmed = name.trim();
                Technology matched = allTechs.stream()
                        .filter(t -> t.getName().equalsIgnoreCase(trimmed))
                        .findFirst()
                        .orElse(null);
                if (matched == null) {
                    matched = allTechs.stream()
                            .filter(t -> trimmed.toLowerCase().contains(t.getName().toLowerCase()) ||
                                         t.getName().toLowerCase().contains(trimmed.toLowerCase()))
                            .findFirst()
                            .orElse(null);
                }
                if (matched != null && addedTechIds.add(matched.getId())) {
                    ProjectTechnology pt = new ProjectTechnology();
                    pt.setProject(project);
                    pt.setTechnology(matched);
                    result.add(pt);
                }
            }
        }

        // Fallback: If no technology matched, link student's primary technology
        if (result.isEmpty() && profile.getTechnology() != null && addedTechIds.add(profile.getTechnology().getId())) {
            ProjectTechnology pt = new ProjectTechnology();
            pt.setProject(project);
            pt.setTechnology(profile.getTechnology());
            result.add(pt);
        }

        return result;
    }

    /**
     * Resolves skill names to existing Skill entities in the database.
     * Uses exact and substring matching. If none match, includes the student's declared skills
     * so the recommendation engine can compute competency fit based on actual quiz scores.
     */
    private List<Skill> resolveSkills(List<String> names, StudentProfile profile) {
        List<Skill> result = new ArrayList<>();
        List<Skill> allSkills = skillRepository.findAll();
        Set<Long> addedSkillIds = new HashSet<>();

        if (names != null) {
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                String trimmed = name.trim();
                Skill matched = allSkills.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(trimmed))
                        .findFirst()
                        .orElse(null);
                if (matched == null) {
                    matched = allSkills.stream()
                            .filter(s -> trimmed.toLowerCase().contains(s.getName().toLowerCase()) ||
                                         s.getName().toLowerCase().contains(trimmed.toLowerCase()))
                            .findFirst()
                            .orElse(null);
                }
                if (matched != null && addedSkillIds.add(matched.getId())) {
                    result.add(matched);
                }
            }
        }

        // Fallback: If no skills matched, link student's declared skills
        if (result.isEmpty() && profile.getStudentSkills() != null) {
            for (StudentSkill ss : profile.getStudentSkills()) {
                if (ss.getSkill() != null && addedSkillIds.add(ss.getSkill().getId())) {
                    result.add(ss.getSkill());
                }
            }
        }

        return result;
    }

    // ─── Scoring Helpers ──────────────────────────────────────────────────────

    private Map<Long, AssessmentResult> buildLatestSkillAssessments(List<AssessmentResult> results) {
        Map<Long, AssessmentResult> map = new HashMap<>();
        for (AssessmentResult ar : results) {
            if (ar.getSkill() != null) {
                Long skillId = ar.getSkill().getId();
                if (!map.containsKey(skillId) || ar.getPercentage() > map.get(skillId).getPercentage()) {
                    map.put(skillId, ar);
                }
            }
        }
        return map;
    }

    private Map<Long, StudentSkill> buildDeclaredSkillsMap(StudentProfile profile) {
        Map<Long, StudentSkill> map = new HashMap<>();
        if (profile.getStudentSkills() != null) {
            for (StudentSkill ss : profile.getStudentSkills()) {
                if (ss.getSkill() != null) {
                    map.put(ss.getSkill().getId(), ss);
                }
            }
        }
        return map;
    }

    private ProjectRecommendationResponse buildMinimalResponse(Project project) {
        ProjectRecommendationResponse r = new ProjectRecommendationResponse();
        r.setProjectId(project.getId());
        r.setTitle(project.getTitle());
        r.setMatchScore(0.0);
        r.setQualityScore(project.getQualityScore());
        r.setDescription(project.getDescription());
        r.setProblemStatement(project.getProblemStatement());
        r.setProposedSolution(project.getProposedSolution());
        r.setExpectedOutcomeKpis(project.getExpectedOutcomeKpis());
        r.setIndustryValue(project.getIndustryValue());
        r.setComplexityLevel(project.getComplexityLevel());
        r.setMinTeamSize(project.getMinTeamSize());
        r.setMaxTeamSize(project.getMaxTeamSize());
        r.setEstimatedWeeks(project.getEstimatedWeeks());
        r.setIsGeneratedIdea(true);
        r.setIsFreshAlternative(true);
        if (project.getDomain() != null) {
            r.setDomainId(project.getDomain().getId());
            r.setDomainName(project.getDomain().getName());
        }
        r.setScoreExplanation("Match score below minimum threshold (" +
                RecommendationEngineService.MIN_MATCH_THRESHOLD + "%) — project generated but below scoring threshold.");
        return r;
    }

    // ─── Utility Helpers ──────────────────────────────────────────────────────

    private List<String> getStringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        try {
            Object val = map.get(key);
            if (val instanceof Number num) return num.intValue();
            if (val != null) return Integer.parseInt(val.toString().trim());
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    private String extractErrorMessage(String responseBody, String defaultMsg) {
        try {
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode node = objectMapper.readTree(responseBody);
                JsonNode errorNode = node.path("error");
                if (errorNode.has("message")) {
                    return errorNode.path("message").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }

    private String sanitize(String text, String apiKey) {
        if (text == null) return "";
        if (apiKey != null && !apiKey.isBlank() && text.contains(apiKey)) {
            return text.replace(apiKey, "[REDACTED]");
        }
        return text;
    }

    // ─── Typed Provider Exceptions ────────────────────────────────────────────

    static class GeminiQuotaException extends RuntimeException {
        GeminiQuotaException(String msg) { super(msg); }
    }

    static class GeminiTimeoutException extends RuntimeException {
        GeminiTimeoutException(String msg) { super(msg); }
    }

    static class GeminiProviderException extends RuntimeException {
        GeminiProviderException(String msg) { super(msg); }
    }
}
