"""
ProjectGuard ML Service (Phase 9)
FastAPI service exposing dataset validation, encoding, and splitting utilities.
"""
from typing import List, Dict, Any, Optional
import os
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field
import pandas as pd

from app.dataset_validator import (
    validate_record,
    validate_dataframe,
    FEATURE_COLUMNS,
    TARGET_COLUMN,
    ALL_COLUMNS,
)
from app.dataset_preprocessor import encode_dataset, get_feature_matrix
from app.dataset_splitter import split_dataset, export_splits_to_csv
from app.dataset_manager import DatasetManager
from app.model_trainer import (
    check_dataset_readiness,
    run_training_pipeline,
    FEATURE_ORDER,
)
from app.model_registry import ModelRegistry
from app.model_predictor import ModelPredictor

app = FastAPI(title="ProjectGuard ML Service", version="1.0.0")
manager = DatasetManager(data_dir=os.path.join(os.path.dirname(__file__), "..", "data"))
registry = ModelRegistry()
predictor = ModelPredictor(registry=registry)


class RecordInput(BaseModel):
    student_skill_level: Optional[float] = None
    skill_gap: Optional[float] = None
    project_complexity: Optional[str] = None
    team_size: Optional[int] = None
    available_hours_per_week: Optional[float] = None
    technology_familiarity: Optional[float] = None
    number_of_requirements: Optional[int] = None
    number_of_features: Optional[int] = None
    previous_experience: Optional[float] = None
    estimated_effort: Optional[float] = None
    actual_outcome: Optional[str] = None
    student_profile_id: Optional[int] = None
    project_id: Optional[int] = None


class DatasetBatchRequest(BaseModel):
    records: List[RecordInput]
    test_size: float = Field(default=0.20, ge=0.05, le=0.50)
    random_state: int = Field(default=42)
    group_by_student: bool = Field(default=False)


@app.get("/health")
def health():
    return {"status": "UP", "service": "projectguard-ml", "version": "0.9.0"}


@app.post("/dataset/validate")
def validate_dataset_records(request: DatasetBatchRequest):
    """
    Validates a list of records against Phase 9 criteria.
    Identifies eligible and excluded records.
    """
    if not request.records:
        return {
            "total_records": 0,
            "valid_records": 0,
            "invalid_records": 0,
            "reasons": {}
        }

    df = pd.DataFrame([r.model_dump() for r in request.records])
    valid_df, invalid_df, summary = validate_dataframe(df)

    return summary


@app.post("/dataset/encode")
def encode_dataset_records(records: List[RecordInput]):
    """
    Encodes project_complexity (0, 1, 2) and actual_outcome (1, 0) for Phase 10 preparation.
    """
    if not records:
        return {"records": []}

    df = pd.DataFrame([r.model_dump() for r in records])
    encoded = encode_dataset(df, include_target=True)
    return {"records": encoded.to_dict(orient="records")}


@app.post("/dataset/split")
def split_dataset_records(request: DatasetBatchRequest):
    """
    Performs 80/20 train/test split on validated records.
    Handles class balance and group-aware splitting to prevent student data leakage.
    """
    df = pd.DataFrame([r.model_dump() for r in request.records])
    group_col = "student_profile_id" if request.group_by_student else None

    result = manager.process_and_split(
        df,
        group_col=group_col,
        test_size=request.test_size,
        random_state=request.random_state
    )
    return result


