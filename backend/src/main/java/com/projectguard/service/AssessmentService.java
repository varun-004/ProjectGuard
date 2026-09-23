package com.projectguard.service;

import com.projectguard.dto.assessment.*;
import com.projectguard.dto.student.SkillResponse;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AssessmentAttemptRepository attemptRepository;

    public AssessmentService(AssessmentQuestionRepository questionRepository,
                             DomainAssessmentResultRepository domainResultRepository,
                             AssessmentResultRepository resultRepository,
                             StudentProfileRepository profileRepository,
                             UserRepository userRepository,
                             SkillRepository skillRepository,
                             StudentSkillRepository studentSkillRepository,
                             TechnologyRepository technologyRepository,
                             AssessmentAttemptRepository attemptRepository) {
        this.questionRepository = questionRepository;
        this.domainResultRepository = domainResultRepository;
        this.resultRepository = resultRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.technologyRepository = technologyRepository;
        this.attemptRepository = attemptRepository;
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

    @Transactional
    public AssessmentAttemptResponse getQuestionsForSkills(List<Long> skillIds, String username) {
        if (skillIds == null || skillIds.isEmpty()) {
            throw new IllegalArgumentException("skillIds must be a non-empty array");
        }

        List<Long> normalizedSkillIds = skillIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (normalizedSkillIds.isEmpty()) {
            throw new IllegalArgumentException("skillIds must contain valid IDs");
        }

        StudentProfile profile = getProfileByUsername(username);

        List<AssessmentQuestion> allQuestions =
                questionRepository.findBySkillIdIn(normalizedSkillIds);

        // Gather recently used questions
        List<AssessmentAttempt> recentAttempts = attemptRepository.findByStudentProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream().limit(3).collect(Collectors.toList());
        Set<Long> recentQuestionIds = recentAttempts.stream()
                .flatMap(a -> a.getIssuedQuestions().stream())
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toSet());

        List<AssessmentQuestion> easy = filterByDifficulty(allQuestions, AssessmentQuestion.Difficulty.EASY);
        List<AssessmentQuestion> medium = filterByDifficulty(allQuestions, AssessmentQuestion.Difficulty.MEDIUM);
        List<AssessmentQuestion> hard = filterByDifficulty(allQuestions, AssessmentQuestion.Difficulty.HARD);

        List<AssessmentQuestion> selected = new ArrayList<>();
        selected.addAll(fairDistribute(easy, normalizedSkillIds, 3, recentQuestionIds));
        selected.addAll(fairDistribute(medium, normalizedSkillIds, 4, recentQuestionIds));
        selected.addAll(fairDistribute(hard, normalizedSkillIds, 3, recentQuestionIds));

        if (selected.size() != 10) {
            throw new IllegalStateException(
                    "Unable to generate a complete assessment. Expected 10 questions but got " + selected.size());
        }

        Collections.shuffle(selected);

        // Save Attempt
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setStudentProfile(profile);
        attempt.setIssuedQuestions(selected);
        attempt = attemptRepository.save(attempt);

        List<AssessmentQuestionDto> dtoList = selected.stream().map(q -> {
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
        
        return new AssessmentAttemptResponse(attempt.getAttemptId(), dtoList);
    }

    private List<AssessmentQuestion> fairDistribute(List<AssessmentQuestion> pool, List<Long> skillIds, int targetCount, Set<Long> recentQuestionIds) {
        Map<Long, List<AssessmentQuestion>> bySkill = new HashMap<>();
        for (Long sid : skillIds) {
            bySkill.put(sid, new ArrayList<>());
        }
        for (AssessmentQuestion q : pool) {
            bySkill.get(q.getSkill().getId()).add(q);
        }
        
        for (List<AssessmentQuestion> qs : bySkill.values()) {
            Collections.shuffle(qs);
            // Sort so unused questions come first
            qs.sort(Comparator.comparing(q -> recentQuestionIds.contains(q.getId())));
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

    @Transactional(rollbackFor = Exception.class)
    public DomainAssessmentResultDto submitAssessment(String username, AssessmentSubmitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Assessment request must be provided");
        }
        if (request.getAttemptId() == null || request.getAttemptId().isBlank()) {
            throw new IllegalArgumentException("attemptId must be provided");
        }
        if (request.getSkillIds() == null || request.getSkillIds().isEmpty()) {
            throw new IllegalArgumentException("skillIds must be a non-empty array");
        }
        if (request.getAnswers() == null) {
            throw new IllegalArgumentException("answers list must be provided");
        }

        List<Long> requestedSkillIds = request.getSkillIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (requestedSkillIds.isEmpty()) {
            throw new IllegalArgumentException("skillIds must contain valid IDs");
        }

        StudentProfile profile = getProfileByUsername(username);

        AssessmentAttempt attempt = attemptRepository.findByAttemptId(request.getAttemptId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid attempt ID"));

        if (attempt.getStudentProfile() == null ||
                !attempt.getStudentProfile().getId().equals(profile.getId())) {
            throw new SecurityException("Attempt does not belong to this user");
        }

        if (attempt.getIssuedQuestions() == null || attempt.getIssuedQuestions().isEmpty()) {
            throw new IllegalStateException("Assessment attempt has no issued questions");
        }

        Set<Long> issuedQuestionIds = attempt.getIssuedQuestions().stream()
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toSet());

        // Source of truth for scoring: questions stored in this attempt.
        if (attempt.isSubmitted()) {
            throw new IllegalStateException("Assessment attempt has already been submitted");
        }

        Set<Long> attemptSkillIds = attempt.getIssuedQuestions().stream()
                .map(q -> q.getSkill().getId())
                .collect(Collectors.toSet());

        // Frontend skill IDs must match the skills actually issued in this attempt.
        if (!attemptSkillIds.equals(new HashSet<>(requestedSkillIds))) {
            throw new IllegalArgumentException("Submitted skills do not match the issued assessment");
        }

        Set<Long> seenQuestionIds = new HashSet<>();
        for (AssessmentSubmitRequest.AnswerSubmission answer : request.getAnswers()) {
            if (answer == null ||
                    answer.getQuestionId() == null ||
                    answer.getSelectedOptionIndex() == null) {
                throw new IllegalArgumentException(
                        "questionId and selectedOptionIndex must be provided");
            }

            if (!seenQuestionIds.add(answer.getQuestionId())) {
                throw new IllegalArgumentException(
                        "Duplicate question ID " + answer.getQuestionId() + " in submission");
            }

            if (!issuedQuestionIds.contains(answer.getQuestionId())) {
                throw new IllegalArgumentException(
                        "Question ID " + answer.getQuestionId() +
                                " was not issued in this attempt");
            }

            if (answer.getSelectedOptionIndex() < 0 ||
                    answer.getSelectedOptionIndex() > 3) {
                throw new IllegalArgumentException(
                        "Invalid option index for question " + answer.getQuestionId());
            }
        }

        List<Long> skillIds = new ArrayList<>(attemptSkillIds);

        DomainAssessmentResult domainResult = new DomainAssessmentResult();
        domainResult.setStudentProfile(profile);
        domainResult.setCreatedAt(LocalDateTime.now());

        Map<Long, SkillStats> skillStatsMap = new HashMap<>();
        for (Long sid : skillIds) {
            Skill skill = skillRepository.findById(sid)
                    .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + sid));
            skillStatsMap.put(sid, new SkillStats(skill));
        }

        int totalScore = 0;
        int maxPossibleScore = 0;
        int correctAnswers = 0;
        int totalQuestions = attempt.getIssuedQuestions().size();
        List<AssessmentReviewDto> reviews = new ArrayList<>();

        for (AssessmentSubmitRequest.AnswerSubmission answer : request.getAnswers()) {
            if (answer.getQuestionId() == null || answer.getSelectedOptionIndex() == null) {
                throw new RuntimeException("questionId and selectedOptionIndex must be provided");
            }
            if (!issuedQuestionIds.contains(answer.getQuestionId())) {
                throw new RuntimeException("Question ID " + answer.getQuestionId() + " was not issued in this attempt");
            }
            AssessmentQuestion question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Question not found: " + answer.getQuestionId()));

            boolean isCorrect = question.getCorrectOptionIndex().equals(answer.getSelectedOptionIndex());
            int points = 0;
            switch (question.getDifficulty()) {
                case EASY -> points = 1;
                case MEDIUM -> points = 2;
                case HARD -> points = 3;
            }
            
            maxPossibleScore += points;
            if (isCorrect) correctAnswers++;
            
            Long sid = question.getSkill().getId();
            SkillStats ss = skillStatsMap.get(sid);
            if (ss != null) {
                ss.maxScore += points;
                if (isCorrect) {
                    totalScore += points;
                    ss.score += points;
                }
            }

            AssessmentAttemptReview entityReview = new AssessmentAttemptReview();
            entityReview.setQuestion(question);
            entityReview.setSelectedOptionIndex(answer.getSelectedOptionIndex());
            entityReview.setIsCorrect(isCorrect);
            domainResult.addAttemptReview(entityReview);
        }

        // Ensure questions that were not answered count towards maxPossibleScore but with 0 points
        for (AssessmentQuestion q : attempt.getIssuedQuestions()) {
            boolean answered = seenQuestionIds.contains(q.getId());
            if (!answered) {
                int points = switch (q.getDifficulty()) {
                    case EASY -> 1;
                    case MEDIUM -> 2;
                    case HARD -> 3;
                };
                maxPossibleScore += points;
                SkillStats ss = skillStatsMap.get(q.getSkill().getId());
                if (ss != null) ss.maxScore += points;
                
                AssessmentAttemptReview entityReview = new AssessmentAttemptReview();
                entityReview.setQuestion(q);
                entityReview.setSelectedOptionIndex(-1); // unanswered
                entityReview.setIsCorrect(false);
                domainResult.addAttemptReview(entityReview);
            }
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

        // Mark only after the complete result graph has been successfully saved.
        attempt.setSubmitted(true);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        DomainAssessmentResultDto dto = mapToDomainDto(saved);
        // Correct answers and total questions will be mapped by mapToDomainDto implicitly, 
        // but we'll populate them there by reading attemptReviews
        return dto;
    }

    private ProficiencyLevel calculateLevel(double percentage) {
        if (percentage <= 40) return ProficiencyLevel.BEGINNER;
        if (percentage <= 70) return ProficiencyLevel.INTERMEDIATE;
        return ProficiencyLevel.ADVANCED;
    }

    @Transactional(readOnly = true)
    public Page<DomainAssessmentResultDto> getMyResults(String username, Pageable pageable) {
        StudentProfile profile = getProfileByUsername(username);
        return domainResultRepository
                .findByStudentProfileIdOrderByCreatedAtDesc(profile.getId(), pageable)
                .map(this::mapToDomainDto);
    }

    private DomainAssessmentResultDto mapToDomainDto(DomainAssessmentResult result) {
        DomainAssessmentResultDto dto = new DomainAssessmentResultDto();
        dto.setId(result.getId());
        dto.setScore(result.getScore());
        dto.setPercentage(result.getPercentage());
        dto.setLevel(result.getLevel().name());
        dto.setCreatedAt(result.getCreatedAt());
        
        int totalQuestions = result.getAttemptReviews() != null ? result.getAttemptReviews().size() : 0;
        int correctAnswers = 0;
        int maxWeightedScore = 0;
        
        List<AssessmentReviewDto> reviews = new ArrayList<>();
        if (result.getAttemptReviews() != null) {
            for (AssessmentAttemptReview ar : result.getAttemptReviews()) {
                if (Boolean.TRUE.equals(ar.getIsCorrect())) correctAnswers++;
                int points = switch (ar.getQuestion().getDifficulty()) {
                    case EASY -> 1;
                    case MEDIUM -> 2;
                    case HARD -> 3;
                };
                maxWeightedScore += points;
                
                AssessmentReviewDto reviewDto = new AssessmentReviewDto();
                reviewDto.setQuestionId(ar.getQuestion().getId());
                reviewDto.setQuestionText(ar.getQuestion().getQuestionText());
                reviewDto.setSelectedOptionIndex(ar.getSelectedOptionIndex());
                reviewDto.setCorrectOptionIndex(ar.getQuestion().getCorrectOptionIndex());
                reviewDto.setCorrect(ar.getIsCorrect());
                reviewDto.setExplanation(ar.getQuestion().getExplanation());
                reviews.add(reviewDto);
            }
        }
        
        dto.setTotalQuestions(totalQuestions);
        dto.setCorrectAnswers(correctAnswers);
        dto.setMaxWeightedScore(maxWeightedScore);
        dto.setReviews(reviews);
        
        List<AssessmentResultDto> skillDtos = result.getSkillResults().stream().map(sr -> {
            AssessmentResultDto sdto = new AssessmentResultDto();
            sdto.setId(sr.getId());
            sdto.setSkillId(sr.getSkill().getId());
            sdto.setSkillName(sr.getSkill().getName());
            sdto.setScore(sr.getScore());
            
            // Calculate max weighted score for this skill from reviews
            int skillMax = 0;
            if (result.getAttemptReviews() != null) {
                skillMax = result.getAttemptReviews().stream()
                    .filter(ar -> ar.getQuestion().getSkill().getId().equals(sr.getSkill().getId()))
                    .mapToInt(ar -> switch(ar.getQuestion().getDifficulty()) {
                        case EASY -> 1;
                        case MEDIUM -> 2;
                        case HARD -> 3;
                    }).sum();
            }
            sdto.setMaxWeightedScore(skillMax);
            
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
