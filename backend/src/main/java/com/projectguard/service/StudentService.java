package com.projectguard.service;

import com.projectguard.dto.student.*;
import com.projectguard.entity.*;
import com.projectguard.entity.enums.AuthProvider;
import com.projectguard.entity.enums.ProficiencyLevel;
import com.projectguard.exception.DuplicateResourceException;
import com.projectguard.exception.ResourceNotFoundException;
import com.projectguard.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    public static final List<String> BRANCH_ORDER = List.of(
            "Computer Science and Engineering",
            "Information Science and Engineering",
            "Electronics and Communication Engineering",
            "Electrical and Electronics Engineering",
            "Mechanical Engineering",
            "Civil Engineering",
            "Bachelor of Computer Applications (BCA)",
            "Master of Computer Applications (MCA)",
            "B.Sc Computer Science",
            "M.Sc Computer Science"
    );

    public static String getBranchDisplayName(String fullName) {
        if (fullName == null) return "";
        switch (fullName.trim()) {
            case "Computer Science and Engineering":
            case "CSE":
                return "CSE";
            case "Information Science and Engineering":
            case "ISE":
                return "ISE";
            case "Electronics and Communication Engineering":
            case "ECE":
                return "ECE";
            case "Electrical and Electronics Engineering":
            case "EEE":
                return "EEE";
            case "Mechanical Engineering":
            case "Mechanical":
                return "Mechanical";
            case "Civil Engineering":
            case "Civil":
                return "Civil";
            case "Bachelor of Computer Applications (BCA)":
            case "BCA":
                return "BCA";
            case "Master of Computer Applications (MCA)":
            case "MCA":
                return "MCA";
            case "B.Sc Computer Science":
                return "B.Sc Computer Science";
            case "M.Sc Computer Science":
                return "M.Sc Computer Science";
            default:
                return fullName;
        }
    }

    public static boolean isEngineeringBranch(String name) {
        if (name == null) return false;
        String lower = name.trim().toLowerCase();
        return lower.equals("cse") ||
                lower.equals("ise") ||
                lower.equals("ece") ||
                lower.equals("eee") ||
                lower.equals("mechanical") ||
                lower.equals("civil") ||
                lower.contains("engineering") ||
                lower.equals("computer science and engineering") ||
                lower.equals("information science and engineering") ||
                lower.equals("electronics and communication engineering") ||
                lower.equals("electrical and electronics engineering") ||
                lower.equals("mechanical engineering") ||
                lower.equals("civil engineering");
    }

    private final StudentProfileRepository studentProfileRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final SkillRepository skillRepository;
    private final EngineeringBranchRepository branchRepository;
    private final ProjectDomainRepository domainRepository;
    private final TechnologyRepository technologyRepository;
    private final UserRepository userRepository;
    private final ProfilePhotoService profilePhotoService;
    private final PasswordService passwordService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public StudentService(
            StudentProfileRepository studentProfileRepository,
            StudentSkillRepository studentSkillRepository,
            SkillRepository skillRepository,
            EngineeringBranchRepository branchRepository,
            ProjectDomainRepository domainRepository,
            TechnologyRepository technologyRepository,
            UserRepository userRepository,
            ProfilePhotoService profilePhotoService,
            PasswordService passwordService) {

        this.studentProfileRepository = studentProfileRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.skillRepository = skillRepository;
        this.branchRepository = branchRepository;
        this.domainRepository = domainRepository;
        this.technologyRepository = technologyRepository;
        this.userRepository = userRepository;
        this.profilePhotoService = profilePhotoService;
        this.passwordService = passwordService;
    }

    // ─── Default Profile Creation (e.g. for Google OAuth) ───────────────────

    public StudentProfile createDefaultProfileForUser(User user) {
        StudentProfile existing = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        List<EngineeringBranch> branches = branchRepository.findAll();
        EngineeringBranch branch = branches.stream()
                .filter(b -> "CSE".equalsIgnoreCase(getBranchDisplayName(b.getName())))
                .findFirst()
                .orElseGet(() -> branches.isEmpty() ? null : branches.get(0));

        if (branch == null) {
            throw new IllegalStateException("No engineering branches found in database");
        }

        List<ProjectDomain> domains = domainRepository.findByBranches_Id(branch.getId());
        ProjectDomain domain = domains.isEmpty() ? null : domains.get(0);
        if (domain == null) {
            List<ProjectDomain> allDomains = domainRepository.findAll();
            domain = allDomains.isEmpty() ? null : allDomains.get(0);
        }

        if (domain == null) {
            throw new IllegalStateException("No project domains found in database");
        }

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setBranch(branch);
        profile.setAcademicLevel(isEngineeringBranch(branch.getName()) ? "UG" : null);
        profile.setDomain(domain);
        profile.setTeamSize(2);
        profile.setAvailableTimeWeeks(8);
        profile.setPreviousExperience("");
        profile.setInterests("");

        List<Technology> techs = technologyRepository.findByDomainId(domain.getId());
        if (!techs.isEmpty()) {
            profile.setTechnology(techs.get(0));
        }

        return studentProfileRepository.save(profile);
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public StudentProfileResponse createProfile(String username, StudentProfileRequest request) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (studentProfileRepository.existsByUserId(user.getId())) {
            throw new DuplicateResourceException("Student profile already exists for this user");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
            userRepository.save(user);
        }

        EngineeringBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Engineering branch not found with ID: " + request.getBranchId()));

        boolean isEng = isEngineeringBranch(branch.getName());
        String academicLevel = null;
        if (isEng) {
            if (request.getAcademicLevel() == null || request.getAcademicLevel().trim().isEmpty()) {
                throw new IllegalArgumentException("Academic level (UG / PG) is required for engineering branches");
            }
            academicLevel = request.getAcademicLevel().trim().toUpperCase();
            if (!"UG".equals(academicLevel) && !"PG".equals(academicLevel)) {
                throw new IllegalArgumentException("Academic level must be UG or PG");
            }
        }

        ProjectDomain domain = domainRepository.findById(request.getDomainId())
                .orElseThrow(() -> new ResourceNotFoundException("Project domain not found with ID: " + request.getDomainId()));

        boolean domainBelongsToBranch = domainRepository.findByBranches_Id(branch.getId())
                .stream()
                .anyMatch(d -> d.getId().equals(domain.getId()));
        if (!domainBelongsToBranch) {
            throw new IllegalArgumentException("Selected domain does not belong to the selected branch/program");
        }

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setBranch(branch);
        profile.setAcademicLevel(academicLevel);
        profile.setDomain(domain);

        if (request.getTechnologyId() != null) {
            Technology tech = technologyRepository.findById(request.getTechnologyId()).orElse(null);
            if (tech != null && tech.getDomain().getId().equals(domain.getId())) {
                profile.setTechnology(tech);
            }
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found with ID: " + id));
        return mapToResponse(profile);
    }

    @Transactional
    public StudentProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    if (user.getAuthProvider() == AuthProvider.GOOGLE) {
                        return createDefaultProfileForUser(user);
                    }
                    throw new ResourceNotFoundException("No student profile found for user: " + username);
                });

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
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found with ID: " + id));

        assertOwner(profile, username);

        User user = profile.getUser();
        boolean userChanged = false;

        // Name
        if (request.getName() != null) {
            user.setName(request.getName().trim());
            userChanged = true;
        }

        // Username
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new DuplicateResourceException("Username already in use: " + newUsername);
                }
                user.setUsername(newUsername);
                userChanged = true;
            }
        }

        // Email
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new DuplicateResourceException("Email already in use: " + newEmail);
                }
                user.setEmail(newEmail);
                userChanged = true;
            }
        }

        // Password
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            String newPassword = request.getPassword().trim();
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            user.setPassword(passwordService.encode(newPassword));
            userChanged = true;
        }

        if (userChanged) {
            userRepository.save(user);
        }

        EngineeringBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Engineering branch not found with ID: " + request.getBranchId()));

        boolean isEng = isEngineeringBranch(branch.getName());
        String academicLevel = null;
        if (isEng) {
            if (request.getAcademicLevel() == null || request.getAcademicLevel().trim().isEmpty()) {
                throw new IllegalArgumentException("Academic level (UG / PG) is required for engineering branches");
            }
            academicLevel = request.getAcademicLevel().trim().toUpperCase();
            if (!"UG".equals(academicLevel) && !"PG".equals(academicLevel)) {
                throw new IllegalArgumentException("Academic level must be UG or PG");
            }
        }

        ProjectDomain domain = domainRepository.findById(request.getDomainId())
                .orElseThrow(() -> new ResourceNotFoundException("Project domain not found with ID: " + request.getDomainId()));

        boolean domainBelongsToBranch = domainRepository.findByBranches_Id(branch.getId())
                .stream()
                .anyMatch(d -> d.getId().equals(domain.getId()));
        if (!domainBelongsToBranch) {
            throw new IllegalArgumentException("Selected domain does not belong to the selected branch/program");
        }

        profile.setBranch(branch);
        profile.setAcademicLevel(academicLevel);
        profile.setDomain(domain);

        if (request.getTechnologyId() != null) {
            Technology tech = technologyRepository.findById(request.getTechnologyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Technology not found with ID: " + request.getTechnologyId()));
            if (!tech.getDomain().getId().equals(domain.getId())) {
                throw new IllegalArgumentException("Selected technology does not belong to the selected domain");
            }
            profile.setTechnology(tech);
        } else if (profile.getTechnology() != null && !profile.getTechnology().getDomain().getId().equals(domain.getId())) {
            profile.setTechnology(null);
        }

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
                .sorted(Comparator.comparingInt(b -> {
                    int idx = BRANCH_ORDER.indexOf(b.getName());
                    if (idx == -1) {
                        for (int i = 0; i < BRANCH_ORDER.size(); i++) {
                            if (getBranchDisplayName(BRANCH_ORDER.get(i)).equalsIgnoreCase(getBranchDisplayName(b.getName()))) {
                                return i;
                            }
                        }
                    }
                    return idx != -1 ? idx : Integer.MAX_VALUE;
                }))
                .map(b -> new EngineeringBranchResponse(
                        b.getId(),
                        b.getName(),
                        getBranchDisplayName(b.getName()),
                        b.getDescription(),
                        isEngineeringBranch(b.getName())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDomainResponse> getAllDomains() {
        return domainRepository.findAll().stream()
                .map(d -> new ProjectDomainResponse(d.getId(), d.getName(), d.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDomainResponse> getDomainsByBranch(Long branchId, String academicLevel) {
        EngineeringBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Engineering branch not found with ID: " + branchId));

        boolean isEng = isEngineeringBranch(branch.getName());
        if (isEng && academicLevel != null && !academicLevel.trim().isEmpty()) {
            String level = academicLevel.trim().toUpperCase();
            if (!"UG".equals(level) && !"PG".equals(level)) {
                throw new IllegalArgumentException("Invalid academic level: " + academicLevel + ". Must be UG or PG.");
            }
        }

        return domainRepository.findByBranches_Id(branchId)
                .stream()
                .map(d -> new ProjectDomainResponse(d.getId(), d.getName(), d.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDomainResponse> getDomainsByBranch(Long branchId) {
        return getDomainsByBranch(branchId, null);
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
            throw new IllegalArgumentException(
                    "Domain with ID " + domainId + " does not belong to branch with ID " + branchId);
        }

        return skillRepository.findByDomains_Id(domainId)
                .stream()
                .map(s -> new SkillResponse(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TechnologyResponse> getTechnologiesByDomain(Long domainId) {
        if (!domainRepository.existsById(domainId)) {
            throw new ResourceNotFoundException("Domain not found with ID: " + domainId);
        }
        return technologyRepository.findByDomainId(domainId).stream()
                .map(t -> new TechnologyResponse(t.getId(), t.getName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkillsByTechnology(Long technologyId) {
        Technology tech = technologyRepository.findById(technologyId)
                .orElseThrow(() -> new ResourceNotFoundException("Technology not found with ID: " + technologyId));
        return tech.getSkills().stream()
                .map(s -> new SkillResponse(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void assertOwner(StudentProfile profile, String username) {
        if (!profile.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Access denied: you do not own this profile");
        }
    }

    private Skill resolveSkill(StudentSkillRequest skillReq) {
        if (skillReq.getSkillId() != null) {
            return skillRepository.findById(skillReq.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + skillReq.getSkillId()));
        }

        if (skillReq.getSkillName() != null && !skillReq.getSkillName().isBlank()) {
            String normalizedName = skillReq.getSkillName().trim();
            return skillRepository.findByName(normalizedName).orElseGet(() -> {
                Skill newSkill = new Skill();
                newSkill.setName(normalizedName);
                return skillRepository.save(newSkill);
            });
        }

        throw new IllegalArgumentException("Each skill entry must provide either skillId or skillName");
    }

    private StudentSkill buildStudentSkill(StudentProfile profile, Skill skill, StudentSkillRequest req) {
        ProficiencyLevel level;
        try {
            level = ProficiencyLevel.valueOf(req.getProficiencyLevel().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid proficiency level: " + req.getProficiencyLevel()
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
        response.setName(profile.getUser().getName() != null ? profile.getUser().getName() : profile.getUser().getUsername());
        response.setUsername(profile.getUser().getUsername());
        response.setEmail(profile.getUser().getEmail());
        response.setBranchId(profile.getBranch().getId());
        response.setBranchName(profile.getBranch().getName());
        response.setAcademicLevel(profile.getAcademicLevel());
        response.setDomainId(profile.getDomain().getId());
        response.setDomainName(profile.getDomain().getName());
        if (profile.getTechnology() != null) {
            response.setTechnologyId(profile.getTechnology().getId());
            response.setTechnologyName(profile.getTechnology().getName());
        }
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