@app.post("/dataset/pipeline/run")
def run_pipeline(request: DatasetBatchRequest):
    """
    Authoritative Phase 9 pipeline endpoint called by Spring Boot:
    1. Receives validated records from Spring Boot.
    2. Runs feature validation and filtering.
    3. Executes 80/20 train/test split with seed 42.
    4. Saves projectguard_ml_dataset.csv, projectguard_train.csv, and projectguard_test.csv.
    5. Returns validation statistics, split report, class distributions, file paths, and readiness status.
    """
    if not request.records:
        return {
            "status": "EMPTY",
            "message": "No records supplied to pipeline.",
            "total_records": 0,
            "training_ready": False,
            "validation_summary": {
                "total_records": 0,
                "valid_records": 0,
                "invalid_records": 0,
                "reasons": {}
            },
            "split_report": {
                "is_ready_for_training": False,
                "message": "Dataset is empty."
            },
            "files": {}
        }

    df = pd.DataFrame([r.model_dump() for r in request.records])
    group_col = "student_profile_id" if request.group_by_student else None

    result = manager.process_and_split(
        df,
        group_col=group_col,
        test_size=request.test_size,
        random_state=request.random_state
    )

    is_ready = result.get("split_report", {}).get("is_ready_for_training", False)
    return {
        "status": "SUCCESS" if is_ready else "INSUFFICIENT_DATA",
        "training_ready": is_ready,
        "total_records": len(df),
        "validation_summary": result.get("validation_summary"),
        "split_report": result.get("split_report"),
        "files": result.get("files")
    }


@app.post("/dataset/process-csv")
def process_csv_content(csv_content: str):
    """
    Processes raw CSV string content: loads into DataFrame and runs pipeline.
    """
    import io
    if not csv_content or not csv_content.strip():
        raise HTTPException(status_code=400, detail="CSV content is empty")

    try:
        df = pd.read_csv(io.StringIO(csv_content))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to parse CSV: {str(e)}")

    group_col = "student_profile_id" if "student_profile_id" in df.columns else None
    result = manager.process_and_split(df, group_col=group_col, test_size=0.20, random_state=42)
    return result


@app.get("/dataset/statistics")
def get_dataset_statistics():
    """
    Returns statistics on the local dataset files if present.
    """
    csv_path = os.path.join(manager.data_dir, "projectguard_ml_dataset.csv")
    if not os.path.exists(csv_path):
        return {
            "file_exists": False,
            "data_directory": manager.data_dir,
            "total_records": 0,
            "training_ready": False,
            "message": "No exported dataset CSV found on disk yet."
        }

    df = pd.read_csv(csv_path, encoding="utf-8")
    valid_df, invalid_df, summary = validate_dataframe(df)

    return {
        "file_exists": True,
        "csv_path": csv_path,
        "total_records": len(df),
        "summary": summary
    }


# ─────────────────────────────────────────────────────────────────────────────
# PHASE 10: MODEL MANAGEMENT & PREDICTION ENDPOINTS
# ─────────────────────────────────────────────────────────────────────────────

class PredictionRequest(BaseModel):
    student_skill_level: float
    skill_gap: float
    project_complexity: str
    team_size: int
    available_hours_per_week: float
    technology_familiarity: float
    number_of_requirements: int
    number_of_features: int
    previous_experience: float
    estimated_effort: float


class ModelTrainRequest(BaseModel):
    records: Optional[List[RecordInput]] = None
    test_size: float = Field(default=0.20, ge=0.05, le=0.50)
    rf_params: Optional[Dict[str, Any]] = None
    gb_params: Optional[Dict[str, Any]] = None


@app.get("/model/status")
def get_model_status():
    """
    Returns the deployment status of the ML model:
    - model_available (bool)
    - active_version
    - algorithm
    - metrics (accuracy, f1, auc, etc.)
    - trained_at
    """
    return registry.get_model_status()


@app.get("/model/versions")
def list_model_versions():
    """
    Lists all saved model versions in the registry and the active version.
    """
    versions = registry.list_versions()
    active = registry.get_active_version()
    version_details = []
    for v in versions:
        try:
            meta = registry.load_metadata(v)
            version_details.append({
                "version": v,
                "is_active": (v == active),
                "algorithm": meta.get("algorithm"),
                "saved_at": meta.get("saved_at") or meta.get("training_timestamp"),
                "f1_weighted": meta.get("metrics", {}).get("f1_weighted"),
                "accuracy": meta.get("metrics", {}).get("accuracy")
            })
        except Exception:
            version_details.append({
                "version": v,
                "is_active": (v == active)
            })
    return {
        "active_version": active,
        "total_versions": len(versions),
        "versions": version_details
    }


