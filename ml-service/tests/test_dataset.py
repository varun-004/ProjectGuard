"""
Unit and Integration Tests for ProjectGuard ML Service Dataset Operations (Phase 9)
Tests:
- CSV reading with pandas
- Feature validation
- Invalid records exclusion
- Pending outcome exclusion
- Encoding consistency
- Reproducible split
- No student/project leakage across train and test sets
- Insufficient data handling
"""
import os
import tempfile
import pandas as pd
import pytest

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


@pytest.fixture
def sample_valid_records():
    records = []
    # Create 12 valid records: 6 FEASIBLE, 6 NOT_FEASIBLE
    for i in range(12):
        records.append({
            "student_profile_id": (i % 4) + 1,  # 4 students
            "project_id": 100 + i,
            "student_skill_level": 2.0 + (i % 3) * 0.5,     # 2.0, 2.5, 3.0
            "skill_gap": 1.5 + (i % 4) * 2.0,              # 1.5, 3.5, 5.5, 7.5
            "project_complexity": ["EASY", "MEDIUM", "HARD"][i % 3],
            "team_size": 2 + (i % 3),
            "available_hours_per_week": 15.0 + (i % 3) * 5.0,
            "technology_familiarity": 2.2 + (i % 3) * 0.4,
            "number_of_requirements": 3 + (i % 4),
            "number_of_features": 2 + (i % 3),
            "previous_experience": float(i % 3),
            "estimated_effort": 180.0 + i * 20.0,
            "actual_outcome": "FEASIBLE" if i % 2 == 0 else "NOT_FEASIBLE"
        })
    return pd.DataFrame(records)


def test_pandas_csv_read_write(sample_valid_records):
    """Verify that exported CSV can be read and written accurately by pandas."""
    with tempfile.TemporaryDirectory() as tmpdir:
        manager = DatasetManager(data_dir=tmpdir)
        csv_path = manager.export_dataset_csv(sample_valid_records)

        assert os.path.exists(csv_path)

        # Read back with pandas
        df_read = manager.load_csv(csv_path)
        assert len(df_read) == 12
        assert list(df_read.columns) == ALL_COLUMNS
        assert df_read["actual_outcome"].iloc[0] in ["FEASIBLE", "NOT_FEASIBLE"]


def test_feature_validation_rules():
    """Verify all 10 features + outcome validation boundaries."""
    # Valid record
    valid_rec = {
        "student_skill_level": 3.0,
        "skill_gap": 4.5,
        "project_complexity": "MEDIUM",
        "team_size": 3,
        "available_hours_per_week": 20.0,
        "technology_familiarity": 3.2,
        "number_of_requirements": 4,
        "number_of_features": 3,
        "previous_experience": 1.5,
        "estimated_effort": 240.0,
        "actual_outcome": "FEASIBLE"
    }
    is_valid, errors = validate_record(valid_rec)
    assert is_valid
    assert len(errors) == 0

    # Skill level out of bounds (>4.0)
    bad_skill = dict(valid_rec, student_skill_level=5.5)
    is_valid, errors = validate_record(bad_skill)
    assert not is_valid
    assert any("proficiency scale" in e for e in errors)

    # Complexity invalid
    bad_comp = dict(valid_rec, project_complexity="SUPER_HARD")
    is_valid, errors = validate_record(bad_comp)
    assert not is_valid
    assert any("project_complexity" in e for e in errors)

    # Team size negative
    bad_team = dict(valid_rec, team_size=-1)
    is_valid, errors = validate_record(bad_team)
    assert not is_valid
    assert any("team_size" in e for e in errors)


def test_pending_outcome_excluded_from_training():
    """Verify records with PENDING_OUTCOME are strictly excluded from training."""
    pending_rec = {
        "student_skill_level": 3.0,
        "skill_gap": 4.0,
        "project_complexity": "MEDIUM",
        "team_size": 2,
        "available_hours_per_week": 15.0,
        "technology_familiarity": 3.0,
        "number_of_requirements": 3,
        "number_of_features": 2,
        "previous_experience": 1.0,
        "estimated_effort": 200.0,
        "actual_outcome": "PENDING_OUTCOME"
    }
    is_valid, errors = validate_record(pending_rec)
    assert not is_valid
    assert any("PENDING_OUTCOME" in e for e in errors)


