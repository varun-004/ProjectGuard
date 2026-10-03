import React, { useEffect, useState } from 'react';
import assessmentService from '../services/assessmentService';
import studentService from '../services/studentService';

// ─── Helpers ────────────────────────────────────────────────────────────────

const levelBadge = (level) => {
    if (level === 'ADVANCED')    return 'bg-green-100 text-green-700 border-green-200';
    if (level === 'INTERMEDIATE') return 'bg-blue-100 text-blue-700 border-blue-200';
    return 'bg-gray-100 text-gray-600 border-gray-200';
};

const levelRing = (pct) => {
    if (pct >= 75) return 'text-emerald-600';
    if (pct >= 50) return 'text-blue-600';
    if (pct >= 25) return 'text-amber-600';
    return 'text-red-500';
};

const diffBadge = (d) => {
    if (d === 'EASY')   return 'bg-green-100 text-green-700';
    if (d === 'MEDIUM') return 'bg-yellow-100 text-yellow-700';
    return 'bg-red-100 text-red-700';
};

const fmt = (dt) => {
    if (!dt) return '';
    const d = new Date(dt);
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) +
        ' · ' + d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
};

// ─── Main Component ─────────────────────────────────────────────────────────

const Assessments = () => {
    // ─── page state ──────────────────────────────────────────────────────────
    const [view, setView] = useState('dashboard'); // dashboard | tech | quiz | result

    // ─── data ────────────────────────────────────────────────────────────────
    const [profile,      setProfile]      = useState(null);
    const [technologies, setTechnologies] = useState([]);
    const [skills,       setSkills]       = useState([]);
    const [results,      setResults]      = useState([]);
    const [currentPage,  setCurrentPage]  = useState(0);
    const [totalPages,   setTotalPages]   = useState(0);
    const [hasNext,      setHasNext]      = useState(false);
    const [hasPrevious,  setHasPrevious]  = useState(false);
    const [historyLoading, setHistoryLoading] = useState(false);

    // ─── selection ───────────────────────────────────────────────────────────
    const [selectedTech,     setSelectedTech]     = useState(null);  // { id, name }
    const [selectedSkillIds, setSelectedSkillIds] = useState([]);

    // ─── quiz ────────────────────────────────────────────────────────────────
    const [attemptId,  setAttemptId]  = useState(null);
    const [questions,  setQuestions]  = useState([]);
    const [answers,    setAnswers]    = useState({});

    // ─── results ─────────────────────────────────────────────────────────────
    const [currentResult, setCurrentResult] = useState(null);

    // ─── loading / error ─────────────────────────────────────────────────────
    const [loading,     setLoading]     = useState(true);
    const [techLoading, setTechLoading] = useState(false);
    const [quizLoading, setQuizLoading] = useState(false);
    const [error,       setError]       = useState('');

    // ─── Effects ─────────────────────────────────────────────────────────────
    useEffect(() => {
        if (view === 'dashboard') loadDashboard();
    }, [view]);

    // ─── Data loading ─────────────────────────────────────────────────────────
    const loadResults = async (page = 0) => {
        setHistoryLoading(true);
        try {
            const res = await assessmentService.getMyResults(page, 10);
            const data = res.data || {};

            setResults(Array.isArray(data.content) ? data.content : []);
            setCurrentPage(data.number ?? page);
            setTotalPages(data.totalPages ?? 0);
            setHasNext(Boolean(data.last === false));
            setHasPrevious(Boolean(data.first === false));
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load assessment history.');
            setResults([]);
        } finally {
            setHistoryLoading(false);
        }
    };

    const loadDashboard = async () => {
        setLoading(true);
        setError('');
        try {
            const [profileRes] = await Promise.all([
                studentService.getMyProfile(),
            ]);

            const p = profileRes.data;
            setProfile(p);

            // Load first page of history
            await loadResults(0);

            // Load technologies for the user's domain
            if (p?.domainId) {
                const techRes = await studentService.getTechnologiesByDomain(p.domainId);
                setTechnologies(techRes.data || []);
            } else {
                setTechnologies([]);
            }
        } catch (err) {
            if (err.response?.status === 400 || err.response?.status === 404) {
                setError('Please complete your Student Profile with Branch and Domain first.');
            } else {
                setError(err.response?.data?.message || 'Failed to load assessment data.');
            }
        } finally {
            setLoading(false);
        }
    };

    const loadSkillsForTech = async (tech) => {
        setTechLoading(true);
        setError('');
        try {
            const res = await studentService.getSkillsByTechnology(tech.id);
            setSkills(res.data || []);
            setSelectedTech(tech);
            setSelectedSkillIds([]);
            setView('tech');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load skills.');
        } finally {
            setTechLoading(false);
        }
    };

    // ─── Skill selection ──────────────────────────────────────────────────────
    const toggleSkill = (id) => {
        setSelectedSkillIds(prev =>
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
        );
    };

    const selectAll = () => setSelectedSkillIds(skills.map(s => s.id));
    const clearAll  = () => setSelectedSkillIds([]);

    // ─── Quiz ─────────────────────────────────────────────────────────────────
    const generateQuiz = async (skillIdsToUse = selectedSkillIds) => {
        if (skillIdsToUse.length === 0) return;
        setQuizLoading(true);
        setError('');
        try {
            const res = await assessmentService.getQuestionsForSkills(skillIdsToUse);
            if (res.data?.questions?.length > 0) {
                setAttemptId(res.data.attemptId);
                setQuestions(res.data.questions);
                setAnswers({});
                if (skillIdsToUse !== selectedSkillIds) setSelectedSkillIds(skillIdsToUse);
                setView('quiz');
            } else {
                setError('No questions available for the selected skills. Please try different skills.');
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load questions.');
        } finally {
            setQuizLoading(false);
        }
    };

    const handleAnswerChange = (questionId, optionIndex) => {
        setAnswers(prev => ({ ...prev, [questionId]: optionIndex }));
    };

    const handleSubmit = async () => {
        if (Object.keys(answers).length < questions.length) {
            if (!window.confirm('You have unanswered questions. Submit anyway?')) return;
        }
        setQuizLoading(true);
        setError('');
        try {
            const submissions = Object.keys(answers).map(qid => ({
                questionId: Number(qid),
                selectedOptionIndex: answers[qid],
            }));
            const res = await assessmentService.submitAssessment(attemptId, selectedSkillIds, submissions);
            setCurrentResult(res.data);
            setView('result');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to submit assessment.');
        } finally {
            setQuizLoading(false);
        }
    };

    // ─── Loading spinner ──────────────────────────────────────────────────────
    if (loading) {
        return (
            <div className="flex items-center justify-center py-24">
                <div className="flex flex-col items-center gap-4 text-gray-400">
                    <svg className="animate-spin h-10 w-10 text-indigo-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    <span className="text-sm font-medium">Loading assessments…</span>
                </div>
            </div>
        );
    }

    // ─── QUIZ VIEW ────────────────────────────────────────────────────────────
    if (view === 'quiz') {
        const answered = Object.keys(answers).length;
        const progress = questions.length > 0 ? Math.round((answered / questions.length) * 100) : 0;

        return (
            <div className="max-w-3xl mx-auto space-y-5">
                {error && (
                    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
                )}

                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                    {/* Header */}
                    <div className="px-6 py-5 border-b border-gray-50 flex justify-between items-center bg-gradient-to-r from-indigo-600 to-violet-600 text-white">
                        <div>
                            <p className="text-xs text-indigo-200 uppercase tracking-widest font-medium">Domain Assessment</p>
                            <h2 className="text-xl font-bold mt-0.5">{selectedTech?.name} Quiz</h2>
                        </div>
                        <button
                            onClick={() => setView('tech')}
                            className="text-sm text-white/70 hover:text-white font-medium border border-white/30 rounded-lg px-3 py-1.5 hover:bg-white/10 transition-colors"
                        >
                            ← Back
                        </button>
                    </div>

                    {/* Progress bar */}
                    <div className="px-6 pt-5">
                        <div className="flex justify-between text-xs text-gray-500 mb-2 font-medium">
                            <span>{answered} of {questions.length} answered</span>
                            <span className="text-indigo-600 font-bold">{progress}%</span>
                        </div>
                        <div className="h-2.5 bg-gray-100 rounded-full overflow-hidden">
                            <div
                                className="h-full bg-gradient-to-r from-indigo-500 to-violet-500 rounded-full transition-all duration-500"
                                style={{ width: `${progress}%` }}
                            />
                        </div>
                    </div>

                    {/* Questions */}
                    <div className="p-6 space-y-6">
                        {questions.map((q, i) => (
                            <div
                                key={q.id}
                                className={`p-5 rounded-2xl border transition-all ${answers[q.id] !== undefined
                                    ? 'border-indigo-200 bg-indigo-50/50 shadow-sm'
                                    : 'border-gray-100 bg-gray-50/80'
                                    }`}
                            >
                                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-2 mb-4">
                                    <h3 className="font-semibold text-gray-800 leading-snug">
                                        <span className="text-indigo-400 font-bold mr-2">Q{i + 1}.</span>
                                        {q.questionText}
                                    </h3>
                                    <div className="flex shrink-0 gap-1.5">
                                        <span className="text-xs px-2.5 py-1 rounded-full font-medium bg-blue-100 text-blue-700 border border-blue-200">
                                            {q.skillName}
                                        </span>
                                        <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${diffBadge(q.difficulty)}`}>
                                            {q.difficulty}
                                        </span>
                                    </div>
                                </div>
                                <div className="space-y-2">
                                    {q.options.map((opt, idx) => (
                                        <label
                                            key={opt.id}
                                            className={`flex items-center gap-3 p-3.5 border rounded-xl cursor-pointer transition-all ${answers[q.id] === idx
                                                ? 'bg-indigo-100 border-indigo-400 shadow-sm'
                                                : 'bg-white border-gray-200 hover:bg-gray-50 hover:border-gray-300'
                                                }`}
                                        >
                                            <input
                                                type="radio"
                                                name={`q-${q.id}`}
                                                className="h-4 w-4 accent-indigo-600 shrink-0"
                                                checked={answers[q.id] === idx}
                                                onChange={() => handleAnswerChange(q.id, idx)}
                                            />
                                            <span className="text-gray-700 text-sm">{opt.optionText}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="px-6 py-5 border-t border-gray-100 bg-gray-50/80 flex justify-between items-center">
                        <p className="text-sm text-gray-500">
                            {questions.length - answered > 0
                                ? `${questions.length - answered} question${questions.length - answered > 1 ? 's' : ''} remaining`
                                : '✓ All questions answered'}
                        </p>
                        <button
                            onClick={handleSubmit}
                            disabled={quizLoading}
                            className="px-8 py-2.5 bg-gradient-to-r from-indigo-600 to-violet-600 text-white font-semibold rounded-xl hover:from-indigo-700 hover:to-violet-700 disabled:opacity-50 transition-all shadow-md shadow-indigo-200"
                        >
                            {quizLoading ? 'Submitting…' : 'Submit Assessment'}
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ─── RESULT VIEW ──────────────────────────────────────────────────────────
    if (view === 'result' && currentResult) {
        const pct = Number(currentResult.percentage) || 0;
        return (
            <div className="max-w-4xl mx-auto space-y-6">

                {/* Overall result */}
                <div className="relative overflow-hidden bg-gradient-to-br from-indigo-600 to-violet-700 rounded-2xl p-10 text-white shadow-xl shadow-indigo-200 text-center">
                    <div className="absolute -top-10 -right-10 h-36 w-36 rounded-full bg-white/10 blur-2xl pointer-events-none" />
                    <p className="text-indigo-200 text-xs uppercase tracking-widest font-semibold mb-2">Assessment Complete</p>
                    <h2 className="text-3xl font-bold mb-8">{selectedTech?.name}</h2>

                    <div className="flex flex-wrap justify-center gap-10 mb-8">
                        {[
                            { val: `${pct.toFixed(0)}%`, sub: 'Score' },
                            { val: `${currentResult.correctAnswers}/${currentResult.totalQuestions}`, sub: 'Correct' },
                            { val: `${currentResult.score}/${currentResult.maxWeightedScore}`, sub: 'Weighted Pts' },
                        ].map(({ val, sub }) => (
                            <div key={sub}>
                                <span className="block text-5xl font-extrabold">{val}</span>
                                <span className="text-indigo-200 text-sm mt-1 block">{sub}</span>
                            </div>
                        ))}
                    </div>

                    <span className={`inline-block px-8 py-2.5 rounded-full text-base font-bold border-2 bg-white/15 border-white/50`}>
                        {currentResult.level}
                    </span>

                    <div className="mt-8 flex justify-center gap-3">
                        <button
                            onClick={() => setView('dashboard')}
                            className="px-6 py-2.5 bg-white/15 border border-white/30 text-white font-medium rounded-xl hover:bg-white/25 transition-colors"
                        >
                            Back to Dashboard
                        </button>
                        <button
                            onClick={() => generateQuiz(currentResult.skillResults.map(s => s.skillId))}
                            className="px-6 py-2.5 bg-white text-indigo-700 font-semibold rounded-xl hover:bg-indigo-50 transition-colors shadow-md"
                        >
                            Retake Same Skills
                        </button>
                    </div>
                </div>

                {/* Skill-wise table */}
                {currentResult.skillResults?.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-50 bg-gray-50/80">
                            <h3 className="font-semibold text-gray-700">Skill-wise Performance</h3>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left">
                                <thead className="text-xs text-gray-500 uppercase border-b bg-gray-50/80">
                                    <tr>
                                        <th className="px-5 py-3 font-semibold">Skill</th>
                                        <th className="px-5 py-3 font-semibold">Score</th>
                                        <th className="px-5 py-3 font-semibold">Percentage</th>
                                        <th className="px-5 py-3 font-semibold">Level</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-50">
                                    {currentResult.skillResults.map(sr => (
                                        <tr key={sr.skillId} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-5 py-3.5 font-medium text-gray-800">{sr.skillName}</td>
                                            <td className="px-5 py-3.5 text-gray-600">{sr.score}/{sr.maxWeightedScore} pts</td>
                                            <td className="px-5 py-3.5">
                                                <div className="flex items-center gap-2">
                                                    <div className="h-1.5 w-20 bg-gray-100 rounded-full overflow-hidden">
                                                        <div
                                                            className={`h-full rounded-full ${Number(sr.percentage) >= 75 ? 'bg-emerald-500' : Number(sr.percentage) >= 50 ? 'bg-blue-500' : 'bg-amber-400'}`}
                                                            style={{ width: `${sr.percentage}%` }}
                                                        />
                                                    </div>
                                                    <span className={`text-sm font-semibold ${levelRing(Number(sr.percentage))}`}>
                                                        {Number(sr.percentage).toFixed(0)}%
                                                    </span>
                                                </div>
                                            </td>
                                            <td className="px-5 py-3.5">
                                                <span className={`inline-block px-2.5 py-0.5 text-xs font-bold uppercase rounded-full border ${levelBadge(sr.level)}`}>
                                                    {sr.level}
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}

                {/* Detailed review */}
                {currentResult.reviews?.length > 0 && (
                    <div className="space-y-3">
                        <h3 className="text-lg font-bold text-gray-800">Detailed Review</h3>
                        {currentResult.reviews.map((r, i) => (
                            <div
                                key={r.questionId}
                                className={`p-5 rounded-2xl border ${r.correct
                                    ? 'bg-emerald-50 border-emerald-200'
                                    : 'bg-red-50 border-red-200'
                                    }`}
                            >
                                <div className="flex items-start gap-3">
                                    <span className={`text-xl shrink-0 mt-0.5 ${r.correct ? 'text-emerald-500' : 'text-red-500'}`}>
                                        {r.correct ? '✓' : '✕'}
                                    </span>
                                    <div>
                                        <p className="font-semibold text-gray-800">Q{i + 1}. {r.questionText}</p>
                                        <p className="text-sm text-gray-600 mt-1.5">
                                            <strong>Explanation: </strong>{r.explanation}
                                        </p>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    // ─── TECHNOLOGY SKILL SELECTION VIEW ─────────────────────────────────────
    if (view === 'tech' && selectedTech) {
        return (
            <div className="max-w-2xl mx-auto space-y-5">
                {error && (
                    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
                )}

                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                    <div className="px-6 py-5 border-b border-gray-50 flex justify-between items-center">
                        <div>
                            <p className="text-xs text-gray-400 uppercase tracking-widest font-medium">{profile?.domainName}</p>
                            <h2 className="text-xl font-bold text-gray-800 mt-0.5">{selectedTech.name}</h2>
                            <p className="text-sm text-gray-500 mt-1">Select the skills to include in your quiz</p>
                        </div>
                        <button
                            onClick={() => setView('dashboard')}
                            className="text-sm text-gray-500 hover:text-gray-700 font-medium border border-gray-200 rounded-xl px-3 py-1.5 hover:bg-gray-50 transition-colors"
                        >
                            ← Back
                        </button>
                    </div>

                    <div className="p-6">
                        {skills.length === 0 ? (
                            <p className="text-gray-500 text-sm text-center py-4">No skills configured for this technology yet.</p>
                        ) : (
                            <div className="space-y-5">
                                <div className="flex items-center gap-3 text-sm">
                                    <button onClick={selectAll} className="text-indigo-600 hover:text-indigo-800 font-semibold transition-colors">Select All</button>
                                    <span className="text-gray-200">|</span>
                                    <button onClick={clearAll} className="text-gray-500 hover:text-gray-700 transition-colors">Clear</button>
                                    <span className="text-gray-400 ml-1 text-xs">({selectedSkillIds.length}/{skills.length} selected)</span>
                                </div>

                                <div className="flex flex-wrap gap-2.5">
                                    {skills.map(s => {
                                        const selected = selectedSkillIds.includes(s.id);
                                        return (
                                            <button
                                                key={s.id}
                                                type="button"
                                                onClick={() => toggleSkill(s.id)}
                                                className={`px-4 py-2 rounded-full text-sm font-medium border transition-all shadow-sm ${selected
                                                    ? 'bg-indigo-600 border-indigo-600 text-white shadow-indigo-200'
                                                    : 'bg-white border-gray-200 text-gray-700 hover:border-indigo-300 hover:text-indigo-700 hover:shadow-indigo-100'
                                                    }`}
                                            >
                                                {selected ? '✓ ' : ''}{s.name}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="px-6 py-5 border-t border-gray-50 bg-gray-50/80">
                        <button
                            onClick={() => generateQuiz()}
                            disabled={selectedSkillIds.length === 0 || quizLoading}
                            className="w-full py-3 bg-gradient-to-r from-indigo-600 to-violet-600 text-white font-semibold rounded-xl hover:from-indigo-700 hover:to-violet-700 disabled:opacity-40 disabled:cursor-not-allowed transition-all shadow-md shadow-indigo-200 text-sm"
                        >
                            {quizLoading
                                ? 'Generating Quiz…'
                                : selectedSkillIds.length === 0
                                    ? 'Select at least one skill'
                                    : `Generate Quiz — ${selectedSkillIds.length} skill${selectedSkillIds.length > 1 ? 's' : ''} selected`}
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ─── DASHBOARD VIEW ───────────────────────────────────────────────────────
    return (
        <div className="max-w-5xl mx-auto space-y-8">

            <header className="border-b border-gray-100 pb-5">
                <h1 className="text-3xl font-bold text-gray-900">Domain Assessments</h1>
                <p className="mt-1.5 text-sm text-gray-500">
                    Pick a Technology Stack → choose skills → take a combined domain quiz.
                </p>
            </header>

            {profile && (
                <div className="rounded-2xl border border-indigo-100 bg-gradient-to-r from-indigo-50 to-violet-50 px-6 py-4 flex flex-wrap gap-4 items-center">
                    <span className="text-lg">🎓</span>
                    <div className="flex flex-wrap gap-4 text-sm text-indigo-700">
                        <span>
                            <strong className="font-semibold">Branch:</strong>{' '}
                            {profile.branchName || 'Not selected'}
                        </span>
                        <span className="text-indigo-300">·</span>
                        <span>
                            <strong className="font-semibold">Domain:</strong>{' '}
                            {profile.domainName || 'Not selected'}
                        </span>
                    </div>
                </div>
            )}

            {error && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                {/* Technology Stacks */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 flex flex-col">
                    <div className="px-6 py-4 border-b border-gray-50 bg-gray-50/80">
                        <h2 className="text-base font-semibold text-gray-800">Technology Stacks</h2>
                        <p className="text-xs text-gray-500 mt-0.5">Pick a stack to choose skills and generate a quiz</p>
                    </div>
                    <div className="p-6 flex-1">
                        {technologies.length === 0 ? (
                            <div className="space-y-2 text-center py-4">
                                <span className="text-3xl">🔍</span>
                                <p className="text-gray-500 text-sm">No technology stacks found for your domain.</p>
                                <p className="text-xs text-gray-400">Update your profile with a Branch and Domain first.</p>
                            </div>
                        ) : (
                            <div className="space-y-2.5">
                                {technologies.map(tech => (
                                    <button
                                        key={tech.id}
                                        type="button"
                                        onClick={() => loadSkillsForTech(tech)}
                                        disabled={techLoading}
                                        className="w-full text-left flex justify-between items-center p-4 rounded-xl border border-gray-100 hover:border-indigo-200 hover:bg-indigo-50/50 transition-all group disabled:opacity-50 shadow-sm hover:shadow-indigo-100"
                                    >
                                        <div>
                                            <span className="font-semibold text-gray-800 group-hover:text-indigo-700 block">{tech.name}</span>
                                        </div>
                                        <svg className="w-5 h-5 text-gray-300 group-hover:text-indigo-400 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                                        </svg>
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Assessment History */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 flex flex-col">
                    <div className="px-6 py-4 border-b border-gray-50 bg-gray-50/80 flex items-center justify-between">
                        <div>
                            <h2 className="text-base font-semibold text-gray-800">Assessment History</h2>
                            {totalPages > 0 && (
                                <p className="text-xs text-gray-400 mt-0.5">Page {currentPage + 1} of {totalPages}</p>
                            )}
                        </div>
                        {historyLoading && (
                            <svg className="animate-spin h-4 w-4 text-indigo-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                        )}
                    </div>
                    <div className="p-6 flex-1 flex flex-col">
                        {results.length === 0 ? (
                            <div className="flex-1 flex flex-col items-center justify-center py-8 text-center gap-3">
                                <span className="text-4xl">🎯</span>
                                <p className="text-gray-500 text-sm">No assessments taken yet.</p>
                                <p className="text-xs text-gray-400">Pick a technology stack on the left to get started!</p>
                            </div>
                        ) : (
                            <>
                                <div className="space-y-3 flex-1 overflow-y-auto max-h-[420px] pr-1">
                                    {results.map(r => {
                                        const pct = Number(r.percentage) || 0;
                                        const skillNames = r.skillResults?.map(s => s.skillName).filter(Boolean) || [];
                                        return (
                                            <div key={r.id} className="p-4 border border-gray-100 rounded-2xl bg-gray-50/80 hover:border-indigo-100 hover:bg-indigo-50/30 transition-colors">
                                                <div className="flex justify-between items-start gap-3 mb-2">
                                                    <div className="min-w-0">
                                                        <p className="text-sm font-semibold text-gray-800 truncate">
                                                            Domain Assessment
                                                        </p>
                                                        <p className="text-xs text-gray-400 mt-0.5">{fmt(r.createdAt)}</p>
                                                    </div>
                                                    <div className="text-right shrink-0">
                                                        <span className={`inline-block px-2.5 py-0.5 text-xs font-bold uppercase rounded-full border ${levelBadge(r.level)}`}>
                                                            {r.level}
                                                        </span>
                                                        <p className={`text-sm font-bold mt-1 ${levelRing(pct)}`}>
                                                            {pct.toFixed(0)}%
                                                        </p>
                                                    </div>
                                                </div>

                                                {/* Progress bar */}
                                                <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden mb-3">
                                                    <div
                                                        className={`h-full rounded-full transition-all ${pct >= 75 ? 'bg-emerald-500' : pct >= 50 ? 'bg-blue-500' : pct >= 25 ? 'bg-amber-400' : 'bg-red-400'}`}
                                                        style={{ width: `${pct}%` }}
                                                    />
                                                </div>

                                                {skillNames.length > 0 && (
                                                    <div className="flex flex-wrap gap-1.5">
                                                        {skillNames.slice(0, 5).map((name, idx) => {
                                                            const sr = r.skillResults[idx];
                                                            return (
                                                                <span
                                                                    key={sr?.skillId ?? idx}
                                                                    className={`text-xs px-2 py-0.5 rounded-full border font-medium ${levelBadge(sr?.level)}`}
                                                                >
                                                                    {name} · {Number(sr?.percentage || 0).toFixed(0)}%
                                                                </span>
                                                            );
                                                        })}
                                                        {skillNames.length > 5 && (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500 border border-gray-200">
                                                                +{skillNames.length - 5} more
                                                            </span>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>

                                {totalPages > 1 && (
                                    <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-100">
                                        <button
                                            type="button"
                                            disabled={!hasPrevious || historyLoading}
                                            onClick={() => loadResults(currentPage - 1)}
                                            className="px-4 py-1.5 text-sm font-medium border border-gray-200 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                                        >
                                            ← Prev
                                        </button>

                                        <span className="text-xs text-gray-500">
                                            {currentPage + 1} / {totalPages}
                                        </span>

                                        <button
                                            type="button"
                                            disabled={!hasNext || historyLoading}
                                            onClick={() => loadResults(currentPage + 1)}
                                            className="px-4 py-1.5 text-sm font-medium border border-gray-200 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                                        >
                                            Next →
                                        </button>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Assessments;
