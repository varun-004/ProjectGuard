package com.projectguard.service;

import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.dto.recommendation.RecommendationListResponse;
import com.projectguard.dto.recommendation.ScoreBreakdownDto;
import com.projectguard.entity.*;
import com.projectguard.exception.BadRequestException;
import com.projectguard.exception.ResourceNotFoundException;
import com.projectguard.repository.ProjectRepository;
import com.projectguard.repository.StudentProfileRepository;
import com.projectguard.repository.StudentProjectRecommendationRepository;
import com.projectguard.repository.StudentRecommendationHistoryRepository;
import com.projectguard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RecommendationService {

    private final RecommendationEngineService recommendationEngine;
    private final StudentProjectRecommendationRepository recommendationRepository;
    private final StudentRecommendationHistoryRepository historyRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectIdeaGeneratorService ideaGeneratorService;

    public RecommendationService(RecommendationEngineService recommendationEngine,
                                 StudentProjectRecommendationRepository recommendationRepository,
                                 StudentRecommendationHistoryRepository historyRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 UserRepository userRepository,
                                 ProjectRepository projectRepository,
                                 ProjectIdeaGeneratorService ideaGeneratorService) {
        this.recommendationEngine = recommendationEngine;
        this.recommendationRepository = recommendationRepository;
        this.historyRepository = historyRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.ideaGeneratorService = ideaGeneratorService;
    }

    /**
     * Get or calculate fresh recommendations for the authenticated user.
     */
    @Transactional
    public RecommendationListResponse getRecommendationsForUser(String usernameOrEmail, boolean forceRefresh) {
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Please complete your student profile before requesting project recommendations."));

        List<StudentProjectRecommendation> persisted =
                recommendationRepository.findByStudentProfileIdOrderByRankOrderAsc(profile.getId());

        List<ProjectRecommendationResponse> resultResponses;

        if (!persisted.isEmpty() && !forceRefresh) {
            // Load from database to satisfy persistence reload requirement
            resultResponses = new ArrayList<>();
            for (StudentProjectRecommendation rec : persisted) {
                Project p = rec.getProject();
                resultResponses.add(mapPersistedToDto(rec, p));
            }
        } else {
            // Retrieve history of previously recommended project IDs to avoid duplicate recommendations
            Set<Long> previouslyRecommendedIds = historyRepository.findRecommendedProjectIdsByStudentProfileId(profile.getId());

            // Calculate fresh recommendations preferring relevant alternatives and generating new ideas when insufficient
            resultResponses = recommendationEngine.generateRecommendations(profile, previouslyRecommendedIds);

            // Persist into database idempotently
            recommendationRepository.deleteByStudentProfileId(profile.getId());

            for (ProjectRecommendationResponse dto : resultResponses) {
                Project p = projectRepository.findById(dto.getProjectId())
                        .orElse(null);
                if (p != null) {
                    StudentProjectRecommendation rec = new StudentProjectRecommendation();
                    rec.setStudentProfile(profile);
                    rec.setProject(p);
                    rec.setMatchScore(dto.getMatchScore());
                    rec.setRankOrder(dto.getRankOrder());

                    ScoreBreakdownDto sb = dto.getScoreBreakdown();
                    if (sb != null) {
                        rec.setDomainFitScore(sb.getDomainFitScore());
                        rec.setTechFitScore(sb.getTechFitScore());
                        rec.setSkillFitScore(sb.getSkillFitScore());
                        rec.setProficiencyScore(sb.getProficiencyScore());
                        rec.setExperienceFitScore(sb.getExperienceFitScore());
                        rec.setTeamFitScore(sb.getTeamFitScore());
                        rec.setTimeFitScore(sb.getTimeFitScore());
                        rec.setPenaltyScore(sb.getPenaltyScore());
                    }

                    rec.setScoreExplanation(dto.getScoreExplanation());
                    rec.setRecommendationReasons(String.join("\n", dto.getRecommendationReasons()));
                    rec.setIdentifiedGaps(String.join("\n", dto.getIdentifiedGaps()));

                    StudentProjectRecommendation saved = recommendationRepository.save(rec);
                    dto.setRecommendationId(saved.getId());

                    // Record recommendation in user history for future freshness tracking
                    historyRepository.save(new StudentRecommendationHistory(profile, p, dto.getMatchScore(), dto.getRankOrder()));
                }
            }
        }

        // Build list response
        RecommendationListResponse response = new RecommendationListResponse();
        response.setStudentProfileId(profile.getId());
        response.setStudentName(user.getUsername());
        if (profile.getBranch() != null) response.setBranchName(profile.getBranch().getName());
        if (profile.getDomain() != null) response.setDomainName(profile.getDomain().getName());
        if (profile.getTechnology() != null) response.setTechnologyName(profile.getTechnology().getName());
        response.setAcademicLevel(profile.getAcademicLevel());
        response.setTeamSize(profile.getTeamSize());
        response.setAvailableTimeWeeks(profile.getAvailableTimeWeeks());
        response.setTotalRecommendations(resultResponses.size());
        response.setRecommendations(resultResponses);
        response.setEvaluatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        response.setDisclaimer("Match percentage measures candidate-project compatibility and skill readiness, not guaranteed project success. Project Quality is evaluated separately based on real-world engineering standards.");
        if (ideaGeneratorService != null) {
            response.setAiConfigurationStatus(ideaGeneratorService.getAiConfigurationStatus());
        }

        if (resultResponses.isEmpty()) {
            response.setMessage("No matching projects found that satisfy your branch, academic level, and time constraints. " +
                    "Try reviewing your available time weeks or selecting an additional technical domain in your profile.");
        } else {
            response.setMessage(String.format("Found %d high-relevance real-world projects matching your assessed technical background.", resultResponses.size()));
        }

        return response;
    }

    /**
     * Get detailed recommendation for a single project for the current user.
     */
    @Transactional(readOnly = true)
    public ProjectRecommendationResponse getProjectRecommendationDetail(String usernameOrEmail, Long projectId) {
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Student profile required"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        Optional<StudentProjectRecommendation> opt =
                recommendationRepository.findByStudentProfileIdAndProjectId(profile.getId(), projectId);

        if (opt.isPresent()) {
            return mapPersistedToDto(opt.get(), project);
        }

        // Compute on the fly if not persisted yet
        List<ProjectRecommendationResponse> recs = recommendationEngine.generateRecommendations(profile);
        return recs.stream()
                .filter(r -> r.getProjectId().equals(projectId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No recommendation found for project ID: " + projectId));
    }

    private ProjectRecommendationResponse mapPersistedToDto(StudentProjectRecommendation rec, Project p) {
        ProjectRecommendationResponse dto = new ProjectRecommendationResponse();
        dto.setRecommendationId(rec.getId());
        dto.setProjectId(p.getId());
        dto.setRankOrder(rec.getRankOrder());
        dto.setMatchScore(rec.getMatchScore());
        dto.setQualityScore(p.getQualityScore());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());

        if (p.getDomain() != null) {
            dto.setDomainId(p.getDomain().getId());
            dto.setDomainName(p.getDomain().getName());
        }

        dto.setComplexityLevel(p.getComplexityLevel());
        dto.setAcademicLevelSuitability(p.getAcademicLevelSuitability());
        dto.setMinTeamSize(p.getMinTeamSize());
        dto.setMaxTeamSize(p.getMaxTeamSize());
        dto.setEstimatedWeeks(p.getEstimatedWeeks());

        if (p.getTargetBranches() != null) {
            List<String> bNames = new ArrayList<>();
            for (EngineeringBranch b : p.getTargetBranches()) bNames.add(b.getName());
            dto.setTargetBranches(bNames);
        }

        if (p.getRequiredSkills() != null) {
            List<String> sNames = new ArrayList<>();
            for (Skill s : p.getRequiredSkills()) sNames.add(s.getName());
            dto.setRequiredSkills(sNames);
        }

        if (p.getProjectTechnologies() != null) {
            List<String> tNames = new ArrayList<>();
            for (ProjectTechnology pt : p.getProjectTechnologies()) {
                if (pt.getTechnology() != null) tNames.add(pt.getTechnology().getName());
            }
            dto.setTechnologies(tNames);
        }

        dto.setScoreBreakdown(new ScoreBreakdownDto(
                rec.getDomainFitScore(), rec.getTechFitScore(), rec.getSkillFitScore(),
                rec.getProficiencyScore(), rec.getExperienceFitScore(), rec.getTeamFitScore(),
                rec.getTimeFitScore(), rec.getPenaltyScore(), rec.getMatchScore()
        ));
        dto.setScoreExplanation(rec.getScoreExplanation());

        if (rec.getRecommendationReasons() != null && !rec.getRecommendationReasons().isBlank()) {
            dto.setRecommendationReasons(Arrays.asList(rec.getRecommendationReasons().split("\n")));
        }
        if (rec.getIdentifiedGaps() != null && !rec.getIdentifiedGaps().isBlank()) {
            dto.setIdentifiedGaps(Arrays.asList(rec.getIdentifiedGaps().split("\n")));
        }

        dto.setProblemStatement(p.getProblemStatement());
        dto.setTargetStakeholders(p.getTargetStakeholders());
        dto.setProposedSolution(p.getProposedSolution());
        dto.setExpectedOutcomeKpis(p.getExpectedOutcomeKpis());
        dto.setIndustryValue(p.getIndustryValue());
        dto.setInnovationDifferentiator(p.getInnovationDifferentiator());
        dto.setDataRequirements(p.getDataRequirements());
        dto.setFunctionalRequirements(p.getFunctionalRequirements());
        dto.setNonFunctionalRequirements(p.getNonFunctionalRequirements());
        dto.setConstraintsDependencies(p.getConstraintsDependencies());
        dto.setTestingValidation(p.getTestingValidation());
        dto.setSecurityPrivacySafety(p.getSecurityPrivacySafety());
        dto.setScalabilityIntegration(p.getScalabilityIntegration());
        dto.setCostSustainability(p.getCostSustainability());
        dto.setDeploymentAcademicDepth(p.getDeploymentAcademicDepth());
        dto.setPrerequisites(p.getPrerequisites());
        dto.setQualityApproved(p.getQualityApproved());
        dto.setQualityEvaluation(p.getQualityEvaluation());

        dto.setSuitabilityScore(rec.getMatchScore());
        double teamFit = rec.getTeamFitScore() != null ? rec.getTeamFitScore() : 2.5;
        double timeFit = rec.getTimeFitScore() != null ? rec.getTimeFitScore() : 2.5;
        double penalty = rec.getPenaltyScore() != null ? rec.getPenaltyScore() : 0.0;
        double qual = p.getQualityScore() != null ? p.getQualityScore() : 85.0;
        double feas = Math.min(100.0, Math.max(0.0, ((teamFit + timeFit) / 10.0 * 60.0) + (qual * 0.4) - (penalty * 0.5)));
        dto.setFeasibilityScore(Math.round(feas * 10.0) / 10.0);

        if (p.getIsCatalog() != null && !p.getIsCatalog()) {
            dto.setIsGeneratedIdea(true);
        }

        return dto;
    }
}
