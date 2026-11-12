import React, { useState, useEffect, useCallback } from 'react';
import mlDatasetService from '../services/mlDatasetService';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const complexityBadge = (comp) => {
    switch (comp?.toUpperCase()) {
        case 'HARD':
        case 'ADVANCED':
            return 'bg-purple-100 text-purple-700 border-purple-200';
        case 'MEDIUM':
        case 'INTERMEDIATE':
            return 'bg-blue-100 text-blue-700 border-blue-200';
        case 'EASY':
        case 'BEGINNER':
            return 'bg-emerald-100 text-emerald-700 border-emerald-200';
        default:
            return 'bg-gray-100 text-gray-700 border-gray-200';
    }
};

const outcomeBadge = (outcome) => {
    switch (outcome?.toUpperCase()) {
        case 'FEASIBLE':
            return 'bg-emerald-100 text-emerald-800 border-emerald-300 font-bold';
        case 'NOT_FEASIBLE':
            return 'bg-rose-100 text-rose-800 border-rose-300 font-bold';
        case 'PENDING_OUTCOME':
        default:
            return 'bg-amber-100 text-amber-800 border-amber-300 font-medium';
    }
};

const valStatusBadge = (status) => {
    switch (status?.toUpperCase()) {
        case 'VALIDATED':
            return 'bg-emerald-50 text-emerald-700 border-emerald-200';
        case 'REJECTED':
            return 'bg-red-50 text-red-700 border-red-200';
        case 'INCOMPLETE':
            return 'bg-orange-50 text-orange-700 border-orange-200';
        case 'PENDING':
        default:
            return 'bg-slate-50 text-slate-700 border-slate-200';
    }
};

// ─── Stat Card ────────────────────────────────────────────────────────────────

const StatCard = ({ title, value, sub, icon, bgGradient = 'from-slate-50 to-white' }) => (
    <div className={`p-5 rounded-2xl border border-gray-100 bg-gradient-to-br ${bgGradient} shadow-sm transition-all hover:shadow-md`}>
        <div className="flex items-center justify-between">
            <span className="text-2xl">{icon}</span>
            <span className="text-2xl font-black text-slate-800">{value ?? 0}</span>
        </div>
        <p className="mt-2 text-sm font-semibold text-slate-700">{title}</p>
        {sub && <p className="text-xs text-slate-400 mt-0.5">{sub}</p>}
    </div>
);

// ─── Outcome Evaluation Modal ─────────────────────────────────────────────────

