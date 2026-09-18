package com.projectguard.controller;

import com.projectguard.dto.student.*;
import com.projectguard.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // ─── Profile CRUD ─────────────────────────────────────────────────────────

    /** POST /api/students — create profile for the authenticated user */
    @PostMapping
    public ResponseEntity<StudentProfileResponse> createProfile(
            @Valid @RequestBody StudentProfileRequest request,
            Authentication authentication) {

        StudentProfileResponse response = studentService.createProfile(
                authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /api/students/me — get the authenticated user's own profile */
    @GetMapping("/me")
    public ResponseEntity<StudentProfileResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(
                studentService.getProfileByUsername(authentication.getName()));
    }

    /** GET /api/students/{id} — get a profile by ID */
    @GetMapping("/{id}")
    public ResponseEntity<StudentProfileResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getProfile(id));
    }

    /** PUT /api/students/{id} — update a profile (owner only) */
    @PutMapping("/{id}")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody StudentProfileRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                studentService.updateProfile(id, authentication.getName(), request));
    }

    // ─── Skills ───────────────────────────────────────────────────────────────

    /** POST /api/students/{id}/skills — add skills (skips duplicates) */
    @PostMapping("/{id}/skills")
    public ResponseEntity<StudentProfileResponse> addSkills(
            @PathVariable Long id,
            @Valid @RequestBody StudentSkillsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                studentService.addSkills(id, authentication.getName(), request));
    }

    /** PUT /api/students/{id}/skills — replace ALL skills */
    @PutMapping("/{id}/skills")
    public ResponseEntity<StudentProfileResponse> replaceSkills(
            @PathVariable Long id,
            @Valid @RequestBody StudentSkillsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                studentService.replaceSkills(id, authentication.getName(), request));
    }

    // ─── Photo ────────────────────────────────────────────────────────────────

    /** POST /api/students/{id}/photo — upload/replace profile photo (multipart) */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentProfileResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo,
            Authentication authentication) {

        return ResponseEntity.ok(
                studentService.uploadPhoto(id, authentication.getName(), photo));
    }

    /** DELETE /api/students/{id}/photo — remove profile photo */
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<StudentProfileResponse> deletePhoto(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                studentService.deletePhoto(id, authentication.getName()));
    }

    // ─── Metadata / Dropdowns ─────────────────────────────────────────────────

    /** GET /api/students/meta/branches — all engineering branches */
    @GetMapping("/meta/branches")
    public ResponseEntity<List<EngineeringBranchResponse>> getBranches() {
        return ResponseEntity.ok(studentService.getAllBranches());
    }

    /**
     * GET /api/students/meta/branches/{branchId}/domains
     * Returns only the domains linked to the selected branch.
     */
    @GetMapping("/meta/branches/{branchId}/domains")
    public ResponseEntity<List<ProjectDomainResponse>> getDomainsByBranch(
            @PathVariable Long branchId) {

        return ResponseEntity.ok(studentService.getDomainsByBranch(branchId));
    }

    /**
     * GET /api/students/meta/branches/{branchId}/domains/{domainId}/skills
     * Returns only the skills linked to the selected branch+domain combination.
     */
    @GetMapping("/meta/branches/{branchId}/domains/{domainId}/skills")
    public ResponseEntity<List<SkillResponse>> getSkillsByDomain(
            @PathVariable Long branchId,
            @PathVariable Long domainId) {

        return ResponseEntity.ok(studentService.getSkillsByDomain(branchId, domainId));
    }

    /** GET /api/students/meta/domains — all domains (fallback) */
    @GetMapping("/meta/domains")
    public ResponseEntity<List<ProjectDomainResponse>> getDomains() {
        return ResponseEntity.ok(studentService.getAllDomains());
    }

    /** GET /api/students/meta/skills — all skills (fallback) */
    @GetMapping("/meta/skills")
    public ResponseEntity<List<SkillResponse>> getSkills() {
        return ResponseEntity.ok(studentService.getAllSkills());
    }
}