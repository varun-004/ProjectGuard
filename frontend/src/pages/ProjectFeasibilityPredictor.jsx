import React, { useState, useEffect } from 'react';
import mlModelService from '../services/mlModelService';
import studentService from '../services/studentService';

const ProjectFeasibilityPredictor = () => {
    const [formData, setFormData] = useState({
        student_skill_level: 2.8,
        skill_gap: 5.0,
        project_complexity: 'MEDIUM',
        team_size: 2,
        available_hours_per_week: 20.0,
        technology_familiarity: 2.5,
        number_of_requirements: 5,
        number_of_features: 4,
        previous_experience: 1.0,
        estimated_effort: 160.0
    });

    const [modelStatus, setModelStatus] = useState(null);
    const [predictionResult, setPredictionResult] = useState(null);
    const [loading, setLoading] = useState(false);
    const [statusLoading, setStatusLoading] = useState(true);
    const [error, setError] = useState(null);
    const [message, setMessage] = useState(null);

    const loadDemoProject = () => {
        setFormData({
            student_skill_level: 1.38,
            skill_gap: 10.67,
            project_complexity: 'MEDIUM',
            team_size: 2,
            available_hours_per_week: 15.0,
            technology_familiarity: 1.45,
            number_of_requirements: 5,
            number_of_features: 5,
            previous_experience: 0.0,
            estimated_effort: 90.0
        });
        setMessage("Loaded actual parameters for recommended project: 'Non-Invasive Industrial Machine Health Monitoring'");
        setError(null);
    };

    useEffect(() => {
        const fetchStatusAndProfile = async () => {
            setStatusLoading(true);
            try {
                const statusRes = await mlModelService.getModelStatus();
                setModelStatus(statusRes.data);
            } catch {
                setModelStatus({ modelAvailable: false });
            }

            try {
                const profileRes = await studentService.getProfile();
                if (profileRes.data) {
                    const p = profileRes.data;
                    setFormData(prev => ({
                        ...prev,
                        available_hours_per_week: p.availableHoursPerWeek || prev.available_hours_per_week,
                        previous_experience: p.yearsOfExperience !== undefined ? p.yearsOfExperience : prev.previous_experience
                    }));
                }
            } catch {
                // profile fetch is optional
            } finally {
                setStatusLoading(false);
            }
        };

        fetchStatusAndProfile();
    }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === 'project_complexity'
                ? value
                : (['team_size', 'number_of_requirements', 'number_of_features'].includes(name)
                    ? parseInt(value, 10) || 0
                    : parseFloat(value) || 0)
        }));
    };

    const handlePredict = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setPredictionResult(null);

        try {
            const payload = {
                student_skill_level: Number(formData.student_skill_level),
                skill_gap: Number(formData.skill_gap),
                project_complexity: formData.project_complexity,
                team_size: Number(formData.team_size),
                available_hours_per_week: Number(formData.available_hours_per_week),
                technology_familiarity: Number(formData.technology_familiarity),
                number_of_requirements: Number(formData.number_of_requirements),
                number_of_features: Number(formData.number_of_features),
                previous_experience: Number(formData.previous_experience),
                estimated_effort: Number(formData.estimated_effort)
            };

            const res = await mlModelService.predict(payload);
            if (res.data?.status === 'SUCCESS' && res.data?.prediction) {
                setPredictionResult(res.data);
                setError(null);
            } else {
                setPredictionResult(null);
                setError(res.data?.message || 'Model prediction is currently unavailable.');
            }
        } catch (err) {
            setPredictionResult(null);
            if (err.response?.status === 404 || err.response?.data?.status === 'NO_MODEL') {
                setError('No trained ML model is currently active. Model training is pending authentic verified dataset records (Minimum 50 records required).');
            } else if (err.response?.status === 503 || err.response?.data?.status === 'UNREACHABLE') {
                setError('ML Service is currently offline or unreachable. Please verify Python FastAPI service is running on port 8000.');
            } else if (err.response?.status === 422) {
                const details = err.response.data?.errors || err.response.data?.detail?.errors || [];
                setError('Validation error: ' + (Array.isArray(details) ? details.join(', ') : JSON.stringify(details)));
            } else {
                setError(err.response?.data?.message || err.message || 'Prediction request failed.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-4xl mx-auto space-y-8 pb-12">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b pb-6">
                <div>
                    <div className="flex items-center gap-2">
                        <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-indigo-100 text-indigo-800">ML Prediction</span>
                        <h1 className="text-2xl font-bold text-gray-900">Project Feasibility Predictor</h1>
                    </div>
                    <p className="text-gray-600 text-sm mt-1">
                        Estimate whether a planned student-project combination is feasible based on the trained supervised classifier.
                    </p>
                </div>
                <button
                    type="button"
                    onClick={loadDemoProject}
                    className="px-4 py-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white font-semibold rounded-xl text-xs transition shadow-sm flex items-center gap-2 w-fit"
                >
                    ⚡ Load "Machine Health Monitoring" Data
                </button>
            </div>

            {message && (
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-xs flex items-center justify-between">
                    <span>✨ {message}</span>
                    <button onClick={() => setMessage(null)} className="text-emerald-600 font-bold hover:underline">✕</button>
                </div>
            )}

            {/* Model Status Banner */}
            {!statusLoading && (
                <div className={`p-4 rounded-xl border flex items-center justify-between text-xs font-semibold ${
                    modelStatus?.modelAvailable
                        ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                        : 'bg-amber-50 border-amber-200 text-amber-800'
                }`}>
                    <div className="flex items-center gap-2">
                        <span className="text-base">{modelStatus?.modelAvailable ? '✅' : '⏳'}</span>
                        <span>
                            {modelStatus?.modelAvailable
                                ? `Active ML Model: ${modelStatus.algorithm} (${modelStatus.activeVersion}) ready for predictions.`
                                : 'Notice: Model training is pending authentic verified dataset collection (Minimum 50 records required).'}
                        </span>
                    </div>
                    {modelStatus?.modelAvailable && (
                        <span className="bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full text-2xs uppercase tracking-wider font-bold">
                            ONLINE
                        </span>
                    )}
                </div>
            )}

            {error && (
                <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm flex items-center justify-between">
                    <span>⚠️ {error}</span>
                    <button onClick={() => setError(null)} className="text-rose-600 font-bold hover:underline">✕</button>
                </div>
            )}

            {/* Prediction Form & Results */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {/* Form */}
                <form onSubmit={handlePredict} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
                    <h2 className="text-base font-bold text-gray-900 border-b pb-3">Input Project & Student Features</h2>

                    {/* Skill Level & Skill Gap */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Student Skill Level (1.0 - 4.0)
                            </label>
                            <input
                                type="number"
                                step="0.1"
                                min="1.0"
                                max="4.0"
                                name="student_skill_level"
                                value={formData.student_skill_level}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Skill Gap (0.0 - 25.0)
                            </label>
                            <input
                                type="number"
                                step="0.1"
                                min="0.0"
                                max="25.0"
                                name="skill_gap"
                                value={formData.skill_gap}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    {/* Complexity & Team Size */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Project Complexity
                            </label>
                            <select
                                name="project_complexity"
                                value={formData.project_complexity}
                                onChange={handleChange}
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-white"
                            >
                                <option value="EASY">EASY</option>
                                <option value="MEDIUM">MEDIUM</option>
                                <option value="HARD">HARD</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Team Size
                            </label>
                            <input
                                type="number"
                                min="1"
                                max="20"
                                name="team_size"
                                value={formData.team_size}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    {/* Available Hours & Tech Familiarity */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Available Hours / Week
                            </label>
                            <input
                                type="number"
                                step="1"
                                min="1"
                                max="80"
                                name="available_hours_per_week"
                                value={formData.available_hours_per_week}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Tech Familiarity (1.0 - 4.0)
                            </label>
                            <input
                                type="number"
                                step="0.1"
                                min="1.0"
                                max="4.0"
                                name="technology_familiarity"
                                value={formData.technology_familiarity}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    {/* Requirements & Features */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Number of Requirements
                            </label>
                            <input
                                type="number"
                                min="0"
                                max="100"
                                name="number_of_requirements"
                                value={formData.number_of_requirements}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Number of Features
                            </label>
                            <input
                                type="number"
                                min="0"
                                max="100"
                                name="number_of_features"
                                value={formData.number_of_features}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    {/* Previous Experience & Estimated Effort */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Previous Experience (Years)
                            </label>
                            <input
                                type="number"
                                step="0.5"
                                min="0"
                                max="30"
                                name="previous_experience"
                                value={formData.previous_experience}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1">
                                Estimated Effort (Hours)
                            </label>
                            <input
                                type="number"
                                step="10"
                                min="10"
                                max="2000"
                                name="estimated_effort"
                                value={formData.estimated_effort}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full mt-4 py-2.5 px-4 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-bold rounded-xl text-sm shadow-sm transition disabled:opacity-50 flex items-center justify-center gap-2"
                    >
                        {loading ? (
                            <>
                                <span className="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
                                Computing Feasibility...
                            </>
                        ) : (
                            <>⚡ Predict Feasibility</>
                        )}
                    </button>
                </form>

                {/* Result Card */}
                <div className="space-y-4">
                    {predictionResult && predictionResult.status === 'SUCCESS' && predictionResult.prediction ? (
                        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-6">
                            <h2 className="text-base font-bold text-gray-900 border-b pb-3">Prediction Outcome</h2>

                            {/* Outcome Badge */}
                            <div className="text-center py-4">
                                <span className="text-xs font-semibold text-gray-400 uppercase tracking-widest">Calculated Result</span>
                                <div className={`mt-2 text-3xl font-black ${
                                    predictionResult.prediction === 'FEASIBLE' ? 'text-emerald-600' : 'text-rose-600'
                                }`}>
                                    {predictionResult.prediction === 'FEASIBLE' ? '✅ FEASIBLE' : (predictionResult.prediction === 'NOT_FEASIBLE' ? '⚠️ NOT FEASIBLE' : predictionResult.prediction)}
                                </div>
                                <p className="text-xs text-gray-500 mt-1">
                                    Model version: <strong>{predictionResult.modelVersion || 'Active Model'}</strong> ({predictionResult.algorithm || 'Supervised Classifier'})
                                </p>
                            </div>

                            {/* Probability Meter */}
                            {predictionResult.probability !== null && predictionResult.probability !== undefined && (
                                <div className="space-y-1.5">
                                    <div className="flex justify-between text-xs font-semibold">
                                        <span className="text-gray-600">Feasibility Probability:</span>
                                        <span className="text-indigo-600 font-bold">
                                            {(predictionResult.probability * 100).toFixed(1)}%
                                        </span>
                                    </div>
                                    <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
                                        <div
                                            className={`h-full rounded-full transition-all duration-500 ${
                                                predictionResult.prediction === 'FEASIBLE' ? 'bg-emerald-500' : 'bg-rose-500'
                                            }`}
                                            style={{ width: `${Math.min(100, predictionResult.probability * 100)}%` }}
                                        />
                                    </div>
                                </div>
                            )}

                            {/* Risk Signals */}
                            {predictionResult.riskFactors && predictionResult.riskFactors.length > 0 && (
                                <div>
                                    <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                                        Identified Risk Factors
                                    </h3>
                                    <ul className="space-y-1.5 text-xs text-amber-800 bg-amber-50/70 border border-amber-200/70 rounded-xl p-3">
                                        {predictionResult.riskFactors.map((risk, idx) => (
                                            <li key={idx} className="flex items-start gap-1.5">
                                                <span>•</span>
                                                <span>{risk}</span>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            {/* Disclaimer */}
                            {predictionResult.disclaimer && (
                                <div className="p-3 bg-gray-50 rounded-xl text-2xs text-gray-500 leading-relaxed border border-gray-100">
                                    <strong className="text-gray-700">Disclaimer:</strong> {predictionResult.disclaimer}
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-8 text-center flex flex-col items-center justify-center min-h-[300px] text-gray-400 space-y-3">
                            <span className="text-4xl">🔮</span>
                            <p className="text-sm font-medium text-gray-600">
                                {modelStatus?.modelAvailable ? 'Ready for Prediction' : 'Model Training Pending'}
                            </p>
                            <p className="text-xs text-gray-400 max-w-xs">
                                {modelStatus?.modelAvailable
                                    ? 'Adjust the parameters and click "Predict Feasibility" to evaluate whether this project configuration is viable.'
                                    : 'A supervised classifier will become active once minimum 50 verified authentic dataset outcomes are collected and trained.'}
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ProjectFeasibilityPredictor;

