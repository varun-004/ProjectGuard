import React, { useEffect, useState } from 'react';
import assessmentService from '../services/assessmentService';
import studentService from '../services/studentService';

// ─── Helpers ────────────────────────────────────────────────────────────────

const levelBadge = (level) => {
    if (level === 'ADVANCED')    return 'bg-green-100 text-green-700 border-green-200';
    if (level === 'INTERMEDIATE') return 'bg-blue-100 text-blue-700 border-blue-200';
    return 'bg-gray-100 text-gray-600 border-gray-200';
};

const diffBadge = (d) => {
    if (d === 'EASY')   return 'bg-green-100 text-green-700';
    if (d === 'MEDIUM') return 'bg-yellow-100 text-yellow-700';
    return 'bg-red-100 text-red-700';
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

    // ─── selection ───────────────────────────────────────────────────────────
    const [selectedTech,     setSelectedTech]     = useState(null);  // { id, name }
    const [selectedSkillIds, setSelectedSkillIds] = useState([]);

    // ─── quiz ────────────────────────────────────────────────────────────────
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
    const loadDashboard = async () => {
        setLoading(true);
        setError('');
        try {
            const [profileRes, resultsRes] = await Promise.all([
                studentService.getMyProfile(),
                assessmentService.getMyResults(),
            ]);
            const p = profileRes.data;
            setProfile(p);
            setResults(resultsRes.data || []);

            // Load technologies for the user's domain
            if (p?.domainId) {
                const techRes = await studentService.getTechnologiesByDomain(p.domainId);
                setTechnologies(techRes.data || []);
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
            if (res.data?.length > 0) {
                setQuestions(res.data);
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
            const res = await assessmentService.submitAssessment(selectedSkillIds, submissions);
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
            <div className="flex items-center justify-center py-20 gap-3">
                <div className="animate-spin h-8 w-8 rounded-full border-4 border-indigo-600 border-t-transparent" />
                <span className="text-gray-500">Loading assessments…</span>
            </div>
        );
    }

    // ─── QUIZ VIEW ────────────────────────────────────────────────────────────
    if (view === 'quiz') {
        const answered = Object.keys(answers).length;
        const progress = Math.round((answered / questions.length) * 100);

        return (
            <div className="max-w-3xl mx-auto space-y-5">
                {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

                <div className="bg-white rounded-xl shadow-sm border border-gray-200">
                    {/* Header */}
                    <div className="px-6 py-4 border-b flex justify-between items-center">
                        <div>
                            <p className="text-xs text-gray-400 uppercase tracking-wide">Domain Assessment</p>
                            <h2 className="text-xl font-bold text-gray-800">{selectedTech?.name} Quiz</h2>
                        </div>
                        <button onClick={() => setView('tech')} className="text-sm text-gray-500 hover:text-gray-700 font-medium border border-gray-200 rounded-lg px-3 py-1.5">
                            ← Back
                        </button>
                    </div>

                    {/* Progress */}
                    <div className="px-6 pt-4">
                        <div className="flex justify-between text-xs text-gray-500 mb-1">
                            <span>{answered}/{questions.length} answered</span>
                            <span>{progress}%</span>
                        </div>
                        <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                            <div className="h-full bg-indigo-500 rounded-full transition-all duration-300" style={{ width: `${progress}%` }} />
                        </div>
                    </div>

                    {/* Questions */}
                    <div className="p-6 space-y-7">
                        {questions.map((q, i) => (
                            <div key={q.id} className={`p-4 rounded-xl border transition-colors ${answers[q.id] !== undefined ? 'border-indigo-200 bg-indigo-50/40' : 'border-gray-100 bg-gray-50'}`}>
                                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-2 mb-3">
                                    <h3 className="font-semibold text-gray-800 leading-snug">
                                        Q{i + 1}. {q.questionText}
                                    </h3>
                                    <div className="flex shrink-0 gap-1.5">
                                        <span className="text-xs px-2 py-1 rounded-full font-medium bg-blue-100 text-blue-700 border border-blue-200">
                                            {q.skillName}
                                        </span>
                                        <span className={`text-xs px-2 py-1 rounded-full font-medium ${diffBadge(q.difficulty)}`}>
                                            {q.difficulty}
                                        </span>
                                    </div>
                                </div>
                                <div className="space-y-2">
                                    {q.options.map((opt, idx) => (
                                        <label key={opt.id} className={`flex items-center gap-3 p-3 border rounded-lg cursor-pointer transition-colors ${answers[q.id] === idx ? 'bg-indigo-100 border-indigo-400 shadow-sm' : 'bg-white border-gray-200 hover:bg-gray-50'}`}>
                                            <input type="radio" name={`q-${q.id}`} className="h-4 w-4 accent-indigo-600"
                                                checked={answers[q.id] === idx}
                                                onChange={() => handleAnswerChange(q.id, idx)} />
                                            <span className="text-gray-700 text-sm">{opt.optionText}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="px-6 py-5 border-t flex justify-between items-center">
                        <p className="text-sm text-gray-500">{questions.length - answered} questions remaining</p>
                        <button onClick={handleSubmit} disabled={quizLoading}
                            className="px-7 py-2.5 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors">
                            {quizLoading ? 'Submitting…' : 'Submit Assessment'}
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ─── RESULT VIEW ──────────────────────────────────────────────────────────
    if (view === 'result' && currentResult) {
        return (
            <div className="max-w-4xl mx-auto space-y-6">
                {/* Overall result card */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
                    <p className="text-sm text-gray-400 uppercase tracking-widest mb-1">Assessment Complete</p>
                    <h2 className="text-3xl font-bold text-gray-800 mb-6">{selectedTech?.name}</h2>

                    <div className="flex flex-wrap justify-center gap-8 mb-6">
                        <div>
                            <span className="block text-5xl font-extrabold text-indigo-600">{currentResult.score}/20</span>
                            <span className="text-sm text-gray-500">Overall Score</span>
                        </div>
                        <div>
                            <span className="block text-5xl font-extrabold text-indigo-600">{Number(currentResult.percentage).toFixed(0)}%</span>
                            <span className="text-sm text-gray-500">Percentage</span>
                        </div>
                    </div>

                    <span className={`inline-block px-8 py-2.5 rounded-full text-xl font-bold border ${levelBadge(currentResult.level)}`}>
                        {currentResult.level}
                    </span>

                    <div className="mt-8 flex justify-center gap-3">
                        <button onClick={() => setView('dashboard')}
                            className="px-6 py-2 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors">
                            Back to Dashboard
                        </button>
                        <button onClick={() => generateQuiz(currentResult.skillResults.map(s => s.skillId))}
                            className="px-6 py-2 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition-colors">
                            Retake Same Skills
                        </button>
                    </div>
                </div>

                {/* Skill-wise table */}
                {currentResult.skillResults?.length > 0 && (
                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                        <div className="px-6 py-4 border-b bg-gray-50">
                            <h3 className="font-semibold text-gray-700">Skill-wise Performance</h3>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left">
                                <thead className="text-xs text-gray-500 uppercase border-b bg-gray-50">
                                    <tr>
                                        <th className="px-5 py-3 font-semibold">Skill</th>
                                        <th className="px-5 py-3 font-semibold">Score</th>
                                        <th className="px-5 py-3 font-semibold">Percentage</th>
                                        <th className="px-5 py-3 font-semibold">Level</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {currentResult.skillResults.map(sr => (
                                        <tr key={sr.skillId} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-5 py-3 font-medium text-gray-800">{sr.skillName}</td>
                                            <td className="px-5 py-3 text-gray-600">{sr.score} pts</td>
                                            <td className="px-5 py-3 text-gray-600">{Number(sr.percentage).toFixed(0)}%</td>
                                            <td className="px-5 py-3">
                                                <span className={`inline-block px-2 py-0.5 text-xs font-bold uppercase rounded border ${levelBadge(sr.level)}`}>
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

                {/* Review / Explanations */}
                {currentResult.reviews?.length > 0 && (
                    <div className="space-y-3">
                        <h3 className="text-lg font-bold text-gray-800">Detailed Review</h3>
                        {currentResult.reviews.map((r, i) => (
                            <div key={r.questionId} className={`p-5 rounded-xl border ${r.correct ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
                                <div className="flex items-start gap-3">
                                    <span className={`mt-0.5 shrink-0 text-lg ${r.correct ? '✅' : '❌'}`}>{r.correct ? '✅' : '❌'}</span>
                                    <div>
                                        <p className="font-semibold text-gray-800">Q{i + 1}. {r.questionText}</p>
                                        <p className="text-sm text-gray-600 mt-1"><strong>Explanation:</strong> {r.explanation}</p>
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
                {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

                <div className="bg-white rounded-xl shadow-sm border border-gray-200">
                    <div className="px-6 py-4 border-b flex justify-between items-center">
                        <div>
                            <p className="text-xs text-gray-400 uppercase tracking-wide">{profile?.domainName}</p>
                            <h2 className="text-xl font-bold text-gray-800">{selectedTech.name}</h2>
                            <p className="text-sm text-gray-500 mt-0.5">Select the skills to include in your quiz</p>
                        </div>
                        <button onClick={() => setView('dashboard')} className="text-sm text-gray-500 hover:text-gray-700 font-medium border border-gray-200 rounded-lg px-3 py-1.5">
                            ← Back
                        </button>
                    </div>

                    <div className="p-6">
                        {skills.length === 0 ? (
                            <p className="text-gray-500 text-sm">No skills configured for this technology yet.</p>
                        ) : (
                            <div className="space-y-4">
                                <div className="flex gap-2 text-sm">
                                    <button onClick={selectAll} className="text-indigo-600 hover:underline font-medium">Select All</button>
                                    <span className="text-gray-300">|</span>
                                    <button onClick={clearAll} className="text-gray-500 hover:underline">Clear</button>
                                    <span className="text-gray-400 ml-2">({selectedSkillIds.length}/{skills.length} selected)</span>
                                </div>

                                <div className="flex flex-wrap gap-2.5">
                                    {skills.map(s => {
                                        const selected = selectedSkillIds.includes(s.id);
                                        return (
                                            <button key={s.id} type="button" onClick={() => toggleSkill(s.id)}
                                                className={`px-4 py-2 rounded-full text-sm font-medium border transition-all shadow-sm ${
                                                    selected
                                                        ? 'bg-indigo-600 border-indigo-600 text-white shadow-indigo-200'
                                                        : 'bg-white border-gray-300 text-gray-700 hover:border-indigo-300 hover:text-indigo-700'
                                                }`}>
                                                {selected ? '✓ ' : ''}{s.name}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="px-6 py-4 border-t bg-gray-50">
                        <button onClick={() => generateQuiz()} disabled={selectedSkillIds.length === 0 || quizLoading}
                            className="w-full py-3 bg-indigo-600 text-white font-semibold rounded-xl hover:bg-indigo-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors text-sm shadow-sm">
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
            <header className="border-b border-gray-200 pb-4">
                <h1 className="text-3xl font-bold text-gray-900">Domain Assessments</h1>
                <p className="mt-1 text-sm text-gray-500">
                    Select a Technology Stack → choose skills → take a combined domain quiz.
                </p>
            </header>

            {profile && (
                <div className="rounded-xl border border-indigo-100 bg-indigo-50 px-5 py-4 flex flex-wrap gap-4">
                    <p className="text-sm text-indigo-700">
                        <strong>Branch:</strong> {profile.branchName || 'Not selected'}
                    </p>
                    <p className="text-sm text-indigo-700">
                        <strong>Domain:</strong> {profile.domainName || 'Not selected'}
                    </p>
                </div>
            )}

            {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Technology Stacks */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 flex flex-col">
                    <div className="px-6 py-4 border-b bg-gray-50">
                        <h2 className="text-lg font-semibold text-gray-800">Technology Stacks</h2>
                        <p className="text-xs text-gray-500 mt-0.5">Pick a stack to choose skills and generate a quiz</p>
                    </div>
                    <div className="p-6 flex-1">
                        {technologies.length === 0 ? (
                            <div className="space-y-2">
                                <p className="text-gray-500 text-sm">No technology stacks found for your domain.</p>
                                <p className="text-xs text-gray-400">Update your profile with a Branch and Domain first.</p>
                            </div>
                        ) : (
                            <div className="space-y-2.5">
                                {technologies.map(tech => (
                                    <button key={tech.id} type="button"
                                        onClick={() => loadSkillsForTech(tech)}
                                        disabled={techLoading}
                                        className="w-full text-left flex justify-between items-center p-4 rounded-xl border border-gray-200 hover:border-indigo-300 hover:bg-indigo-50/40 transition-all group disabled:opacity-50">
                                        <span className="font-medium text-gray-800 group-hover:text-indigo-700">{tech.name}</span>
                                        <span className="text-gray-400 group-hover:text-indigo-500 text-lg">→</span>
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Assessment History */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 flex flex-col">
                    <div className="px-6 py-4 border-b bg-gray-50">
                        <h2 className="text-lg font-semibold text-gray-800">Assessment History</h2>
                    </div>
                    <div className="p-6 flex-1">
                        {results.length === 0 ? (
                            <p className="text-gray-500 text-sm">No assessments taken yet. Pick a technology stack to get started!</p>
                        ) : (
                            <div className="space-y-4 max-h-[520px] overflow-y-auto pr-1">
                                {results.map(r => (
                                    <div key={r.id} className="p-4 border border-gray-200 rounded-xl bg-gray-50">
                                        <div className="flex justify-between items-start mb-2">
                                            <div>
                                                <p className="font-semibold text-gray-800">Domain Quiz Attempt</p>
                                                <p className="text-xs text-gray-400 mt-0.5">
                                                    {r.createdAt ? new Date(r.createdAt).toLocaleString() : ''}
                                                </p>
                                            </div>
                                            <div className="text-right">
                                                <span className={`inline-block px-2 py-0.5 text-xs font-bold uppercase rounded border ${levelBadge(r.level)}`}>
                                                    {r.level}
                                                </span>
                                                <p className="text-sm font-semibold text-gray-700 mt-1">
                                                    {Number(r.percentage).toFixed(0)}% · {r.score}/20 pts
                                                </p>
                                            </div>
                                        </div>

                                        {r.skillResults?.length > 0 && (
                                            <div className="mt-3 pt-3 border-t border-gray-200">
                                                <p className="text-xs font-semibold text-gray-500 mb-2">Skills Tested:</p>
                                                <div className="flex flex-wrap gap-1.5">
                                                    {r.skillResults.map(sr => (
                                                        <span key={sr.skillId}
                                                            className={`text-xs px-2 py-0.5 rounded-full border font-medium ${levelBadge(sr.level)}`}>
                                                            {sr.skillName} · {Number(sr.percentage).toFixed(0)}%
                                                        </span>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Assessments;