@app.post("/model/activate/{version}")
def activate_model_version(version: str):
    """
    Activates an existing model version.
    """
    try:
        registry.activate_version(version)
        return {
            "status": "SUCCESS",
            "message": f"Model version '{version}' is now active.",
            "active_version": version
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get("/model/metrics")
def get_model_metrics(version: Optional[str] = None):
    """
    Returns detailed evaluation metrics and feature importance for the active or specified version.
    """
    try:
        metadata = registry.load_metadata(version)
        return {
            "version": metadata.get("version"),
            "algorithm": metadata.get("algorithm"),
            "metrics": metadata.get("metrics"),
            "comparison": metadata.get("comparison"),
            "hyperparameters": metadata.get("hyperparameters"),
            "training_timestamp": metadata.get("training_timestamp") or metadata.get("saved_at")
        }
    except FileNotFoundError as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.post("/model/train")
def train_model(request: ModelTrainRequest = ModelTrainRequest()):
    """
    Authoritative Phase 10 Model Training endpoint.
    1. Checks the dataset readiness gate (>= 50 authentic records, both classes).
    2. If blocked, returns clear response with required counts without training.
    3. If ready, trains Random Forest & Gradient Boosting, evaluates both,
       selects the best model by F1-weighted score, saves to registry, and activates it.
    """
    # Source records: from request or from local exported CSV
    df = None
    if request.records and len(request.records) > 0:
        raw_df = pd.DataFrame([r.model_dump() for r in request.records])
        valid_df, _, _ = validate_dataframe(raw_df)
        df = valid_df
    else:
        csv_path = os.path.join(manager.data_dir, "projectguard_ml_dataset.csv")
        if os.path.exists(csv_path):
            raw_df = pd.read_csv(csv_path)
            valid_df, _, _ = validate_dataframe(raw_df)
            df = valid_df

    if df is None or len(df) == 0:
        readiness = check_dataset_readiness(pd.DataFrame())
        return {
            "status": "TRAINING_BLOCKED",
            "message": "No dataset records available for training.",
            "available_records": 0,
            "required_records": 50,
            "readiness": readiness,
            "metrics": None
        }

    # Run training pipeline
    result = run_training_pipeline(
        df=df,
        rf_params=request.rf_params,
        gb_params=request.gb_params,
        test_size=request.test_size
    )

    if result["status"] == "TRAINING_BLOCKED":
        readiness = result["readiness"]
        return {
            "status": "TRAINING_BLOCKED",
            "message": readiness.get("message", "Training blocked by readiness gate."),
            "available_records": readiness.get("total_records", 0),
            "required_records": readiness.get("required_records", 50),
            "records_needed": readiness.get("records_needed", 50),
            "class_distribution": readiness.get("class_distribution", {}),
            "readiness": readiness,
            "metrics": None
        }

    # Save the best model
    best_name = result["best_model"]
    best_clf = result["models"][best_name]
    best_metrics = result["comparison"][best_name]

    metadata = {
        "algorithm": best_name,
        "metrics": best_metrics,
        "comparison": result["comparison"],
        "train_size": result["train_size"],
        "test_size": result["test_size"],
        "feature_order": FEATURE_ORDER,
        "dataset_record_count": len(df),
        "hyperparameters": getattr(best_clf, "get_params", lambda: {})()
    }

    version = registry.save_model(best_clf, metadata, activate=True)

    return {
        "status": "TRAINING_COMPLETE",
        "message": f"Successfully trained and activated model version '{version}' ({best_name}).",
        "active_version": version,
        "best_model": best_name,
        "metrics": best_metrics,
        "comparison": result["comparison"],
        "train_size": result["train_size"],
        "test_size": result["test_size"]
    }


@app.post("/model/predict")
def predict_project_feasibility(request: PredictionRequest):
    """
    Feasibility prediction endpoint.
    Called via Spring Boot backend for authenticated students/evaluators.
    """
    res = predictor.predict(request.model_dump())
    if res.get("status") == "NO_MODEL":
        raise HTTPException(status_code=404, detail=res.get("message", "No active model available."))
    if res.get("status") == "VALIDATION_ERROR":
        raise HTTPException(status_code=422, detail={"errors": res.get("errors")})
    return res

