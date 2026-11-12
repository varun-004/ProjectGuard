# Phase 9: Reliable ML Dataset Collection, Validation & Preparation

## 1. Overview and Architecture

Phase 9 establishes the authoritative, production-grade dataset engineering pipeline for **ProjectGuard**. Its sole mission is to collect, validate, clean, and stratify authentic student-project feasibility records in preparation for supervised machine learning (Random Forest and Gradient Boosting classifiers) in Phase 10.

> **Important Boundary:** Phase 9 prepares and verifies the dataset pipeline. **No ML model has been trained yet.** Supervised model training, cross-validation, hyperparameter tuning, and decision-tree export belong exclusively to Phase 10.

### Authoritative Architecture Flow

```
+-------------------------------------------------------------------------+
|                              Data Sources                               |
|  - Student Profile (skills, academic level, primary technology)         |
|  - Phase 5 Assessments (objective skill assessment scores)              |
|  - Phase 7 Personalized Recommendations (unmodified Phase 7 skill gap)  |
|  - Project Catalog & Gemini AI Ideas (requirements, features, weeks)    |
|  - Student Workflow Registration (observed available hours/week)       |
+------------------------------------+------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                  Spring Boot 3.5.x Backend (Java 21)                    |
|  - MlDataset Entity & MySQL `ml_dataset` table                          |
|  - MlDatasetService: 10-feature computation without arbitrary fallbacks |
|  - Evaluator Outcome Verification: requires evidence + confirmation     |
|  - Idempotent snapshots per student-project pair                        |
|  - Prepare & Validate routine: excludes PENDING_OUTCOME & invalid rows  |
|  - Cleaned UTF-8 CSV Export (10 features + 1 target)                    |
+------------------------------------+------------------------------------+
                                     |
           Authoritative REST Flow   |  POST /dataset/pipeline/run
                                     v
+-------------------------------------------------------------------------+
|                  Python ML Service (FastAPI + Pandas)                   |
|  - dataset_validator.py: boundary & range enforcement                   |
|  - dataset_preprocessor.py: ordinal & binary categorical encoding      |
|  - dataset_splitter.py: reproducible 80/20 train/test split (seed=42)   |
|  - GroupShuffleSplit by student to prevent data leakage across splits   |
|  - Class imbalance guard: minimum 10 verified records, >=2 per class    |
|  - Disk output: projectguard_ml_dataset.csv, projectguard_train.csv,   |
|                 projectguard_test.csv                                   |
+-------------------------------------------------------------------------+
```

---

## 2. Eleven-Column Dataset Schema

Each record models one concrete student-project combination.

| No. | Column Name | Type | Valid Range / Categories | Source | Description |
|---|---|---|---|---|---|
| 1 | `student_skill_level` | Float | [1.0, 4.0] | Profile & Quiz Scores | Average proficiency across student's declared and assessed skills. Missing values remain null (never assumed zero). |
| 2 | `skill_gap` | Float | [0.0, 25.0] | Phase 7 Scoring Engine | Existing unmodified Phase 7 skill gap (25.0 - skillFitScore). |
| 3 | `project_complexity` | Categorical | `EASY`, `MEDIUM`, `HARD` | Project definition | Categorical complexity level of the proposed project. |
| 4 | `team_size` | Integer | [1, 50] | Student Profile / Project | Number of students collaborating on the project. No blind default to 1. |
| 5 | `available_hours_per_week` | Float | [1.0, 80.0] | Student Registration | Real observed student time commitment. Never hardcoded as 15 hrs/week. |
| 6 | `technology_familiarity` | Float | [1.0, 4.0] | Profile & Assessments | Average familiarity with the technologies required by the project. |
| 7 | `number_of_requirements` | Integer | [0, 500] | Project Definition | Count of functional, non-functional, and data requirements. Null if missing. |
| 8 | `number_of_features` | Integer | [0, 500] | Project Definition | Count of distinct project features. Null if missing. |
| 9 | `previous_experience` | Float | [0.0, 40.0] | Student Profile | Relevant domain/tech experience in years. Never invented. |
| 10 | `estimated_effort` | Float | [1.0, 5000.0] | Derived (`weeks * hours`) | Estimated total effort in person-hours. Null if hours or duration unobserved. |
| 11 | `actual_outcome` | Target | `FEASIBLE`, `NOT_FEASIBLE` | Evaluator Verification | Verified ground-truth feasibility label. Null for pending records. |

---

## 3. Data Collection Sources & Integrity Rules

### Zero Arbitrary Fallback Values
1. **Available Hours**:
   - For Gemini-generated project snapshots (`dataSource = AI_GENERATION`), available hours per week are **not observed** at generation time.
   - `available_hours_per_week` remains `null`.
   - The snapshot is marked `INCOMPLETE` and safely excluded from supervised training until the student explicitly registers the project and inputs their actual weekly hours.
2. **Team Size**:
   - Taken strictly from student profile `teamSize`, project `minTeamSize`, or explicit registration request. Never defaulted to 1 blindly.
3. **Requirements & Features**:
   - Parsed strictly from project database requirements and requirement sections. Never defaulted to 2 requirements or 3 features.
4. **Estimated Effort**:
   - Requires both observed `available_hours_per_week` and valid project `estimated_weeks`. If either is missing, effort remains `null`.
