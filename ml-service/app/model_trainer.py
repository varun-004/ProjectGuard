"""
ProjectGuard ML Model Trainer (Phase 10)

Implements and compares two binary classifiers:
  - Model A: Random Forest Classifier
  - Model B: Gradient Boosting Classifier

Target: FEASIBLE (1) / NOT_FEASIBLE (0)

Training gate: Minimum 50 authentic verified records with both classes present.
This is a development threshold, not a statistical guarantee.
"""
from typing import Dict, Any, List, Optional, Tuple
import numpy as np
import pandas as pd

from app.dataset_preprocessor import get_feature_matrix

# ── Training gate constants ────────────────────────────────────────────────────
MIN_TRAINING_RECORDS: int = 50
MIN_CLASS_COUNT: int = 5        # minimum examples per class
RANDOM_STATE: int = 42

FEATURE_ORDER: List[str] = [
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


# ── Dataset readiness ──────────────────────────────────────────────────────────

def check_dataset_readiness(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Returns a readiness report before training.

    Returns is_ready=True only when:
      - Total authentic record count ≥ MIN_TRAINING_RECORDS
      - Both target classes are present with at least MIN_CLASS_COUNT examples
    """
    total = len(df)
    blocked_reasons: List[str] = []

    if total < MIN_TRAINING_RECORDS:
        blocked_reasons.append(
            f"Insufficient records: {total} available, {MIN_TRAINING_RECORDS} required."
        )

    # Class distribution
    class_distribution: Dict[str, int] = {}
    if "actual_outcome" in df.columns:
        counts = df["actual_outcome"].value_counts().to_dict()
        class_distribution = {str(k): int(v) for k, v in counts.items()}

        for cls in ["FEASIBLE", "NOT_FEASIBLE"]:
            count = class_distribution.get(cls, 0)
            if count < MIN_CLASS_COUNT:
                blocked_reasons.append(
                    f"Class '{cls}' has only {count} examples; need at least {MIN_CLASS_COUNT}."
                )
    else:
        blocked_reasons.append("Target column 'actual_outcome' is missing from dataset.")

    is_ready = len(blocked_reasons) == 0

    return {
        "is_ready": is_ready,
        "total_records": total,
        "required_records": MIN_TRAINING_RECORDS,
        "records_needed": max(0, MIN_TRAINING_RECORDS - total),
        "class_distribution": class_distribution,
        "min_class_count_required": MIN_CLASS_COUNT,
        "blocked_reasons": blocked_reasons,
        "message": (
            "Dataset is ready for training."
            if is_ready
            else f"Training blocked: {'; '.join(blocked_reasons)}"
        ),
    }


# ── Model training ─────────────────────────────────────────────────────────────

def train_random_forest(
    X_train: pd.DataFrame,
    y_train: pd.Series,
    params: Optional[Dict[str, Any]] = None,
) -> Any:
    """
    Trains a Random Forest Classifier.

    Default hyperparameters chosen for a small dataset with class imbalance.
    class_weight='balanced' compensates for unequal class frequencies.
    """
    from sklearn.ensemble import RandomForestClassifier  # type: ignore

    defaults: Dict[str, Any] = {
        "n_estimators": 100,
        "max_depth": None,
        "min_samples_split": 2,
        "min_samples_leaf": 1,
        "class_weight": "balanced",
        "random_state": RANDOM_STATE,
        "n_jobs": 1,
    }
    if params:
        defaults.update(params)

    clf = RandomForestClassifier(**defaults)
    clf.fit(X_train, y_train)
    return clf


def train_gradient_boosting(
    X_train: pd.DataFrame,
    y_train: pd.Series,
    params: Optional[Dict[str, Any]] = None,
) -> Any:
    """
    Trains a Gradient Boosting Classifier.

    Uses subsample < 1 for stochastic boosting, which helps with small datasets.
    """
    from sklearn.ensemble import GradientBoostingClassifier  # type: ignore

    defaults: Dict[str, Any] = {
        "n_estimators": 100,
        "learning_rate": 0.1,
        "max_depth": 3,
        "subsample": 0.8,
        "random_state": RANDOM_STATE,
    }
    if params:
        defaults.update(params)

    clf = GradientBoostingClassifier(**defaults)
    clf.fit(X_train, y_train)
    return clf


# ── Model evaluation ───────────────────────────────────────────────────────────

def evaluate_model(
    clf: Any,
    X_test: pd.DataFrame,
    y_test: pd.Series,
    model_name: str,
) -> Dict[str, Any]:
    """
    Evaluates a fitted classifier on the held-out test set.

    Returns:
      accuracy, precision, recall, f1 (all weighted), roc_auc (when applicable),
      confusion matrix, per-class report, and actual vs predicted labels.
    """
    from sklearn.metrics import (  # type: ignore
        accuracy_score,
        precision_score,
        recall_score,
        f1_score,
        confusion_matrix,
        classification_report,
        roc_auc_score,
    )

    y_pred = clf.predict(X_test)
    y_pred_proba = None
    roc_auc = None

    # ROC-AUC requires probability estimates and both classes in test set
    unique_test_classes = set(y_test.unique())
    if hasattr(clf, "predict_proba") and len(unique_test_classes) == 2:
        try:
            y_pred_proba = clf.predict_proba(X_test)[:, 1]
            roc_auc = float(roc_auc_score(y_test, y_pred_proba))
        except Exception:
            roc_auc = None

    cm = confusion_matrix(y_test, y_pred).tolist()
    report = classification_report(y_test, y_pred, output_dict=True, zero_division=0)

    metrics: Dict[str, Any] = {
        "model_name": model_name,
        "accuracy": float(accuracy_score(y_test, y_pred)),
        "precision_weighted": float(
            precision_score(y_test, y_pred, average="weighted", zero_division=0)
        ),
        "recall_weighted": float(
            recall_score(y_test, y_pred, average="weighted", zero_division=0)
        ),
        "f1_weighted": float(
            f1_score(y_test, y_pred, average="weighted", zero_division=0)
        ),
        "roc_auc": roc_auc,
        "confusion_matrix": cm,
        "per_class_report": report,
        "test_set_size": len(y_test),
        "class_distribution_test": {
            str(k): int(v) for k, v in zip(*np.unique(y_test, return_counts=True))
        },
        "actual_labels": y_test.tolist(),
        "predicted_labels": y_pred.tolist(),
    }

    # Feature importance (RF and GB both support this)
    if hasattr(clf, "feature_importances_"):
        importances = clf.feature_importances_
        metrics["feature_importance"] = {
            feat: float(imp)
            for feat, imp in zip(FEATURE_ORDER, importances)
        }

    return metrics


def compare_models(
    models: Dict[str, Any],
    X_test: pd.DataFrame,
    y_test: pd.Series,
) -> Dict[str, Any]:
    """
    Evaluates all models and returns a comparison dictionary.
    """
    evaluations: Dict[str, Any] = {}
    for name, clf in models.items():
        evaluations[name] = evaluate_model(clf, X_test, y_test, name)
    return evaluations


def select_best_model(comparison: Dict[str, Any]) -> str:
    """
    Selects the best model by weighted F1-score.
    Ties are broken by ROC-AUC, then accuracy.
    Does NOT select solely on accuracy.
    """
    best_name = ""
    best_f1 = -1.0
    best_auc = -1.0

    for name, metrics in comparison.items():
        f1 = metrics.get("f1_weighted", 0.0)
        auc = metrics.get("roc_auc") or 0.0
        if f1 > best_f1 or (f1 == best_f1 and auc > best_auc):
            best_f1 = f1
            best_auc = auc
            best_name = name

    return best_name


# ── Full training pipeline ─────────────────────────────────────────────────────

def run_training_pipeline(
    df: pd.DataFrame,
    rf_params: Optional[Dict[str, Any]] = None,
    gb_params: Optional[Dict[str, Any]] = None,
    test_size: float = 0.20,
) -> Dict[str, Any]:
    """
    End-to-end training pipeline:
      1. Readiness gate
      2. Feature encoding
      3. Train/test split
      4. Train RF + GBM
      5. Evaluate and compare
      6. Return results (models + metrics); persistence is handled by ModelRegistry
    """
    from sklearn.model_selection import train_test_split  # type: ignore

    readiness = check_dataset_readiness(df)
    if not readiness["is_ready"]:
        return {
            "status": "TRAINING_BLOCKED",
            "readiness": readiness,
            "models": {},
            "comparison": {},
            "best_model": None,
        }

    X, y = get_feature_matrix(df)

    # Drop rows where X or y has NaN (should not happen after validation but safety check)
    valid_mask = X.notna().all(axis=1) & y.notna()
    X = X[valid_mask]
    y = y[valid_mask]

    if len(X) < MIN_TRAINING_RECORDS:
        readiness["is_ready"] = False
        readiness["blocked_reasons"].append(
            f"After removing NaN rows, only {len(X)} valid records remain."
        )
        return {
            "status": "TRAINING_BLOCKED",
            "readiness": readiness,
            "models": {},
            "comparison": {},
            "best_model": None,
        }

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=RANDOM_STATE, stratify=y
    )

    rf_model = train_random_forest(X_train, y_train, rf_params)
    gb_model = train_gradient_boosting(X_train, y_train, gb_params)

    models = {
        "RandomForest": rf_model,
        "GradientBoosting": gb_model,
    }

    comparison = compare_models(models, X_test, y_test)
    best_name = select_best_model(comparison)

    return {
        "status": "TRAINING_COMPLETE",
        "readiness": readiness,
        "models": models,          # fitted sklearn estimators (not serialized here)
        "comparison": comparison,
        "best_model": best_name,
        "train_size": len(X_train),
        "test_size": len(X_test),
    }

