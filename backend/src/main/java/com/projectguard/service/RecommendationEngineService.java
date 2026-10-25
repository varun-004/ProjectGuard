package com.projectguard.service;

import com.projectguard.dto.recommendation.AssessedSkillMatchDto;
import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.dto.recommendation.ScoreBreakdownDto;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.repository.AssessmentResultRepository;
import com.projectguard.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecommendationEngineService {

    // Configurable documented weights (Total base = 100.0)
    public static final double WEIGHT_DOMAIN = 25.0;
    public static final double WEIGHT_TECHNOLOGY = 20.0;
    public static final double WEIGHT_SKILLS = 25.0;
    public static final double WEIGHT_PROFICIENCY = 15.0;
    public static final double WEIGHT_EXPERIENCE_INTERESTS = 5.0;
    public static final double WEIGHT_CONSTRAINTS = 10.0; // Team: 5.0, Time: 5.0

    // Minimum match threshold to qualify for recommendation
    public static final double MIN_MATCH_THRESHOLD = 35.0;

    private final ProjectRepository projectRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final ProjectQualityGateService qualityGateService;
    private final ProjectIdeaGeneratorService ideaGeneratorService;

    @org.springframework.beans.factory.annotation.Autowired
    public RecommendationEngineService(ProjectRepository projectRepository,
                                       AssessmentResultRepository assessmentResultRepository,
                                       ProjectQualityGateService qualityGateService,
                                       ProjectIdeaGeneratorService ideaGeneratorService) {
        this.projectRepository = projectRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.qualityGateService = qualityGateService;
        this.ideaGeneratorService = ideaGeneratorService;
    }

    public RecommendationEngineService(ProjectRepository projectRepository,
                                       AssessmentResultRepository assessmentResultRepository,
                                       ProjectQualityGateService qualityGateService) {
        this(projectRepository, assessmentResultRepository, qualityGateService, null);
    }

    /**
     * Computes ranked recommendations for a student profile using rule-based scoring across all 4 inputs.
     */
    public List<ProjectRecommendationResponse> generateRecommendations(StudentProfile studentProfile) {
        return generateRecommendations(studentProfile, Collections.emptySet());
    }

    /**
     * Computes fresh personalized recommendations preferring unseen alternatives and generating new project ideas when needed.
     */
    public List<ProjectRecommendationResponse> generateRecommendations(StudentProfile studentProfile, Set<Long> previouslyRecommendedIds) {
        if (studentProfile == null) {
            return Collections.emptyList();
        }

        // 1. Fetch catalog projects
        List<Project> catalog = projectRepository.findAllApprovedCatalogProjectsWithDetails();
        if (catalog == null || catalog.isEmpty()) {
            catalog = projectRepository.findByIsCatalogTrue();
        }

        // Include any approved student-specific projects
        if (studentProfile.getId() != null) {
            try {
                List<Project> studentProjects = projectRepository.findByStudentProfileId(studentProfile.getId());
                if (studentProjects != null && !studentProjects.isEmpty()) {
                    List<Project> merged = new ArrayList<>(catalog != null ? catalog : Collections.emptyList());
                    for (Project sp : studentProjects) {
                        if (sp.getQualityApproved() != null && sp.getQualityApproved()) {
                            merged.add(sp);
                        }
                    }
                    catalog = merged;
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Fetch student's Phase 5 assessment results
        List<AssessmentResult> assessmentResults =
                assessmentResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(studentProfile.getId());

        // Map best assessed result per skill
        Map<Long, AssessmentResult> latestSkillAssessments = new HashMap<>();
        for (AssessmentResult ar : assessmentResults) {
            if (ar.getSkill() != null) {
                Long skillId = ar.getSkill().getId();
                if (!latestSkillAssessments.containsKey(skillId) ||
                        ar.getPercentage() > latestSkillAssessments.get(skillId).getPercentage()) {
                    latestSkillAssessments.put(skillId, ar);
                }
            }
        }

        // Map student declared skills
        Map<Long, StudentSkill> declaredSkills = new HashMap<>();
        if (studentProfile.getStudentSkills() != null) {
            for (StudentSkill ss : studentProfile.getStudentSkills()) {
                if (ss.getSkill() != null) {
                    declaredSkills.put(ss.getSkill().getId(), ss);
                }
            }
        }

        List<ProjectRecommendationResponse> unseenScored = new ArrayList<>();
        List<ProjectRecommendationResponse> previouslySeenScored = new ArrayList<>();

        if (catalog != null) {
            for (Project project : catalog) {
                ProjectRecommendationResponse resp = scoreProject(project, studentProfile, latestSkillAssessments, declaredSkills);
                if (resp != null) {
                    if (previouslyRecommendedIds != null && previouslyRecommendedIds.contains(project.getId())) {
                        resp.setIsFreshAlternative(false);
                        previouslySeenScored.add(resp);
                    } else {
                        resp.setIsFreshAlternative(true);
                        unseenScored.add(resp);
                    }
                }
            }
        }

        int catalogEligibleCount = unseenScored.size() + previouslySeenScored.size();

        // 3. When suitable unseen alternatives are insufficient, generate new project ideas
        // Only generate if student has realistic time feasibility (>= 6 weeks)
        boolean hasFeasibleTime = studentProfile.getAvailableTimeWeeks() == null || studentProfile.getAvailableTimeWeeks() >= 6;
        if (unseenScored.size() < 3 && ideaGeneratorService != null && studentProfile.getId() != null && hasFeasibleTime) {
            int neededCount = Math.max(2, 4 - unseenScored.size());
            List<Project> generatedProjects = ideaGeneratorService.generateInnovativeProjects(studentProfile, neededCount);
            for (Project genProject : generatedProjects) {
                ProjectRecommendationResponse resp = scoreProject(genProject, studentProfile, latestSkillAssessments, declaredSkills);
                if (resp != null) {
                    resp.setIsFreshAlternative(true);
                    resp.setIsGeneratedIdea(true);
                    unseenScored.add(resp);
                }
            }
        }

        // Sort both pools by matchScore desc, then by qualityScore desc
        Comparator<ProjectRecommendationResponse> comparator = (a, b) -> {
            int cmp = Double.compare(b.getMatchScore(), a.getMatchScore());
            if (cmp != 0) return cmp;
            return Double.compare(
                    b.getQualityScore() != null ? b.getQualityScore() : 0.0,
                    a.getQualityScore() != null ? a.getQualityScore() : 0.0
            );
        };

        unseenScored.sort(comparator);
        previouslySeenScored.sort(comparator);

        // Fresh unseen alternatives appear first; backfill previously seen up to target capacity
        List<ProjectRecommendationResponse> combined = new ArrayList<>(unseenScored);
        int targetCapacity = Math.max(catalogEligibleCount, unseenScored.size());
        for (ProjectRecommendationResponse seen : previouslySeenScored) {
            if (combined.size() >= targetCapacity) {
                break;
            }
            if (combined.stream().noneMatch(c -> c.getProjectId().equals(seen.getProjectId()))) {
                combined.add(seen);
            }
        }

        // Assign rank order
        int rank = 1;
        for (ProjectRecommendationResponse resp : combined) {
            resp.setRankOrder(rank++);
        }

        return combined;
    }

    /**
     * Evaluates a project candidate against the student's 4 inputs:
     * 1. Engineering branch
     * 2. Technologies
     * 3. Skills and proficiency
     * 4. Assessment/quiz scores
     */
    public ProjectRecommendationResponse scoreProject(Project project,
                                                       StudentProfile studentProfile,
                                                       Map<Long, AssessmentResult> latestSkillAssessments,
                                                       Map<Long, StudentSkill> declaredSkills) {
        // Quality Gate check
        if (project.getQualityApproved() == null || !project.getQualityApproved()) {
            return null;
        }

        // Hard filter: Branch compatibility (Input 1)
        if (!isBranchCompatible(studentProfile.getBranch(), project)) {
            return null;
        }

        // Calculate fit scores across all 4 inputs
        double domainScore = computeDomainFit(studentProfile, project);
        double techScore = computeTechnologyFit(studentProfile, project); // Input 2
        List<AssessedSkillMatchDto> skillMatches = new ArrayList<>();
        List<String> identifiedGaps = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        double skillScore = computeSkillFit(project, latestSkillAssessments, declaredSkills, skillMatches, identifiedGaps, reasons); // Input 3 & 4
        double profScore = computeProficiencyFit(project, latestSkillAssessments, declaredSkills, skillMatches, reasons, identifiedGaps); // Input 3 & 4
        double expScore = computeExperienceAndInterestsFit(studentProfile, project, reasons);
        double teamScore = computeTeamFit(studentProfile, project, reasons, identifiedGaps);
        double timeScore = computeTimeFit(studentProfile, project, reasons, identifiedGaps);
        double constraintsScore = teamScore + timeScore;
        double penaltyScore = computePenalties(studentProfile, project, latestSkillAssessments, identifiedGaps);

        double totalScore = domainScore + techScore + skillScore + profScore + expScore + constraintsScore - penaltyScore;
        totalScore = Math.max(0.0, Math.min(100.0, Math.round(totalScore * 10.0) / 10.0));

        if (totalScore < MIN_MATCH_THRESHOLD) {
            return null;
        }

        // Domain match reason
        if (domainScore >= 20.0) {
            reasons.add(0, "Exact match with your chosen domain: " + project.getDomain().getName());
        } else if (domainScore > 0) {
            reasons.add(0, "Adjacent domain relevant to your engineering branch curriculum: " +
                    (project.getDomain() != null ? project.getDomain().getName() : "General"));
        }

        // Tech match reason (Input 2)
        if (studentProfile.getTechnology() != null && techScore >= 18.0) {
            reasons.add("Directly utilizes your selected primary technology stack: " + studentProfile.getTechnology().getName());
        }

        // Build explanation
        String explanation = String.format(
                "Match Score: %.1f%% (Domain Fit: %.1f/25, Technology: %.1f/20, Skills Compatibility: %.1f/25, " +
                "Assessed Mastery: %.1f/15, Experience/Interests: %.1f/5, Constraints: %.1f/10, Penalties: -%.1f). " +
                "Evaluated independently from project quality score (%.1f/100).",
                totalScore, domainScore, techScore, skillScore, profScore, expScore, constraintsScore, penaltyScore,
                project.getQualityScore() != null ? project.getQualityScore() : 85.0
        );

        ScoreBreakdownDto breakdown = new ScoreBreakdownDto(
                round1(domainScore), round1(techScore), round1(skillScore),
                round1(profScore), round1(expScore), round1(teamScore),
                round1(timeScore), round1(penaltyScore), round1(totalScore)
        );

        ProjectRecommendationResponse resp = mapToResponse(project, totalScore, breakdown, explanation,
                reasons, identifiedGaps, skillMatches);

        resp.setSuitabilityScore(totalScore);
        double feasScore = Math.min(100.0, Math.max(0.0,
                ((teamScore + timeScore) / 10.0 * 60.0) +
                (project.getQualityScore() != null ? project.getQualityScore() : 85.0) * 0.4 -
                penaltyScore * 0.5));
        resp.setFeasibilityScore(round1(feasScore));

        if (project.getIsCatalog() != null && !project.getIsCatalog()) {
            resp.setIsGeneratedIdea(true);
        }

        return resp;
    }

    private boolean isBranchCompatible(EngineeringBranch studentBranch, Project project) {
        if (studentBranch == null) return true;
        if (project.getTargetBranches() == null || project.getTargetBranches().isEmpty()) {
            return true;
        }
        for (EngineeringBranch b : project.getTargetBranches()) {
            if (b.getId().equals(studentBranch.getId()) ||
                    b.getName().equalsIgnoreCase(studentBranch.getName())) {
                return true;
            }
        }
        return false;
    }

    private double computeDomainFit(StudentProfile profile, Project project) {
        if (project.getDomain() == null || profile.getDomain() == null) {
            return 5.0;
        }
        if (project.getDomain().getId().equals(profile.getDomain().getId())) {
            return WEIGHT_DOMAIN; // 25.0
        }
        // Check if branch offers this domain
        if (profile.getBranch() != null && profile.getBranch().getDomains() != null) {
            for (ProjectDomain pd : profile.getBranch().getDomains()) {
                if (pd.getId().equals(project.getDomain().getId())) {
                    return 12.5; // related domain within branch
                }
            }
        }
        return 0.0;
    }

    private double computeTechnologyFit(StudentProfile profile, Project project) {
        Technology chosenTech = profile.getTechnology();
        if (chosenTech != null) {
            if (project.getProjectTechnologies() != null) {
                for (ProjectTechnology pt : project.getProjectTechnologies()) {
                    if (pt.getTechnology() != null && pt.getTechnology().getId().equals(chosenTech.getId())) {
                        return WEIGHT_TECHNOLOGY; // 20.0
                    }
                }
            }
            // Check if domain matches
            if (chosenTech.getDomain() != null && project.getDomain() != null &&
                    chosenTech.getDomain().getId().equals(project.getDomain().getId())) {
                return 10.0;
            }
            return 4.0;
        }

        // Student has not selected a specific technology yet
        if (project.getDomain() != null && profile.getDomain() != null &&
                project.getDomain().getId().equals(profile.getDomain().getId())) {
            return 14.0;
        }
        return 8.0;
    }

    private double computeSkillFit(Project project,
                                   Map<Long, AssessmentResult> latestSkillAssessments,
                                   Map<Long, StudentSkill> declaredSkills,
                                   List<AssessedSkillMatchDto> skillMatches,
                                   List<String> identifiedGaps,
                                   List<String> reasons) {
        List<Skill> required = project.getRequiredSkills();
        if (required == null || required.isEmpty()) {
            return 18.0;
        }

        double matchedPoints = 0.0;
        int assessedCount = 0;

        for (Skill s : required) {
            AssessmentResult ar = latestSkillAssessments.get(s.getId());
            StudentSkill ss = declaredSkills.get(s.getId());

            if (ar != null) {
                assessedCount++;
                boolean passed = ar.getPercentage() >= 50.0;
                double fraction = Math.min(1.0, ar.getPercentage() / 100.0);
                matchedPoints += fraction;

                skillMatches.add(new AssessedSkillMatchDto(
                        s.getName(),
                        ar.getLevel() != null ? ar.getLevel().name() : "ASSESSED",
                        round1(ar.getPercentage()),
                        true,
                        passed,
                        passed ? "Assessed competency verified (" + ar.getScore() + " pts)" : "Assessed score below benchmark (<50%)"
                ));

                if (passed && ar.getPercentage() >= 75.0) {
                    reasons.add("High assessed proficiency in required skill: " + s.getName() + " (" + round1(ar.getPercentage()) + "%)");
                }
            } else if (ss != null) {
                // Declared in profile with proficiency level
                double declaredFraction = switch (ss.getProficiencyLevel() != null ? ss.getProficiencyLevel() : ProficiencyLevel.BEGINNER) {
                    case EXPERT -> 0.85;
                    case ADVANCED -> 0.70;
                    case INTERMEDIATE -> 0.55;
                    case BEGINNER -> 0.40;
                };
                if (ss.getYearsOfExperience() != null && ss.getYearsOfExperience() > 0) {
                    declaredFraction = Math.min(0.95, declaredFraction + Math.min(0.08, ss.getYearsOfExperience() * 0.02));
                }
                matchedPoints += declaredFraction;
                skillMatches.add(new AssessedSkillMatchDto(
                        s.getName(),
                        ss.getProficiencyLevel() != null ? ss.getProficiencyLevel().name() : "BEGINNER",
                        0.0,
                        false,
                        false,
                        "Self-declared in profile (" + (ss.getProficiencyLevel() != null ? ss.getProficiencyLevel().name() : "BEGINNER") + "); Phase 5 assessment pending"
                ));
                identifiedGaps.add("Required skill '" + s.getName() + "' is declared (" +
                        (ss.getProficiencyLevel() != null ? ss.getProficiencyLevel().name() : "BEGINNER") +
                        ") but not yet verified via Phase 5 assessment");
            } else {
                // Missing skill
                skillMatches.add(new AssessedSkillMatchDto(
                        s.getName(),
                        "NOT_ASSESSED",
                        0.0,
                        false,
                        false,
                        "Skill gap: not yet in profile or assessment history"
                ));
                identifiedGaps.add("Skill gap: '" + s.getName() + "' is required for this project");
            }
        }

        if (assessedCount > 0) {
            reasons.add(String.format("Verified %d of %d required technical competencies through Phase 5 assessments", assessedCount, required.size()));
        }

        double ratio = matchedPoints / (double) required.size();
        return Math.min(WEIGHT_SKILLS, Math.max(0.0, ratio * WEIGHT_SKILLS));
    }

    private double computeProficiencyFit(Project project,
                                         Map<Long, AssessmentResult> latestSkillAssessments,
                                         Map<Long, StudentSkill> declaredSkills,
                                         List<AssessedSkillMatchDto> skillMatches,
                                         List<String> reasons,
                                         List<String> identifiedGaps) {
        String complexity = project.getComplexityLevel() != null ? project.getComplexityLevel().toUpperCase() : "INTERMEDIATE";
        List<Skill> required = project.getRequiredSkills();

        if (required == null || required.isEmpty()) {
            return 10.0;
        }

        double sumWeights = 0.0;
        int count = 0;

        for (Skill s : required) {
            AssessmentResult ar = latestSkillAssessments.get(s.getId());
            StudentSkill ss = declaredSkills != null ? declaredSkills.get(s.getId()) : null;

            if (ar != null) {
                count++;
                ProficiencyLevel lvl = ar.getLevel();
                double assessWeight;
                if (lvl == ProficiencyLevel.ADVANCED) {
                    assessWeight = 1.0;
                } else if (lvl == ProficiencyLevel.INTERMEDIATE) {
                    assessWeight = (complexity.equals("ADVANCED")) ? 0.70 : 0.90;
                } else {
                    assessWeight = (complexity.equals("ADVANCED")) ? 0.35 : 0.60;
                }

                if (ss != null) {
                    double declWeight = switch (ss.getProficiencyLevel() != null ? ss.getProficiencyLevel() : ProficiencyLevel.BEGINNER) {
                        case EXPERT -> 1.0;
                        case ADVANCED -> 0.85;
                        case INTERMEDIATE -> (complexity.equals("ADVANCED")) ? 0.65 : 0.80;
                        case BEGINNER -> (complexity.equals("ADVANCED")) ? 0.30 : 0.50;
                    };
                    sumWeights += (assessWeight * 0.75 + declWeight * 0.25);
                } else {
                    sumWeights += assessWeight;
                }
            } else if (ss != null) {
                count++;
                double declWeight = switch (ss.getProficiencyLevel() != null ? ss.getProficiencyLevel() : ProficiencyLevel.BEGINNER) {
                    case EXPERT -> 0.95;
                    case ADVANCED -> 0.80;
                    case INTERMEDIATE -> (complexity.equals("ADVANCED")) ? 0.60 : 0.75;
                    case BEGINNER -> (complexity.equals("ADVANCED")) ? 0.30 : 0.50;
                };
                sumWeights += declWeight;
            }
        }

        if (count == 0) {
            return 4.0; // unverified
        }

        double avgProficiencyRatio = sumWeights / count;
        double score = avgProficiencyRatio * WEIGHT_PROFICIENCY;

        if (complexity.equals("ADVANCED") && avgProficiencyRatio < 0.6) {
            identifiedGaps.add("Project has ADVANCED complexity; recommended to strengthen intermediate and advanced skills before kickoff");
        }

        return Math.min(WEIGHT_PROFICIENCY, Math.max(0.0, score));
    }

    private double computeExperienceAndInterestsFit(StudentProfile profile, Project project, List<String> reasons) {
        double score = 2.0; // base
        String interests = profile.getInterests() != null ? profile.getInterests().toLowerCase() : "";
        String exp = profile.getPreviousExperience() != null ? profile.getPreviousExperience().toLowerCase() : "";

        String title = project.getTitle() != null ? project.getTitle().toLowerCase() : "";
        String desc = project.getDescription() != null ? project.getDescription().toLowerCase() : "";

        List<String> tokens = new ArrayList<>();
        if (!interests.isEmpty()) tokens.addAll(Arrays.asList(interests.split("[,;\\s]+")));
        if (!exp.isEmpty()) tokens.addAll(Arrays.asList(exp.split("[,;\\s]+")));

        int matches = 0;
        for (String token : tokens) {
            if (token.length() > 3 && (title.contains(token) || desc.contains(token))) {
                matches++;
            }
        }

        if (matches >= 2) {
            score = WEIGHT_EXPERIENCE_INTERESTS;
            reasons.add("Aligns with your documented project interests and previous technical experience");
        } else if (matches == 1) {
            score = 3.5;
        }

        return Math.min(WEIGHT_EXPERIENCE_INTERESTS, score);
    }

    private double computeTeamFit(StudentProfile profile, Project project, List<String> reasons, List<String> identifiedGaps) {
        double teamFit = 2.5;
        Integer teamSize = profile.getTeamSize();
        Integer minT = project.getMinTeamSize() != null ? project.getMinTeamSize() : 1;
        Integer maxT = project.getMaxTeamSize() != null ? project.getMaxTeamSize() : 4;

        if (teamSize != null) {
            if (teamSize >= minT && teamSize <= maxT) {
                teamFit = 5.0;
                reasons.add(String.format("Preferred team size of %d perfectly fits project capacity (%d–%d members)", teamSize, minT, maxT));
            } else if (Math.abs(teamSize - minT) == 1 || Math.abs(teamSize - maxT) == 1) {
                teamFit = 2.5;
            } else {
                teamFit = 0.0;
                identifiedGaps.add(String.format("Team size advisory: You specified %d members, but project recommends %d–%d members", teamSize, minT, maxT));
            }
        }
        return teamFit;
    }

    private double computeTimeFit(StudentProfile profile, Project project, List<String> reasons, List<String> identifiedGaps) {
        double timeFit = 2.5;
        Integer availableWeeks = profile.getAvailableTimeWeeks();
        Integer estimatedWeeks = project.getEstimatedWeeks() != null ? project.getEstimatedWeeks() : 12;

        if (availableWeeks != null) {
            if (availableWeeks >= estimatedWeeks) {
                timeFit = 5.0;
                reasons.add(String.format("Available timeline (%d weeks) comfortably accommodates project duration (~%d weeks)", availableWeeks, estimatedWeeks));
            } else if (availableWeeks >= estimatedWeeks * 0.75) {
                timeFit = 3.5;
            } else if (availableWeeks >= estimatedWeeks * 0.5) {
                timeFit = 1.5;
                identifiedGaps.add(String.format("Timeline caution: Estimated effort is %d weeks, while your profile lists %d available weeks", estimatedWeeks, availableWeeks));
            } else {
                timeFit = 0.0;
                identifiedGaps.add(String.format("Significant timeline constraint: Available time (%d weeks) is less than half of estimated project effort (%d weeks)", availableWeeks, estimatedWeeks));
            }
        }
        return timeFit;
    }

    private double computePenalties(StudentProfile profile, Project project,
                                    Map<Long, AssessmentResult> latestSkillAssessments,
                                    List<String> identifiedGaps) {
        double penalty = 0.0;

        // 1. Infeasible time constraint penalty
        if (profile.getAvailableTimeWeeks() != null && project.getEstimatedWeeks() != null) {
            if (profile.getAvailableTimeWeeks() < project.getEstimatedWeeks() * 0.5) {
                penalty += 10.0;
            }
        }

        // 2. Failed skill assessment penalty (<35% in required skill)
        if (project.getRequiredSkills() != null) {
            for (Skill s : project.getRequiredSkills()) {
                AssessmentResult ar = latestSkillAssessments.get(s.getId());
                if (ar != null && ar.getPercentage() < 35.0) {
                    penalty += 4.0;
                    identifiedGaps.add("Critical skill shortfall: Assessed score in '" + s.getName() + "' is below 35%");
                }
            }
        }

        // 3. Academic level penalty (e.g. strict PG project for UG student)
        if ("UG".equalsIgnoreCase(profile.getAcademicLevel()) &&
                "PG_ONLY".equalsIgnoreCase(project.getAcademicLevelSuitability())) {
            penalty += 8.0;
            identifiedGaps.add("Project has postgraduate-level research depth requirement");
        }

        return Math.min(25.0, penalty);
    }

    private ProjectRecommendationResponse mapToResponse(Project project, double matchScore,
                                                         ScoreBreakdownDto breakdown, String explanation,
                                                         List<String> reasons, List<String> gaps,
                                                         List<AssessedSkillMatchDto> skillMatches) {
        ProjectRecommendationResponse r = new ProjectRecommendationResponse();
        r.setProjectId(project.getId());
        r.setMatchScore(matchScore);
        r.setQualityScore(project.getQualityScore());
        r.setTitle(project.getTitle());
        r.setDescription(project.getDescription());

        if (project.getDomain() != null) {
            r.setDomainId(project.getDomain().getId());
            r.setDomainName(project.getDomain().getName());
        }

        r.setComplexityLevel(project.getComplexityLevel());
        r.setAcademicLevelSuitability(project.getAcademicLevelSuitability());
        r.setMinTeamSize(project.getMinTeamSize());
        r.setMaxTeamSize(project.getMaxTeamSize());
        r.setEstimatedWeeks(project.getEstimatedWeeks());

        if (project.getTargetBranches() != null) {
            List<String> branchNames = new ArrayList<>();
            for (EngineeringBranch eb : project.getTargetBranches()) {
                branchNames.add(eb.getName());
            }
            r.setTargetBranches(branchNames);
        }

        if (project.getRequiredSkills() != null) {
            List<String> skillNames = new ArrayList<>();
            for (Skill s : project.getRequiredSkills()) {
                skillNames.add(s.getName());
            }
            r.setRequiredSkills(skillNames);
        }

        if (project.getProjectTechnologies() != null) {
            List<String> techNames = new ArrayList<>();
            for (ProjectTechnology pt : project.getProjectTechnologies()) {
                if (pt.getTechnology() != null) {
                    techNames.add(pt.getTechnology().getName());
                }
            }
            r.setTechnologies(techNames);
        }

        r.setScoreBreakdown(breakdown);
        r.setScoreExplanation(explanation);
        r.setRecommendationReasons(reasons);
        r.setIdentifiedGaps(gaps);
        r.setAssessedSkillsMatch(skillMatches);

        r.setProblemStatement(project.getProblemStatement());
        r.setTargetStakeholders(project.getTargetStakeholders());
        r.setProposedSolution(project.getProposedSolution());
        r.setExpectedOutcomeKpis(project.getExpectedOutcomeKpis());
        r.setIndustryValue(project.getIndustryValue());
        r.setInnovationDifferentiator(project.getInnovationDifferentiator());
        r.setDataRequirements(project.getDataRequirements());
        r.setFunctionalRequirements(project.getFunctionalRequirements());
        r.setNonFunctionalRequirements(project.getNonFunctionalRequirements());
        r.setConstraintsDependencies(project.getConstraintsDependencies());
        r.setTestingValidation(project.getTestingValidation());
        r.setSecurityPrivacySafety(project.getSecurityPrivacySafety());
        r.setScalabilityIntegration(project.getScalabilityIntegration());
        r.setCostSustainability(project.getCostSustainability());
        r.setDeploymentAcademicDepth(project.getDeploymentAcademicDepth());
        r.setPrerequisites(project.getPrerequisites());
        r.setQualityApproved(project.getQualityApproved());
        r.setQualityEvaluation(project.getQualityEvaluation());

        return r;
    }

    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
