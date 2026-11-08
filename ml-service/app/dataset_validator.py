"""
ProjectGuard ML Dataset Validator (Phase 9)
Enforces validation rules for the 10 ML input features and actual outcome label.
"""
from typing import Dict, Any, List, Tuple
import pandas as pd

FEATURE_COLUMNS = [
    "student_skill_level",
    "skill_gap",
    "project_complexity",
    "team_size",
    "available_hours_per_week",
    "technology_familiarity",
    "number_of_requirements",
    "number_of_features",
    "previous_experience",
    "estimated_effort"
]

TARGET_COLUMN = "actual_outcome"
ALL_COLUMNS = FEATURE_COLUMNS + [TARGET_COLUMN]

VALID_COMPLEXITIES = {"EASY", "MEDIUM", "HARD"}
VALID_OUTCOMES = {"FEASIBLE", "NOT_FEASIBLE"}


def validate_record(record: Dict[str, Any]) -> Tuple[bool, List[str]]:
    """
    Validates a single student-project record according to Phase 9 rules.
    Returns (is_valid, list_of_errors).
    """
    errors: List[str] = []

    # 1. student_skill_level
    val = record.get("student_skill_level")
    if val is None or pd.isna(val):
        errors.append("student_skill_level is missing")
    else:
        try:
            num = float(val)
            if not (1.0 <= num <= 4.0):
                errors.append(f"student_skill_level ({num}) outside valid proficiency scale [1.0, 4.0]")
        except (ValueError, TypeError):
            errors.append("student_skill_level must be numeric")

    # 2. skill_gap
    val = record.get("skill_gap")
    if val is None or pd.isna(val):
        errors.append("skill_gap is missing")
    else:
        try:
            num = float(val)
            if not (0.0 <= num <= 25.0):
                errors.append(f"skill_gap ({num}) outside Phase 7 scale [0.0, 25.0]")
        except (ValueError, TypeError):
            errors.append("skill_gap must be numeric")

    # 3. project_complexity
    comp = record.get("project_complexity")
    if comp is None or pd.isna(comp):
        errors.append("project_complexity is missing")
    else:
        comp_str = str(comp).strip().upper()
        if comp_str not in VALID_COMPLEXITIES:
            errors.append(f"project_complexity '{comp_str}' is invalid; must be EASY, MEDIUM, or HARD")

    # 4. team_size
    ts = record.get("team_size")
    if ts is None or pd.isna(ts):
        errors.append("team_size is missing")
    else:
        try:
            num_ts = int(ts)
            if num_ts <= 0:
                errors.append(f"team_size ({num_ts}) must be a positive integer")
        except (ValueError, TypeError):
            errors.append("team_size must be an integer")

    # 5. available_hours_per_week
    ah = record.get("available_hours_per_week")
    if ah is None or pd.isna(ah):
        errors.append("available_hours_per_week is missing")
    else:
        try:
            num_ah = float(ah)
            if not (1.0 <= num_ah <= 80.0):
                errors.append(f"available_hours_per_week ({num_ah}) outside reasonable range [1.0, 80.0]")
        except (ValueError, TypeError):
            errors.append("available_hours_per_week must be numeric")

    # 6. technology_familiarity
    tf = record.get("technology_familiarity")
    if tf is None or pd.isna(tf):
        errors.append("technology_familiarity is missing")
    else:
        try:
            num_tf = float(tf)
            if not (1.0 <= num_tf <= 4.0):
                errors.append(f"technology_familiarity ({num_tf}) outside proficiency scale [1.0, 4.0]")
        except (ValueError, TypeError):
            errors.append("technology_familiarity must be numeric")

    # 7. number_of_requirements
    nr = record.get("number_of_requirements")
    if nr is None or pd.isna(nr):
        errors.append("number_of_requirements is missing")
    else:
        try:
            num_nr = int(nr)
            if num_nr < 0:
                errors.append("number_of_requirements must be non-negative")
        except (ValueError, TypeError):
            errors.append("number_of_requirements must be integer")

    # 8. number_of_features
    nf = record.get("number_of_features")
    if nf is None or pd.isna(nf):
        errors.append("number_of_features is missing")
    else:
        try:
            num_nf = int(nf)
            if num_nf < 0:
                errors.append("number_of_features must be non-negative")
        except (ValueError, TypeError):
            errors.append("number_of_features must be integer")

    # 9. previous_experience
    pe = record.get("previous_experience")
    if pe is None or pd.isna(pe):
        errors.append("previous_experience is missing")
    else:
        try:
            num_pe = float(pe)
            if num_pe < 0.0:
                errors.append("previous_experience must be non-negative number in years")
        except (ValueError, TypeError):
            errors.append("previous_experience must be numeric")

    # 10. estimated_effort
    ee = record.get("estimated_effort")
    if ee is None or pd.isna(ee):
        errors.append("estimated_effort is missing")
    else:
        try:
            num_ee = float(ee)
            if num_ee <= 0.0:
                errors.append("estimated_effort must be a positive number in hours")
        except (ValueError, TypeError):
            errors.append("estimated_effort must be numeric")

    # 11. actual_outcome
    outcome = record.get("actual_outcome")
    if outcome is None or pd.isna(outcome):
        errors.append("actual_outcome is missing or pending (PENDING_OUTCOME excluded from training)")
    else:
        out_str = str(outcome).strip().upper()
        if out_str == "PENDING_OUTCOME":
            errors.append("actual_outcome is PENDING_OUTCOME; cannot be used for supervised training")
        elif out_str not in VALID_OUTCOMES:
            errors.append(f"actual_outcome '{out_str}' is invalid; must be FEASIBLE or NOT_FEASIBLE")

    return (len(errors) == 0, errors)


def validate_dataframe(df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.DataFrame, Dict[str, Any]]:
    """
    Validates a pandas DataFrame containing dataset rows.
    Returns (valid_df, invalid_df, summary_stats).
    """
    valid_indices = []
    invalid_indices = []
    reasons = {}

    for idx, row in df.iterrows():
        is_val, errs = validate_record(row.to_dict())
        if is_val:
            valid_indices.append(idx)
        else:
            invalid_indices.append(idx)
            reasons[idx] = errs

    valid_df = df.loc[valid_indices].copy()
    invalid_df = df.loc[invalid_indices].copy()

    summary = {
        "total_records": len(df),
        "valid_records": len(valid_df),
        "invalid_records": len(invalid_df),
        "feasible_count": int((valid_df[TARGET_COLUMN].str.upper() == "FEASIBLE").sum()) if not valid_df.empty and TARGET_COLUMN in valid_df else 0,
        "not_feasible_count": int((valid_df[TARGET_COLUMN].str.upper() == "NOT_FEASIBLE").sum()) if not valid_df.empty and TARGET_COLUMN in valid_df else 0,
        "reasons": reasons
    }

    return valid_df, invalid_df, summary

