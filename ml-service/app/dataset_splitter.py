"""
ProjectGuard 80/20 Dataset Splitter (Phase 9)
Prepares reproducible training and testing splits:
- 80% train, 20% test
- Random seed = 42 for reproducibility
- Stratified sampling when class distribution permits (>=2 per class)
- Group-aware splitting when student identifier is available to avoid student leakage
- Handles insufficient data cleanly without artificial label manufacturing
"""
from typing import Dict, Any, Tuple, Optional
import os
import pandas as pd
import numpy as np

MIN_RECORDS_FOR_SPLIT = 10
MIN_PER_CLASS_FOR_STRATIFY = 2


def split_dataset(
    df: pd.DataFrame,
    test_size: float = 0.20,
    random_state: int = 42,
    group_col: Optional[str] = None
) -> Tuple[pd.DataFrame, pd.DataFrame, Dict[str, Any]]:
    """
    Performs 80/20 train/test split on verified, labelled records.
    Returns (train_df, test_df, split_report).
    """
    report: Dict[str, Any] = {
        "total_records": len(df),
        "test_size_ratio": test_size,
        "train_size_ratio": 1.0 - test_size,
        "random_state": random_state,
        "is_ready_for_training": False,
        "message": ""
    }

    if df.empty or len(df) < MIN_RECORDS_FOR_SPLIT:
        report["message"] = (
            f"Dataset contains {len(df)} records, which is below the minimum required "
            f"({MIN_RECORDS_FOR_SPLIT}) for a reliable 80/20 train/test split. "
            "Model training cannot proceed until sufficient verified outcomes are recorded."
        )
        return pd.DataFrame(columns=df.columns), pd.DataFrame(columns=df.columns), report

    target_col = "actual_outcome"
    if target_col not in df.columns:
        report["message"] = f"Missing target column '{target_col}' in dataset."
        return pd.DataFrame(columns=df.columns), pd.DataFrame(columns=df.columns), report

    # Check class counts
    class_counts = df[target_col].astype(str).str.upper().value_counts().to_dict()
    feasible_count = class_counts.get("FEASIBLE", 0)
    not_feasible_count = class_counts.get("NOT_FEASIBLE", 0)

    report["feasible_count"] = feasible_count
    report["not_feasible_count"] = not_feasible_count

    if feasible_count < MIN_PER_CLASS_FOR_STRATIFY or not_feasible_count < MIN_PER_CLASS_FOR_STRATIFY:
        report["message"] = (
            f"Class imbalance constraint violated: FEASIBLE={feasible_count}, NOT_FEASIBLE={not_feasible_count}. "
            f"At least {MIN_PER_CLASS_FOR_STRATIFY} examples of each outcome are required for reliable stratified splitting."
        )
        return pd.DataFrame(columns=df.columns), pd.DataFrame(columns=df.columns), report

    rng = np.random.RandomState(random_state)

    # Group-aware splitting if group_col exists
    if group_col and group_col in df.columns:
        groups = df[group_col].unique()
        rng.shuffle(groups)
        n_test_groups = max(1, int(round(len(groups) * test_size)))
        test_groups = set(groups[:n_test_groups])

        train_mask = ~df[group_col].isin(test_groups)
        test_mask = df[group_col].isin(test_groups)

        train_df = df[train_mask].copy()
        test_df = df[test_mask].copy()
        report["splitting_strategy"] = f"Group-aware shuffle by '{group_col}' to prevent data leakage"
    else:
        # Stratified sampling by outcome
        shuffled = df.sample(frac=1.0, random_state=random_state).reset_index(drop=True)
        feasible_df = shuffled[shuffled[target_col].str.upper() == "FEASIBLE"]
        not_feasible_df = shuffled[shuffled[target_col].str.upper() == "NOT_FEASIBLE"]

        n_feas_test = max(1, int(round(len(feasible_df) * test_size)))
        n_not_feas_test = max(1, int(round(len(not_feasible_df) * test_size)))

        test_feas = feasible_df.iloc[:n_feas_test]
        train_feas = feasible_df.iloc[n_feas_test:]

        test_not_feas = not_feasible_df.iloc[:n_not_feas_test]
        train_not_feas = not_feasible_df.iloc[n_not_feas_test:]

        train_df = pd.concat([train_feas, train_not_feas]).sample(frac=1.0, random_state=random_state).reset_index(drop=True)
        test_df = pd.concat([test_feas, test_not_feas]).sample(frac=1.0, random_state=random_state).reset_index(drop=True)
        report["splitting_strategy"] = "Stratified sampling by actual_outcome"

    report["is_ready_for_training"] = True
    report["train_records"] = len(train_df)
    report["test_records"] = len(test_df)
    report["train_feasible"] = int((train_df[target_col].str.upper() == "FEASIBLE").sum())
    report["train_not_feasible"] = int((train_df[target_col].str.upper() == "NOT_FEASIBLE").sum())
    report["test_feasible"] = int((test_df[target_col].str.upper() == "FEASIBLE").sum())
    report["test_not_feasible"] = int((test_df[target_col].str.upper() == "NOT_FEASIBLE").sum())
    report["message"] = f"Successfully generated 80/20 stratified split: {len(train_df)} train, {len(test_df)} test."

    return train_df, test_df, report


def export_splits_to_csv(train_df: pd.DataFrame, test_df: pd.DataFrame, output_dir: str) -> Tuple[str, str]:
    """
    Exports train and test dataframes to projectguard_train.csv and projectguard_test.csv.
    """
    os.makedirs(output_dir, exist_ok=True)
    train_path = os.path.join(output_dir, "projectguard_train.csv")
    test_path = os.path.join(output_dir, "projectguard_test.csv")

    train_df.to_csv(train_path, index=False, encoding="utf-8")
    test_df.to_csv(test_path, index=False, encoding="utf-8")

    return train_path, test_path

