"""
ProjectGuard ML Dataset Preprocessor (Phase 9)
Prepares features for ML models (Random Forest / Gradient Boosting) in Phase 10:
- Categorical encoding: project_complexity (EASY=0, MEDIUM=1, HARD=2)
- Target encoding: actual_outcome (FEASIBLE=1, NOT_FEASIBLE=0)
- Preserves raw values in database.
- Does not introduce unnecessary numerical scaling (unnecessary for tree algorithms).
"""
from typing import Dict, Any, Tuple
import pandas as pd

COMPLEXITY_MAP = {
    "EASY": 0,
    "MEDIUM": 1,
    "HARD": 2
}

OUTCOME_MAP = {
    "FEASIBLE": 1,
    "NOT_FEASIBLE": 0
}


def encode_dataset(df: pd.DataFrame, include_target: bool = True) -> pd.DataFrame:
    """
    Applies documented encoding rules to a validated DataFrame:
    - Encodes project_complexity to ordinal integer (0, 1, 2)
    - Encodes actual_outcome to binary label (1, 0) if present and requested
    - Preserves all numerical columns without artificial scaling
    """
    encoded = df.copy()

    if "project_complexity" in encoded.columns:
        encoded["project_complexity_encoded"] = encoded["project_complexity"].astype(str).str.upper().map(COMPLEXITY_MAP)
        # Fall back if unmapped
        encoded["project_complexity_encoded"] = encoded["project_complexity_encoded"].fillna(1).astype(int)

    if include_target and "actual_outcome" in encoded.columns:
        encoded["actual_outcome_encoded"] = encoded["actual_outcome"].astype(str).str.upper().map(OUTCOME_MAP)

    return encoded


def get_feature_matrix(df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
    """
    Extracts X (feature matrix) and y (target vector) ready for scikit-learn estimators in Phase 10.
    """
    encoded = encode_dataset(df, include_target=True)

    feature_cols = [
        "student_skill_level",
        "skill_gap",
        "project_complexity_encoded",
        "team_size",
        "available_hours_per_week",
        "technology_familiarity",
        "number_of_requirements",
        "number_of_features",
        "previous_experience",
        "estimated_effort"
    ]

    X = encoded[feature_cols].copy()
    y = encoded["actual_outcome_encoded"].copy() if "actual_outcome_encoded" in encoded else None
    return X, y

