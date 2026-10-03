package com.projectguard.service;

import com.projectguard.dto.student.EngineeringBranchResponse;
import com.projectguard.dto.student.ProjectDomainResponse;
import com.projectguard.dto.student.StudentProfileRequest;
import com.projectguard.dto.student.StudentProfileResponse;
import com.projectguard.entity.EngineeringBranch;
import com.projectguard.entity.ProjectDomain;
import com.projectguard.entity.StudentProfile;
import com.projectguard.entity.User;
import com.projectguard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private StudentSkillRepository studentSkillRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private EngineeringBranchRepository branchRepository;

    @Mock
    private ProjectDomainRepository domainRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfilePhotoService profilePhotoService;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private StudentService studentService;

    private User testUser;
    private EngineeringBranch cseBranch;
    private EngineeringBranch bcaProgram;
    private ProjectDomain webDevDomain;
    private ProjectDomain civilDomain;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("teststudent");

        cseBranch = new EngineeringBranch();
        cseBranch.setId(1L);
        cseBranch.setName("Computer Science and Engineering");

        bcaProgram = new EngineeringBranch();
        bcaProgram.setId(7L);
        bcaProgram.setName("Bachelor of Computer Applications (BCA)");

        webDevDomain = new ProjectDomain();
        webDevDomain.setId(10L);
        webDevDomain.setName("Web Development");

        civilDomain = new ProjectDomain();
        civilDomain.setId(20L);
        civilDomain.setName("Structural Engineering");
    }

    @Test
    void testGetAllBranches_Returns10BranchesInSpecifiedOrder() {
        List<EngineeringBranch> rawList = new ArrayList<>();

        String[] branchNames = {
                "Master of Computer Applications (MCA)",
                "Computer Science and Engineering",
                "Civil Engineering",
                "Bachelor of Computer Applications (BCA)",
                "Information Science and Engineering",
                "M.Sc Computer Science",
                "Electronics and Communication Engineering",
                "Electrical and Electronics Engineering",
                "Mechanical Engineering",
                "B.Sc Computer Science"
        };

        long idCounter = 1;
        for (String name : branchNames) {
            EngineeringBranch b = new EngineeringBranch();
            b.setId(idCounter++);
            b.setName(name);
            rawList.add(b);
        }

        when(branchRepository.findAll()).thenReturn(rawList);

        List<EngineeringBranchResponse> result = studentService.getAllBranches();

        assertEquals(10, result.size());
        assertEquals("Computer Science and Engineering", result.get(0).getName());
        assertTrue(result.get(0).getIsEngineering());

        assertEquals("Information Science and Engineering", result.get(1).getName());
        assertTrue(result.get(1).getIsEngineering());

        assertEquals("Electronics and Communication Engineering", result.get(2).getName());
        assertTrue(result.get(2).getIsEngineering());

        assertEquals("Electrical and Electronics Engineering", result.get(3).getName());
        assertTrue(result.get(3).getIsEngineering());

        assertEquals("Mechanical Engineering", result.get(4).getName());
        assertTrue(result.get(4).getIsEngineering());

        assertEquals("Civil Engineering", result.get(5).getName());
        assertTrue(result.get(5).getIsEngineering());

        assertEquals("Bachelor of Computer Applications (BCA)", result.get(6).getName());
        assertFalse(result.get(6).getIsEngineering());

        assertEquals("Master of Computer Applications (MCA)", result.get(7).getName());
        assertFalse(result.get(7).getIsEngineering());

        assertEquals("B.Sc Computer Science", result.get(8).getName());
        assertFalse(result.get(8).getIsEngineering());

        assertEquals("M.Sc Computer Science", result.get(9).getName());
        assertFalse(result.get(9).getIsEngineering());
    }

    @Test
    void testGetDomainsByBranch_EngineeringWithValidUG_ReturnsDomains() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(cseBranch));
        when(domainRepository.findByBranches_Id(1L)).thenReturn(List.of(webDevDomain));

        List<ProjectDomainResponse> domains = studentService.getDomainsByBranch(1L, "UG");

        assertNotNull(domains);
        assertEquals(1, domains.size());
        assertEquals("Web Development", domains.get(0).getName());
    }

    @Test
    void testGetDomainsByBranch_EngineeringWithValidPG_ReturnsDomains() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(cseBranch));
        when(domainRepository.findByBranches_Id(1L)).thenReturn(List.of(webDevDomain));

        List<ProjectDomainResponse> domains = studentService.getDomainsByBranch(1L, "PG");

        assertNotNull(domains);
        assertEquals(1, domains.size());
        assertEquals("Web Development", domains.get(0).getName());
    }

    @Test
    void testGetDomainsByBranch_EngineeringWithInvalidAcademicLevel_ThrowsException() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(cseBranch));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            studentService.getDomainsByBranch(1L, "PHD");
        });
        assertTrue(ex.getMessage().contains("Invalid academic level"));
    }

    @Test
    void testGetDomainsByBranch_NonEngineeringProgram_ReturnsDomainsWithoutLevel() {
        when(branchRepository.findById(7L)).thenReturn(Optional.of(bcaProgram));
        when(domainRepository.findByBranches_Id(7L)).thenReturn(List.of(webDevDomain));

        List<ProjectDomainResponse> domains = studentService.getDomainsByBranch(7L, null);

        assertNotNull(domains);
        assertEquals(1, domains.size());
        assertEquals("Web Development", domains.get(0).getName());
    }

    @Test
    void testCreateProfile_EngineeringWithoutAcademicLevel_ThrowsException() {
        when(userRepository.findByUsername("teststudent")).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(cseBranch));

        StudentProfileRequest request = new StudentProfileRequest();
        request.setBranchId(1L);
        request.setAcademicLevel(null);
        request.setDomainId(10L);
        request.setTeamSize(2);
        request.setAvailableTimeWeeks(8);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            studentService.createProfile("teststudent", request);
        });
        assertTrue(ex.getMessage().contains("Academic level (UG / PG) is required"));
    }

    @Test
    void testCreateProfile_EngineeringWithInvalidAcademicLevel_ThrowsException() {
        when(userRepository.findByUsername("teststudent")).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(cseBranch));

        StudentProfileRequest request = new StudentProfileRequest();
        request.setBranchId(1L);
        request.setAcademicLevel("DIPLOMA");
        request.setDomainId(10L);
        request.setTeamSize(2);
        request.setAvailableTimeWeeks(8);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            studentService.createProfile("teststudent", request);
        });
        assertTrue(ex.getMessage().contains("Academic level must be UG or PG"));
    }

    @Test
    void testCreateProfile_EngineeringWithValidAcademicLevel_Success() {
        when(userRepository.findByUsername("teststudent")).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(cseBranch));
        when(domainRepository.findById(10L)).thenReturn(Optional.of(webDevDomain));
        when(domainRepository.findByBranches_Id(1L)).thenReturn(List.of(webDevDomain));

        StudentProfileRequest request = new StudentProfileRequest();
        request.setBranchId(1L);
        request.setAcademicLevel("ug");
        request.setDomainId(10L);
        request.setTeamSize(4);
        request.setAvailableTimeWeeks(12);

        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile sp = invocation.getArgument(0);
            sp.setId(100L);
            return sp;
        });

        StudentProfileResponse response = studentService.createProfile("teststudent", request);

        assertNotNull(response);
        assertEquals(1L, response.getBranchId());
        assertEquals("UG", response.getAcademicLevel());
        assertEquals(10L, response.getDomainId());
    }

    @Test
    void testCreateProfile_NonEngineeringProgram_IgnoresAcademicLevel() {
        when(userRepository.findByUsername("teststudent")).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(branchRepository.findById(7L)).thenReturn(Optional.of(bcaProgram));
        when(domainRepository.findById(10L)).thenReturn(Optional.of(webDevDomain));
        when(domainRepository.findByBranches_Id(7L)).thenReturn(List.of(webDevDomain));

        StudentProfileRequest request = new StudentProfileRequest();
        request.setBranchId(7L);
        request.setAcademicLevel("UG"); // should be ignored/nulled for non-engineering program
        request.setDomainId(10L);
        request.setTeamSize(3);
        request.setAvailableTimeWeeks(10);

        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile sp = invocation.getArgument(0);
            sp.setId(101L);
            return sp;
        });

        StudentProfileResponse response = studentService.createProfile("teststudent", request);

        assertNotNull(response);
        assertEquals(7L, response.getBranchId());
        assertNull(response.getAcademicLevel());
        assertEquals(10L, response.getDomainId());
    }

    @Test
    void testCreateProfile_InvalidDomainSelection_ThrowsException() {
        when(userRepository.findByUsername("teststudent")).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(cseBranch));
        when(domainRepository.findById(20L)).thenReturn(Optional.of(civilDomain));
        // Civil domain is not in CSE branch domains
        when(domainRepository.findByBranches_Id(1L)).thenReturn(List.of(webDevDomain));

        StudentProfileRequest request = new StudentProfileRequest();
        request.setBranchId(1L);
        request.setAcademicLevel("UG");
        request.setDomainId(20L); // invalid domain for CSE
        request.setTeamSize(2);
        request.setAvailableTimeWeeks(8);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            studentService.createProfile("teststudent", request);
        });
        assertTrue(ex.getMessage().contains("Selected domain does not belong to the selected branch/program"));
    }
}
