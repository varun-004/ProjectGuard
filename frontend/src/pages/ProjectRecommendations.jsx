import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import recommendationService from '../services/recommendationService';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const scoreColor = (score) => {
    if (score >= 80) return 'text-emerald-700 bg-emerald-50 border-emerald-200';
    if (score >= 65) return 'text-indigo-700 bg-indigo-50 border-indigo-200';
    if (score >= 50) return 'text-amber-700 bg-amber-50 border-amber-200';
    return 'text-rose-700 bg-rose-50 border-rose-200';
};

const scoreRingGradient = (score) => {
    if (score >= 80) return 'from-emerald-500 to-teal-600';
    if (score >= 65) return 'from-indigo-600 to-violet-600';
    if (score >= 50) return 'from-amber-500 to-orange-600';
    return 'from-rose-500 to-red-600';
};

const complexityColor = (comp) => {
    switch (comp?.toUpperCase()) {
        case 'ADVANCED': return 'bg-purple-100 text-purple-700 border-purple-200';
        case 'INTERMEDIATE': return 'bg-blue-100 text-blue-700 border-blue-200';
        case 'BEGINNER': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
        default: return 'bg-gray-100 text-gray-700 border-gray-200';
    }
};

const levelBadgeColor = (level) => {
    switch (level?.toUpperCase()) {
        case 'ADVANCED': return 'bg-emerald-100 text-emerald-800';
        case 'INTERMEDIATE': return 'bg-blue-100 text-blue-800';
        case 'BEGINNER': return 'bg-amber-100 text-amber-800';
        default: return 'bg-gray-100 text-gray-600';
    }
};

// ─── Project Detail Modal ─────────────────────────────────────────────────────