5. **Proficiency & Experience**:
   - Missing proficiency is **not** assumed to be zero. Missing experience is **not** invented.

---

## 4. Controlled Actual Outcome Verification Process

A project must **never** become training-ready simply because a label was submitted or inferred from recommendation scores.

### Verification Criteria:
1. **Accepted Labels**: Exclusively `FEASIBLE` or `NOT_FEASIBLE`. `PENDING_OUTCOME` is never a training label.
2. **Explicit Confirmation**: The evaluator/admin must provide `validationConfirmed = true`.
3. **Meaningful Evidence**: The evaluator must provide `labelEvidence` with at least 10 characters detailing live demonstration, capstone presentation, code review, or milestone audit.
4. **Optional Notes**: `labelNotes` records contextual evaluator notes.
5. **Role Security**: Only users with the `ADMIN` role can record verified outcomes. Students cannot verify outcomes.
6. **Safe Correction Process**:
   - If a record has already been verified and a user attempts to modify the outcome, the request is rejected unless `allowCorrection = true` is supplied.
   - When `allowCorrection = true`, an immutable audit trail is appended to `labelNotes`:
     `[AUDIT: Outcome corrected from <OLD> to <NEW> by <ADMIN> at <TIMESTAMP>]`

---

## 5. Training Eligibility & Validation Rules

A record in `ml_dataset` is classified as `VALIDATED` (training-ready) if and only if:
1. All 10 input features are non-null and within valid numerical/categorical bounds.
2. Actual outcome is `FEASIBLE` or `NOT_FEASIBLE`.
3. Meaningful label evidence (>= 10 chars) is present.
4. The record is not a duplicate snapshot of an existing student-project pair.
5. `validationStatus == VALIDATED` and `outcomeStatus != PENDING_OUTCOME`.

Incomplete, unverified, or pending records are retained in MySQL for administrative review and audit, but strictly omitted from training CSV exports.

---

## 6. Python ML Service & 80/20 Train/Test Split Methodology

### Split Parameters:
- **Train / Test Ratio**: 80% train (`0.80`), 20% test (`0.20`).
- **Reproducibility**: Random seed fixed at `42` across all runs.
- **Stratification**: Preserves class ratio between `FEASIBLE` and `NOT_FEASIBLE` across partitions.
- **Student Leakage Prevention**: Uses `group_col="student_profile_id"` so that all records from any single student are assigned exclusively to either train or test, preventing data leakage.
- **Class Imbalance Guard**: Requires a minimum of 10 complete records and at least 2 verified examples of each class before declaring readiness.

### Generated Artifacts (on disk in `ml-service/data/`):
- `projectguard_ml_dataset.csv` — Full cleaned, validated dataset (11 columns).
- `projectguard_train.csv` — 80% training split.
- `projectguard_test.csv` — 20% evaluation split.

---

## 7. API Endpoints

### Backend (Spring Boot — Port 8080)
- `GET /api/ml-dataset` — Paginated dataset records. Role-filtered (Students view only their own records; Admins view all).
- `GET /api/ml-dataset/statistics` — Real-time dataset statistics (total, complete, incomplete, rejected, pending, feasible, not-feasible, training-ready, duplicates).
- `GET /api/ml-dataset/pending-outcomes` — Paginated records awaiting evaluator outcome verification (`ADMIN`).
- `PUT /api/ml-dataset/{id}/outcome` — Submit verified outcome with evidence, confirmation, and safe correction (`ADMIN`).
- `POST /api/ml-dataset/prepare` — Execute database cleaning and preparation routine (`ADMIN`).
- `GET /api/ml-dataset/export` — Download UTF-8 CSV of training-ready records (`ADMIN`).
- `GET /api/ml-dataset/split` — Readiness and split status report (`ADMIN`).
- `POST /api/ml-dataset/pipeline/trigger` — End-to-end synchronization: Spring Boot MySQL → Python FastAPI pipeline (`ADMIN`).
- `POST /api/ml-dataset/register` — Student registers project with observed weekly hours (`STUDENT`, `ADMIN`).

### ML Service (FastAPI — Port 8000)
- `GET /health` — Service health check.
- `POST /dataset/validate` — Validate batch records.
- `POST /dataset/encode` — Ordinal complexity and binary outcome encoding.
- `POST /dataset/split` — Stratified 80/20 split with leakage prevention.
- `POST /dataset/pipeline/run` — Authoritative endpoint executing validation, split, and CSV generation.
- `GET /dataset/statistics` — Summary of local dataset files on disk.

---

## 8. Configuration

### Backend `application.yml`:
```yaml
ml:
  service:
    url: ${ML_SERVICE_URL:http://localhost:8000}
    timeout-seconds: ${ML_SERVICE_TIMEOUT_SECONDS:10}
```

---

## 9. Current Limitations & Next Steps

1. **Current Record Count**: Real production records with verified outcomes are collected over time as students build projects and evaluators grade them.
2. **Phase 10 Readiness**: Once at least 10 authentic records with at least 2 of each class (`FEASIBLE` and `NOT_FEASIBLE`) are verified, model training (Random Forest and Gradient Boosting) will be executed in Phase 10.

