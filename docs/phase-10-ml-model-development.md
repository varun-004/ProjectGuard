# ProjectGuard — Phase 10: ML Model Development, Training, Evaluation & Prediction

## 1. Executive Summary

Phase 10 builds upon the Phase 9 ML dataset collection and preparation pipeline by implementing a complete, production-grade supervised machine learning system for predicting student project feasibility.

The prediction target is binary:
- **`FEASIBLE`** (encoded as `1`): The student-project combination is estimated to be viable given the student's background, available hours, and project scope.
- **`NOT_FEASIBLE`** (encoded as `0`): The project configuration presents substantial risk factors that exceed typical delivery feasibility.

Two candidate algorithms are trained, evaluated, and compared on held-out test data:
1. **Model A: Random Forest Classifier** (bagging ensemble with balanced class weights)
2. **Model B: Gradient Boosting Classifier** (sequential boosting ensemble with stochastic subsampling)

Model selection is strictly guided by the **weighted F1-score** rather than accuracy alone, ensuring resilience to class imbalance.

---

## 2. Strict Architectural Invariants & Guardrails Preserved

As mandated by ProjectGuard design principles:
1. **Preserved Phase 7 Skill-Gap Calculation**: The existing `0.0 - 25.0` skill-gap scale and formula remain completely unchanged.
2. **Preserved Recommendation Scoring & Ranking**: Recommendation algorithms and 4-factor inputs are untouched.
3. **Preserved Gemini API Integration**: Project generation and quality gate workflows remain untouched.
4. **Preserved Authentication & RBAC**: JWT and Google OAuth flows remain unchanged.
5. **No Data Fabrication**: No synthetic training records or Swagger test records are inserted into the database.
6. **No False Training Claims**: With zero verified records in production MySQL, the training pipeline truthfully reports `TRAINING_BLOCKED` rather than claiming a model exists.
7. **Readiness Gate Protection**: Minimum 50 authentic verified records with both target classes present are required before training executes.

---

## 3. ML Feature Schema (10 Input Features + 1 Target)

Each record represents one unique student-project combination:

| No. | Feature Name | Data Type | Valid Range | Encoding / Handling |
|---|---|---|---|---|
| 1 | `student_skill_level` | Float | `[1.0, 4.0]` | Average proficiency across student skills |
| 2 | `skill_gap` | Float | `[0.0, 25.0]` | Existing Phase 7 skill-gap score |
| 3 | `project_complexity` | Categorical | `EASY`, `MEDIUM`, `HARD` | Mapped to ordinal integer: `0`, `1`, `2` |
| 4 | `team_size` | Integer | `≥ 1` | Raw team headcount |
| 5 | `available_hours_per_week` | Float | `[1.0, 80.0]` | Dedicated student hours per week |
| 6 | `technology_familiarity` | Float | `[1.0, 4.0]` | Average familiarity score for required tech |
| 7 | `number_of_requirements` | Integer | `≥ 0` | Total functional and non-functional requirements |
| 8 | `number_of_features` | Integer | `≥ 0` | Total discrete project deliverables |
| 9 | `previous_experience` | Float | `≥ 0.0` | Relevant prior experience in years |
| 10 | `estimated_effort` | Float | `≥ 1.0` | Estimated project duration in hours |
| **T** | **`actual_outcome`** | **Binary Target** | `FEASIBLE`, `NOT_FEASIBLE` | Target label: `FEASIBLE=1`, `NOT_FEASIBLE=0` |

---

## 4. Dataset Readiness Gate

Before fitting any estimator, `check_dataset_readiness()` checks:
- **Minimum record count**: At least **50 authentic verified records** must be present.
- **Both target classes represented**: At least **5 verified records** for each class (`FEASIBLE` and `NOT_FEASIBLE`).
- **Data integrity**: Incomplete, rejected, or `PENDING_OUTCOME` records are excluded.

### Readiness Response When Blocked
When the gate fails, the pipeline returns HTTP 200 with:
```json
{
  "status": "TRAINING_BLOCKED",
  "message": "Training blocked: Insufficient records: 0 available, 50 required.",
  "available_records": 0,
  "required_records": 50,
  "records_needed": 50,
  "class_distribution": {},
  "metrics": null
}
```

---

## 5. Candidate Models & Training Methodology

### Model A: Random Forest Classifier
- **Implementation**: `sklearn.ensemble.RandomForestClassifier`
- **Hyperparameters**:
  - `n_estimators`: 100
  - `class_weight`: `"balanced"` (inversely proportional to class frequencies)
  - `random_state`: 42 (reproducible seed)
  - `n_jobs`: 1 (safe single-worker execution on Windows)
