package com.projectguard.service;

import com.projectguard.dto.student.*;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final SkillRepository skillRepository;
    private final EngineeringBranchRepository branchRepository;
    private final ProjectDomainRepository domainRepository;
    private final UserRepository userRepository;
    private final ProfilePhotoService profilePhotoService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public StudentService(
            StudentProfileRepository studentProfileRepository,
            StudentSkillRepository studentSkillRepository,
            SkillRepository skillRepository,
            EngineeringBranchRepository branchRepository,
            ProjectDomainRepository domainRepository,
            UserRepository userRepository,
            ProfilePhotoService profilePhotoService) {

        this.studentProfileRepository = studentProfileRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.skillRepository = skillRepository;
        this.branchRepository = branchRepository;
        this.domainRepository = domainRepository;
        this.userRepository = userRepository;
        this.profilePhotoService = profilePhotoService;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public StudentProfileResponse createProfile(String username, StudentProfileRequest request) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (studentProfileRepository.existsByUserId(user.getId())) {
            throw new RuntimeException("Student profile already exists for this user");
        }

        EngineeringBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Engineering branch not found with ID: " + request.getBranchId()));

        ProjectDomain domain = domainRepository.findById(request.getDomainId())
                .orElseThrow(() -> new RuntimeException("Project domain not found with ID: " + request.getDomainId()));

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setBranch(branch);
        profile.setDomain(domain);
        profile.setTeamSize(request.getTeamSize());
        profile.setAvailableTimeWeeks(request.getAvailableTimeWeeks());
        profile.setPreviousExperience(request.getPreviousExperience());
        profile.setInterests(request.getInterests());

        StudentProfile saved = studentProfileRepository.save(profile);
        return mapToResponse(saved);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long id) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student profile not found with ID: " + id));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("No student profile found for user: " + username));

        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public boolean hasProfile(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        return studentProfileRepository.existsByUserId(user.getId());
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public StudentProfileResponse updateProfile(Long id, String username, StudentProfileRequest request) {

        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student profile not found with ID: " + id));

        assertOwner(profile, username);

        EngineeringBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Engineering branch not found with ID: " + request.getBranchId()));

        ProjectDomain domain = domainRepository.findById(request.getDomainId())
                .orElseThrow(() -> new RuntimeException("Project domain not found with ID: " + request.getDomainId()));

        profile.setBranch(branch);
        profile.setDomain(domain);
        profile.setTeamSize(request.getTeamSize());
        profile.setAvailableTimeWeeks(request.getAvailableTimeWeeks());
        profile.setPreviousExperience(request.getPreviousExperience());
        profile.setInterests(request.getInterests());

        StudentProfile saved = studentProfileRepository.save(profile);
        return mapToResponse(saved);
    }

    // ─── Skills ───────────────────────────────────────────────────────────────

    public StudentProfileResponse addSkills(Long id, String username, StudentSkillsRequest request) {

        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student profile not found with ID: " + id));

        assertOwner(profile, username);

        for (StudentSkillRequest skillReq : request.getSkills()) {
            Skill skill = resolveSkill(skillReq);

            boolean alreadyExists = studentSkillRepository
                    .existsByStudentProfileIdAndSkillId(profile.getId(), skill.getId());

            if (!alreadyExists) {
                StudentSkill studentSkill = buildStudentSkill(profile, skill, skillReq);
                studentSkillRepository.save(studentSkill);
            }
        }

        StudentProfile updated = studentProfileRepository.findById(id).orElseThrow();
        return mapToResponse(updated);
    }

    public StudentProfileResponse replaceSkills(Long id, String username, StudentSkillsRequest request) {

        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student profile not found with ID: " + id));

        assertOwner(profile, username);

        List<StudentSkill> existing = studentSkillRepository.findByStudentProfileId(profile.getId());
        studentSkillRepository.deleteAll(existing);

        for (StudentSkillRequest skillReq : request.getSkills()) {
            Skill skill = resolveSkill(skillReq);
            StudentSkill studentSkill = buildStudentSkill(profile, skill, skillReq);
            studentSkillRepository.save(studentSkill);
        }

        StudentProfile updated = studentProfileRepository.findById(id).orElseThrow();
        return mapToResponse(updated);
    }

    // ─── Photo ────────────────────────────────────────────────────────────────

    public StudentProfileResponse uploadPhoto(Long id, String username, MultipartFile file) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student profile not found with ID: " + id));

        assertOwner(profile, username);

        // Delete old photo if one exists
        if (profile.getPhotoPath() != null) {
            profilePhotoService.delete(profile.getPhotoPath());
        }

        try {
            String relativePath = profilePhotoService.store(file);
            profile.setPhotoPath(relativePath);
            StudentProfile saved = studentProfileRepository.save(profile);
            return mapToResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store photo: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public StudentProfileResponse deletePhoto(Long id, String username) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student profile not found with ID: " + id));

        assertOwner(profile, username);

        if (profile.getPhotoPath() != null) {
            profilePhotoService.delete(profile.getPhotoPath());
            profile.setPhotoPath(null);
            studentProfileRepository.save(profile);
        }

        return mapToResponse(profile);
    }

    // ─── Metadata / Dropdowns ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EngineeringBranchResponse> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(b -> new EngineeringBranchResponse(b.getId(), b.getName(), b.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDomainResponse> getAllDomains() {
        return domainRepository.findAll().stream()
                .map(d -> new ProjectDomainResponse(d.getId(), d.getName(), d.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDomainResponse> getDomainsByBranch(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new RuntimeException("Engineering branch not found with ID: " + branchId);
        }
        return domainRepository.findByBranches_Id(branchId)
                .stream()
                .map(d -> new ProjectDomainResponse(d.getId(), d.getName(), d.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(s -> new SkillResponse(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkillsByDomain(Long branchId, Long domainId) {
        // Validate that the domain belongs to the branch
        boolean domainBelongsToBranch = domainRepository
                .findByBranches_Id(branchId)
                .stream()
                .anyMatch(d -> d.getId().equals(domainId));

        if (!domainBelongsToBranch) {
            throw new RuntimeException(
                    "Domain with ID " + domainId + " does not belong to branch with ID " + branchId);
        }

        return skillRepository.findByDomains_Id(domainId)
                .stream()
                .map(s -> new SkillResponse(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void assertOwner(StudentProfile profile, String username) {
        if (!profile.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Access denied: you do not own this profile");
        }
    }

    private Skill resolveSkill(StudentSkillRequest skillReq) {
        if (skillReq.getSkillId() != null) {
            return skillRepository.findById(skillReq.getSkillId())
                    .orElseThrow(() -> new RuntimeException("Skill not found with ID: " + skillReq.getSkillId()));
        }

        if (skillReq.getSkillName() != null && !skillReq.getSkillName().isBlank()) {
            String normalizedName = skillReq.getSkillName().trim();
            return skillRepository.findByName(normalizedName).orElseGet(() -> {
                Skill newSkill = new Skill();
                newSkill.setName(normalizedName);
                return skillRepository.save(newSkill);
            });
        }

        throw new RuntimeException("Each skill entry must provide either skillId or skillName");
    }

    private StudentSkill buildStudentSkill(StudentProfile profile, Skill skill, StudentSkillRequest req) {
        ProficiencyLevel level;
        try {
            level = ProficiencyLevel.valueOf(req.getProficiencyLevel().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid proficiency level: " + req.getProficiencyLevel()
                    + ". Valid values: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT");
        }

        StudentSkill studentSkill = new StudentSkill();
        studentSkill.setStudentProfile(profile);
        studentSkill.setSkill(skill);
        studentSkill.setProficiencyLevel(level);
        studentSkill.setYearsOfExperience(req.getYearsOfExperience());
        return studentSkill;
    }

    private StudentProfileResponse mapToResponse(StudentProfile profile) {
        StudentProfileResponse response = new StudentProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setUsername(profile.getUser().getUsername());
        response.setBranchId(profile.getBranch().getId());
        response.setBranchName(profile.getBranch().getName());
        response.setDomainId(profile.getDomain().getId());
        response.setDomainName(profile.getDomain().getName());
        response.setTeamSize(profile.getTeamSize());
        response.setAvailableTimeWeeks(profile.getAvailableTimeWeeks());
        response.setPreviousExperience(profile.getPreviousExperience());
        response.setInterests(profile.getInterests());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        // Build full photo URL if a photo exists
        if (profile.getPhotoPath() != null) {
            response.setPhotoUrl(baseUrl + "/uploads/" + profile.getPhotoPath());
        }

        List<StudentSkillResponse> skillResponses = studentSkillRepository
                .findByStudentProfileId(profile.getId())
                .stream()
                .map(ss -> new StudentSkillResponse(
                        ss.getId(),
                        ss.getSkill().getId(),
                        ss.getSkill().getName(),
                        ss.getProficiencyLevel().name(),
                        ss.getYearsOfExperience()))
                .collect(Collectors.toList());

        response.setSkills(skillResponses);
        return response;
    }
}
