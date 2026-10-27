package com.projectguard.service;

import com.projectguard.dto.admin.AdminStatsResponse;
import com.projectguard.entity.enums.Role;
import com.projectguard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminService — aggregates platform-wide statistics for the admin dashboard.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProjectRepository projectRepository;
    private final EngineeringBranchRepository branchRepository;
    private final ProjectDomainRepository domainRepository;
    private final SkillRepository skillRepository;
    private final TechnologyRepository technologyRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentResultRepository assessmentResultRepository;

    public AdminService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            ProjectRepository projectRepository,
            EngineeringBranchRepository branchRepository,
            ProjectDomainRepository domainRepository,
            SkillRepository skillRepository,
            TechnologyRepository technologyRepository,
            AssessmentAttemptRepository assessmentAttemptRepository,
            AssessmentResultRepository assessmentResultRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.projectRepository = projectRepository;
        this.branchRepository = branchRepository;
        this.domainRepository = domainRepository;
        this.skillRepository = skillRepository;
        this.technologyRepository = technologyRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.assessmentResultRepository = assessmentResultRepository;
    }

    /**
     * Returns aggregate platform statistics.
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        // User counts
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);
        stats.setTotalUsers(totalUsers);
        stats.setTotalStudents(totalStudents);
        stats.setTotalAdmins(totalAdmins);

        // Project counts
        long totalProjects = projectRepository.count();
        long catalogProjects = projectRepository.countByIsCatalogTrue();
        long approvedProjects = projectRepository.countByIsCatalogTrueAndQualityApprovedTrue();
        long aiGeneratedProjects = projectRepository.countByStudentProfileIdIsNotNull();
        stats.setTotalProjects(totalProjects);
        stats.setCatalogProjects(catalogProjects);
        stats.setApprovedProjects(approvedProjects);
        stats.setAiGeneratedProjects(aiGeneratedProjects);

        // Reference data counts
        stats.setTotalBranches(branchRepository.count());
        stats.setTotalDomains(domainRepository.count());
        stats.setTotalSkills(skillRepository.count());
        stats.setTotalTechnologies(technologyRepository.count());

        // Assessment counts
        stats.setTotalAssessmentAttempts(assessmentAttemptRepository.count());
        stats.setTotalAssessmentResults(assessmentResultRepository.count());

        return stats;
    }
}
