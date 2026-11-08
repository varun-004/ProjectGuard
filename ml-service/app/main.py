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

app = FastAPI(title="ProjectGuard ML Service", version="0.9.0")
manager = DatasetManager(data_dir=os.path.join(os.path.dirname(__file__), "..", "data"))


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

    result = manager.process_and_split(df, group_col=group_col)
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
