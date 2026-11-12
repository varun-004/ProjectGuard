"""
ProjectGuard Phase 10 Model Tests
Tests for training pipeline, readiness gate, registry, and prediction.
All synthetic fixtures here are ephemeral in-memory test data, NEVER written to MySQL.
"""
import pytest
import numpy as np
import pandas as pd
from fastapi.testclient import TestClient

from app.model_trainer import (
    check_dataset_readiness,
    train_random_forest,
    train_gradient_boosting,
    evaluate_model,
    compare_models,
    select_best_model,
    run_training_pipeline,
    MIN_TRAINING_RECORDS,
)
from app.model_registry import ModelRegistry
from app.model_predictor import ModelPredictor
from app.main import app


@pytest.fixture
def sample_small_df():
    """Dataset with only 5 records (below the 50 threshold)."""
    return pd.DataFrame([
        {
            "student_skill_level": 3.0,
            "skill_gap": 4.0,
            "project_complexity": "MEDIUM",
            "team_size": 2,
            "available_hours_per_week": 20.0,
            "technology_familiarity": 3.0,
            "number_of_requirements": 5,
            "number_of_features": 4,
            "previous_experience": 1.0,
            "estimated_effort": 150.0,
            "actual_outcome": "FEASIBLE"
        }
        for _ in range(5)
    ])


@pytest.fixture
def sample_synthetic_60_df():
    """Synthetic balanced dataset with 60 authentic-format records for testing."""
    records = []
    np.random.seed(42)
    for i in range(60):
        is_feasible = (i % 2 == 0)
        records.append({
            "student_skill_level": float(np.clip(3.2 if is_feasible else 1.8 + np.random.normal(0, 0.3), 1.0, 4.0)),
            "skill_gap": float(np.clip(3.0 if is_feasible else 12.0 + np.random.normal(0, 1.0), 0.0, 25.0)),
            "project_complexity": "EASY" if is_feasible else ("HARD" if i % 4 == 1 else "MEDIUM"),
            "team_size": 3 if is_feasible else 1,
            "available_hours_per_week": float(np.clip(25.0 if is_feasible else 10.0 + np.random.normal(0, 2.0), 1.0, 80.0)),
            "technology_familiarity": float(np.clip(3.5 if is_feasible else 1.5 + np.random.normal(0, 0.2), 1.0, 4.0)),
            "number_of_requirements": int(np.clip(4 if is_feasible else 8, 1, 30)),
            "number_of_features": int(np.clip(3 if is_feasible else 6, 1, 20)),
            "previous_experience": float(np.clip(1.5 if is_feasible else 0.0, 0.0, 10.0)),
            "estimated_effort": float(np.clip(120.0 if is_feasible else 280.0, 10.0, 1000.0)),
            "actual_outcome": "FEASIBLE" if is_feasible else "NOT_FEASIBLE"
        })
    return pd.DataFrame(records)


def test_readiness_blocks_insufficient_records(sample_small_df):
    readiness = check_dataset_readiness(sample_small_df)
    assert readiness["is_ready"] is False
    assert readiness["total_records"] == 5
    assert readiness["required_records"] == MIN_TRAINING_RECORDS
    assert readiness["records_needed"] == 45
    assert any("Insufficient records" in r for r in readiness["blocked_reasons"])


def test_readiness_blocks_missing_class():
    # 60 records, all FEASIBLE (missing NOT_FEASIBLE)
    df = pd.DataFrame([
        {
            "student_skill_level": 3.0,
            "skill_gap": 2.0,
            "project_complexity": "EASY",
            "team_size": 2,
            "available_hours_per_week": 20.0,
            "technology_familiarity": 3.0,
            "number_of_requirements": 3,
            "number_of_features": 3,
            "previous_experience": 1.0,
            "estimated_effort": 100.0,
            "actual_outcome": "FEASIBLE"
        }
        for _ in range(60)
    ])
    readiness = check_dataset_readiness(df)
    assert readiness["is_ready"] is False
    assert any("NOT_FEASIBLE" in r for r in readiness["blocked_reasons"])


def test_readiness_passes_with_60_records(sample_synthetic_60_df):
    readiness = check_dataset_readiness(sample_synthetic_60_df)
    assert readiness["is_ready"] is True
    assert readiness["total_records"] == 60
    assert readiness["records_needed"] == 0
    assert len(readiness["blocked_reasons"]) == 0


def test_training_pipeline_and_comparison(sample_synthetic_60_df):
    result = run_training_pipeline(sample_synthetic_60_df)
    assert result["status"] == "TRAINING_COMPLETE"
    assert "RandomForest" in result["models"]
    assert "GradientBoosting" in result["models"]
    assert result["best_model"] in ["RandomForest", "GradientBoosting"]

    comparison = result["comparison"]
    for m in ["RandomForest", "GradientBoosting"]:
        assert "accuracy" in comparison[m]
        assert "f1_weighted" in comparison[m]
        assert "precision_weighted" in comparison[m]
        assert "recall_weighted" in comparison[m]
        assert "confusion_matrix" in comparison[m]
        assert "feature_importance" in comparison[m]


