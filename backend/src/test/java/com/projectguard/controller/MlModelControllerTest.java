package com.projectguard.controller;

import com.projectguard.dto.mlmodel.MlModelStatusDto;
import com.projectguard.dto.mlmodel.MlPredictionRequestDto;
import com.projectguard.dto.mlmodel.MlPredictionResponseDto;
import com.projectguard.dto.mlmodel.MlTrainingStatusDto;
import com.projectguard.service.MlModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MlModelControllerTest {

    @Mock
    private MlModelService mlModelService;

    @InjectMocks
    private MlModelController controller;

    private MlPredictionRequestDto samplePredictionRequest;

    @BeforeEach
    void setUp() {
        samplePredictionRequest = new MlPredictionRequestDto();
        samplePredictionRequest.setStudentSkillLevel(3.2);
        samplePredictionRequest.setSkillGap(4.5);
        samplePredictionRequest.setProjectComplexity("MEDIUM");
        samplePredictionRequest.setTeamSize(2);
        samplePredictionRequest.setAvailableHoursPerWeek(20.0);
        samplePredictionRequest.setTechnologyFamiliarity(3.0);
        samplePredictionRequest.setNumberOfRequirements(5);
        samplePredictionRequest.setNumberOfFeatures(4);
        samplePredictionRequest.setPreviousExperience(1.0);
        samplePredictionRequest.setEstimatedEffort(150.0);
    }

    @Test
    void testGetModelStatus_ModelUnavailable() {
        MlModelStatusDto statusDto = new MlModelStatusDto();
        statusDto.setModelAvailable(false);
        statusDto.setMessage("No trained model is available.");
        when(mlModelService.getModelStatus()).thenReturn(statusDto);

        ResponseEntity<MlModelStatusDto> response = controller.getModelStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isModelAvailable());
        assertEquals("No trained model is available.", response.getBody().getMessage());
        verify(mlModelService, times(1)).getModelStatus();
    }

    @Test
    void testGetModelStatus_ModelAvailable() {
        MlModelStatusDto statusDto = new MlModelStatusDto();
        statusDto.setModelAvailable(true);
        statusDto.setActiveVersion("v1");
        statusDto.setAlgorithm("RandomForest");
        statusDto.setMetrics(Map.of("accuracy", 0.85, "f1_weighted", 0.84));
        when(mlModelService.getModelStatus()).thenReturn(statusDto);

        ResponseEntity<MlModelStatusDto> response = controller.getModelStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isModelAvailable());
        assertEquals("v1", response.getBody().getActiveVersion());
        assertEquals("RandomForest", response.getBody().getAlgorithm());
    }

    @Test
    void testPredict_Successful() {
        MlPredictionResponseDto predictionResponse = new MlPredictionResponseDto();
        predictionResponse.setStatus("SUCCESS");
        predictionResponse.setPrediction("FEASIBLE");
        predictionResponse.setProbability(0.82);
        predictionResponse.setModelVersion("v1");
        predictionResponse.setAlgorithm("RandomForest");
        predictionResponse.setRiskFactors(List.of());
        predictionResponse.setDisclaimer("Estimate only.");

        when(mlModelService.predict(any(MlPredictionRequestDto.class))).thenReturn(predictionResponse);

        ResponseEntity<MlPredictionResponseDto> response = controller.predict(samplePredictionRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FEASIBLE", response.getBody().getPrediction());
        assertEquals(0.82, response.getBody().getProbability());
        assertEquals("v1", response.getBody().getModelVersion());
    }

    @Test
    void testPredict_NoModelActive_Returns404() {
        MlPredictionResponseDto noModelResponse = new MlPredictionResponseDto();
        noModelResponse.setStatus("NO_MODEL");
        noModelResponse.setMessage("No trained model is currently active.");

        when(mlModelService.predict(any(MlPredictionRequestDto.class))).thenReturn(noModelResponse);

        ResponseEntity<MlPredictionResponseDto> response = controller.predict(samplePredictionRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NO_MODEL", response.getBody().getStatus());
    }

    @Test
    void testTrainModel_ReturnsStatus_WhenBlockedByGate() {
        MlTrainingStatusDto trainingStatus = new MlTrainingStatusDto();
        trainingStatus.setStatus("TRAINING_BLOCKED");
        trainingStatus.setAvailableRecords(0);
        trainingStatus.setRequiredRecords(50);
        trainingStatus.setRecordsNeeded(50);
        trainingStatus.setMessage("Training blocked: Insufficient records: 0 available, 50 required.");

        when(mlModelService.triggerTraining()).thenReturn(trainingStatus);

        ResponseEntity<MlTrainingStatusDto> response = controller.trainModel();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TRAINING_BLOCKED", response.getBody().getStatus());
        assertEquals(0, response.getBody().getAvailableRecords());
        assertEquals(50, response.getBody().getRequiredRecords());
    }

    @Test
    void testGetMetrics() {
        when(mlModelService.getModelMetrics("v1")).thenReturn(Map.of("accuracy", 0.9, "algorithm", "RandomForest"));

        ResponseEntity<Map<String, Object>> response = controller.getModelMetrics("v1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RandomForest", response.getBody().get("algorithm"));
    }

    @Test
    void testListVersions() {
        when(mlModelService.listVersions()).thenReturn(Map.of("total_versions", 2, "active_version", "v2"));

        ResponseEntity<Map<String, Object>> response = controller.listVersions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().get("total_versions"));
    }

    @Test
    void testActivateVersion() {
        when(mlModelService.activateVersion("v1")).thenReturn(Map.of("status", "SUCCESS", "active_version", "v1"));

        ResponseEntity<Map<String, Object>> response = controller.activateVersion("v1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("v1", response.getBody().get("active_version"));
    }

    @Test
    void testPredictionRequestDto_JacksonSnakeCaseAndCamelCaseDeserialization() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 1. Test snake_case JSON from browser
        String snakeCaseJson = """
            {
                "student_skill_level": 1.38,
                "skill_gap": 10.67,
                "project_complexity": "MEDIUM",
                "team_size": 2,
                "available_hours_per_week": 15.0,
                "technology_familiarity": 1.45,
                "number_of_requirements": 5,
                "number_of_features": 5,
                "previous_experience": 0.0,
                "estimated_effort": 90.0
            }
            """;
        MlPredictionRequestDto fromSnake = mapper.readValue(snakeCaseJson, MlPredictionRequestDto.class);
        assertEquals(1.38, fromSnake.getStudentSkillLevel());
        assertEquals(10.67, fromSnake.getSkillGap());
        assertEquals("MEDIUM", fromSnake.getProjectComplexity());
        assertEquals(2, fromSnake.getTeamSize());
        assertEquals(15.0, fromSnake.getAvailableHoursPerWeek());
        assertEquals(1.45, fromSnake.getTechnologyFamiliarity());
        assertEquals(5, fromSnake.getNumberOfRequirements());
        assertEquals(5, fromSnake.getNumberOfFeatures());
        assertEquals(0.0, fromSnake.getPreviousExperience());
        assertEquals(90.0, fromSnake.getEstimatedEffort());

        // 2. Test camelCase JSON
        String camelCaseJson = """
            {
                "studentSkillLevel": 2.5,
                "skillGap": 8.0,
                "projectComplexity": "EASY",
                "teamSize": 3,
                "availableHoursPerWeek": 20.0,
                "technologyFamiliarity": 3.0,
                "numberOfRequirements": 4,
                "numberOfFeatures": 4,
                "previousExperience": 1.5,
                "estimatedEffort": 120.0
            }
            """;
        MlPredictionRequestDto fromCamel = mapper.readValue(camelCaseJson, MlPredictionRequestDto.class);
        assertEquals(2.5, fromCamel.getStudentSkillLevel());
        assertEquals(8.0, fromCamel.getSkillGap());
        assertEquals("EASY", fromCamel.getProjectComplexity());

        // 3. Test serialization outputs snake_case for Python ML Service
        String serialized = mapper.writeValueAsString(fromSnake);
        assertTrue(serialized.contains("\"student_skill_level\":1.38"));
        assertTrue(serialized.contains("\"skill_gap\":10.67"));
        assertTrue(serialized.contains("\"project_complexity\":\"MEDIUM\""));
    }
}

