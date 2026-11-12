"""
ProjectGuard Dataset Manager (Phase 9)
High-level service orchestrating loading, validation, encoding, splitting, and CSV generation.
"""
from typing import Dict, Any, Tuple, Optional, List
import os
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


class DatasetManager:

    def __init__(self, data_dir: str = "data"):
        self.data_dir = data_dir
        os.makedirs(self.data_dir, exist_ok=True)

    def load_csv(self, filepath: str) -> pd.DataFrame:
        """Loads dataset CSV using pandas."""
        if not os.path.exists(filepath):
            raise FileNotFoundError(f"Dataset CSV not found at: {filepath}")
        return pd.read_csv(filepath, encoding="utf-8")

    def export_dataset_csv(self, df: pd.DataFrame, filename: str = "projectguard_ml_dataset.csv") -> str:
        """
        Exports training-ready DataFrame to CSV with exact 11 columns in documented order.
        """
        # Ensure exact column order
        cols_present = [c for c in ALL_COLUMNS if c in df.columns]
        export_df = df[cols_present].copy()

        output_path = os.path.join(self.data_dir, filename)
        export_df.to_csv(output_path, index=False, encoding="utf-8")
        return output_path

    def process_and_split(
        self,
        df: pd.DataFrame,
        group_col: Optional[str] = None,
        test_size: float = 0.20,
        random_state: int = 42
    ) -> Dict[str, Any]:
        """
        Validates, filters eligible records, and produces 80/20 train/test split.
        Saves projectguard_ml_dataset.csv and, when ready, projectguard_train.csv and projectguard_test.csv.
        """
        valid_df, invalid_df, val_summary = validate_dataframe(df)

        train_df, test_df, split_report = split_dataset(
            valid_df,
            test_size=test_size,
            random_state=random_state,
            group_col=group_col
        )

        files = {}
        if split_report.get("is_ready_for_training", False):
            train_path, test_path = export_splits_to_csv(train_df, test_df, self.data_dir)
            files["train_csv"] = train_path
            files["test_csv"] = test_path

        full_csv = self.export_dataset_csv(valid_df, "projectguard_ml_dataset.csv")
        files["dataset_csv"] = full_csv

        return {
            "validation_summary": val_summary,
            "split_report": split_report,
            "files": files
        }

