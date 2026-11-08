package com.projectguard.service;

import com.projectguard.dto.mldataset.*;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.MlOutcomeStatus;
import com.projectguard.entity.enums.MlValidationStatus;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.entity.enums.ProjectComplexity;
import com.projectguard.exception.BadRequestException;
import com.projectguard.exception.ResourceNotFoundException;
import com.projectguard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class MlDatasetService {

    private static final Logger log = LoggerFactory.getLogger(MlDatasetService.class);

    private final MlDatasetRepository mlDatasetRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AssessmentResultRepository assessmentResultRepository;

    public MlDatasetService(MlDatasetRepository mlDatasetRepository,
                            StudentProfileRepository studentProfileRepository,
                            ProjectRepository projectRepository,
                            UserRepository userRepository,
                            AssessmentResultRepository assessmentResultRepository) {
        this.mlDatasetRepository = mlDatasetRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.assessmentResultRepository = assessmentResultRepository;
    }

    /**
     * Records or updates an ML dataset snapshot for a student-project combination.
     * Prevents duplicate snapshots by updating existing unverified record or maintaining idempotent record.
     */
    @Transactional
    public MlDataset recordDatasetSnapshot(StudentProfile profile,
                                           Project project,
                                           Double availableHoursPerWeek,
                                           Integer overrideTeamSize,
                                           Double overridePreviousExperience,
                                           Double overrideEstimatedEffort,
                                           Integer overrideNumberOfFeatures,
                                           String dataSource) {
        if (profile == null || project == null) {
            throw new IllegalArgumentException("StudentProfile and Project are required to capture an ML dataset record");
        }

        Optional<MlDataset> existingOpt = mlDatasetRepository.findByStudentProfileIdAndProjectId(profile.getId(), project.getId());
        MlDataset record = existingOpt.orElseGet(MlDataset::new);

        record.setStudentProfile(profile);
        record.setProject(project);

        if (dataSource != null && !dataSource.isBlank()) {
            record.setDataSource(dataSource);
        }

        // Feature 1: student_skill_level (1.0 to 4.0)
        Double skillLevel = calculateStudentSkillLevel(profile);
        record.setStudentSkillLevel(skillLevel);

        // Feature 2: skill_gap (Phase 7 scale: 0.0 to 25.0)
        Double skillGap = calculateSkillGap(profile, project);
        record.setSkillGap(skillGap);

        // Feature 3: project_complexity (EASY, MEDIUM, HARD)
        ProjectComplexity complexity = ProjectComplexity.fromString(project.getComplexityLevel());
        record.setProjectComplexity(complexity);

        // Feature 4: team_size
        Integer teamSize = overrideTeamSize != null && overrideTeamSize > 0
                ? overrideTeamSize
                : (profile.getTeamSize() != null ? profile.getTeamSize() : project.getMinTeamSize());
        record.setTeamSize(teamSize != null && teamSize > 0 ? teamSize : 1);

        // Feature 5: available_hours_per_week
        Double hoursPerWeek = availableHoursPerWeek != null && availableHoursPerWeek > 0.0
                ? round2(availableHoursPerWeek)
                : (record.getAvailableHoursPerWeek() != null ? record.getAvailableHoursPerWeek() : 15.0);
        record.setAvailableHoursPerWeek(hoursPerWeek);

        // Feature 6: technology_familiarity (1.0 to 4.0)
        Double techFamiliarity = calculateTechnologyFamiliarity(profile, project);
        record.setTechnologyFamiliarity(techFamiliarity);

        // Feature 7: number_of_requirements
        Integer numReqs = calculateNumberOfRequirements(project);
        record.setNumberOfRequirements(numReqs);

        // Feature 8: number_of_features
        Integer numFeatures = overrideNumberOfFeatures != null && overrideNumberOfFeatures >= 0
                ? overrideNumberOfFeatures
                : calculateNumberOfFeatures(project);
        record.setNumberOfFeatures(numFeatures);

        // Feature 9: previous_experience in years (never invented)
        Double prevExp = overridePreviousExperience != null && overridePreviousExperience >= 0.0
                ? round2(overridePreviousExperience)
                : extractPreviousExperience(profile);
        record.setPreviousExperience(prevExp);

        // Feature 10: estimated_effort in hours
        Double effort = overrideEstimatedEffort != null && overrideEstimatedEffort > 0.0
                ? round2(overrideEstimatedEffort)
                : calculateEstimatedEffort(project, hoursPerWeek);
        record.setEstimatedEffort(effort);

        // Ensure outcome is not automatically decided
        if (record.getId() == null || record.getOutcomeStatus() == null) {
            record.setOutcomeStatus(MlOutcomeStatus.PENDING_OUTCOME);
            record.setActualOutcome(null);
            record.setValidationStatus(isRecordComplete(record) ? MlValidationStatus.PENDING : MlValidationStatus.INCOMPLETE);
        }

        MlDataset saved = mlDatasetRepository.save(record);
        log.info("Saved ML dataset snapshot id={} for studentProfileId={} and projectId={} (outcome={})",
                saved.getId(), profile.getId(), project.getId(), saved.getOutcomeStatus());
        return saved;
    }

    /**
     * Authenticated student registers a project candidate, providing available hours and workflow parameters.
     */
    @Transactional
    public MlDatasetRecordDto registerStudentProject(String usernameOrEmail, MlProjectRegistrationRequest request) {
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Student profile not found. Please complete profile first."));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.getProjectId()));

        // Associate project with student if not already catalog or owned
        if (project.getStudentProfile() == null && (project.getIsCatalog() == null || !project.getIsCatalog())) {
            project.setStudentProfile(profile);
            projectRepository.save(project);
        }

        MlDataset saved = recordDatasetSnapshot(
                profile,
                project,
                request.getAvailableHoursPerWeek(),
                request.getTeamSize(),
                request.getPreviousExperience(),
                request.getEstimatedEffort(),
                request.getNumberOfFeatures(),
                "PROJECT_REGISTRATION"
        );

        return mapToDto(saved);
    }

    /**
     * Controlled evaluator / administrator outcome verification.
     */
    @Transactional
    public MlDatasetRecordDto recordActualOutcome(Long datasetId, MlOutcomeUpdateRequest request) {
        MlDataset record = mlDatasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("ML dataset record not found: " + datasetId));

        String outcome = request.getActualOutcome().trim().toUpperCase();
        if (!"FEASIBLE".equals(outcome) && !"NOT_FEASIBLE".equals(outcome)) {
            throw new BadRequestException("Actual outcome must be FEASIBLE or NOT_FEASIBLE");
        }

        record.setActualOutcome(outcome);
        record.setOutcomeStatus(MlOutcomeStatus.valueOf(outcome));
        record.setLabelEvidence(request.getLabelEvidence());
        record.setLabelNotes(request.getLabelNotes());

        if (isRecordComplete(record)) {
            record.setValidationStatus(MlValidationStatus.VALIDATED);
        } else {
            record.setValidationStatus(MlValidationStatus.INCOMPLETE);
        }

        MlDataset updated = mlDatasetRepository.save(record);
        log.info("Recorded verified outcome '{}' for ML dataset record id={}", outcome, updated.getId());
        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public MlDatasetStatisticsDto getStatistics() {
        List<MlDataset> all = mlDatasetRepository.findAll();
        MlDatasetStatisticsDto stats = new MlDatasetStatisticsDto();
        stats.setTotalRecords(all.size());

        long complete = 0;
        long incomplete = 0;
        long pending = 0;
        long feasible = 0;
        long notFeasible = 0;
        long trainingReady = 0;

        Set<String> studentProjectKeys = new HashSet<>();
        long duplicates = 0;

        for (MlDataset d : all) {
            String key = (d.getStudentProfile() != null ? d.getStudentProfile().getId() : "null") + "_" +
                    (d.getProject() != null ? d.getProject().getId() : "null");
            if (!studentProjectKeys.add(key)) {
                duplicates++;
            }

            if (isRecordComplete(d)) {
                complete++;
            } else {
                incomplete++;
            }

            if (d.getOutcomeStatus() == MlOutcomeStatus.PENDING_OUTCOME) {
                pending++;
            } else if (d.getOutcomeStatus() == MlOutcomeStatus.FEASIBLE) {
                feasible++;
                if (isRecordComplete(d) && d.getActualOutcome() != null) {
                    trainingReady++;
                }
            } else if (d.getOutcomeStatus() == MlOutcomeStatus.NOT_FEASIBLE) {
                notFeasible++;
                if (isRecordComplete(d) && d.getActualOutcome() != null) {
                    trainingReady++;
                }
            }
        }

        stats.setCompleteRecords(complete);
        stats.setIncompleteRecords(incomplete);
        stats.setPendingOutcomeRecords(pending);
        stats.setVerifiedFeasibleRecords(feasible);
        stats.setVerifiedNotFeasibleRecords(notFeasible);
        stats.setTrainingReadyRecords(trainingReady);
        stats.setDuplicateRecords(duplicates);

        return stats;
    }

    @Transactional(readOnly = true)
    public Page<MlDatasetRecordDto> getRecords(Pageable pageable, MlOutcomeStatus outcomeStatus, Long studentProfileId) {
        Page<MlDataset> page;
        if (studentProfileId != null) {
            page = mlDatasetRepository.findByStudentProfileId(studentProfileId, pageable);
        } else if (outcomeStatus != null) {
            page = mlDatasetRepository.findByOutcomeStatus(outcomeStatus, pageable);
        } else {
            page = mlDatasetRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<MlDatasetRecordDto> getPendingOutcomes(Pageable pageable) {
        return mlDatasetRepository.findByOutcomeStatus(MlOutcomeStatus.PENDING_OUTCOME, pageable)
                .map(this::mapToDto);
    }

    /**
     * Dedicated Step 6 dataset preparation and validation routine.
     */
    @Transactional
    public MlDatasetPrepareSummaryDto prepareAndValidateDataset() {
        List<MlDataset> all = mlDatasetRepository.findAll();
        MlDatasetPrepareSummaryDto summary = new MlDatasetPrepareSummaryDto();
        summary.setTotalDatabaseRecords(all.size());

        List<MlDatasetPrepareSummaryDto.ExcludedRecordDetail> details = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        long eligible = 0;
        long excluded = 0;
        long pending = 0;
        long feasible = 0;
        long notFeasible = 0;
        long duplicates = 0;

        for (MlDataset rec : all) {
            Long sid = rec.getStudentProfile() != null ? rec.getStudentProfile().getId() : null;
            Long pid = rec.getProject() != null ? rec.getProject().getId() : null;
            String key = sid + "_" + pid;

            boolean isDup = !seenKeys.add(key);
            if (isDup) {
                duplicates++;
            }

            if (rec.getOutcomeStatus() == MlOutcomeStatus.PENDING_OUTCOME) {
                pending++;
                excluded++;
                details.add(new MlDatasetPrepareSummaryDto.ExcludedRecordDetail(
                        rec.getId(), sid, pid, "Pending outcome: not yet verified by evaluator (excluded from supervised training)"
                ));
                continue;
            }

            if (rec.getOutcomeStatus() == MlOutcomeStatus.FEASIBLE) feasible++;
            if (rec.getOutcomeStatus() == MlOutcomeStatus.NOT_FEASIBLE) notFeasible++;

            List<String> errors = validateRecordFields(rec);
            if (isDup) {
                errors.add("Duplicate student-project snapshot");
            }

            if (!errors.isEmpty()) {
                excluded++;
                rec.setValidationStatus(MlValidationStatus.REJECTED);
                details.add(new MlDatasetPrepareSummaryDto.ExcludedRecordDetail(
                        rec.getId(), sid, pid, String.join("; ", errors)
                ));
            } else {
                eligible++;
                rec.setValidationStatus(MlValidationStatus.VALIDATED);
            }
        }

        mlDatasetRepository.saveAll(all);

        summary.setEligibleRecords(eligible);
        summary.setExcludedRecords(excluded);
        summary.setPendingOutcomeRecords(pending);
        summary.setVerifiedFeasibleRecords(feasible);
        summary.setVerifiedNotFeasibleRecords(notFeasible);
        summary.setDuplicateRecords(duplicates);
        summary.setExclusionDetails(details);

        return summary;
    }

    /**
     * Step 8 CSV Export: exports only complete, validated records with verified actual outcomes.
     */
    @Transactional(readOnly = true)
    public void exportCleanedTrainingCsv(Writer writer) {
        PrintWriter pw = new PrintWriter(writer);
        // Step 2 exact column names
        pw.println("student_skill_level,skill_gap,project_complexity,team_size,available_hours_per_week,technology_familiarity,number_of_requirements,number_of_features,previous_experience,estimated_effort,actual_outcome");

        List<MlDataset> all = mlDatasetRepository.findAll();
        for (MlDataset rec : all) {
            if (rec.getOutcomeStatus() == MlOutcomeStatus.PENDING_OUTCOME) {
                continue; // Do not export pending outcomes
            }
            if (rec.getActualOutcome() == null || (!"FEASIBLE".equalsIgnoreCase(rec.getActualOutcome()) && !"NOT_FEASIBLE".equalsIgnoreCase(rec.getActualOutcome()))) {
                continue;
            }
            List<String> errors = validateRecordFields(rec);
            if (!errors.isEmpty()) {
                continue; // Skip invalid records
            }

            pw.printf(Locale.US, "%.2f,%.2f,%s,%d,%.1f,%.2f,%d,%d,%.2f,%.1f,%s%n",
                    rec.getStudentSkillLevel(),
                    rec.getSkillGap(),
                    rec.getProjectComplexity().name(),
                    rec.getTeamSize(),
                    rec.getAvailableHoursPerWeek(),
                    rec.getTechnologyFamiliarity(),
                    rec.getNumberOfRequirements(),
                    rec.getNumberOfFeatures(),
                    rec.getPreviousExperience(),
                    rec.getEstimatedEffort(),
                    rec.getActualOutcome().toUpperCase()
            );
        }
        pw.flush();
    }

    /**
     * Step 9 Split statistics and readiness report.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSplitReport() {
        List<MlDataset> all = mlDatasetRepository.findAll();
        List<MlDataset> trainingReady = all.stream()
                .filter(r -> r.getOutcomeStatus() != MlOutcomeStatus.PENDING_OUTCOME)
                .filter(r -> "FEASIBLE".equalsIgnoreCase(r.getActualOutcome()) || "NOT_FEASIBLE".equalsIgnoreCase(r.getActualOutcome()))
                .filter(r -> validateRecordFields(r).isEmpty())
                .toList();

        long feasibleCount = trainingReady.stream().filter(r -> "FEASIBLE".equalsIgnoreCase(r.getActualOutcome())).count();
        long notFeasibleCount = trainingReady.stream().filter(r -> "NOT_FEASIBLE".equalsIgnoreCase(r.getActualOutcome())).count();
        int total = trainingReady.size();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalTrainingReadyRecords", total);
        report.put("feasibleCount", feasibleCount);
        report.put("notFeasibleCount", notFeasibleCount);
        report.put("trainRatio", 0.80);
        report.put("testRatio", 0.20);
        report.put("randomSeed", 42);

        boolean ready = total >= 10 && feasibleCount >= 2 && notFeasibleCount >= 2;
        report.put("isReadyForTraining", ready);
        if (ready) {
            int trainSize = (int) Math.round(total * 0.80);
            int testSize = total - trainSize;
            report.put("estimatedTrainSize", trainSize);
            report.put("estimatedTestSize", testSize);
            report.put("message", "Dataset has verified outcomes ready for stratified 80/20 train/test split in Phase 10.");
        } else {
            report.put("estimatedTrainSize", 0);
            report.put("estimatedTestSize", 0);
            report.put("message", "Dataset contains insufficient verified real outcome labels (" + total + " verified records). " +
                    "A minimum of verified FEASIBLE and NOT_FEASIBLE records is required before reliable 80/20 model training.");
        }
        return report;
    }

    // ─── Feature Computation Helpers ──────────────────────────────────────────

    /**
     * Feature 1: Average of student's available skill proficiency scores on 1.0 - 4.0 scale.
     * BEGINNER=1.0, INTERMEDIATE=2.0, ADVANCED=3.0, EXPERT=4.0.
     * Incorporates Phase 5 assessment results where available.
     * Does NOT assume missing values are zero.
     */
    public Double calculateStudentSkillLevel(StudentProfile profile) {
        if (profile == null) return null;

        List<StudentSkill> declared = profile.getStudentSkills();
        List<AssessmentResult> assessments = assessmentResultRepository
                .findByStudentProfileIdOrderByCreatedAtDesc(profile.getId());

        Map<Long, Double> skillScores = new HashMap<>();

        if (declared != null) {
            for (StudentSkill ss : declared) {
                if (ss.getSkill() != null && ss.getProficiencyLevel() != null) {
                    double score = proficiencyToScore(ss.getProficiencyLevel());
                    skillScores.put(ss.getSkill().getId(), score);
                }
            }
        }

        if (assessments != null) {
            for (AssessmentResult ar : assessments) {
                if (ar.getSkill() != null) {
                    Long skillId = ar.getSkill().getId();
                    double assessScore = ar.getLevel() != null
                            ? proficiencyToScore(ar.getLevel())
                            : Math.min(4.0, Math.max(1.0, 1.0 + (ar.getPercentage() / 100.0) * 3.0));

                    if (skillScores.containsKey(skillId)) {
                        // Weighted: 75% assessed, 25% declared
                        double blended = assessScore * 0.75 + skillScores.get(skillId) * 0.25;
                        skillScores.put(skillId, blended);
                    } else {
                        skillScores.put(skillId, assessScore);
                    }
                }
            }
        }

        if (skillScores.isEmpty()) {
            return null; // Missing, do NOT assume zero
        }

        double sum = 0.0;
        for (double v : skillScores.values()) {
            sum += v;
        }
        return round2(sum / skillScores.size());
    }

    /**
     * Feature 2: Existing Phase 7 skill-gap value.
     * In Phase 7: WEIGHT_SKILLS = 25.0. Skill fit score is computed via computeSkillFit.
     * Skill gap = 25.0 - skillFitScore (range: 0.0 to 25.0).
     */
    public Double calculateSkillGap(StudentProfile profile, Project project) {
        if (profile == null || project == null) return null;

        List<Skill> required = project.getRequiredSkills();
        if (required == null || required.isEmpty()) {
            return 0.0; // No required skills implies zero skill gap
        }

        List<AssessmentResult> assessments = assessmentResultRepository
                .findByStudentProfileIdOrderByCreatedAtDesc(profile.getId());

        Map<Long, AssessmentResult> latestAssessments = new HashMap<>();
        if (assessments != null) {
            for (AssessmentResult ar : assessments) {
                if (ar.getSkill() != null) {
                    Long sid = ar.getSkill().getId();
                    if (!latestAssessments.containsKey(sid) ||
                            ar.getPercentage() > latestAssessments.get(sid).getPercentage()) {
                        latestAssessments.put(sid, ar);
                    }
                }
            }
        }

        Map<Long, StudentSkill> declared = new HashMap<>();
        if (profile.getStudentSkills() != null) {
            for (StudentSkill ss : profile.getStudentSkills()) {
                if (ss.getSkill() != null) {
                    declared.put(ss.getSkill().getId(), ss);
                }
            }
        }

        double matchedPoints = 0.0;
        for (Skill s : required) {
            AssessmentResult ar = latestAssessments.get(s.getId());
            StudentSkill ss = declared.get(s.getId());

            if (ar != null) {
                matchedPoints += Math.min(1.0, ar.getPercentage() / 100.0);
            } else if (ss != null) {
                double declFraction = switch (ss.getProficiencyLevel() != null ? ss.getProficiencyLevel() : ProficiencyLevel.BEGINNER) {
                    case EXPERT -> 0.85;
                    case ADVANCED -> 0.70;
                    case INTERMEDIATE -> 0.55;
                    case BEGINNER -> 0.40;
                };
                if (ss.getYearsOfExperience() != null && ss.getYearsOfExperience() > 0) {
                    declFraction = Math.min(0.95, declFraction + Math.min(0.08, ss.getYearsOfExperience() * 0.02));
                }
                matchedPoints += declFraction;
            }
        }

        double ratio = matchedPoints / (double) required.size();
        double skillFitScore = Math.min(25.0, Math.max(0.0, ratio * 25.0));
        double skillGap = Math.max(0.0, Math.min(25.0, 25.0 - skillFitScore));
        return round2(skillGap);
    }

    /**
     * Feature 6: Average familiarity score for project's required technologies (1.0 to 4.0).
     */
    public Double calculateTechnologyFamiliarity(StudentProfile profile, Project project) {
        if (profile == null || project == null) return null;

        List<ProjectTechnology> projectTechs = project.getProjectTechnologies();
        if (projectTechs == null || projectTechs.isEmpty()) {
            // Project has no specific technologies; if student has general skill level, use it as fallback
            return calculateStudentSkillLevel(profile);
        }

        Map<Long, StudentSkill> declaredSkills = new HashMap<>();
        if (profile.getStudentSkills() != null) {
            for (StudentSkill ss : profile.getStudentSkills()) {
                if (ss.getSkill() != null) {
                    declaredSkills.put(ss.getSkill().getId(), ss);
                }
            }
        }

        List<AssessmentResult> assessments = assessmentResultRepository
                .findByStudentProfileIdOrderByCreatedAtDesc(profile.getId());
        Map<Long, Double> skillScores = new HashMap<>();
        if (assessments != null) {
            for (AssessmentResult ar : assessments) {
                if (ar.getSkill() != null) {
                    skillScores.put(ar.getSkill().getId(), ar.getLevel() != null
                            ? proficiencyToScore(ar.getLevel())
                            : Math.min(4.0, Math.max(1.0, 1.0 + (ar.getPercentage() / 100.0) * 3.0)));
                }
            }
        }

        List<Double> techScores = new ArrayList<>();
        Technology studentPrimaryTech = profile.getTechnology();

        for (ProjectTechnology pt : projectTechs) {
            Technology tech = pt.getTechnology();
            if (tech == null) continue;

            List<Double> compScores = new ArrayList<>();
            if (tech.getSkills() != null && !tech.getSkills().isEmpty()) {
                for (Skill s : tech.getSkills()) {
                    if (skillScores.containsKey(s.getId())) {
                        compScores.add(skillScores.get(s.getId()));
                    } else if (declaredSkills.containsKey(s.getId())) {
                        compScores.add(proficiencyToScore(declaredSkills.get(s.getId()).getProficiencyLevel()));
                    }
                }
            }

            if (!compScores.isEmpty()) {
                double avg = compScores.stream().mapToDouble(Double::doubleValue).average().orElse(2.0);
                techScores.add(avg);
            } else if (studentPrimaryTech != null && studentPrimaryTech.getId().equals(tech.getId())) {
                // Student declared this tech stack as primary; derive from overall skill level or default INTERMEDIATE
                Double studentLevel = calculateStudentSkillLevel(profile);
                techScores.add(studentLevel != null ? studentLevel : 2.5);
            }
        }

        if (techScores.isEmpty()) {
            return null; // Missing technology familiarity, do NOT assume zero
        }

        double sum = techScores.stream().mapToDouble(Double::doubleValue).sum();
        return round2(sum / techScores.size());
    }

    /**
     * Feature 7: Total project requirements (non-negative integer).
     */
    public Integer calculateNumberOfRequirements(Project project) {
        if (project == null) return 0;
        if (project.getRequirements() != null && !project.getRequirements().isEmpty()) {
            return project.getRequirements().size();
        }
        int count = 0;
        if (project.getFunctionalRequirements() != null && !project.getFunctionalRequirements().isBlank()) {
            count += splitCount(project.getFunctionalRequirements());
        }
        if (project.getNonFunctionalRequirements() != null && !project.getNonFunctionalRequirements().isBlank()) {
            count += splitCount(project.getNonFunctionalRequirements());
        }
        return Math.max(count, 2); // Baseline project requirements count
    }

    /**
     * Feature 8: Total project features (non-negative integer).
     */
    public Integer calculateNumberOfFeatures(Project project) {
        if (project == null) return 0;
        if (project.getFunctionalRequirements() != null && !project.getFunctionalRequirements().isBlank()) {
            return Math.max(1, splitCount(project.getFunctionalRequirements()));
        }
        if (project.getProposedSolution() != null && !project.getProposedSolution().isBlank()) {
            return Math.max(1, splitCount(project.getProposedSolution()));
        }
        return 3; // Standard baseline features
    }

    /**
     * Feature 9: Relevant previous experience in years.
     */
    public Double extractPreviousExperience(StudentProfile profile) {
        if (profile == null) return null;
        if (profile.getStudentSkills() != null && !profile.getStudentSkills().isEmpty()) {
            double maxExp = 0.0;
            boolean hasExp = false;
            for (StudentSkill ss : profile.getStudentSkills()) {
                if (ss.getYearsOfExperience() != null) {
                    hasExp = true;
                    if (ss.getYearsOfExperience() > maxExp) {
                        maxExp = ss.getYearsOfExperience();
                    }
                }
            }
            if (hasExp) {
                return round2(maxExp);
            }
        }
        // Do NOT invent experience if missing
        return null;
    }

    /**
     * Feature 10: Estimated project effort in hours.
     * Derived from project estimated weeks and available hours per week.
     */
    public Double calculateEstimatedEffort(Project project, Double availableHoursPerWeek) {
        if (project == null) return null;
        int weeks = project.getEstimatedWeeks() != null && project.getEstimatedWeeks() > 0
                ? project.getEstimatedWeeks()
                : 12;
        double hoursPerWeek = availableHoursPerWeek != null && availableHoursPerWeek > 0.0
                ? availableHoursPerWeek
                : 15.0;
        return round1(weeks * hoursPerWeek);
    }

    public boolean isRecordComplete(MlDataset r) {
        return r.getStudentSkillLevel() != null &&
                r.getSkillGap() != null &&
                r.getProjectComplexity() != null &&
                r.getTeamSize() != null && r.getTeamSize() > 0 &&
                r.getAvailableHoursPerWeek() != null && r.getAvailableHoursPerWeek() > 0.0 &&
                r.getTechnologyFamiliarity() != null &&
                r.getNumberOfRequirements() != null && r.getNumberOfRequirements() >= 0 &&
                r.getNumberOfFeatures() != null && r.getNumberOfFeatures() >= 0 &&
                r.getPreviousExperience() != null && r.getPreviousExperience() >= 0.0 &&
                r.getEstimatedEffort() != null && r.getEstimatedEffort() > 0.0;
    }

    public List<String> validateRecordFields(MlDataset r) {
        List<String> errors = new ArrayList<>();
        if (r.getStudentSkillLevel() == null || r.getStudentSkillLevel() < 1.0 || r.getStudentSkillLevel() > 4.0) {
            errors.add("Student skill level missing or outside [1.0, 4.0]");
        }
        if (r.getSkillGap() == null || r.getSkillGap() < 0.0 || r.getSkillGap() > 25.0) {
            errors.add("Skill gap missing or outside [0.0, 25.0]");
        }
        if (r.getProjectComplexity() == null) {
            errors.add("Project complexity missing");
        }
        if (r.getTeamSize() == null || r.getTeamSize() <= 0) {
            errors.add("Team size must be positive integer");
        }
        if (r.getAvailableHoursPerWeek() == null || r.getAvailableHoursPerWeek() <= 0.0 || r.getAvailableHoursPerWeek() > 80.0) {
            errors.add("Available hours per week must be between 1.0 and 80.0");
        }
        if (r.getTechnologyFamiliarity() == null || r.getTechnologyFamiliarity() < 1.0 || r.getTechnologyFamiliarity() > 4.0) {
            errors.add("Technology familiarity missing or outside [1.0, 4.0]");
        }
        if (r.getNumberOfRequirements() == null || r.getNumberOfRequirements() < 0) {
            errors.add("Number of requirements must be non-negative integer");
        }
        if (r.getNumberOfFeatures() == null || r.getNumberOfFeatures() < 0) {
            errors.add("Number of features must be non-negative integer");
        }
        if (r.getPreviousExperience() == null || r.getPreviousExperience() < 0.0) {
            errors.add("Previous experience missing or negative");
        }
        if (r.getEstimatedEffort() == null || r.getEstimatedEffort() <= 0.0) {
            errors.add("Estimated effort must be positive number in hours");
        }
        if (r.getActualOutcome() == null || (!"FEASIBLE".equalsIgnoreCase(r.getActualOutcome()) && !"NOT_FEASIBLE".equalsIgnoreCase(r.getActualOutcome()))) {
            errors.add("Actual outcome label missing or invalid (must be FEASIBLE or NOT_FEASIBLE)");
        }
        return errors;
    }

    private double proficiencyToScore(ProficiencyLevel level) {
        if (level == null) return 1.0;
        return switch (level) {
            case EXPERT -> 4.0;
            case ADVANCED -> 3.0;
            case INTERMEDIATE -> 2.0;
            case BEGINNER -> 1.0;
        };
    }

    private int splitCount(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] items = text.split("[,;\\n•\\r]+");
        int count = 0;
        for (String s : items) {
            if (!s.trim().isBlank()) {
                count++;
            }
        }
        return count;
    }

    private Double round1(Double val) {
        if (val == null) return null;
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private Double round2(Double val) {
        if (val == null) return null;
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public MlDatasetRecordDto mapToDto(MlDataset entity) {
        MlDatasetRecordDto dto = new MlDatasetRecordDto();
        dto.setId(entity.getId());
        if (entity.getStudentProfile() != null) {
            dto.setStudentProfileId(entity.getStudentProfile().getId());
            if (entity.getStudentProfile().getUser() != null) {
                dto.setStudentName(entity.getStudentProfile().getUser().getUsername());
                dto.setStudentEmail(entity.getStudentProfile().getUser().getEmail());
            }
        }
        if (entity.getProject() != null) {
            dto.setProjectId(entity.getProject().getId());
            dto.setProjectTitle(entity.getProject().getTitle());
        }
        dto.setStudentSkillLevel(entity.getStudentSkillLevel());
        dto.setSkillGap(entity.getSkillGap());
        dto.setProjectComplexity(entity.getProjectComplexity() != null ? entity.getProjectComplexity().name() : "MEDIUM");
        dto.setTeamSize(entity.getTeamSize());
        dto.setAvailableHoursPerWeek(entity.getAvailableHoursPerWeek());
        dto.setTechnologyFamiliarity(entity.getTechnologyFamiliarity());
        dto.setNumberOfRequirements(entity.getNumberOfRequirements());
        dto.setNumberOfFeatures(entity.getNumberOfFeatures());
        dto.setPreviousExperience(entity.getPreviousExperience());
        dto.setEstimatedEffort(entity.getEstimatedEffort());
        dto.setActualOutcome(entity.getActualOutcome());
        dto.setOutcomeStatus(entity.getOutcomeStatus() != null ? entity.getOutcomeStatus().name() : "PENDING_OUTCOME");
        dto.setDataSource(entity.getDataSource());
        dto.setValidationStatus(entity.getValidationStatus() != null ? entity.getValidationStatus().name() : "PENDING");
        dto.setLabelEvidence(entity.getLabelEvidence());
        dto.setLabelNotes(entity.getLabelNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

