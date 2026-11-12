"""
ProjectGuard ML Model Registry (Phase 10)

Manages model persistence, versioning and activation.

Directory layout:
  ml-service/models/
    v1/
      model.joblib
      metadata.json
    v2/
      model.joblib
      metadata.json
    active_version.txt   ← points to the active version name, e.g. "v1"
"""
import json
import os
import re
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

import joblib  # type: ignore

MODELS_DIR = os.path.join(os.path.dirname(__file__), "..", "models")
ACTIVE_VERSION_FILE = "active_version.txt"
MODEL_FILE = "model.joblib"
METADATA_FILE = "metadata.json"


def _ensure_models_dir() -> str:
    models_dir = os.path.abspath(MODELS_DIR)
    os.makedirs(models_dir, exist_ok=True)
    return models_dir


def _version_int(version_name: str) -> int:
    """Returns the integer from a version string like 'v3' → 3."""
    match = re.match(r"v(\d+)$", version_name, re.IGNORECASE)
    return int(match.group(1)) if match else 0


class ModelRegistry:
    """
    Manages trained model files on disk.

    Each saved version is stored in its own sub-directory.
    The active version is tracked in active_version.txt.
    """

    def __init__(self, models_dir: Optional[str] = None) -> None:
        self.models_dir = os.path.abspath(models_dir or MODELS_DIR)
        os.makedirs(self.models_dir, exist_ok=True)

    # ── Internal helpers ───────────────────────────────────────────────────────

    def _version_dir(self, version: str) -> str:
        return os.path.join(self.models_dir, version)

    def _active_version_path(self) -> str:
        return os.path.join(self.models_dir, ACTIVE_VERSION_FILE)

    def _next_version(self) -> str:
        existing = self.list_versions()
        if not existing:
            return "v1"
        max_v = max(_version_int(v) for v in existing)
        return f"v{max_v + 1}"

    # ── Public API ─────────────────────────────────────────────────────────────

    def list_versions(self) -> List[str]:
        """Returns a sorted list of all saved version directories."""
        try:
            entries = [
                d for d in os.listdir(self.models_dir)
                if os.path.isdir(os.path.join(self.models_dir, d))
                and re.match(r"v\d+$", d, re.IGNORECASE)
            ]
            return sorted(entries, key=_version_int)
        except FileNotFoundError:
            return []

    def get_active_version(self) -> Optional[str]:
        """Returns the name of the currently active version, or None."""
        path = self._active_version_path()
        if not os.path.exists(path):
            return None
        with open(path, "r", encoding="utf-8") as f:
            return f.read().strip() or None

    def save_model(
        self,
        model: Any,
        metadata: Dict[str, Any],
        activate: bool = True,
    ) -> str:
        """
        Saves a fitted sklearn model and its metadata to a new version directory.

        Args:
            model: Fitted sklearn estimator.
            metadata: Arbitrary dict (algorithm, metrics, feature_order, etc.).
            activate: If True, sets this version as active after saving.

        Returns:
            Version string (e.g. "v1").
        """
        version = self._next_version()
        version_dir = self._version_dir(version)
        os.makedirs(version_dir, exist_ok=True)

        # Persist model
        model_path = os.path.join(version_dir, MODEL_FILE)
        joblib.dump(model, model_path)

        # Persist metadata
        metadata_to_save = {
            **metadata,
            "version": version,
            "saved_at": datetime.now(timezone.utc).isoformat(),
            "model_file": MODEL_FILE,
        }
        metadata_path = os.path.join(version_dir, METADATA_FILE)
        with open(metadata_path, "w", encoding="utf-8") as f:
            json.dump(metadata_to_save, f, indent=2, default=str)

        if activate:
            self.activate_version(version)

        return version

    def load_model(self, version: Optional[str] = None) -> Any:
        """
        Loads the model for the specified version, or the active version.

        Raises FileNotFoundError if no model is available.
        """
        target = version or self.get_active_version()
        if not target:
            raise FileNotFoundError("No active model version found in registry.")

        model_path = os.path.join(self._version_dir(target), MODEL_FILE)
        if not os.path.exists(model_path):
            raise FileNotFoundError(
                f"Model file not found for version '{target}': {model_path}"
            )
        return joblib.load(model_path)

    def load_metadata(self, version: Optional[str] = None) -> Dict[str, Any]:
        """Loads metadata for the specified or active version."""
        target = version or self.get_active_version()
        if not target:
            raise FileNotFoundError("No active model version found in registry.")

        metadata_path = os.path.join(self._version_dir(target), METADATA_FILE)
        if not os.path.exists(metadata_path):
            raise FileNotFoundError(
                f"Metadata not found for version '{target}': {metadata_path}"
            )
        with open(metadata_path, "r", encoding="utf-8") as f:
            return json.load(f)

    def activate_version(self, version: str) -> None:
        """Sets the given version as the active model."""
        version_dir = self._version_dir(version)
        if not os.path.isdir(version_dir):
            raise ValueError(
                f"Cannot activate version '{version}': directory does not exist."
            )
        active_path = self._active_version_path()
        with open(active_path, "w", encoding="utf-8") as f:
            f.write(version)

    def get_model_status(self) -> Dict[str, Any]:
        """
        Returns whether any active model exists, its version, metrics, and
        training timestamp. Safe to call when no model has been trained yet.
        """
        active_version = self.get_active_version()
        all_versions = self.list_versions()

        if not active_version:
            return {
                "model_available": False,
                "active_version": None,
                "all_versions": all_versions,
                "algorithm": None,
                "metrics": None,
                "trained_at": None,
                "dataset_fingerprint": None,
                "message": "No trained model is available. Training has not been completed.",
            }

        try:
            metadata = self.load_metadata(active_version)
        except FileNotFoundError:
            return {
                "model_available": False,
                "active_version": active_version,
                "all_versions": all_versions,
                "algorithm": None,
                "metrics": None,
                "trained_at": None,
                "dataset_fingerprint": None,
                "message": f"Metadata missing for active version '{active_version}'.",
            }

        return {
            "model_available": True,
            "active_version": active_version,
            "all_versions": all_versions,
            "algorithm": metadata.get("algorithm"),
            "metrics": metadata.get("metrics"),
            "trained_at": metadata.get("training_timestamp") or metadata.get("saved_at"),
            "dataset_fingerprint": metadata.get("dataset_fingerprint"),
            "hyperparameters": metadata.get("hyperparameters"),
            "feature_order": metadata.get("feature_order"),
            "message": f"Active model: {metadata.get('algorithm', 'unknown')} version {active_version}.",
        }

    def delete_version(self, version: str) -> None:
        """
        Removes a model version directory. Refuses to delete the active version.
        """
        active = self.get_active_version()
        if version == active:
            raise ValueError(
                f"Cannot delete the active version '{version}'. Activate another version first."
            )
        import shutil
        version_dir = self._version_dir(version)
        if os.path.isdir(version_dir):
            shutil.rmtree(version_dir)