const ProjectDetailModal = ({ project, onClose }) => {
    if (!project) return null;

    return (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 sm:p-6 animate-fadeIn">
            <div 
                className="bg-white rounded-2xl max-w-4xl w-full max-h-[92vh] flex flex-col shadow-2xl border border-gray-100 overflow-hidden transform transition-all"
                role="dialog"
                aria-modal="true"
                aria-labelledby="modal-title"
            >
                {/* Modal Header */}
                <div className="px-6 py-5 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white flex items-start justify-between gap-4 border-b border-indigo-900/40">
                    <div>
                        <div className="flex items-center gap-2 mb-2 flex-wrap">
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-500/30 text-indigo-200 border border-indigo-400/30">
                                Rank #{project.rankOrder}
                            </span>
                            {project.isFreshAlternative && (
                                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-400/20 text-amber-200 border border-amber-400/30">
                                    ✨ Fresh Alternative
                                </span>
                            )}
                            {project.isGeneratedIdea && (
                                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-400/20 text-purple-200 border border-purple-400/30">
                                    💡 AI Synthesized
                                </span>
                            )}
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-400/30">
                                Quality Score: {project.qualityScore ? Math.round(project.qualityScore) : 85}/100
                            </span>
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-white/10 text-white/90">
                                {project.domainName || 'General Engineering'}
                            </span>
                        </div>
                        <h2 id="modal-title" className="text-xl sm:text-2xl font-bold leading-tight text-white">
                            {project.title}
                        </h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-white bg-white/10 hover:bg-white/20 p-2 rounded-xl transition-colors shrink-0"
                        aria-label="Close modal"
                    >
                        ✕
                    </button>
                </div>

                {/* Modal Body */}
                <div className="overflow-y-auto p-6 space-y-6 text-sm text-gray-700 divide-y divide-gray-100">
                    
                    {/* Compatibility & Score Explanation Banner */}
                    <div className="bg-indigo-50/80 border border-indigo-100 rounded-xl p-4 space-y-3">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                            <span className="font-bold text-indigo-950 text-sm">Personalized Student Compatibility & Profile Match</span>
                            <div className="flex items-center gap-2 flex-wrap">
                                <span className="px-2.5 py-1 rounded-lg text-xs font-extrabold bg-indigo-100 text-indigo-800 border border-indigo-200">
                                    Suitability: {Math.round(project.suitabilityScore || project.matchScore)}%
                                </span>
                                <span className="px-2.5 py-1 rounded-lg text-xs font-extrabold bg-emerald-100 text-emerald-800 border border-emerald-200">
                                    Feasibility: {Math.round(project.feasibilityScore || 85)}%
                                </span>
                                <span className="px-2.5 py-1 rounded-lg text-xs font-extrabold bg-slate-900 text-white shadow-sm">
                                    Overall: {Math.round(project.matchScore)}% Match
                                </span>
                            </div>
                        </div>
                        <p className="text-xs text-indigo-900 leading-relaxed font-medium">
                            {project.scoreExplanation}
                        </p>
                        {project.recommendationReasons && project.recommendationReasons.length > 0 && (
                            <div className="pt-2 border-t border-indigo-100/80 space-y-1">
                                <span className="text-[11px] font-bold text-indigo-900 uppercase tracking-wide block">Why this project matches your profile:</span>
                                <ul className="space-y-1 text-xs text-indigo-900">
                                    {project.recommendationReasons.map((reason, i) => (
                                        <li key={i} className="flex items-start gap-1.5">
                                            <span className="text-emerald-600 font-bold shrink-0">✓</span>
                                            <span>{reason}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                        {project.scoreBreakdown && (
                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 text-xs">
                                <div className="bg-white/90 p-2 rounded-lg border border-indigo-100">
                                    <span className="text-gray-500 block">Domain Fit</span>
                                    <span className="font-semibold text-gray-800">{project.scoreBreakdown.domainFitScore} / 25</span>
                                </div>
                                <div className="bg-white/90 p-2 rounded-lg border border-indigo-100">
                                    <span className="text-gray-500 block">Tech Fit</span>
                                    <span className="font-semibold text-gray-800">{project.scoreBreakdown.techFitScore} / 20</span>
                                </div>
                                <div className="bg-white/90 p-2 rounded-lg border border-indigo-100">
                                    <span className="text-gray-500 block">Skills Compatibility</span>
                                    <span className="font-semibold text-gray-800">{project.scoreBreakdown.skillFitScore} / 25</span>
                                </div>
                                <div className="bg-white/90 p-2 rounded-lg border border-indigo-100">
                                    <span className="text-gray-500 block">Assessed Mastery</span>
                                    <span className="font-semibold text-gray-800">{project.scoreBreakdown.proficiencyScore} / 15</span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Section 1: Real-World Problem & Stakeholders */}
                    <div className="pt-4 space-y-3">
                        <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                            <span>🚨</span> Problem Context & Operational Pain Point
                        </h3>
                        <p className="leading-relaxed text-gray-800 bg-gray-50 p-4 rounded-xl border border-gray-100">
                            {project.problemStatement || project.description}
                        </p>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
                            <div className="bg-blue-50/60 border border-blue-100 p-3.5 rounded-xl">
                                <span className="text-xs font-bold text-blue-900 uppercase block mb-1">Target Stakeholders & Users</span>
                                <p className="text-xs text-blue-950">{project.targetStakeholders || 'Industry practitioners and domain specialists.'}</p>
                            </div>
                            <div className="bg-emerald-50/60 border border-emerald-100 p-3.5 rounded-xl">
                                <span className="text-xs font-bold text-emerald-900 uppercase block mb-1">Measurable Outcome & KPIs</span>
                                <p className="text-xs text-emerald-950">{project.expectedOutcomeKpis || 'Measurable engineering performance indicators.'}</p>
                            </div>
                        </div>
                    </div>

                    {/* Section 2: Solution & Operational Workflow */}
                    <div className="pt-4 space-y-3">
                        <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                            <span>⚙️</span> Proposed Solution & Operational Workflow
                        </h3>
                        <p className="leading-relaxed text-gray-800 bg-gray-50 p-4 rounded-xl border border-gray-100">
                            {project.proposedSolution || 'System architecture and end-to-end execution flow.'}
                        </p>
                        {project.innovationDifferentiator && (
                            <div className="p-3.5 rounded-xl bg-purple-50/70 border border-purple-100 text-xs text-purple-950">
                                <span className="font-bold block uppercase mb-0.5 text-purple-900">Innovation & Differentiation</span>
                                {project.innovationDifferentiator}
                            </div>
                        )}
                    </div>

                    {/* Section 3: Industry & Social Value */}
                    {project.industryValue && (
                        <div className="pt-4 space-y-2">
                            <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                                <span>🌐</span> Industry, Economic & Social Value
                            </h3>
                            <p className="leading-relaxed text-gray-700 bg-gray-50 p-3.5 rounded-xl border border-gray-100">
                                {project.industryValue}
                            </p>
                        </div>
                    )}

                    {/* Section 4: Data & Requirements */}
                    <div className="pt-4 space-y-3">
                        <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                            <span>📋</span> Requirements & Data Specifications
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                            <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-100 space-y-1">
                                <span className="font-bold text-gray-900 uppercase block">Functional Requirements</span>
                                <p className="text-gray-700">{project.functionalRequirements || 'Core functional milestones.'}</p>
                            </div>
                            <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-100 space-y-1">
                                <span className="font-bold text-gray-900 uppercase block">Non-Functional & Quality</span>
                                <p className="text-gray-700">{project.nonFunctionalRequirements || 'Latency, reliability and scalability criteria.'}</p>
                            </div>
                        </div>
                        {project.dataRequirements && (
                            <div className="bg-amber-50/60 border border-amber-100 p-3 rounded-xl text-xs text-amber-950">
                                <span className="font-bold uppercase block mb-0.5 text-amber-900">Required Datasets & Sensor Sources</span>
                                {project.dataRequirements}
                            </div>
                        )}
                    </div>

                    {/* Section 5: Testing, Validation & Safety */}
                    <div className="pt-4 space-y-3">
                        <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                            <span>🛡️</span> Validation, Testing & Safety Considerations
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                            <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-100 space-y-1">
                                <span className="font-bold text-gray-900 uppercase block">Testing & Validation Methodology</span>
                                <p className="text-gray-700">{project.testingValidation || 'Verification and bench testing.'}</p>
                            </div>
                            <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-100 space-y-1">
                                <span className="font-bold text-gray-900 uppercase block">Security, Safety & Privacy</span>
                                <p className="text-gray-700">{project.securityPrivacySafety || 'Failsafe protection, access control and encryption.'}</p>
                            </div>
                        </div>
                    </div>

                    {/* Section 6: Required Technologies & Interdisciplinary Skills */}
                    <div className="pt-4 space-y-3">
                        <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                            <span>💻</span> Required Technologies & Technical Skills
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                            <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-100 space-y-2">
                                <span className="font-bold text-gray-900 uppercase block">Required Technologies</span>
                                <div className="flex flex-wrap gap-1.5">
                                    {project.technologies && project.technologies.length > 0 ? (
                                        project.technologies.map((t, i) => (
                                            <span key={i} className="px-2.5 py-1 rounded-lg bg-blue-50 text-blue-800 border border-blue-200 font-semibold text-xs">
                                                {t}
                                            </span>
                                        ))
                                    ) : (
                                        <span className="text-gray-500 italic">Core engineering stack</span>
                                    )}
                                </div>
                            </div>
                            <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-100 space-y-2">
                                <span className="font-bold text-gray-900 uppercase block">Required Skills</span>
                                <div className="flex flex-wrap gap-1.5">
                                    {project.requiredSkills && project.requiredSkills.length > 0 ? (
                                        project.requiredSkills.map((s, i) => (
                                            <span key={i} className="px-2.5 py-1 rounded-lg bg-purple-50 text-purple-800 border border-purple-200 font-semibold text-xs">
                                                {s}
                                            </span>
                                        ))
                                    ) : (
                                        <span className="text-gray-500 italic">Interdisciplinary competencies</span>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Identified Skill Gaps & Action Items */}
                    {project.identifiedGaps && project.identifiedGaps.length > 0 && (
                        <div className="pt-4 space-y-2">
                            <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                                <span>⚠️</span> Identified Skill Gaps & Action Items
                            </h3>
                            <div className="bg-amber-50/70 border border-amber-200 rounded-xl p-4 text-xs space-y-2">
                                <p className="text-amber-900 font-medium">
                                    Before undertaking this project, the system identified the following technical gaps based on your profile and assessments:
                                </p>
                                <ul className="space-y-1.5 text-amber-950">
                                    {project.identifiedGaps.map((gap, i) => (
                                        <li key={i} className="flex items-start gap-2">
                                            <span className="text-amber-600 font-bold shrink-0">•</span>
                                            <span>{gap}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    )}

                    {/* Section 7: Assessed Skills Match Breakdown */}
                    {project.assessedSkillsMatch && project.assessedSkillsMatch.length > 0 && (
                        <div className="pt-4 space-y-3">
                            <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                                <span>🎯</span> Required Skills vs. Your Assessed Competencies
                            </h3>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                                {project.assessedSkillsMatch.map((sm, idx) => (
                                    <div key={idx} className="flex items-center justify-between p-3 rounded-xl border bg-gray-50 border-gray-200">
                                        <div>
                                            <p className="font-semibold text-gray-900">{sm.skillName}</p>
                                            <p className="text-[11px] text-gray-500 mt-0.5">{sm.note}</p>
                                        </div>
                                        <div className="text-right shrink-0 ml-2">
                                            <span className={`px-2 py-0.5 rounded-full font-bold text-[10px] ${levelBadgeColor(sm.assessedLevel)}`}>
                                                {sm.assessedLevel}
                                            </span>
                                            {sm.isAssessed && (
                                                <p className="text-[11px] font-bold text-gray-700 mt-0.5">{sm.assessedPercentage}%</p>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Section 7: Constraints & Metadata */}
                    <div className="pt-4 grid grid-cols-2 sm:grid-cols-4 gap-3 text-center text-xs">
                        <div className="p-3 bg-gray-50 rounded-xl border border-gray-100">
                            <span className="text-gray-400 block mb-0.5">Complexity</span>
                            <span className="font-bold text-gray-800">{project.complexityLevel}</span>
                        </div>
                        <div className="p-3 bg-gray-50 rounded-xl border border-gray-100">
                            <span className="text-gray-400 block mb-0.5">Team Size</span>
                            <span className="font-bold text-gray-800">{project.minTeamSize} – {project.maxTeamSize} members</span>
                        </div>
                        <div className="p-3 bg-gray-50 rounded-xl border border-gray-100">
                            <span className="text-gray-400 block mb-0.5">Estimated Duration</span>
                            <span className="font-bold text-gray-800">{project.estimatedWeeks} weeks</span>
                        </div>
                        <div className="p-3 bg-gray-50 rounded-xl border border-gray-100">
                            <span className="text-gray-400 block mb-0.5">Academic Level</span>
                            <span className="font-bold text-gray-800">{project.academicLevelSuitability || 'UG / PG'}</span>
                        </div>
                    </div>
                </div>

                {/* Modal Footer */}
                <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex items-center justify-between">
                    <p className="text-xs text-gray-500 italic">
                        * Match score represents candidate-project compatibility and readiness based on Phase 5 results.
                    </p>
                    <button
                        onClick={onClose}
                        className="px-5 py-2 text-sm font-semibold bg-gray-900 text-white rounded-xl hover:bg-gray-800 transition-colors"
                    >
                        Close Dossier
                    </button>
                </div>
            </div>
        </div>
    );
};

// ─── Main Recommendations Component ──────────────────────────────────────────

const ProjectRecommendations = () => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState(null);
    const [selectedProject, setSelectedProject] = useState(null);

    // Filters and sorting
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedComplexity, setSelectedComplexity] = useState('ALL');
    const [minMatch, setMinMatch] = useState(0);
    const [sortBy, setSortBy] = useState('RANK');

    const fetchRecommendations = async (forceRefresh = false) => {
        if (forceRefresh) setRefreshing(true);
        else setLoading(true);
        setError(null);

        try {
            const res = await recommendationService.getRecommendations(forceRefresh);
            setData(res.data);
        } catch (err) {
            console.error('Error loading recommendations:', err);
            const msg = err.response?.data?.message || err.response?.data?.error ||
                'Unable to load recommendations. Please verify your profile and assessment completion.';
            setError(msg);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        fetchRecommendations(false);
    }, []);

    // Filter and sort items
    const rawList = data?.recommendations || [];

    const filteredList = rawList
        .filter((item) => {
            if (selectedComplexity !== 'ALL' && item.complexityLevel !== selectedComplexity) {
                return false;
            }
            if (item.matchScore < minMatch) {
                return false;
            }
            if (searchTerm.trim()) {
                const term = searchTerm.toLowerCase();
                const matchTitle = item.title?.toLowerCase().includes(term);
                const matchDomain = item.domainName?.toLowerCase().includes(term);
                const matchProblem = item.problemStatement?.toLowerCase().includes(term);
                const matchTech = item.technologies?.some(t => t.toLowerCase().includes(term));
                if (!matchTitle && !matchDomain && !matchProblem && !matchTech) {
                    return false;
                }
            }
            return true;
        })
        .sort((a, b) => {
            if (sortBy === 'RANK') return a.rankOrder - b.rankOrder;
            if (sortBy === 'MATCH_DESC') return b.matchScore - a.matchScore;
            if (sortBy === 'QUALITY_DESC') return (b.qualityScore || 0) - (a.qualityScore || 0);
            if (sortBy === 'DURATION_ASC') return a.estimatedWeeks - b.estimatedWeeks;
            return 0;
        });

    return (
        <div className="max-w-7xl mx-auto space-y-8 pb-12">

            {/* ─── Hero Header ──────────────────────────────────────────────── */}
            <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 p-8 sm:p-10 text-white shadow-xl shadow-slate-950/20 border border-indigo-900/30">
                <div className="absolute top-0 right-0 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
                <div className="absolute bottom-0 left-1/4 w-80 h-80 bg-violet-600/10 rounded-full blur-3xl pointer-events-none" />

                <div className="relative flex flex-col md:flex-row md:items-center justify-between gap-6">
                    <div className="space-y-2 max-w-2xl">
                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                            <span>✨ Phase 6 Engine</span>
                            <span>•</span>
                            <span>Quality Gate Verified</span>
                        </div>
                        <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
                            Personalized Project Recommendations
                        </h1>
                        <p className="text-indigo-200/90 text-sm sm:text-base leading-relaxed">
                            {data ? (
                                <>
                                    Engineered for <span className="text-white font-medium">{data.studentName}</span> ({data.branchName || 'Engineering'}) in{' '}
                                    <span className="text-white font-medium">{data.domainName || 'Selected Domain'}</span>. Ranked using your Phase 5 assessed competencies.
                                </>
                            ) : (
                                'Evaluating industry-vetted project proposals aligned with your assessed competencies and constraints.'
                            )}
                        </p>
                    </div>

                    <div className="flex items-center gap-3 shrink-0">
                        <button
                            onClick={() => fetchRecommendations(true)}
                            disabled={refreshing || loading}
                            className={`flex items-center gap-2 px-5 py-2.5 text-sm font-semibold rounded-xl bg-gradient-to-r from-indigo-500 to-violet-600 hover:from-indigo-600 hover:to-violet-700 text-white shadow-lg shadow-indigo-500/25 transition-all ${
                                refreshing ? 'opacity-70 cursor-not-allowed' : 'active:scale-95'
                            }`}
                        >
                            <svg className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            {refreshing ? 'Recalculating…' : 'Refresh Recommendations'}
                        </button>
                    </div>
                </div>

                {/* Transparency Notice */}
                <div className="mt-6 pt-4 border-t border-indigo-900/50 flex items-start gap-3 text-xs text-indigo-300/80">
                    <span className="text-base text-indigo-400">💡</span>
                    <p className="leading-relaxed">
                        <strong className="text-indigo-200">Recommendation Transparency:</strong> The match percentage reflects candidate-project fit based on your branch, academic level, and Phase 5 assessment scores. Projects are independently evaluated against industry quality gates requiring verified real-world problems and measurable outcomes.
                    </p>
                </div>
            </div>

            {/* ─── AI Configuration & Idea Synthesis Engine Status Banner ────── */}
            {data?.aiConfigurationStatus && (
                <div className="bg-slate-900 text-slate-200 border border-slate-800 rounded-2xl p-4 sm:p-5 shadow-sm flex items-start gap-3.5 text-xs">
                    <span className="text-xl">🤖</span>
                    <div className="flex-1 space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-bold text-white uppercase tracking-wider text-[11px]">
                                Idea Synthesis Engine:
                            </span>
                            <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                                data.aiConfigurationStatus.includes('Live External')
                                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                                    : 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                            }`}>
                                {data.aiConfigurationStatus.includes('Live External') ? 'Live External LLM Active' : 'Domain-Grounded Synthesizer Active'}
                            </span>
                        </div>
                        <p className="text-slate-300 leading-relaxed">
                            {data.aiConfigurationStatus}
                        </p>
                    </div>
                </div>
            )}

            {/* ─── Controls: Search, Filters & Sorting ───────────────────────── */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 sm:p-5 flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">
                <div className="relative flex-1">
                    <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
                        🔍
                    </span>
                    <input
                        type="text"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        placeholder="Search by title, technology, problem keyword..."
                        className="w-full pl-10 pr-4 py-2 text-sm bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all placeholder:text-gray-400"
                    />
                </div>

                <div className="flex flex-wrap items-center gap-3">
                    {/* Complexity Filter */}
                    <select
                        value={selectedComplexity}
                        onChange={(e) => setSelectedComplexity(e.target.value)}
                        className="px-3.5 py-2 text-xs font-medium text-gray-700 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
                    >
                        <option value="ALL">All Complexities</option>
                        <option value="BEGINNER">Beginner</option>
                        <option value="INTERMEDIATE">Intermediate</option>
                        <option value="ADVANCED">Advanced</option>
                    </select>

                    {/* Min Score Filter */}
                    <select
                        value={minMatch}
                        onChange={(e) => setMinMatch(Number(e.target.value))}
                        className="px-3.5 py-2 text-xs font-medium text-gray-700 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
                    >
                        <option value={0}>All Matches (35%+)</option>
                        <option value={60}>Strong Matches (60%+)</option>
                        <option value={75}>Top Matches (75%+)</option>
                    </select>

                    {/* Sort By */}
                    <select
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value)}
                        className="px-3.5 py-2 text-xs font-medium text-gray-700 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
                    >
                        <option value="RANK">Sort by Best Match</option>
                        <option value="MATCH_DESC">Highest Match %</option>
                        <option value="QUALITY_DESC">Highest Quality Score</option>
                        <option value="DURATION_ASC">Shortest Duration</option>
                    </select>
                </div>
            </div>

            {/* ─── Loading Skeletons ─────────────────────────────────────────── */}
            {loading && (
                <div className="space-y-4">
                    {[1, 2, 3].map((n) => (
                        <div key={n} className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm animate-pulse space-y-4">
                            <div className="flex justify-between items-start">
                                <div className="space-y-2 w-2/3">
                                    <div className="h-4 bg-gray-200 rounded-md w-1/4" />
                                    <div className="h-6 bg-gray-200 rounded-md w-3/4" />
                                </div>
                                <div className="h-12 w-12 bg-gray-200 rounded-full" />
                            </div>
                            <div className="h-16 bg-gray-100 rounded-xl w-full" />
                            <div className="flex gap-2">
                                <div className="h-6 bg-gray-200 rounded-full w-20" />
                                <div className="h-6 bg-gray-200 rounded-full w-24" />
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* ─── Error State ──────────────────────────────────────────────── */}
            {!loading && error && (
                <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center space-y-3 shadow-sm">
                    <span className="text-3xl">⚠️</span>
                    <h3 className="text-base font-bold text-rose-900">Unable to Generate Recommendations</h3>
                    <p className="text-sm text-rose-700 max-w-lg mx-auto">{error}</p>
                    <div className="flex justify-center gap-3 pt-2">
                        <Link
                            to="/profile"
                            className="px-4 py-2 text-xs font-semibold bg-white border border-rose-200 text-rose-800 rounded-xl hover:bg-rose-100 transition-colors"
                        >
                            Review Profile
                        </Link>
                        <Link
                            to="/assessments"
                            className="px-4 py-2 text-xs font-semibold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors"
                        >
                            Take Skill Assessments
                        </Link>
                    </div>
                </div>
            )}

            {/* ─── Empty State ──────────────────────────────────────────────── */}
            {!loading && !error && filteredList.length === 0 && (
                <div className="bg-white rounded-2xl border border-gray-100 p-12 text-center space-y-4 shadow-sm">
                    <span className="text-5xl">🎯</span>
                    <h3 className="text-lg font-bold text-gray-800">No Projects Found Matching Current Criteria</h3>
                    <p className="text-sm text-gray-500 max-w-md mx-auto">
                        {data?.message || 'Try widening your search keywords or resetting your minimum match percentage filter.'}
                    </p>
                    <button
                        onClick={() => {
                            setSearchTerm('');
                            setSelectedComplexity('ALL');
                            setMinMatch(0);
                        }}
                        className="px-5 py-2.5 text-xs font-semibold bg-indigo-50 text-indigo-700 rounded-xl hover:bg-indigo-100 transition-colors"
                    >
                        Reset All Filters
                    </button>
                </div>
            )}

            {/* ─── Recommendations Cards List ──────────────────────────────── */}
            {!loading && !error && filteredList.length > 0 && (
                <div className="space-y-6">
                    <div className="flex items-center justify-between text-xs text-gray-500 px-1">
                        <span>Showing <strong>{filteredList.length}</strong> vetted real-world project recommendations</span>
                        <span>Quality Gate: 100% Passed</span>
                    </div>

                    {filteredList.map((item) => (
                        <div
                            key={item.projectId}
                            className="bg-white rounded-2xl border border-gray-100 hover:border-indigo-200 shadow-sm hover:shadow-md transition-all overflow-hidden flex flex-col md:flex-row divide-y md:divide-y-0 md:divide-x divide-gray-100"
                        >
                            {/* Left Score Column */}
                            <div className="p-6 md:w-56 shrink-0 flex flex-col items-center justify-center text-center bg-gradient-to-b from-gray-50/50 to-white">
                                <div className="mb-2 flex flex-col items-center gap-1">
                                    <span className="px-3 py-1 rounded-full text-xs font-extrabold uppercase bg-slate-900 text-white tracking-wide shadow-sm">
                                        Rank #{item.rankOrder}
                                    </span>
                                    {item.isFreshAlternative && (
                                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                                            ✨ Fresh Alternative
                                        </span>
                                    )}
                                    {item.isGeneratedIdea && (
                                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-100 text-purple-800 border border-purple-300">
                                            💡 AI Synthesized
                                        </span>
                                    )}
                                </div>

                                {/* Circular Score Display */}
                                <div className="relative my-2 h-20 w-20 flex items-center justify-center">
                                    <div className={`absolute inset-0 rounded-full bg-gradient-to-tr ${scoreRingGradient(item.matchScore)} opacity-20`} />
                                    <div className="relative flex flex-col items-center">
                                        <span className="text-2xl font-black text-gray-900 leading-none">
                                            {Math.round(item.matchScore)}%
                                        </span>
                                        <span className="text-[10px] font-bold text-gray-500 uppercase mt-0.5 tracking-tight">
                                            Match
                                        </span>
                                    </div>
                                </div>

                                {/* Feasibility & Suitability Scores */}
                                <div className="mt-2 space-y-1.5 w-full px-1">
                                    <div className="flex items-center justify-between text-[11px] font-semibold bg-indigo-50 text-indigo-800 border border-indigo-200 px-2 py-1 rounded-lg">
                                        <span>Suitability:</span>
                                        <span className="font-extrabold">{Math.round(item.suitabilityScore || item.matchScore)}%</span>
                                    </div>
                                    <div className="flex items-center justify-between text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200 px-2 py-1 rounded-lg">
                                        <span>Feasibility:</span>
                                        <span className="font-extrabold">{Math.round(item.feasibilityScore || 85)}%</span>
                                    </div>
                                    <span className="inline-block w-full text-center px-2 py-0.5 rounded-md text-[10px] font-medium bg-gray-100 text-gray-600">
                                        Quality: {item.qualityScore ? Math.round(item.qualityScore) : 85}/100
                                    </span>
                                </div>
                            </div>

                            {/* Center Project Info */}
                            <div className="p-6 flex-1 space-y-4">
                                <div>
                                    <div className="flex items-center gap-2 mb-2 flex-wrap">
                                        <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${complexityColor(item.complexityLevel)}`}>
                                            {item.complexityLevel}
                                        </span>
                                        <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700">
                                            {item.domainName}
                                        </span>
                                        <span className="text-xs text-gray-500 flex items-center gap-1">
                                            👥 {item.minTeamSize}–{item.maxTeamSize} members
                                        </span>
                                        <span className="text-xs text-gray-500 flex items-center gap-1">
                                            ⏱ ~{item.estimatedWeeks} weeks
                                        </span>
                                        {item.academicLevelSuitability && (
                                            <span className="text-xs text-gray-500 flex items-center gap-1">
                                                🎓 {item.academicLevelSuitability}
                                            </span>
                                        )}
                                    </div>

                                    <h2 className="text-lg sm:text-xl font-bold text-gray-900 hover:text-indigo-600 transition-colors">
                                        {item.title}
                                    </h2>
                                </div>

                                {/* Problem Summary */}
                                <div className="bg-gray-50 rounded-xl p-3.5 border border-gray-100 text-xs text-gray-700 leading-relaxed">
                                    <strong className="text-gray-900 block mb-1">🚨 Real-World Problem Context:</strong>
                                    {item.problemStatement ? (
                                        <p className="line-clamp-2">{item.problemStatement}</p>
                                    ) : (
                                        <p className="line-clamp-2">{item.description}</p>
                                    )}
                                </div>

                                {/* Proposed Solution & Industry Use Case Snippet */}
                                {item.proposedSolution && (
                                    <div className="bg-slate-50 rounded-xl p-3 border border-slate-200/70 text-xs text-slate-700 leading-relaxed">
                                        <strong className="text-slate-900 block mb-0.5">⚙️ Proposed Solution & Workflow:</strong>
                                        <p className="line-clamp-2">{item.proposedSolution}</p>
                                    </div>
                                )}

                                {item.industryValue && (
                                    <div className="bg-blue-50/60 rounded-xl p-2.5 border border-blue-100 text-xs text-blue-900 leading-relaxed flex items-start gap-1.5">
                                        <strong className="shrink-0 text-blue-800">🌐 Industry Use Case:</strong>
                                        <p className="line-clamp-1">{item.industryValue}</p>
                                    </div>
                                )}

                                {/* Outcome / KPIs Snippet */}
                                {item.expectedOutcomeKpis && (
                                    <div className="text-xs text-emerald-900 bg-emerald-50/60 border border-emerald-100 p-2.5 rounded-lg flex items-start gap-2">
                                        <span className="text-emerald-600 font-bold">🎯 KPIs:</span>
                                        <p className="line-clamp-1">{item.expectedOutcomeKpis}</p>
                                    </div>
                                )}

                                {/* Recommendation Reasons & Disclosed Gaps */}
                                <div className="space-y-2 pt-1">
                                    {item.recommendationReasons && item.recommendationReasons.length > 0 && (
                                        <div>
                                            <span className="text-[10px] font-bold text-gray-500 uppercase tracking-wider block mb-1">Why Matched:</span>
                                            <div className="flex flex-wrap gap-1.5">
                                                {item.recommendationReasons.slice(0, 2).map((reason, i) => (
                                                    <span key={i} className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-100">
                                                        <span>✓</span> {reason}
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    )}

                                    {item.identifiedGaps && item.identifiedGaps.length > 0 && (
                                        <div>
                                            <span className="text-[10px] font-bold text-amber-700 uppercase tracking-wider block mb-1">Identified Gaps:</span>
                                            <div className="flex flex-wrap gap-1.5">
                                                {item.identifiedGaps.slice(0, 2).map((gap, i) => (
                                                    <span key={i} className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-md bg-amber-50 text-amber-800 border border-amber-200">
                                                        <span>⚠️</span> {gap}
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Required Technologies & Skills Badges and Action Button */}
                                <div className="pt-2 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-t border-gray-100">
                                    <div className="flex flex-col gap-1 text-[11px]">
                                        {item.technologies && item.technologies.length > 0 && (
                                            <div className="flex items-center gap-1.5 flex-wrap">
                                                <span className="font-bold text-gray-500 text-[10px] uppercase">Tech:</span>
                                                {item.technologies.slice(0, 4).map((t, i) => (
                                                    <span key={i} className="text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-100 px-2 py-0.5 rounded">
                                                        {t}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                        {item.requiredSkills && item.requiredSkills.length > 0 && (
                                            <div className="flex items-center gap-1.5 flex-wrap">
                                                <span className="font-bold text-gray-500 text-[10px] uppercase">Skills:</span>
                                                {item.requiredSkills.slice(0, 4).map((s, i) => (
                                                    <span key={i} className="text-[10px] font-semibold bg-purple-50 text-purple-700 border border-purple-100 px-2 py-0.5 rounded">
                                                        {s}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    <button
                                        onClick={() => setSelectedProject(item)}
                                        className="px-4 py-2 text-xs font-bold rounded-xl bg-gray-900 hover:bg-indigo-600 text-white shadow-sm transition-colors flex items-center gap-1.5 shrink-0 self-end sm:self-auto"
                                    >
                                        <span>View Full Project Dossier</span>
                                        <span>→</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* ─── Detail Modal ─────────────────────────────────────────────── */}
            <ProjectDetailModal
                project={selectedProject}
                onClose={() => setSelectedProject(null)}
            />
        </div>
    );
};

export default ProjectRecommendations;
