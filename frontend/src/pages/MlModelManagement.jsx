import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import mlModelService from '../services/mlModelService';

const MlModelManagement = () => {
    const [modelStatus, setModelStatus] = useState(null);
    const [metrics, setMetrics] = useState(null);
    const [versions, setVersions] = useState([]);
    const [trainingResult, setTrainingResult] = useState(null);
    const [loading, setLoading] = useState(true);
    const [trainingLoading, setTrainingLoading] = useState(false);
    const [activatingVersion, setActivatingVersion] = useState(null);
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);

    const loadData = async () => {
        setLoading(true);
        setError(null);
        try {
            const [statusRes, versionsRes] = await Promise.all([
                mlModelService.getModelStatus().catch(err => ({ data: { modelAvailable: false, message: err.message } })),
                mlModelService.listVersions().catch(() => ({ data: { versions: [] } }))
            ]);

            setModelStatus(statusRes.data);
            setVersions(versionsRes.data?.versions || []);

            if (statusRes.data?.modelAvailable) {
                try {
                    const metricsRes = await mlModelService.getModelMetrics();
                    setMetrics(metricsRes.data);
                } catch {
                    setMetrics(null);
                }
            } else {
                setMetrics(null);
            }
        } catch (err) {
            setError('Failed to load ML model status: ' + (err.response?.data?.message || err.message));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleTrainModel = async () => {
        setTrainingLoading(true);
        setMessage(null);
        setError(null);
        setTrainingResult(null);

        try {
            const res = await mlModelService.trainModel();
            setTrainingResult(res.data);

            if (res.data?.status === 'TRAINING_COMPLETE') {
                setMessage(res.data.message || 'Model training completed and activated successfully!');
                await loadData();
            } else if (res.data?.status === 'TRAINING_BLOCKED') {
                setMessage(res.data.message || 'Training blocked by dataset readiness gate.');
            }
        } catch (err) {
            setError('Failed to trigger training: ' + (err.response?.data?.message || err.message));
        } finally {
            setTrainingLoading(false);
        }
    };

    const handleActivateVersion = async (version) => {
        setActivatingVersion(version);
        setMessage(null);
        setError(null);
        try {
            await mlModelService.activateVersion(version);
            setMessage(`Model version '${version}' is now active.`);
            await loadData();
        } catch (err) {
            setError(`Failed to activate version '${version}': ` + (err.response?.data?.message || err.message));
        } finally {
            setActivatingVersion(null);
        }
    };

    if (loading) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-4">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                <p className="text-gray-500 font-medium">Loading ML Model Engine...</p>
            </div>
        );
    }

    const isModelAvailable = modelStatus?.modelAvailable;
    const activeVersion = modelStatus?.activeVersion;
    const algorithm = modelStatus?.algorithm;

    return (
        <div className="space-y-8 pb-12">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b pb-6">
                <div>
                    <div className="flex items-center gap-2">
                        <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-indigo-100 text-indigo-800">Phase 10</span>
                        <h1 className="text-2xl font-bold text-gray-900">ML Model Development & Evaluation</h1>
                    </div>
                    <p className="text-gray-600 text-sm mt-1">
                        Supervised binary classification (Random Forest vs Gradient Boosting) for project feasibility prediction.
                    </p>
                </div>
                <div className="flex items-center gap-3">
                    <Link
                        to="/predict"
                        className="px-4 py-2 border border-indigo-200 text-indigo-700 hover:bg-indigo-50 font-semibold rounded-xl text-sm transition"
                    >
                        🎯 Feasibility Predictor
                    </Link>
                    <Link
                        to="/admin/ml-dataset"
                        className="px-4 py-2 border border-gray-200 text-gray-700 hover:bg-gray-50 font-semibold rounded-xl text-sm transition"
                    >
                        📊 ML Dataset Studio
                    </Link>
                    <button
                        onClick={handleTrainModel}
                        disabled={trainingLoading}
                        className="px-4 py-2 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-semibold rounded-xl text-sm transition shadow-sm disabled:opacity-50 flex items-center gap-2"
                    >
                        {trainingLoading ? (
                            <>
                                <span className="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
                                Evaluating & Training...
                            </>
                        ) : (
                            <>🚀 Run Model Training</>
                        )}
                    </button>
                </div>
            </div>

            {/* Notification messages */}
            {message && (
                <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <span>ℹ️</span>
                        <span>{message}</span>
                    </div>
                    <button onClick={() => setMessage(null)} className="text-emerald-600 font-bold hover:underline">✕</button>
                </div>
            )}
            {error && (
                <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <span>⚠️</span>
                        <span>{error}</span>
                    </div>
                    <button onClick={() => setError(null)} className="text-rose-600 font-bold hover:underline">✕</button>
                </div>
            )}

            {/* Active Model Status Card */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-3">
                        <div className={`h-3 w-3 rounded-full ${isModelAvailable ? 'bg-emerald-500 animate-pulse' : 'bg-amber-400'}`} />
                        <h2 className="text-lg font-bold text-gray-900">
                            Active Model Status:{' '}
                            <span className={isModelAvailable ? 'text-emerald-700' : 'text-amber-700'}>
                                {isModelAvailable ? `${algorithm} (${activeVersion})` : 'No Production Model Trained'}
                            </span>
                        </h2>
                    </div>
                    <span className={`px-3 py-1 rounded-full text-xs font-bold ${isModelAvailable ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
                        {isModelAvailable ? 'ACTIVE' : 'PENDING DATASET'}
                    </span>
                </div>

                <p className="text-sm text-gray-600 mb-6">
                    {modelStatus?.message || 'Readiness gate requires verified authentic student-project outcome records before training.'}
                </p>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <span className="text-xs font-semibold text-gray-500">Active Version</span>
                        <p className="text-xl font-bold text-gray-900 mt-1">{activeVersion || '—'}</p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <span className="text-xs font-semibold text-gray-500">Algorithm</span>
                        <p className="text-xl font-bold text-gray-900 mt-1">{algorithm || '—'}</p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <span className="text-xs font-semibold text-gray-500">F1-Weighted Score</span>
                        <p className="text-xl font-bold text-indigo-600 mt-1">
                            {modelStatus?.metrics?.f1_weighted ? (modelStatus.metrics.f1_weighted * 100).toFixed(1) + '%' : '—'}
                        </p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <span className="text-xs font-semibold text-gray-500">Total Versions</span>
                        <p className="text-xl font-bold text-gray-900 mt-1">{versions.length}</p>
                    </div>
                </div>
            </div>

            {/* Dataset Readiness Gate Section */}
            <div className="bg-gradient-to-br from-slate-50 to-indigo-50/30 rounded-2xl border border-indigo-100 shadow-sm p-6">
                <div className="flex items-center gap-3 mb-4">
                    <span className="text-2xl">🛡️</span>
                    <div>
                        <h2 className="text-lg font-bold text-gray-900">Dataset Readiness Gate</h2>
                        <p className="text-xs text-gray-500">Requirements that must be satisfied before model training can execute</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                    <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-2xs">
                        <p className="text-xs font-semibold text-gray-500">Minimum Verified Records</p>
                        <p className="text-2xl font-black text-gray-900 mt-1">50</p>
                        <p className="text-xs text-gray-500 mt-1">Minimum pilot threshold for initial development</p>
                    </div>
                    <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-2xs">
                        <p className="text-xs font-semibold text-gray-500">Target Representation</p>
                        <p className="text-2xl font-black text-gray-900 mt-1">Both Classes</p>
                        <p className="text-xs text-gray-500 mt-1">Must contain both FEASIBLE and NOT_FEASIBLE examples</p>
                    </div>
                    <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-2xs">
                        <p className="text-xs font-semibold text-gray-500">Data Integrity</p>
                        <p className="text-2xl font-black text-gray-900 mt-1">Authentic Only</p>
                        <p className="text-xs text-gray-500 mt-1">Pending outcomes, illustrative test data, and rejected records blocked</p>
                    </div>
                </div>

                {trainingResult && trainingResult.status === 'TRAINING_BLOCKED' && (
                    <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 text-xs leading-relaxed">
                        <p className="font-bold text-sm text-amber-800 mb-1">⚠️ Training Blocked by Readiness Gate</p>
                        <p>{trainingResult.message}</p>
                        <div className="mt-2 flex gap-4 font-semibold">
                            <span>Available: {trainingResult.availableRecords ?? 0}</span>
                            <span>Required: {trainingResult.requiredRecords ?? 50}</span>
                            <span>Needed: {trainingResult.recordsNeeded ?? 50}</span>
                        </div>
                    </div>
                )}
            </div>

            {/* Model Comparison Table (if metrics available) */}
            {metrics?.comparison && (
                <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h2 className="text-lg font-bold text-gray-900">Model Evaluation & Comparison</h2>
                            <p className="text-xs text-gray-500">Random Forest Classifier vs Gradient Boosting Classifier on held-out 20% test set</p>
                        </div>
                        <span className="text-xs text-gray-400">Stratified 80/20 split (seed=42)</span>
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm border-collapse">
                            <thead>
                                <tr className="bg-gray-50 border-b border-gray-200 text-gray-600 font-semibold text-xs">
                                    <th className="py-3 px-4">Model Algorithm</th>
                                    <th className="py-3 px-4">Accuracy</th>
                                    <th className="py-3 px-4">Precision (Weighted)</th>
                                    <th className="py-3 px-4">Recall (Weighted)</th>
                                    <th className="py-3 px-4">F1-Score (Weighted)</th>
                                    <th className="py-3 px-4">ROC-AUC</th>
                                    <th className="py-3 px-4">Status</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {Object.entries(metrics.comparison).map(([name, m]) => {
                                    const isBest = name === metrics.algorithm;
                                    return (
                                        <tr key={name} className={isBest ? 'bg-indigo-50/40 font-medium' : ''}>
                                            <td className="py-3 px-4 flex items-center gap-2">
                                                {isBest && <span className="text-amber-500">★</span>}
                                                <span className="font-bold text-gray-900">{name}</span>
                                            </td>
                                            <td className="py-3 px-4">{(m.accuracy * 100).toFixed(1)}%</td>
                                            <td className="py-3 px-4">{(m.precision_weighted * 100).toFixed(1)}%</td>
                                            <td className="py-3 px-4">{(m.recall_weighted * 100).toFixed(1)}%</td>
                                            <td className="py-3 px-4 font-bold text-indigo-700">{(m.f1_weighted * 100).toFixed(1)}%</td>
                                            <td className="py-3 px-4">{m.roc_auc !== null ? m.roc_auc.toFixed(3) : 'N/A'}</td>
                                            <td className="py-3 px-4">
                                                {isBest ? (
                                                    <span className="px-2 py-0.5 rounded-full text-2xs font-bold bg-emerald-100 text-emerald-800">
                                                        SELECTED (BEST F1)
                                                    </span>
                                                ) : (
                                                    <span className="text-xs text-gray-400">Evaluated</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>

                    {/* Feature Importance & Confusion Matrix */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4 border-t">
                        {/* Feature Importance */}
                        <div>
                            <h3 className="text-sm font-bold text-gray-800 mb-3">Feature Importance (Top Predictors)</h3>
                            <div className="space-y-2">
                                {metrics?.metrics?.feature_importance &&
                                    Object.entries(metrics.metrics.feature_importance)
                                        .sort((a, b) => b[1] - a[1])
                                        .map(([feat, imp]) => (
                                            <div key={feat} className="flex items-center gap-3 text-xs">
                                                <span className="w-44 text-gray-600 truncate">{feat}</span>
                                                <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                                                    <div
                                                        className="h-full bg-indigo-500 rounded-full"
                                                        style={{ width: `${Math.min(100, imp * 100)}%` }}
                                                    />
                                                </div>
                                                <span className="w-12 text-right font-semibold text-gray-700">
                                                    {(imp * 100).toFixed(1)}%
                                                </span>
                                            </div>
                                        ))}
                            </div>
                        </div>

                        {/* Confusion Matrix */}
                        <div>
                            <h3 className="text-sm font-bold text-gray-800 mb-3">Confusion Matrix (Held-out Test Set)</h3>
                            {metrics?.metrics?.confusion_matrix ? (
                                <div className="border border-gray-200 rounded-xl p-4 bg-gray-50/50">
                                    <div className="grid grid-cols-2 gap-3 text-center text-xs">
                                        <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-3">
                                            <p className="text-2xs text-emerald-600 font-semibold uppercase">True Negative (NOT_FEASIBLE)</p>
                                            <p className="text-2xl font-black text-emerald-800 mt-1">
                                                {metrics.metrics.confusion_matrix[0]?.[0] ?? 0}
                                            </p>
                                        </div>
                                        <div className="bg-rose-50 border border-rose-200 rounded-lg p-3">
                                            <p className="text-2xs text-rose-600 font-semibold uppercase">False Positive (Type I)</p>
                                            <p className="text-2xl font-black text-rose-800 mt-1">
                                                {metrics.metrics.confusion_matrix[0]?.[1] ?? 0}
                                            </p>
                                        </div>
                                        <div className="bg-amber-50 border border-amber-200 rounded-lg p-3">
                                            <p className="text-2xs text-amber-600 font-semibold uppercase">False Negative (Type II)</p>
                                            <p className="text-2xl font-black text-amber-800 mt-1">
                                                {metrics.metrics.confusion_matrix[1]?.[0] ?? 0}
                                            </p>
                                        </div>
                                        <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-3">
                                            <p className="text-2xs text-emerald-600 font-semibold uppercase">True Positive (FEASIBLE)</p>
                                            <p className="text-2xl font-black text-emerald-800 mt-1">
                                                {metrics.metrics.confusion_matrix[1]?.[1] ?? 0}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <p className="text-xs text-gray-400">No confusion matrix recorded.</p>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Model Version Registry */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h2 className="text-lg font-bold text-gray-900">Saved Model Versions Registry</h2>
                        <p className="text-xs text-gray-500">Persistent joblib model artifacts saved in ml-service/models/</p>
                    </div>
                    <span className="text-xs font-semibold text-gray-400">{versions.length} versions recorded</span>
                </div>

                {versions.length === 0 ? (
                    <div className="text-center py-8 text-gray-400 text-sm">
                        No model versions saved yet. Run model training when verified dataset records are ready.
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm border-collapse">
                            <thead>
                                <tr className="bg-gray-50 border-b border-gray-200 text-gray-600 font-semibold text-xs">
                                    <th className="py-3 px-4">Version</th>
                                    <th className="py-3 px-4">Algorithm</th>
                                    <th className="py-3 px-4">F1-Score</th>
                                    <th className="py-3 px-4">Accuracy</th>
                                    <th className="py-3 px-4">Trained Timestamp</th>
                                    <th className="py-3 px-4 text-right">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {versions.map((v) => (
                                    <tr key={v.version} className={v.is_active ? 'bg-emerald-50/30' : ''}>
                                        <td className="py-3 px-4 font-bold text-gray-900 flex items-center gap-2">
                                            {v.version}
                                            {v.is_active && (
                                                <span className="px-2 py-0.5 rounded-full text-2xs font-bold bg-emerald-100 text-emerald-800">
                                                    ACTIVE
                                                </span>
                                            )}
                                        </td>
                                        <td className="py-3 px-4 text-gray-700">{v.algorithm || '—'}</td>
                                        <td className="py-3 px-4 font-semibold text-indigo-700">
                                            {v.f1_weighted ? (v.f1_weighted * 100).toFixed(1) + '%' : '—'}
                                        </td>
                                        <td className="py-3 px-4 text-gray-600">
                                            {v.accuracy ? (v.accuracy * 100).toFixed(1) + '%' : '—'}
                                        </td>
                                        <td className="py-3 px-4 text-xs text-gray-500">{v.saved_at || '—'}</td>
                                        <td className="py-3 px-4 text-right">
                                            {!v.is_active && (
                                                <button
                                                    onClick={() => handleActivateVersion(v.version)}
                                                    disabled={activatingVersion === v.version}
                                                    className="px-3 py-1 bg-gray-100 hover:bg-indigo-50 hover:text-indigo-700 text-gray-700 font-semibold rounded-lg text-xs transition disabled:opacity-50"
                                                >
                                                    {activatingVersion === v.version ? 'Activating...' : 'Activate'}
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export default MlModelManagement;

