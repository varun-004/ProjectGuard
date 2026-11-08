package com.projectguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectguard.dto.mldataset.*;
import com.projectguard.entity.User;
import com.projectguard.entity.enums.Role;
import com.projectguard.repository.StudentProfileRepository;
import com.projectguard.repository.UserRepository;
import com.projectguard.security.CustomUserDetails;
import com.projectguard.service.MlDatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MlDatasetControllerTest {

    @Mock
    private MlDatasetService mlDatasetService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private MlDatasetController controller;

    private User adminUser;
    private User studentUser;
    private Authentication adminAuth;
    private Authentication studentAuth;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);

        studentUser = new User();
        studentUser.setId(2L);
        studentUser.setUsername("student1");
        studentUser.setEmail("student1@test.com");
        studentUser.setRole(Role.STUDENT);

        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        adminAuth = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());

        CustomUserDetails studentDetails = new CustomUserDetails(studentUser);
        studentAuth = new UsernamePasswordAuthenticationToken(studentDetails, null, studentDetails.getAuthorities());
    }

    @Test
    void testGetStatistics_ReturnsStats() {
        MlDatasetStatisticsDto stats = new MlDatasetStatisticsDto();
        stats.setTotalRecords(10);
        stats.setTrainingReadyRecords(5);
        when(mlDatasetService.getStatistics()).thenReturn(stats);

        ResponseEntity<MlDatasetStatisticsDto> response = controller.getStatistics();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getTotalRecords());
        assertEquals(5, response.getBody().getTrainingReadyRecords());
    }

    @Test
    void testGetPendingOutcomes_ReturnsPage() {
        MlDatasetRecordDto dto = new MlDatasetRecordDto();
        dto.setId(101L);
        dto.setOutcomeStatus("PENDING_OUTCOME");
        when(mlDatasetService.getPendingOutcomes(any())).thenReturn(new PageImpl<>(List.of(dto)));

        ResponseEntity<Page<MlDatasetRecordDto>> response = controller.getPendingOutcomes(0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("PENDING_OUTCOME", response.getBody().getContent().get(0).getOutcomeStatus());
    }

    @Test
    void testRecordActualOutcome_AdminSuccess() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(adminUser));

        MlOutcomeUpdateRequest req = new MlOutcomeUpdateRequest();
        req.setActualOutcome("FEASIBLE");
        req.setLabelEvidence("Complete demonstration verified");

        MlDatasetRecordDto updated = new MlDatasetRecordDto();
        updated.setId(55L);
        updated.setActualOutcome("FEASIBLE");
        updated.setOutcomeStatus("FEASIBLE");

        when(mlDatasetService.recordActualOutcome(eq(55L), any(MlOutcomeUpdateRequest.class)))
                .thenReturn(updated);

        ResponseEntity<MlDatasetRecordDto> response = controller.recordActualOutcome(55L, req, adminAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("FEASIBLE", response.getBody().getActualOutcome());
    }

    @Test
    void testRecordActualOutcome_StudentDenied() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(studentUser));

        MlOutcomeUpdateRequest req = new MlOutcomeUpdateRequest();
        req.setActualOutcome("FEASIBLE");

        assertThrows(RuntimeException.class, () -> controller.recordActualOutcome(55L, req, studentAuth));
    }

    @Test
    void testPrepareDataset_AdminSuccess() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(adminUser));
        MlDatasetPrepareSummaryDto summary = new MlDatasetPrepareSummaryDto();
        summary.setTotalDatabaseRecords(12);
        summary.setEligibleRecords(8);
        when(mlDatasetService.prepareAndValidateDataset()).thenReturn(summary);

        ResponseEntity<MlDatasetPrepareSummaryDto> response = controller.prepareDataset(adminAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(12, response.getBody().getTotalDatabaseRecords());
        assertEquals(8, response.getBody().getEligibleRecords());
    }

    @Test
    void testGetSplitStatistics_AdminSuccess() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(adminUser));
        Map<String, Object> map = Map.of("isReadyForTraining", false, "totalTrainingReadyRecords", 0);
        when(mlDatasetService.getSplitReport()).thenReturn(map);

        ResponseEntity<Map<String, Object>> response = controller.getSplitStatistics(adminAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().get("isReadyForTraining"));
    }

    @Test
    void testRegisterProject_Success() {
        MlProjectRegistrationRequest req = new MlProjectRegistrationRequest();
        req.setProjectId(500L);
        req.setAvailableHoursPerWeek(20.0);

        MlDatasetRecordDto dto = new MlDatasetRecordDto();
        dto.setId(99L);
        dto.setProjectId(500L);
        dto.setAvailableHoursPerWeek(20.0);
        when(mlDatasetService.registerStudentProject(anyString(), any())).thenReturn(dto);

        ResponseEntity<MlDatasetRecordDto> response = controller.registerProject(req, studentAuth);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(99L, response.getBody().getId());
    }
}