const EvaluateOutcomeModal = ({ record, onClose, onEvaluated }) => {
    const [outcome, setOutcome] = useState('FEASIBLE');
    const [evidence, setEvidence] = useState('');
    const [notes, setNotes] = useState('');
    const [confirmed, setConfirmed] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');

    if (!record) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!confirmed) {
            setError('Please confirm that this evaluation is based on actual project evidence.');
            return;
        }
        setIsSubmitting(true);
        setError('');
        try {
            await mlDatasetService.recordActualOutcome(record.id, {
                actualOutcome: outcome,
                labelEvidence: evidence,
                labelNotes: notes,
                validationConfirmed: true,
                allowCorrection: Boolean(record.actualOutcome)
            });
            onEvaluated();
            onClose();
        } catch (err) {
            console.error('Failed to submit outcome:', err);
            setError(err.response?.data?.message || 'Failed to record project outcome.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl border border-gray-100 animate-fadeIn">
                <div className="flex items-start justify-between border-b pb-4">
                    <div>
                        <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">Actual Outcome Verification</span>
                        <h3 className="text-lg font-bold text-slate-900 mt-0.5">
                            {record.projectTitle || `Project #${record.projectId}`}
                        </h3>
                        <p className="text-xs text-slate-500">Student: {record.studentName || `ID ${record.studentProfileId}`}</p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-lg">✕</button>
                </div>

                <form onSubmit={handleSubmit} className="mt-4 space-y-4">
                    {record.actualOutcome && (
                        <div className="p-3 rounded-lg bg-amber-50 text-amber-800 text-xs border border-amber-200">
                            ⚠️ This project record already has a verified outcome of <strong>{record.actualOutcome}</strong>. Submitting this evaluation will record an auditable correction.
                        </div>
                    )}

                    {error && (
                        <div className="p-3 rounded-lg bg-red-50 text-red-700 text-xs border border-red-200">
                            {error}
                        </div>
                    )}

                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wide text-slate-700 mb-1.5">
                            Verified Execution Outcome *
                        </label>
                        <div className="grid grid-cols-2 gap-3">
                            <button
                                type="button"
                                onClick={() => setOutcome('FEASIBLE')}
                                className={`py-3 px-4 rounded-xl border text-sm font-bold flex items-center justify-center gap-2 transition-all ${
                                    outcome === 'FEASIBLE'
                                        ? 'bg-emerald-600 text-white border-emerald-600 shadow-md shadow-emerald-200'
                                        : 'bg-white text-emerald-800 border-emerald-200 hover:bg-emerald-50'
                                }`}
                            >
                                <span>✓</span> FEASIBLE
                            </button>
                            <button
                                type="button"
                                onClick={() => setOutcome('NOT_FEASIBLE')}
                                className={`py-3 px-4 rounded-xl border text-sm font-bold flex items-center justify-center gap-2 transition-all ${
                                    outcome === 'NOT_FEASIBLE'
                                        ? 'bg-rose-600 text-white border-rose-600 shadow-md shadow-rose-200'
                                        : 'bg-white text-rose-800 border-rose-200 hover:bg-rose-50'
                                }`}
                            >
                                <span>✕</span> NOT_FEASIBLE
                            </button>
                        </div>
                        <p className="text-[11px] text-slate-400 mt-1">
                            FEASIBLE indicates project was completed on schedule with functioning demonstration; NOT_FEASIBLE indicates critical timeline or scope failure.
                        </p>
                    </div>

                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wide text-slate-700 mb-1">
                            Evaluation Evidence / Deliverables Demonstration
                        </label>
                        <textarea
                            rows={3}
                            value={evidence}
                            onChange={(e) => setEvidence(e.target.value)}
                            placeholder="e.g. Demonstrated functional prototype at milestone review; source code repositories verified; all key unit tests passing."
                            className="w-full text-xs rounded-xl border border-gray-300 p-2.5 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wide text-slate-700 mb-1">
                            Evaluator Notes
                        </label>
                        <textarea
                            rows={2}
                            value={notes}
                            onChange={(e) => setNotes(e.target.value)}
                            placeholder="Additional observations or notes..."
                            className="w-full text-xs rounded-xl border border-gray-300 p-2.5 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                        />
                    </div>

                    <div className="flex items-start gap-2 pt-1">
                        <input
                            type="checkbox"
                            id="confirm-outcome"
                            checked={confirmed}
                            onChange={(e) => setConfirmed(e.target.checked)}
                            className="mt-1 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                        />
                        <label htmlFor="confirm-outcome" className="text-xs text-slate-600 leading-snug">
                            I verify that this outcome is supported by actual project execution evidence and should be included in supervised training.
                        </label>
                    </div>

                    <div className="flex items-center justify-end gap-3 pt-4 border-t">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 rounded-xl text-xs font-medium text-slate-600 hover:bg-slate-100"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting || !confirmed}
                            className="px-5 py-2.5 rounded-xl text-xs font-bold bg-indigo-600 text-white hover:bg-indigo-700 shadow-md shadow-indigo-200 disabled:opacity-50"
                        >
                            {isSubmitting ? 'Recording...' : 'Submit Verified Outcome'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

// ─── Main Component ───────────────────────────────────────────────────────────

const MlDatasetManagement = () => {
    const [stats, setStats] = useState(null);
    const [records, setRecords] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [filterOutcome, setFilterOutcome] = useState('ALL');
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionMessage, setActionMessage] = useState('');

    const [evaluateRecord, setEvaluateRecord] = useState(null);
    const [prepSummary, setPrepSummary] = useState(null);
    const [isPreparing, setIsPreparing] = useState(false);
    const [splitReport, setSplitReport] = useState(null);
    const [isDownloading, setIsDownloading] = useState(false);
    const [isRunningPipeline, setIsRunningPipeline] = useState(false);

    const loadData = useCallback(async () => {
        setIsLoading(true);
        setError('');
        try {
            const [statsRes, recordsRes, splitRes] = await Promise.all([
                mlDatasetService.getStatistics(),
                mlDatasetService.getRecords(page, 15, filterOutcome),
                mlDatasetService.getSplitReport()
            ]);
            setStats(statsRes.data);
            setRecords(recordsRes.data.content || []);
            setTotalPages(recordsRes.data.totalPages || 1);
            setSplitReport(splitRes.data);
        } catch (err) {
            console.error('Failed to load ML dataset data:', err);
            setError(err.response?.data?.message || 'Failed to load dataset records. Please ensure backend is active.');
        } finally {
            setIsLoading(false);
        }
    }, [page, filterOutcome]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handlePrepare = async () => {
        setIsPreparing(true);
        setActionMessage('');
        try {
            const res = await mlDatasetService.prepareDataset();
            setPrepSummary(res.data);
            setActionMessage(`Preparation complete: ${res.data.eligibleRecords} training-ready, ${res.data.excludedRecords} excluded.`);
            await loadData();
        } catch (err) {
            console.error('Dataset preparation failed:', err);
            setError('Dataset preparation failed: ' + (err.response?.data?.message || err.message));
        } finally {
            setIsPreparing(false);
        }
    };

    const handleDownloadCsv = async () => {
        setIsDownloading(true);
        try {
            await mlDatasetService.downloadCleanedCsv();
            setActionMessage('Cleaned training-ready CSV downloaded successfully.');
        } catch (err) {
            console.error('Failed to download CSV:', err);
            setError('Failed to download CSV: ' + (err.response?.data?.message || err.message));
        } finally {
            setIsDownloading(false);
        }
    };

    const handleTriggerPipeline = async () => {
        setIsRunningPipeline(true);
        setError('');
        try {
            const res = await mlDatasetService.triggerPipeline();
            const data = res.data;
            if (data.databasePreparationSummary) {
                setPrepSummary(data.databasePreparationSummary);
            }
            if (data.mlServicePipeline?.split_report) {
                setSplitReport(data.mlServicePipeline.split_report);
            }
            setActionMessage(`ML Service pipeline executed successfully: ${data.trainingReadyRecordsCount} training-ready records synchronized.`);
            await loadData();
        } catch (err) {
            console.error('ML pipeline execution failed:', err);
            setError('ML pipeline execution failed: ' + (err.response?.data?.message || err.message));
        } finally {
            setIsRunningPipeline(false);
        }
    };

    return (
        <div className="space-y-8 animate-fadeIn pb-12">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
                <div>
                    <div className="flex items-center gap-2">
                        <span className="text-2xl">📊</span>
                        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                            ML Dataset Management
                        </h1>
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-100 text-indigo-700">
                            Phase 9
                        </span>
                    </div>
                    <p className="mt-1 text-sm text-slate-500">
                        10-feature student-project dataset pipeline with automated collection and verified actual outcomes.
                    </p>
                </div>

                <div className="flex items-center gap-2 flex-wrap">
                    <button
                        onClick={handlePrepare}
                        disabled={isPreparing}
                        className="px-4 py-2 rounded-xl text-xs font-bold bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm shadow-indigo-200 transition-all flex items-center gap-1.5 disabled:opacity-50"
                    >
                        <span>⚙️</span>
                        <span>{isPreparing ? 'Validating...' : 'Validate & Prepare Dataset'}</span>
                    </button>
                    <button
                        onClick={handleTriggerPipeline}
                        disabled={isRunningPipeline}
                        className="px-4 py-2 rounded-xl text-xs font-bold bg-purple-600 text-white hover:bg-purple-700 shadow-sm shadow-purple-200 transition-all flex items-center gap-1.5 disabled:opacity-50"
                    >
                        <span>🔄</span>
                        <span>{isRunningPipeline ? 'Synchronizing...' : 'Run ML Pipeline (Python)'}</span>
                    </button>
                    <button
                        onClick={handleDownloadCsv}
                        disabled={isDownloading}
                        className="px-4 py-2 rounded-xl text-xs font-bold bg-emerald-600 text-white hover:bg-emerald-700 shadow-sm shadow-emerald-200 transition-all flex items-center gap-1.5 disabled:opacity-50"
                    >
                        <span>📥</span>
                        <span>{isDownloading ? 'Exporting...' : 'Export Training CSV'}</span>
                    </button>
                </div>
            </div>

            {/* Action Feedback Messages */}
            {actionMessage && (
                <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium flex items-center justify-between">
                    <span>✓ {actionMessage}</span>
                    <button onClick={() => setActionMessage('')} className="text-emerald-600 font-bold">✕</button>
                </div>
            )}
            {error && (
                <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs font-medium flex items-center justify-between">
                    <span>⚠️ {error}</span>
                    <button onClick={() => setError('')} className="text-red-600 font-bold">✕</button>
                </div>
            )}

            {/* SECTION A: Dataset Overview KPI Cards */}
            <div>
                <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500 mb-3">
                    Dataset Overview
                </h2>
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-7 gap-3">
                    <StatCard
                        title="Total Records"
                        value={stats?.totalRecords}
                        icon="📦"
                        bgGradient="from-slate-50 to-slate-100"
                    />
                    <StatCard
                        title="Complete"
                        value={stats?.completeRecords}
                        icon="✅"
                        bgGradient="from-blue-50 to-indigo-50"
                    />
                    <StatCard
                        title="Incomplete"
                        value={stats?.incompleteRecords}
                        icon="⚠️"
                        bgGradient="from-orange-50 to-amber-50"
                    />
                    <StatCard
                        title="Pending Outcome"
                        value={stats?.pendingOutcomeRecords}
                        icon="⏳"
                        bgGradient="from-amber-50 to-yellow-50"
                    />
                    <StatCard
                        title="Verified Feasible"
                        value={stats?.verifiedFeasibleRecords}
                        icon="🎯"
                        bgGradient="from-emerald-50 to-teal-50"
                    />
                    <StatCard
                        title="Verified Not-Feas."
                        value={stats?.verifiedNotFeasibleRecords}
                        icon="🛑"
                        bgGradient="from-rose-50 to-pink-50"
                    />
                    <StatCard
                        title="Training Ready"
                        value={stats?.trainingReadyRecords}
                        icon="🚀"
                        bgGradient="from-purple-50 to-indigo-100"
                    />
                </div>
            </div>

            {/* SECTION D (Details if validation run): Preparation Summary */}
            {prepSummary && (
                <div className="p-5 rounded-2xl border border-indigo-100 bg-indigo-50/60 space-y-3">
                    <div className="flex items-center justify-between">
                        <span className="font-bold text-sm text-indigo-950 flex items-center gap-2">
                            <span>🔍</span> Dataset Validation & Cleaning Summary
                        </span>
                        <button onClick={() => setPrepSummary(null)} className="text-indigo-400 hover:text-indigo-600 text-xs font-bold">✕ Dismiss</button>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                        <div className="bg-white p-3 rounded-xl border border-indigo-100">
                            <span className="text-slate-400 block">Total Checked</span>
                            <span className="text-base font-extrabold text-slate-800">{prepSummary.totalDatabaseRecords}</span>
                        </div>
                        <div className="bg-white p-3 rounded-xl border border-indigo-100">
                            <span className="text-slate-400 block">Eligible (Training-Ready)</span>
                            <span className="text-base font-extrabold text-emerald-600">{prepSummary.eligibleRecords}</span>
                        </div>
                        <div className="bg-white p-3 rounded-xl border border-indigo-100">
                            <span className="text-slate-400 block">Excluded Records</span>
                            <span className="text-base font-extrabold text-rose-600">{prepSummary.excludedRecords}</span>
                        </div>
                        <div className="bg-white p-3 rounded-xl border border-indigo-100">
                            <span className="text-slate-400 block">Duplicates Handled</span>
                            <span className="text-base font-extrabold text-amber-600">{prepSummary.duplicateRecords}</span>
                        </div>
                    </div>
                    {prepSummary.exclusionDetails?.length > 0 && (
                        <div className="mt-3">
                            <span className="text-xs font-semibold text-slate-700 block mb-1">Exclusion Reasons Sample:</span>
                            <ul className="space-y-1 max-h-32 overflow-y-auto text-xs text-slate-600 bg-white p-3 rounded-xl border border-indigo-100 divide-y divide-gray-50">
                                {prepSummary.exclusionDetails.slice(0, 10).map((d, i) => (
                                    <li key={i} className="py-1 flex items-start justify-between gap-2">
                                        <span>Record #{d.recordId} (Student #{d.studentProfileId}, Project #{d.projectId})</span>
                                        <span className="text-rose-600 font-medium text-[11px] text-right">{d.reason}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            )}

            {/* SECTION E: 80/20 Train/Test Split Pipeline Status */}
            {splitReport && (
                <div className="p-5 rounded-2xl border border-gray-200 bg-white shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div>
                        <div className="flex items-center gap-2">
                            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Phase 10 Training Pipeline Readiness</span>
                            <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${
                                splitReport.isReadyForTraining ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-800'
                            }`}>
                                {splitReport.isReadyForTraining ? '✓ Split Ready' : '⏳ Data Collection Phase'}
                            </span>
                        </div>
                        <p className="text-sm font-semibold text-slate-800 mt-1">{splitReport.message}</p>
                    </div>

                    <div className="flex items-center gap-4 text-xs shrink-0 bg-slate-50 p-3 rounded-xl border border-slate-100">
                        <div>
                            <span className="text-slate-400 block text-[10px] uppercase">Train / Test</span>
                            <span className="font-extrabold text-slate-700">80% / 20%</span>
                        </div>
                        <div className="border-l pl-3">
                            <span className="text-slate-400 block text-[10px] uppercase">Random Seed</span>
                            <span className="font-extrabold text-slate-700">{splitReport.randomSeed}</span>
                        </div>
                        <div className="border-l pl-3">
                            <span className="text-slate-400 block text-[10px] uppercase">Est. Train / Test</span>
                            <span className="font-extrabold text-slate-700">{splitReport.estimatedTrainSize} / {splitReport.estimatedTestSize}</span>
                        </div>
                    </div>
                </div>
            )}

            {/* SECTION B: Dataset Records Table */}
            <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
                {/* Table Header Controls */}
                <div className="p-4 sm:p-5 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-50/50">
                    <div>
                        <h2 className="text-base font-bold text-slate-900">Dataset Records (10 Features + Outcome)</h2>
                        <p className="text-xs text-slate-500">Each record models one student-project pairing for feasibility prediction.</p>
                    </div>

                    <div className="flex items-center gap-2">
                        <label className="text-xs font-medium text-slate-500">Filter Outcome:</label>
                        <select
                            value={filterOutcome}
                            onChange={(e) => {
                                setFilterOutcome(e.target.value);
                                setPage(0);
                            }}
                            className="text-xs rounded-xl border border-gray-300 py-1.5 px-3 bg-white font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        >
                            <option value="ALL">All Records</option>
                            <option value="PENDING_OUTCOME">Pending Outcome</option>
                            <option value="FEASIBLE">Feasible</option>
                            <option value="NOT_FEASIBLE">Not Feasible</option>
                        </select>
                    </div>
                </div>

                {/* Table Content */}
                {isLoading ? (
                    <div className="p-12 text-center text-slate-400 text-xs">
                        <span className="inline-block animate-spin text-lg mb-2">↻</span>
                        <p>Loading dataset snapshots from ProjectGuard...</p>
                    </div>
                ) : records.length === 0 ? (
                    <div className="p-12 text-center text-slate-500">
                        <span className="text-4xl block mb-2">📋</span>
                        <h3 className="text-sm font-bold text-slate-800">No Dataset Records Found</h3>
                        <p className="text-xs text-slate-400 mt-1 max-w-md mx-auto">
                            Snapshots are automatically captured when students generate AI ideas or register recommended projects.
                        </p>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs text-slate-600">
                            <thead className="bg-slate-100 text-slate-700 text-[11px] font-bold uppercase tracking-wide border-b border-gray-200">
                                <tr>
                                    <th className="py-3 px-3">ID / Ref</th>
                                    <th className="py-3 px-3">Project Title</th>
                                    <th className="py-3 px-2">Skill Level</th>
                                    <th className="py-3 px-2">Skill Gap</th>
                                    <th className="py-3 px-2">Complexity</th>
                                    <th className="py-3 px-2">Team</th>
                                    <th className="py-3 px-2">Avail Hrs/Wk</th>
                                    <th className="py-3 px-2">Tech Fam.</th>
                                    <th className="py-3 px-2">Reqs/Feats</th>
                                    <th className="py-3 px-2">Prev Exp</th>
                                    <th className="py-3 px-2">Effort (hrs)</th>
                                    <th className="py-3 px-3">Outcome</th>
                                    <th className="py-3 px-3">Action</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {records.map((r) => (
                                    <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                                        <td className="py-3 px-3 font-semibold text-slate-800">
                                            #{r.id}
                                            <span className="block text-[10px] text-slate-400">S:{r.studentProfileId || 'N/A'}</span>
                                        </td>
                                        <td className="py-3 px-3 max-w-[180px] truncate" title={r.projectTitle}>
                                            <span className="font-semibold text-slate-900 block truncate">{r.projectTitle || `Project #${r.projectId}`}</span>
                                            <span className="text-[10px] text-slate-400">{r.dataSource || 'COLLECTION'}</span>
                                        </td>
                                        <td className="py-3 px-2 font-mono">{r.studentSkillLevel != null ? r.studentSkillLevel.toFixed(2) : '—'}</td>
                                        <td className="py-3 px-2 font-mono">{r.skillGap != null ? r.skillGap.toFixed(2) : '—'}</td>
                                        <td className="py-3 px-2">
                                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${complexityBadge(r.projectComplexity)}`}>
                                                {r.projectComplexity}
                                            </span>
                                        </td>
                                        <td className="py-3 px-2 font-mono">{r.teamSize ?? '—'}</td>
                                        <td className="py-3 px-2 font-mono">{r.availableHoursPerWeek != null ? `${r.availableHoursPerWeek}h` : '—'}</td>
                                        <td className="py-3 px-2 font-mono">{r.technologyFamiliarity != null ? r.technologyFamiliarity.toFixed(2) : '—'}</td>
                                        <td className="py-3 px-2 font-mono">{r.numberOfRequirements ?? '—'}/{r.numberOfFeatures ?? '—'}</td>
                                        <td className="py-3 px-2 font-mono">{r.previousExperience != null ? `${r.previousExperience}y` : '—'}</td>
                                        <td className="py-3 px-2 font-mono font-bold text-slate-800">{r.estimatedEffort != null ? `${r.estimatedEffort}h` : '—'}</td>
                                        <td className="py-3 px-3">
                                            <span className={`px-2 py-0.5 rounded-full text-[10px] border ${outcomeBadge(r.actualOutcome || r.outcomeStatus)}`}>
                                                {r.actualOutcome || r.outcomeStatus}
                                            </span>
                                            <span className={`block mt-1 text-[9px] px-1.5 py-0.2 rounded border w-fit ${valStatusBadge(r.validationStatus)}`}>
                                                {r.validationStatus}
                                            </span>
                                        </td>
                                        <td className="py-3 px-3">
                                            {r.outcomeStatus === 'PENDING_OUTCOME' ? (
                                                <button
                                                    onClick={() => setEvaluateRecord(r)}
                                                    className="px-2.5 py-1 rounded-lg text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-100 transition-colors"
                                                >
                                                    Evaluate
                                                </button>
                                            ) : (
                                                <button
                                                    onClick={() => setEvaluateRecord(r)}
                                                    className="px-2 py-1 rounded-lg text-[10px] font-medium text-slate-500 hover:bg-slate-100"
                                                    title="Re-evaluate outcome"
                                                >
                                                    Edit
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="p-4 border-t border-gray-100 flex items-center justify-between text-xs text-slate-500 bg-slate-50/40">
                        <span>Page {page + 1} of {totalPages}</span>
                        <div className="flex gap-2">
                            <button
                                disabled={page === 0}
                                onClick={() => setPage((p) => Math.max(0, p - 1))}
                                className="px-3 py-1.5 rounded-lg border border-gray-200 bg-white text-slate-700 disabled:opacity-40"
                            >
                                Previous
                            </button>
                            <button
                                disabled={page + 1 >= totalPages}
                                onClick={() => setPage((p) => p + 1)}
                                className="px-3 py-1.5 rounded-lg border border-gray-200 bg-white text-slate-700 disabled:opacity-40"
                            >
                                Next
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {/* Evaluation Modal */}
            {evaluateRecord && (
                <EvaluateOutcomeModal
                    record={evaluateRecord}
                    onClose={() => setEvaluateRecord(null)}
                    onEvaluated={loadData}
                />
            )}
        </div>
    );
};

export default MlDatasetManagement;