def test_invalid_records_exclusion(sample_valid_records):
    """Verify invalid rows are partitioned and excluded from training set."""
    df_with_bad = sample_valid_records.copy()
    # Add a row missing features
    bad_row = {
        "student_skill_level": None,  # Missing
        "skill_gap": 5.0,
        "project_complexity": "INVALID",
        "team_size": 2,
        "available_hours_per_week": 15.0,
        "technology_familiarity": 3.0,
        "number_of_requirements": 2,
        "number_of_features": 1,
        "previous_experience": 0.0,
        "estimated_effort": 100.0,
        "actual_outcome": "PENDING_OUTCOME"
    }
    df_with_bad = pd.concat([df_with_bad, pd.DataFrame([bad_row])], ignore_index=True)

    valid_df, invalid_df, summary = validate_dataframe(df_with_bad)
    assert len(valid_df) == 12
    assert len(invalid_df) == 1
    assert summary["valid_records"] == 12
    assert summary["invalid_records"] == 1


def test_encoding_consistency(sample_valid_records):
    """Verify project_complexity and actual_outcome encoding consistency."""
    encoded = encode_dataset(sample_valid_records)

    assert "project_complexity_encoded" in encoded.columns
    assert "actual_outcome_encoded" in encoded.columns

    # Complexity encoding: EASY=0, MEDIUM=1, HARD=2
    easy_mask = sample_valid_records["project_complexity"] == "EASY"
    assert (encoded.loc[easy_mask, "project_complexity_encoded"] == 0).all()

    med_mask = sample_valid_records["project_complexity"] == "MEDIUM"
    assert (encoded.loc[med_mask, "project_complexity_encoded"] == 1).all()

    hard_mask = sample_valid_records["project_complexity"] == "HARD"
    assert (encoded.loc[hard_mask, "project_complexity_encoded"] == 2).all()

    # Outcome encoding: FEASIBLE=1, NOT_FEASIBLE=0
    feas_mask = sample_valid_records["actual_outcome"] == "FEASIBLE"
    assert (encoded.loc[feas_mask, "actual_outcome_encoded"] == 1).all()

    not_feas_mask = sample_valid_records["actual_outcome"] == "NOT_FEASIBLE"
    assert (encoded.loc[not_feas_mask, "actual_outcome_encoded"] == 0).all()

    # Feature matrix X, y extraction
    X, y = get_feature_matrix(sample_valid_records)
    assert len(X.columns) == 10
    assert "project_complexity_encoded" in X.columns
    assert len(y) == len(sample_valid_records)


def test_reproducible_80_20_split(sample_valid_records):
    """Verify split reproducibility and ratios with seed=42."""
    train_df1, test_df1, rep1 = split_dataset(sample_valid_records, test_size=0.20, random_state=42)
    train_df2, test_df2, rep2 = split_dataset(sample_valid_records, test_size=0.20, random_state=42)

    assert rep1["is_ready_for_training"]
    assert len(train_df1) == len(train_df2)
    assert len(test_df1) == len(test_df2)

    # Identical record indices due to fixed seed
    pd.testing.assert_frame_equal(train_df1, train_df2)
    pd.testing.assert_frame_equal(test_df1, test_df2)

    # Both classes present in train and test
    assert rep1["train_feasible"] > 0
    assert rep1["train_not_feasible"] > 0
    assert rep1["test_feasible"] > 0
    assert rep1["test_not_feasible"] > 0


def test_group_aware_splitting_prevents_student_leakage(sample_valid_records):
    """Verify no student profile appears in both train and test sets when grouped."""
    train_df, test_df, report = split_dataset(
        sample_valid_records,
        test_size=0.25,
        random_state=42,
        group_col="student_profile_id"
    )

    train_students = set(train_df["student_profile_id"].unique())
    test_students = set(test_df["student_profile_id"].unique())

    overlap = train_students.intersection(test_students)
    assert len(overlap) == 0, f"Data leakage detected! Students {overlap} appeared in both train and test sets."