def test_model_registry_lifecycle(tmp_path, sample_synthetic_60_df):
    reg = ModelRegistry(models_dir=str(tmp_path / "models"))
    status = reg.get_model_status()
    assert status["model_available"] is False
    assert status["active_version"] is None

    # Train a model
    result = run_training_pipeline(sample_synthetic_60_df)
    best_clf = result["models"][result["best_model"]]
    meta = {
        "algorithm": result["best_model"],
        "metrics": result["comparison"][result["best_model"]],
        "training_timestamp": "2026-11-11T12:00:00Z"
    }

    v1 = reg.save_model(best_clf, meta, activate=True)
    assert v1 == "v1"
    assert reg.get_active_version() == "v1"
    assert reg.list_versions() == ["v1"]

    # Verify model status
    status2 = reg.get_model_status()
    assert status2["model_available"] is True
    assert status2["active_version"] == "v1"
    assert status2["algorithm"] == result["best_model"]

    # Save a second version
    v2 = reg.save_model(best_clf, meta, activate=False)
    assert v2 == "v2"
    assert reg.get_active_version() == "v1"  # still v1 because activate=False
    assert reg.list_versions() == ["v1", "v2"]

    # Activate v2
    reg.activate_version("v2")
    assert reg.get_active_version() == "v2"


def test_model_predictor_validation_and_inference(tmp_path, sample_synthetic_60_df):
    reg = ModelRegistry(models_dir=str(tmp_path / "models"))
    pred = ModelPredictor(registry=reg)

    # 1. No model available yet
    valid_features = {
        "student_skill_level": 3.0,
        "skill_gap": 4.0,
        "project_complexity": "MEDIUM",
        "team_size": 2,
        "available_hours_per_week": 20.0,
        "technology_familiarity": 3.0,
        "number_of_requirements": 4,
        "number_of_features": 3,
        "previous_experience": 1.0,
        "estimated_effort": 120.0
    }
    no_model_res = pred.predict(valid_features)
    assert no_model_res["status"] == "NO_MODEL"

    # 2. Validation error on invalid range
    invalid_features = valid_features.copy()
    invalid_features["student_skill_level"] = 10.0  # max is 4.0
    val_res = pred.predict(invalid_features)
    assert val_res["status"] == "VALIDATION_ERROR"
    assert len(val_res["errors"]) > 0

    # 3. Train and save model
    result = run_training_pipeline(sample_synthetic_60_df)
    reg.save_model(
        result["models"][result["best_model"]],
        {"algorithm": result["best_model"], "metrics": result["comparison"][result["best_model"]]},
        activate=True
    )

    # 4. Successful prediction
    pred_res = pred.predict(valid_features)
    assert pred_res["status"] == "SUCCESS"
    assert pred_res["prediction"] in ["FEASIBLE", "NOT_FEASIBLE"]
    assert 0.0 <= pred_res["probability"] <= 1.0
    assert pred_res["model_version"] == "v1"
    assert "disclaimer" in pred_res
    assert isinstance(pred_res["risk_factors"], list)


def test_fastapi_endpoints(sample_synthetic_60_df, tmp_path, monkeypatch):
    test_models_dir = str(tmp_path / "models")
    test_reg = ModelRegistry(models_dir=test_models_dir)
    test_pred = ModelPredictor(registry=test_reg)
    import app.main as main_mod
    monkeypatch.setattr(main_mod, "registry", test_reg)
    monkeypatch.setattr(main_mod, "predictor", test_pred)

    client = TestClient(app)

    # Health check
    res = client.get("/health")
    assert res.status_code == 200

    # Model status endpoint
    res = client.get("/model/status")
    assert res.status_code == 200
    data = res.json()
    assert "model_available" in data

    # Train with empty records -> blocked
    res = client.post("/model/train", json={"records": []})
    assert res.status_code == 200
    blocked_data = res.json()
    assert blocked_data["status"] == "TRAINING_BLOCKED"
    assert blocked_data["required_records"] == 50

    # Train with 60 records -> complete
    records = sample_synthetic_60_df.to_dict(orient="records")
    res = client.post("/model/train", json={"records": records})
    assert res.status_code == 200
    trained_data = res.json()
    assert trained_data["status"] == "TRAINING_COMPLETE"
    assert "best_model" in trained_data
    assert "active_version" in trained_data

    # Predict via API
    pred_payload = {
        "student_skill_level": 3.5,
        "skill_gap": 2.0,
        "project_complexity": "EASY",
        "team_size": 3,
        "available_hours_per_week": 25.0,
        "technology_familiarity": 3.8,
        "number_of_requirements": 3,
        "number_of_features": 3,
        "previous_experience": 2.0,
        "estimated_effort": 100.0
    }
    res = client.post("/model/predict", json=pred_payload)
    assert res.status_code == 200
    pdata = res.json()
    assert pdata["status"] == "SUCCESS"
    assert pdata["prediction"] in ["FEASIBLE", "NOT_FEASIBLE"]
    assert pdata["probability"] is not None

