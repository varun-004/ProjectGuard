package com.projectguard.service;

import com.projectguard.dto.mldataset.MlDatasetRecordDto;
import com.projectguard.dto.mlmodel.MlModelStatusDto;
import com.projectguard.dto.mlmodel.MlPredictionRequestDto;
import com.projectguard.dto.mlmodel.MlPredictionResponseDto;
import com.projectguard.dto.mlmodel.MlTrainingStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MlModelServiceTest {

    @Mock
    private MlServiceClient mlServiceClient;

    @Mock
    private MlDatasetService mlDatasetService;

    @InjectMocks
    private MlModelService mlModelService;

    private MlPredictionRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = new MlPredictionRequestDto();
        sampleRequest.setStudentSkillLevel(3.0);
        sampleRequest.setSkillGap(5.0);
        sampleRequest.setProjectComplexity("MEDIUM");
        sampleRequest.setTeamSize(2);
        sampleRequest.setAvailableHoursPerWeek(20.0);
        sampleRequest.setTechnologyFamiliarity(3.0);
        sampleRequest.setNumberOfRequirements(4);
        sampleRequest.setNumberOfFeatures(3);
        sampleRequest.setPreviousExperience(1.0);
        sampleRequest.setEstimatedEffort(120.0);
    }

    @Test
    void testGetModelStatus_MapsCorrectly() {
        when(mlServiceClient.getModelStatus()).thenReturn(Map.of(
                "model_available", true,
                "active_version", "v1",
                "algorithm", "GradientBoosting",
                "message", "Active model ready.",
                "all_versions", List.of("v1")
        ));

        MlModelStatusDto status = mlModelService.getModelStatus();

        assertTrue(status.isModelAvailable());
        assertEquals("v1", status.getActiveVersion());
        assertEquals("GradientBoosting", status.getAlgorithm());
        assertEquals(List.of("v1"), status.getAllVersions());
    }

    @Test
    void testTriggerTraining_ChecksDatabaseAndDelegatesToClient() {
        when(mlDatasetService.getTrainingReadyRecords()).thenReturn(Collections.emptyList());
        when(mlServiceClient.triggerModelTraining(anyList(), anyDouble(), any(), any())).thenReturn(Map.of(
                "status", "TRAINING_BLOCKED",
                "message", "Training blocked: Insufficient records: 0 available, 50 required.",
                "available_records", 0,
                "required_records", 50,
                "records_needed", 50
        ));

        MlTrainingStatusDto result = mlModelService.triggerTraining();

        verify(mlDatasetService, times(1)).prepareAndValidateDataset();
        verify(mlDatasetService, times(1)).getTrainingReadyRecords();
        verify(mlServiceClient, times(1)).triggerModelTraining(eq(Collections.emptyList()), eq(0.20), any(), any());

        assertEquals("TRAINING_BLOCKED", result.getStatus());
        assertEquals(0, result.getAvailableRecords());
        assertEquals(50, result.getRequiredRecords());
    }

    @Test
    void testPredict_MapsResponse() {
        when(mlServiceClient.predict(any(MlPredictionRequestDto.class))).thenReturn(Map.of(
                "status", "SUCCESS",
                "prediction", "FEASIBLE",
                "probability", 0.78,
                "model_version", "v1",
                "algorithm", "RandomForest",
                "risk_factors", List.of("Solo project"),
                "disclaimer", "Estimate only."
        ));

        MlPredictionResponseDto response = mlModelService.predict(sampleRequest);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("FEASIBLE", response.getPrediction());
        assertEquals(0.78, response.getProbability());
        assertEquals("v1", response.getModelVersion());
        assertEquals(List.of("Solo project"), response.getRiskFactors());
    }
}

