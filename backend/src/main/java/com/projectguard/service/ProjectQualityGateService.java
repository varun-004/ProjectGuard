package com.projectguard.service;

import com.projectguard.entity.Project;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectQualityGateService {

    public static class QualityGateResult {
        private final boolean passed;
        private final double qualityScore;
        private final String evaluationSummary;
        private final List<String> flags;
        private final Map<String, Double> criteriaScores;

        public QualityGateResult(boolean passed, double qualityScore, String evaluationSummary,
                                 List<String> flags, Map<String, Double> criteriaScores) {
            this.passed = passed;
            this.qualityScore = qualityScore;
            this.evaluationSummary = evaluationSummary;
            this.flags = flags;
            this.criteriaScores = criteriaScores;
        }

        public boolean isPassed() { return passed; }
        public double getQualityScore() { return qualityScore; }
        public String getEvaluationSummary() { return evaluationSummary; }
        public List<String> getFlags() { return flags; }
        public Map<String, Double> getCriteriaScores() { return criteriaScores; }
    }

    /**
     * Evaluates a project catalog entry independently from any student matching.
     * Requires genuine problem, identifiable stakeholder, measurable outcome,
     * realistic scope, and industry-appropriate validation.
     */
    public QualityGateResult evaluate(Project project) {
        List<String> flags = new ArrayList<>();
        Map<String, Double> criteriaScores = new LinkedHashMap<>();

        // 1. Problem Context & Pain Point (Max 20.0)
        double problemScore = evaluateProblem(project, flags);
        criteriaScores.put("problemContext", problemScore);

        // 2. Target Stakeholders & Operational Workflow (Max 20.0)
        double stakeholderScore = evaluateStakeholders(project, flags);
        criteriaScores.put("targetStakeholders", stakeholderScore);

        // 3. Measurable Outcome & KPIs (Max 20.0)
        double outcomeScore = evaluateOutcomeKpis(project, flags);
        criteriaScores.put("outcomeKpis", outcomeScore);

        // 4. Realistic Scope & Academic/Technical Depth (Max 20.0)
        double scopeScore = evaluateScopeAndDepth(project, flags);
        criteriaScores.put("scopeAndDepth", scopeScore);

        // 5. Testing, Validation & Contextual Quality (Max 20.0)
        double validationScore = evaluateContextualValidation(project, flags);
        criteriaScores.put("validationAndReliability", validationScore);

        double totalScore = problemScore + stakeholderScore + outcomeScore + scopeScore + validationScore;
        totalScore = Math.min(100.0, Math.max(0.0, Math.round(totalScore * 10.0) / 10.0));

        // Core gate requirements: problem, stakeholder, outcome, realistic scope must not fail severely
        boolean hasCriticalFailure = problemScore < 10.0 || stakeholderScore < 10.0 ||
                outcomeScore < 10.0 || scopeScore < 10.0;

        boolean passed = totalScore >= 70.0 && !hasCriticalFailure;

        StringBuilder summary = new StringBuilder();
        if (passed) {
            summary.append(String.format("Passed Quality Gate (Score: %.1f/100). Verified real-world relevance, identifiable stakeholders, measurable KPIs, and sound engineering scope.", totalScore));
        } else {
            summary.append(String.format("Failed Quality Gate (Score: %.1f/100). Excluded from active recommendations. Deficiencies: %s",
                    totalScore, String.join("; ", flags)));
        }

        return new QualityGateResult(passed, totalScore, summary.toString(), flags, criteriaScores);
    }

    private double evaluateProblem(Project project, List<String> flags) {
        String problem = project.getProblemStatement();
        if (problem == null || problem.trim().length() < 30) {
            flags.add("Problem statement missing or excessively brief");
            return 4.0;
        }

        String lower = problem.toLowerCase();
        // Reject generic CRUD/tutorial clones
        if (lower.contains("crud only") || lower.contains("basic management system") ||
                lower.contains("tutorial clone") || lower.contains("dummy demo") ||
                lower.contains("simple to-do")) {
            flags.add("Rejected: Generic CRUD or tutorial clone without genuine problem solving");
            return 3.0;
        }

        double score = 14.0;
        if (problem.length() >= 80) score += 3.0;
        if (lower.contains("latency") || lower.contains("failure") || lower.contains("loss") ||
                lower.contains("bottleneck") || lower.contains("inefficiency") || lower.contains("risk") ||
                lower.contains("cost") || lower.contains("manual") || lower.contains("hazard")) {
            score += 3.0;
        }
        return Math.min(20.0, score);
    }

    private double evaluateStakeholders(Project project, List<String> flags) {
        String stakeholders = project.getTargetStakeholders();
        if (stakeholders == null || stakeholders.trim().length() < 25) {
            flags.add("Target stakeholders missing or vague");
            return 5.0;
        }

        double score = 14.0;
        if (stakeholders.length() >= 60) score += 3.0;
        String lower = stakeholders.toLowerCase();
        if (lower.contains("engineer") || lower.contains("operator") || lower.contains("clinician") ||
                lower.contains("farmer") || lower.contains("administrator") || lower.contains("municipality") ||
                lower.contains("technician") || lower.contains("inspector") || lower.contains("patient") ||
                lower.contains("analyst") || lower.contains("worker") || lower.contains("enterprise")) {
            score += 3.0;
        }
        return Math.min(20.0, score);
    }

    private double evaluateOutcomeKpis(Project project, List<String> flags) {
        String kpis = project.getExpectedOutcomeKpis();
        if (kpis == null || kpis.trim().length() < 25) {
            flags.add("Measurable outcome / KPIs missing or vague");
            return 5.0;
        }

        double score = 14.0;
        if (kpis.length() >= 60) score += 3.0;
        String lower = kpis.toLowerCase();
        // Quantifiable indicators
        if (lower.contains("%") || lower.contains("ms") || lower.contains("reduction") ||
                lower.contains("improvement") || lower.contains("accuracy") || lower.contains("throughput") ||
                lower.contains("rate") || lower.contains("f1") || lower.contains("kwh") ||
                lower.contains("precision") || lower.contains("hours") || lower.contains("cost")) {
            score += 3.0;
        }
        return Math.min(20.0, score);
    }

    private double evaluateScopeAndDepth(Project project, List<String> flags) {
        double score = 12.0;

        // Check estimated weeks
        Integer weeks = project.getEstimatedWeeks();
        if (weeks != null && weeks >= 6 && weeks <= 24) {
            score += 3.0;
        } else if (weeks == null || weeks < 4) {
            flags.add("Estimated effort is too low for an engineering capstone/project");
            score -= 3.0;
        }

        // Check team bounds
        Integer minT = project.getMinTeamSize();
        Integer maxT = project.getMaxTeamSize();
        if (minT != null && maxT != null && minT <= maxT && minT >= 1 && maxT <= 6) {
            score += 2.0;
        }

        // Functional requirements depth
        String func = project.getFunctionalRequirements();
        if (func != null && func.length() >= 50) {
            score += 3.0;
        }

        return Math.min(20.0, Math.max(0.0, score));
    }

    private double evaluateContextualValidation(Project project, List<String> flags) {
        String testVal = project.getTestingValidation();
        if (testVal == null || testVal.trim().length() < 20) {
            flags.add("Testing and validation approach not adequately detailed");
            return 6.0;
        }

        double score = 12.0;
        if (testVal.length() >= 60) score += 4.0;

        // Context-sensitive checks based on domain / characteristics
        String domainName = (project.getDomain() != null) ? project.getDomain().getName().toLowerCase() : "";
        
        if (domainName.contains("cyber") || domainName.contains("cloud") || domainName.contains("health")) {
            if (project.getSecurityPrivacySafety() != null && project.getSecurityPrivacySafety().length() >= 30) {
                score += 4.0;
            } else {
                flags.add("Advisory: Security, privacy, or safety considerations are important for this domain");
            }
        } else if (domainName.contains("civil") || domainName.contains("mechanical") || domainName.contains("structural") ||
                   domainName.contains("thermal") || domainName.contains("environmental")) {
            if ((project.getCostSustainability() != null && project.getCostSustainability().length() >= 25) ||
                (project.getSecurityPrivacySafety() != null && project.getSecurityPrivacySafety().length() >= 25)) {
                score += 4.0;
            } else {
                score += 2.0;
            }
        } else {
            // General engineering validation
            if (project.getScalabilityIntegration() != null && project.getScalabilityIntegration().length() >= 25) {
                score += 4.0;
            } else {
                score += 2.0;
            }
        }

        return Math.min(20.0, score);
    }
}