- **Strengths**: Robust against outliers, low hyperparameter sensitivity, built-in feature importance.

### Model B: Gradient Boosting Classifier
- **Implementation**: `sklearn.ensemble.GradientBoostingClassifier`
- **Hyperparameters**:
  - `n_estimators`: 100
  - `learning_rate`: 0.1
  - `max_depth`: 3
  - `subsample`: 0.8 (stochastic gradient boosting to reduce variance on small datasets)
  - `random_state`: 42
- **Strengths**: High predictive capacity on structured tabular data, models non-linear interactions.

### Splitting & Validation Methodology
- Held-out test set size: **20%** (`test_size=0.20`), stratified by target class `y`.
- Seed: **42** for full reproducibility.
- Neither candidate model touches the test set during fitting.

---

## 6. Evaluation Framework & Model Selection

Both models are evaluated on the identical held-out test split using:
- **Accuracy**: Overall correct prediction fraction.
- **Weighted Precision**: $\sum w_i \cdot \frac{TP_i}{TP_i + FP_i}$
- **Weighted Recall**: $\sum w_i \cdot \frac{TP_i}{TP_i + FN_i}$
- **Weighted F1-Score**: Harmonic mean of precision and recall.
- **Confusion Matrix**: $[[TN, FP], [FN, TP]]$.
- **ROC-AUC**: Receiver operating characteristic area under curve.
- **Feature Importance**: Gini / impurity decrease attribution per feature.

### Decision Rule
The best model is selected based on **weighted F1-score**. In case of a tie, ROC-AUC is used as the tie-breaker, followed by accuracy. **Accuracy alone is never used to pick the winner.**

---

## 7. Model Persistence, Registry & Versioning

Artifacts are managed by `ModelRegistry` in `ml-service/models/`:
```text
ml-service/models/
  v1/
    model.joblib     # Serialized scikit-learn estimator
    metadata.json    # Algorithm, evaluation metrics, hyperparameters, timestamp
  v2/
    model.joblib
    metadata.json
  active_version.txt # Pointer to current active version (e.g. "v1")
```

- Each training run writes to a new version sub-directory (`v1`, `v2`, ...).
- Metadata stores the algorithm name, metrics, feature order, and train/test size.
- Administrators can activate and roll back versions dynamically via API.

---

## 8. Spring Boot Integration Architecture

The Spring Boot backend acts as the authoritative gate between the user interface and the Python ML service:

```
[ Frontend: React UI ]
        │
        ▼ (JWT Auth)
[ Spring Boot Backend: /api/ml-model/* ]
        │ (MlServiceClient via HTTP)
        ▼
[ Python ML Service: FastAPI on port 8000 ]
        │
        ├── model_trainer.py
        ├── model_registry.py
        └── model_predictor.py
```

### Endpoints Implemented

| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/ml-model/status` | GET | STUDENT, ADMIN | Checks if a model is deployed, active version, algorithm, metrics |
| `/api/ml-model/predict` | POST | STUDENT, ADMIN | Evaluates 10 input features; returns outcome, probability, and risks |
| `/api/ml-model/train` | POST | ADMIN | Triggers authoritative training pipeline with MySQL records |
| `/api/ml-model/metrics` | GET | ADMIN | Returns evaluation metrics and feature importance |
| `/api/ml-model/versions` | GET | ADMIN | Lists all saved versions in registry |
| `/api/ml-model/activate/{v}`| POST | ADMIN | Activates a specific saved model version |

### Prediction Response Example
```json
{
  "status": "SUCCESS",
  "prediction": "FEASIBLE",
  "probability": 0.825,
  "modelVersion": "v1",
  "algorithm": "RandomForest",
  "featureSummary": {
    "student_skill_level": 3.2,
    "skill_gap": 4.5,
    "project_complexity": "MEDIUM",
    "team_size": 2,
    "available_hours_per_week": 20.0,
    "technology_familiarity": 3.0,
    "number_of_requirements": 5,
    "number_of_features": 4,
    "previous_experience": 1.0,
    "estimated_effort": 150.0
  },
  "riskFactors": [],
  "disclaimer": "This prediction is an estimate based on historical patterns, not a guarantee of project success. Use it as one input among many."
}
```

---

## 9. Frontend User Interfaces

1. **Admin ML Model Management (`/admin/ml-model`)**:
   - Active model status badge and KPIs.
   - Dataset readiness gate status with available vs required records display.
   - "Run Model Training" action button.
   - Side-by-side comparison table (RF vs GBM metrics).
   - Interactive feature importance bar chart.
   - Visual confusion matrix grid ($2 \times 2$).
   - Version history table with one-click "Activate" controls.

2. **Project Feasibility Predictor (`/predict`)**:
   - Accessible to both students and administrators.
   - 10 feature input fields (with profile auto-fill for students).
   - Real-time prediction display with outcome badge (`FEASIBLE` / `NOT_FEASIBLE`).
   - Confidence probability bar.
   - Interpretable risk signals list.
   - Clear pending status banner when no production model is deployed.

---

## 10. Verification & Test Results

All layers pass automated test suites:
- **Python ML Service**: `20 passed, 0 failed` (`pytest`)
  - 13 Phase 9 dataset operations tests (`test_dataset.py`)
  - 7 Phase 10 model training, comparison, registry, and prediction tests (`test_model.py`)
- **Spring Boot Backend**: `79 passed, 0 failures, 0 errors` (`mvn test`)
  - 9 `MlModelControllerTest` tests
  - 3 `MlModelServiceTest` tests
  - 8 `MlDatasetControllerTest` tests
  - 13 `MlDatasetServiceTest` tests
  - 14 `AiProjectGeneratorServiceTest` tests
  - 10 `Phase6RecommendationEndToEndTest` tests
  - 10 `StudentServiceTest` tests
  - 5 `PersonalizedProjectRecommendationTest` tests
  - 4 `RecommendationEngineServiceTest` tests
  - 3 `ProjectQualityGateServiceTest` tests
- **Frontend**: Clean production build (`npx vite build` succeeded with 117 modules transformed, 0 errors)

---

## 11. Root Cause Analysis & Resolution (HTTP 422 & NO_MODEL Handling)

### Issue 1: HTTP 422 Unprocessable Entity
- **Root Cause**:
  1. The frontend `ProjectFeasibilityPredictor.jsx` was transmitting dual-key JSON payloads containing both `snake_case` and `camelCase` keys in the same object as an earlier ad-hoc workaround.
  2. Spring Boot's `MlPredictionRequestDto` initially lacked explicit Jackson serialization mapping, causing inconsistencies when forwarding serialized JSON from Spring Boot to Python FastAPI.
- **Resolution**:
  1. Configured `@JsonProperty("field_name")` and `@JsonAlias({"fieldName", "field_name"})` on all 10 fields in `MlPredictionRequestDto.java`, ensuring seamless bidirectional deserialization from browser and normalized `snake_case` serialization towards Python.
  2. In `ProjectFeasibilityPredictor.jsx`, removed the redundant dual-key block and established a clean, single-format `snake_case` request payload.
  3. FastAPI `PredictionRequest` Pydantic model uses `AliasChoices` and validation constraints matching Phase 9 ranges.

### Issue 2: Frontend Prediction State Synchronization
- **Root Cause**:
  1. UI had fallback rendering (`|| 'v1'`, `|| 'ML Classifier'`) and evaluated `prediction !== 'FEASIBLE'` before verifying whether a prediction actually succeeded. When the API returned `NO_MODEL`, the UI mistakenly displayed a `NOT_FEASIBLE` badge alongside a warning banner.
- **Resolution**:
  1. Replaced fallback evaluations with strict `status === 'SUCCESS' && predictionResult?.prediction` guarding.
  2. Display the authoritative `Model Training Pending` card when `NO_MODEL` (HTTP 404) is received.

---

## 12. Known Limitations & Production Readiness

1. **No Production Model Currently Deployed**:
   The model registry (`ml-service/models/`) is empty, and `active_version.txt` does not exist. This is the correct, honest state before genuine dataset collection.
2. **Dataset Readiness Gate Requirement**:
   ML model training strictly requires a minimum of **50 authentic verified records** with both `FEASIBLE` and `NOT_FEASIBLE` project outcomes represented. Currently, only 1 unverified pending record exists in the database.
3. **Feasibility Predictor UI Behavior**:
   The UI correctly presents a **"Model Training Pending"** state until authentic project outcome records are submitted and validated.
4. **Service Dependency**:
   The Python FastAPI ML service must be running on port 8000 for ML model management and prediction endpoints to respond. Spring Boot returns HTTP 503 (`UNREACHABLE`) if the service is offline.
5. **Windows Process Constraints**:
   `RandomForestClassifier` uses `n_jobs=1` to guarantee stable execution on Windows within Python virtual environment processes.

