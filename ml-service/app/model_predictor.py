"""
ProjectGuard ML Model Predictor (Phase 10)

Validates input features, encodes them, runs the active model,
and returns a structured prediction response.

Security note: This module is called only by the Spring Boot backend
through authenticated API endpoints. FastAPI endpoints must NOT be
exposed directly to end users.
"""
from typing import Any, Dict, List, Optional

import numpy as np
import pandas as pd

from app.model_registry import ModelRegistry
from app.dataset_preprocessor import COMPLEXITY_MAP

DISCLAIMER = (
    "This prediction is an estimate based on historical patterns, "
    "not a guarantee of project success. Use it as one input among many."
)

# Feature ranges from Phase 9 dataset_validator.py
FEATURE_RANGES: Dict[str, Dict[str, Any]] = {
    "student_skill_level":      {"min": 1.0,  "max": 4.0,   "type": float},
    "skill_gap":                {"min": 0.0,  "max": 25.0,  "type": float},
    "project_complexity":       {"values": ["EASY", "MEDIUM", "HARD"], "type": str},
    "team_size":                {"min": 1,    "max": None,  "type": int},
    "available_hours_per_week": {"min": 1.0,  "max": 80.0,  "type": float},
    "technology_familiarity":   {"min": 1.0,  "max": 4.0,   "type": float},
    "number_of_requirements":   {"min": 0,    "max": None,  "type": int},
    "number_of_features":       {"min": 0,    "max": None,  "type": int},
    "previous_experience":      {"min": 0.0,  "max": None,  "type": float},
    "estimated_effort":         {"min": 1.0,  "max": None,  "type": float},
}

FEATURE_ORDER = [
    "student_skill_level",
    "skill_gap",
    "project_complexity_encoded",
    "team_size",
    "available_hours_per_week",
    "technology_familiarity",
    "number_of_requirements",
    "number_of_features",
    "previous_experience",
    "estimated_effort",
]

LABEL_MAP = {1: "FEASIBLE", 0: "NOT_FEASIBLE"}


def _validate_features(features: Dict[str, Any]) -> List[str]:
    """Returns a list of validation error messages (empty = valid)."""
    errors: List[str] = []

    for field, rules in FEATURE_RANGES.items():
        value = features.get(field)
        if value is None:
            errors.append(f"Missing required feature: '{field}'.")
            continue

        expected_type = rules["type"]

        # Categorical
        if "values" in rules:
            if not isinstance(value, str) or value.upper() not in rules["values"]:
                errors.append(
                    f"'{field}' must be one of {rules['values']}, got '{value}'."
                )
            continue

        # Numeric
        try:
            numeric_value = expected_type(value)
        except (TypeError, ValueError):
            errors.append(f"'{field}' must be a {expected_type.__name__}, got '{value}'.")
            continue

        min_v = rules.get("min")
        max_v = rules.get("max")
        if min_v is not None and numeric_value < min_v:
            errors.append(f"'{field}' must be ≥ {min_v}, got {numeric_value}.")
        if max_v is not None and numeric_value > max_v:
            errors.append(f"'{field}' must be ≤ {max_v}, got {numeric_value}.")

    return errors


def _encode_features(features: Dict[str, Any]) -> pd.DataFrame:
    """
    Encodes input features to match the training feature matrix.
    project_complexity → project_complexity_encoded (0/1/2)
    """
    complexity_raw = str(features.get("project_complexity", "MEDIUM")).upper()
    complexity_encoded = COMPLEXITY_MAP.get(complexity_raw, 1)

    row = {
        "student_skill_level":      float(features["student_skill_level"]),
        "skill_gap":                float(features["skill_gap"]),
        "project_complexity_encoded": int(complexity_encoded),
        "team_size":                int(features["team_size"]),
        "available_hours_per_week": float(features["available_hours_per_week"]),
        "technology_familiarity":   float(features["technology_familiarity"]),
        "number_of_requirements":   int(features["number_of_requirements"]),
        "number_of_features":       int(features["number_of_features"]),
        "previous_experience":      float(features["previous_experience"]),
        "estimated_effort":         float(features["estimated_effort"]),
    }
    return pd.DataFrame([row], columns=FEATURE_ORDER)


