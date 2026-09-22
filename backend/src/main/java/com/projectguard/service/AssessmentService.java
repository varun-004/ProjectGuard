package com.projectguard.service;

import com.projectguard.dto.assessment.*;
import com.projectguard.dto.student.SkillResponse;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentService {

    private final AssessmentQuestionRepository questionRepository;
    private final DomainAssessmentResultRepository domainResultRepository;
    private final AssessmentResultRepository resultRepository;
    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final TechnologyRepository technologyRepository;

    public AssessmentService(AssessmentQuestionRepository questionRepository,
                             DomainAssessmentResultRepository domainResultRepository,
                             AssessmentResultRepository resultRepository,
                             StudentProfileRepository profileRepository,
                             UserRepository userRepository,
                             SkillRepository skillRepository,
                             StudentSkillRepository studentSkillRepository,
                             TechnologyRepository technologyRepository) {
        this.questionRepository = questionRepository;
        this.domainResultRepository = domainResultRepository;
        this.resultRepository = resultRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.technologyRepository = technologyRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getRelevantSkills(String username) {
        return getRelevantSkills(username, null);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getRelevantSkills(String username, Long technologyId) {
        StudentProfile profile = getProfileByUsername(username);

        // If a specific technology is requested, return only its skills
        if (technologyId != null) {
            Technology tech = technologyRepository.findById(technologyId).orElse(null);
            if (tech != null) {
                return tech.getSkills().stream()
                        .map(s -> new SkillResponse(s.getId(), s.getName()))
                        .sorted(Comparator.comparing(SkillResponse::getName))
                        .collect(Collectors.toList());
            }
        }

        // Default: collect from the user's domain (all technologies + direct domain skills)
        ProjectDomain rootDomain = profile.getDomain();
        Set<Skill> relevantSkills = new HashSet<>();
        collectSkillsFromDomain(rootDomain, relevantSkills);

        return relevantSkills.stream()
                .map(s -> new SkillResponse(s.getId(), s.getName()))
                .sorted(Comparator.comparing(SkillResponse::getName))
                .collect(Collectors.toList());
    }

    private void collectSkillsFromDomain(ProjectDomain domain, Set<Skill> skills) {
        if (domain == null) return;
        skills.addAll(domain.getSkills());
        for (Technology tech : domain.getTechnologies()) {
            skills.addAll(tech.getSkills());
        }
        for (ProjectDomain sub : domain.getSubDomains()) {
            collectSkillsFromDomain(sub, skills);
        }
    }

    @Transactional(readOnly = true)
    public List<AssessmentQuestionDto> getQuestionsForSkills(List<Long> skillIds) {
        List<AssessmentQuestion> allQuestions = questionRepository.findAll().stream()
                .filter(q -> skillIds.contains(q.getSkill().getId()))
                .collect(Collectors.toList());

        List<AssessmentQuestion> easy = filterByDifficulty(allQuestions, AssessmentQuestion.Difficulty.EASY);
        List<AssessmentQuestion> medium = filterByDifficulty(allQuestions, AssessmentQuestion.Difficulty.MEDIUM);
        List<AssessmentQuestion> hard = filterByDifficulty(allQuestions, AssessmentQuestion.Difficulty.HARD);

        List<AssessmentQuestion> selected = new ArrayList<>();
        selected.addAll(fairDistribute(easy, skillIds, 3));
        selected.addAll(fairDistribute(medium, skillIds, 4));
        selected.addAll(fairDistribute(hard, skillIds, 3));

        Collections.shuffle(selected);

        return selected.stream().map(q -> {
            List<AssessmentOptionDto> options = q.getOptions().stream()
                    .map(o -> new AssessmentOptionDto(o.getId(), o.getOptionText()))
                    .collect(Collectors.toList());
            
            return new AssessmentQuestionDto(
                    q.getId(),
                    q.getQuestionText(),
                    q.getSkill().getName(),
                    q.getDifficulty().name(),
                    options
            );
        }).collect(Collectors.toList());
    }

    private List<AssessmentQuestion> fairDistribute(List<AssessmentQuestion> pool, List<Long> skillIds, int targetCount) {
        Map<Long, List<AssessmentQuestion>> bySkill = new HashMap<>();
        for (Long sid : skillIds) {
            bySkill.put(sid, new ArrayList<>());
        }
        for (AssessmentQuestion q : pool) {
            bySkill.get(q.getSkill().getId()).add(q);
        }
        
        for (List<AssessmentQuestion> qs : bySkill.values()) {
            Collections.shuffle(qs);
        }

        List<AssessmentQuestion> result = new ArrayList<>();
        int added = 0;
        boolean keepsGoing = true;
        
        while (added < targetCount && keepsGoing) {
            keepsGoing = false;
            for (Long sid : skillIds) {
                if (added >= targetCount) break;
                List<AssessmentQuestion> qs = bySkill.get(sid);
                if (!qs.isEmpty()) {
                    result.add(qs.remove(0));
                    added++;
                    keepsGoing = true;
                }
            }
        }
        return result;
    }

    private List<AssessmentQuestion> filterByDifficulty(List<AssessmentQuestion> list, AssessmentQuestion.Difficulty diff) {
        return list.stream().filter(q -> q.getDifficulty() == diff).collect(Collectors.toList());
    }

    public DomainAssessmentResultDto submitAssessment(String username, AssessmentSubmitRequest request) {
        if (request.getSkillIds() == null || request.getSkillIds().isEmpty()) {
            throw new RuntimeException("skillIds must be a non-empty array");
        }
        if (request.getAnswers() == null) {
            throw new RuntimeException("answers list must be provided");
        }

        StudentProfile profile = getProfileByUsername(username);
        List<Long> skillIds = request.getSkillIds();

        DomainAssessmentResult domainResult = new DomainAssessmentResult();
        domainResult.setStudentProfile(profile);
        domainResult.setCreatedAt(LocalDateTime.now());

        Map<Long, SkillStats> skillStatsMap = new HashMap<>();
        for (Long sid : skillIds) {
            Skill skill = skillRepository.findById(sid).orElseThrow();
            skillStatsMap.put(sid, new SkillStats(skill));
        }

        int totalScore = 0;
        int maxPossibleScore = 0;
        List<AssessmentReviewDto> reviews = new ArrayList<>();

        for (AssessmentSubmitRequest.AnswerSubmission answer : request.getAnswers()) {
            if (answer.getQuestionId() == null || answer.getSelectedOptionIndex() == null) {
                throw new RuntimeException("questionId and selectedOptionIndex must be provided");
            }
            AssessmentQuestion question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            if (!skillIds.contains(question.getSkill().getId())) {
                throw new RuntimeException("Submitted question does not belong to the selected skills.");
            }

            boolean isCorrect = question.getCorrectOptionIndex().equals(answer.getSelectedOptionIndex());
            int points = 0;
            switch (question.getDifficulty()) {
                case EASY -> points = 1;
                case MEDIUM -> points = 2;
                case HARD -> points = 3;
            }
            
            maxPossibleScore += points;
            
            Long sid = question.getSkill().getId();
            SkillStats ss = skillStatsMap.get(sid);
            if (ss != null) {
                ss.maxScore += points;
                if (isCorrect) {
                    totalScore += points;
                    ss.score += points;
                }
            }

            AssessmentReviewDto review = new AssessmentReviewDto();
            review.setQuestionId(question.getId());
            review.setQuestionText(question.getQuestionText());
            review.setSelectedOptionIndex(answer.getSelectedOptionIndex());
            review.setCorrectOptionIndex(question.getCorrectOptionIndex());
            review.setCorrect(isCorrect);
            review.setExplanation(question.getExplanation());
            reviews.add(review);
        }

        // Overall
        double overallPercentage = maxPossibleScore == 0 ? 0 : (totalScore / (double) maxPossibleScore) * 100.0;
        domainResult.setScore(totalScore);
        domainResult.setPercentage(overallPercentage);
        domainResult.setLevel(calculateLevel(overallPercentage));

        // Skill wise
        for (SkillStats ss : skillStatsMap.values()) {
            if (ss.maxScore == 0) continue; // No questions for this skill
            double percentage = (ss.score / (double) ss.maxScore) * 100.0;
            ProficiencyLevel level = calculateLevel(percentage);
            
            AssessmentResult ar = new AssessmentResult();
            ar.setStudentProfile(profile);
            ar.setSkill(ss.skill);
            ar.setScore(ss.score);
            ar.setPercentage(percentage);
            ar.setLevel(level);
            ar.setCreatedAt(LocalDateTime.now());
            
            domainResult.addSkillResult(ar);
            
            // Sync with StudentSkill
            Optional<StudentSkill> existing = studentSkillRepository.findByStudentProfileIdAndSkillId(profile.getId(), ss.skill.getId());
            if (existing.isPresent()) {
                StudentSkill stSkill = existing.get();
                // Overwrite with latest assessment result
                stSkill.setProficiencyLevel(level);
                studentSkillRepository.save(stSkill);
            }
        }

        DomainAssessmentResult saved = domainResultRepository.save(domainResult);

        DomainAssessmentResultDto dto = mapToDomainDto(saved);
        dto.setReviews(reviews);
        return dto;
    }

    private ProficiencyLevel calculateLevel(double percentage) {
        if (percentage < 50) return ProficiencyLevel.BEGINNER;
        if (percentage < 75) return ProficiencyLevel.INTERMEDIATE;
        return ProficiencyLevel.ADVANCED;
    }

    @Transactional(readOnly = true)
    public List<DomainAssessmentResultDto> getMyResults(String username) {
        StudentProfile profile = getProfileByUsername(username);
        return domainResultRepository.findByStudentProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(this::mapToDomainDto)
                .collect(Collectors.toList());
    }

    private DomainAssessmentResultDto mapToDomainDto(DomainAssessmentResult result) {
        DomainAssessmentResultDto dto = new DomainAssessmentResultDto();
        dto.setId(result.getId());
        dto.setScore(result.getScore());
        dto.setPercentage(result.getPercentage());
        dto.setLevel(result.getLevel().name());
        dto.setCreatedAt(result.getCreatedAt());
        
        List<AssessmentResultDto> skillDtos = result.getSkillResults().stream().map(sr -> {
            AssessmentResultDto sdto = new AssessmentResultDto();
            sdto.setId(sr.getId());
            sdto.setSkillId(sr.getSkill().getId());
            sdto.setSkillName(sr.getSkill().getName());
            sdto.setScore(sr.getScore());
            sdto.setPercentage(sr.getPercentage());
            sdto.setLevel(sr.getLevel().name());
            sdto.setCreatedAt(result.getCreatedAt());
            return sdto;
        }).collect(Collectors.toList());
        
        dto.setSkillResults(skillDtos);
        return dto;
    }

    private StudentProfile getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }
    
    private static class SkillStats {
        Skill skill;
        int score = 0;
        int maxScore = 0;
        SkillStats(Skill skill) { this.skill = skill; }
    }
}