def test_insufficient_data_handling():
    """Verify graceful handling when dataset lacks enough verified examples."""
    sparse_df = pd.DataFrame([
        {"student_skill_level": 3.0, "actual_outcome": "FEASIBLE"}
    ])
    train_df, test_df, report = split_dataset(sparse_df, test_size=0.20, random_state=42)

    assert not report["is_ready_for_training"]
    assert "below the minimum required" in report["message"]
    assert train_df.empty
    assert test_df.empty


def test_missing_labels_rejected():
    """Verify records missing outcome labels are strictly rejected from training."""
    rec_missing = {
        "student_skill_level": 3.0,
        "skill_gap": 4.0,
        "project_complexity": "MEDIUM",
        "team_size": 2,
        "available_hours_per_week": 20.0,
        "technology_familiarity": 3.0,
        "number_of_requirements": 3,
        "number_of_features": 2,
        "previous_experience": 1.0,
        "estimated_effort": 200.0,
        "actual_outcome": None
    }
    is_valid, errors = validate_record(rec_missing)
    assert not is_valid
    assert any("actual_outcome is missing" in e for e in errors)


def test_invalid_numeric_ranges_and_categories():
    """Verify invalid numeric ranges and categories are rejected with explicit errors."""
    base = {
        "student_skill_level": 3.0,
        "skill_gap": 4.0,
        "project_complexity": "MEDIUM",
        "team_size": 2,
        "available_hours_per_week": 20.0,
        "technology_familiarity": 3.0,
        "number_of_requirements": 3,
        "number_of_features": 2,
        "previous_experience": 1.0,
        "estimated_effort": 200.0,
        "actual_outcome": "FEASIBLE"
    }

    # Skill gap negative
    v, errs = validate_record(dict(base, skill_gap=-1.0))
    assert not v and any("skill_gap" in e for e in errs)

    # Hours > 80
    v, errs = validate_record(dict(base, available_hours_per_week=120.0))
    assert not v and any("available_hours_per_week" in e for e in errs)

    # Tech familiarity < 1.0
    v, errs = validate_record(dict(base, technology_familiarity=0.5))
    assert not v and any("technology_familiarity" in e for e in errs)

    # Effort negative
    v, errs = validate_record(dict(base, estimated_effort=-50.0))
    assert not v and any("estimated_effort" in e for e in errs)

    # Experience negative
    v, errs = validate_record(dict(base, previous_experience=-2.0))
    assert not v and any("previous_experience" in e for e in errs)


def test_class_imbalance_detection(sample_valid_records):
    """Verify class imbalance (e.g. only 1 or 0 of one class) halts readiness."""
    imbalanced = sample_valid_records.copy()
    # Change all but one to FEASIBLE
    imbalanced["actual_outcome"] = "FEASIBLE"
    imbalanced.loc[0, "actual_outcome"] = "NOT_FEASIBLE"

    train_df, test_df, report = split_dataset(imbalanced, test_size=0.20, random_state=42)
    assert not report["is_ready_for_training"]
    assert "Class imbalance constraint violated" in report["message"]


def test_empty_dataset_handling():
    """Verify empty dataset does not fail and reports zero records."""
    empty_df = pd.DataFrame(columns=ALL_COLUMNS)
    train_df, test_df, report = split_dataset(empty_df, test_size=0.20, random_state=42)
    assert not report["is_ready_for_training"]
    assert report["total_records"] == 0
    assert train_df.empty


def test_pipeline_run_integration(sample_valid_records):
    """Verify full DatasetManager pipeline execution and file creation."""
    with tempfile.TemporaryDirectory() as tmpdir:
        mgr = DatasetManager(data_dir=tmpdir)
        result = mgr.process_and_split(sample_valid_records, test_size=0.20, random_state=42)

        assert result["split_report"]["is_ready_for_training"]
        assert "train_csv" in result["files"]
        assert "test_csv" in result["files"]
        assert "dataset_csv" in result["files"]

        # Check files exist
        assert os.path.exists(result["files"]["train_csv"])
        assert os.path.exists(result["files"]["test_csv"])
        assert os.path.exists(result["files"]["dataset_csv"])

        # Check content
        train_read = pd.read_csv(result["files"]["train_csv"])
        test_read = pd.read_csv(result["files"]["test_csv"])
        assert len(train_read) + len(test_read) == 12