def _build_risk_factors(features: Dict[str, Any], prediction: str) -> List[str]:
    """
    Derives simple interpretable risk signals from input values.
    These are heuristic observations, not causal explanations.
    """
    factors: List[str] = []

    if float(features.get("skill_gap", 0)) > 15:
        factors.append("High skill gap detected — significant upskilling may be required.")
    if float(features.get("student_skill_level", 4)) < 2.0:
        factors.append("Low student skill level relative to typical project requirements.")
    if float(features.get("available_hours_per_week", 20)) < 10:
        factors.append("Low available hours per week may limit timely project delivery.")
    if float(features.get("technology_familiarity", 4)) < 2.0:
        factors.append("Low technology familiarity — additional learning time may be needed.")
    if str(features.get("project_complexity", "EASY")).upper() == "HARD":
        factors.append("Project is classified as HARD complexity.")
    if float(features.get("estimated_effort", 100)) > 500:
        factors.append("High estimated effort — scope management will be critical.")
    if int(features.get("team_size", 1)) == 1:
        factors.append("Solo project — no peer support or task distribution available.")

    return factors


class ModelPredictor:
    """
    Wraps the active model for prediction.
    Thread-safe for read operations; model is loaded once per call.
    """

    def __init__(self, registry: Optional[ModelRegistry] = None) -> None:
        self._registry = registry or ModelRegistry()

    def predict(self, features: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validates inputs, loads active model, returns prediction.

        Returns a structured dict always — never raises HTTP exceptions.
        The caller is responsible for HTTP error mapping.
        """
        # 1. Validate features
        errors = _validate_features(features)
        if errors:
            return {
                "status": "VALIDATION_ERROR",
                "prediction": None,
                "probability": None,
                "model_version": None,
                "feature_summary": features,
                "risk_factors": [],
                "disclaimer": DISCLAIMER,
                "errors": errors,
            }

        # 2. Load model
        try:
            model = self._registry.load_model()
            metadata = self._registry.load_metadata()
            active_version = self._registry.get_active_version()
        except FileNotFoundError as exc:
            return {
                "status": "NO_MODEL",
                "prediction": None,
                "probability": None,
                "model_version": None,
                "feature_summary": features,
                "risk_factors": [],
                "disclaimer": DISCLAIMER,
                "message": str(exc),
            }

        # 3. Encode features
        X = _encode_features(features)

        # 4. Predict
        pred_int = int(model.predict(X)[0])
        prediction_label = LABEL_MAP.get(pred_int, "UNKNOWN")

        probability: Optional[float] = None
        if hasattr(model, "predict_proba"):
            try:
                proba = model.predict_proba(X)[0]
                # Index 1 → FEASIBLE probability
                probability = float(proba[1]) if len(proba) > 1 else float(proba[0])
            except Exception:
                probability = None

        # 5. Risk factors
        risk_factors = _build_risk_factors(features, prediction_label)

        return {
            "status": "SUCCESS",
            "prediction": prediction_label,
            "probability": round(probability, 4) if probability is not None else None,
            "model_version": active_version,
            "algorithm": metadata.get("algorithm"),
            "feature_summary": {
                "student_skill_level": float(features["student_skill_level"]),
                "skill_gap": float(features["skill_gap"]),
                "project_complexity": str(features["project_complexity"]).upper(),
                "team_size": int(features["team_size"]),
                "available_hours_per_week": float(features["available_hours_per_week"]),
                "technology_familiarity": float(features["technology_familiarity"]),
                "number_of_requirements": int(features["number_of_requirements"]),
                "number_of_features": int(features["number_of_features"]),
                "previous_experience": float(features["previous_experience"]),
                "estimated_effort": float(features["estimated_effort"]),
            },
            "risk_factors": risk_factors,
            "disclaimer": DISCLAIMER,
        }

