import React, { useEffect, useState, useContext } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import adminService from '../services/adminService';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (n) => (n ?? 0).toLocaleString();

// ─── Stat Card ────────────────────────────────────────────────────────────────

const StatCard = ({ label, value, sub, icon, gradient, delay = 0 }) => (
    <div
        className="relative bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-lg transition-all duration-300 overflow-hidden group"
        style={{ animationDelay: `${delay}ms` }}
    >
        {/* Gradient accent bar */}
        <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${gradient}`} />
        <div className="p-6">
            <div className="flex items-start justify-between">
                <div>
                    <p className="text-3xl font-black text-gray-900 leading-none mb-1">{fmt(value)}</p>
                    <p className="text-sm font-semibold text-gray-600">{label}</p>
                    {sub && <p className="text-xs text-gray-400 mt-1">{sub}</p>}
                </div>
                <div className={`h-12 w-12 rounded-2xl bg-gradient-to-br ${gradient} flex items-center justify-center shadow-md group-hover:scale-110 transition-transform duration-200`}>
                    <span className="text-white text-xl">{icon}</span>
                </div>
            </div>
        </div>
    </div>
);

// ─── Section Header ────────────────────────────────────────────────────────────

const SectionHeader = ({ icon, title, subtitle }) => (
    <div className="flex items-center gap-3 mb-6">
        <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center shadow-sm">
            <span className="text-lg">{icon}</span>
        </div>
        <div>
            <h2 className="text-lg font-bold text-gray-900">{title}</h2>
            {subtitle && <p className="text-xs text-gray-500 mt-0.5">{subtitle}</p>}
        </div>
    </div>
);

// ─── Metric Row ───────────────────────────────────────────────────────────────

const MetricRow = ({ label, value, total, color = 'indigo' }) => {
    const pct = total > 0 ? Math.round((value / total) * 100) : 0;
    const barColors = {
        indigo: 'bg-indigo-500',
        emerald: 'bg-emerald-500',
        violet: 'bg-violet-500',
        amber: 'bg-amber-500',
        rose: 'bg-rose-500',
        sky: 'bg-sky-500',
    };
    return (
        <div className="flex items-center gap-4">
            <p className="text-sm font-medium text-gray-700 w-44 shrink-0">{label}</p>
            <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                <div
                    className={`h-full ${barColors[color] || 'bg-indigo-500'} rounded-full transition-all duration-700`}
                    style={{ width: `${pct}%` }}
                />
            </div>
            <p className="text-sm font-bold text-gray-900 w-16 text-right shrink-0">{fmt(value)}</p>
            <p className="text-xs text-gray-400 w-10 text-right shrink-0">{pct}%</p>
        </div>
    );
};

// ─── Loading Skeleton ──────────────────────────────────────────────────────────

const Skeleton = ({ className = '' }) => (
    <div className={`animate-pulse bg-gray-200 rounded-xl ${className}`} />
);

// ─── Main Component ───────────────────────────────────────────────────────────

const AdminDashboard = () => {
    const { user } = useContext(AuthContext);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Guard: only ADMIN role can see this page
    if (user && user.role !== 'ROLE_ADMIN') {
        return <Navigate to="/dashboard" replace />;
    }

    useEffect(() => {
        let isMounted = true;
        const fetchStats = async () => {
            setLoading(true);
            setError('');
            try {
                const res = await adminService.getStats();
                if (isMounted) setStats(res.data);
            } catch (err) {
                if (isMounted) {
                    setError(
                        err?.response?.status === 403
                            ? 'Access denied. Admin role required.'
                            : err?.response?.data?.message || 'Failed to load admin statistics.'
                    );
                }
            } finally {
                if (isMounted) setLoading(false);
            }
        };
        fetchStats();
        return () => { isMounted = false; };
    }, []);

    const refreshStats = () => {
        setLoading(true);
        setStats(null);
        adminService.getStats()
            .then(res => setStats(res.data))
            .catch(err => setError(err?.response?.data?.message || 'Failed to refresh.'))
            .finally(() => setLoading(false));
    };

    return (
        <div className="space-y-8 pb-10">

            {/* ─── Page Header ────────────────────────────────────────────── */}
            <div className="relative bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 rounded-3xl px-8 py-10 overflow-hidden shadow-xl">
                {/* Decorative blobs */}
                <div className="absolute -top-16 -right-16 h-64 w-64 rounded-full bg-indigo-500/10 blur-3xl pointer-events-none" />
                <div className="absolute -bottom-10 -left-10 h-48 w-48 rounded-full bg-violet-500/10 blur-2xl pointer-events-none" />

                <div className="relative flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                    <div>
                        <div className="flex items-center gap-2 mb-3">
                            <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-red-500/20 text-red-300 border border-red-400/30 uppercase tracking-wider">
                                🔐 Admin Console
                            </span>
                        </div>
                        <h1 className="text-3xl sm:text-4xl font-black text-white leading-tight">
                            Platform Overview
                        </h1>
                        <p className="mt-2 text-sm text-indigo-300 max-w-xl">
                            Real-time statistics across all students, projects, assessments, and AI-generated content on ProjectGuard.
                        </p>
                    </div>
                    <div className="flex flex-col items-start sm:items-end gap-2 shrink-0">
                        <button
                            onClick={refreshStats}
                            disabled={loading}
                            className="flex items-center gap-2 px-5 py-2.5 text-sm font-bold bg-white/10 hover:bg-white/20 text-white border border-white/20 rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <svg className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            {loading ? 'Loading…' : 'Refresh Stats'}
                        </button>
                        <p className="text-xs text-indigo-400">
                            Logged in as <strong className="text-indigo-200">{user?.username}</strong>
                        </p>
                    </div>
                </div>
            </div>

            {/* ─── Error State ────────────────────────────────────────────── */}
            {error && (
                <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 flex items-start gap-4">
                    <span className="text-3xl">⚠️</span>
                    <div>
                        <p className="text-base font-bold text-rose-900">Unable to Load Statistics</p>
                        <p className="text-sm text-rose-700 mt-1">{error}</p>
                        <p className="text-xs text-rose-500 mt-2">
                            Make sure the backend is running and your account has the ADMIN role.
                        </p>
                    </div>
                </div>
            )}

            {/* ─── KPI Grid ───────────────────────────────────────────────── */}
            {loading ? (
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                    {Array.from({ length: 8 }).map((_, i) => (
                        <div key={i} className="bg-white rounded-2xl border border-gray-100 p-6 space-y-3">
                            <Skeleton className="h-8 w-20" />
                            <Skeleton className="h-4 w-32" />
                        </div>
                    ))}
                </div>
            ) : stats && (
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                    <StatCard
                        label="Total Users"
                        value={stats.totalUsers}
                        sub="All registered accounts"
                        icon="👥"
                        gradient="from-indigo-500 to-indigo-600"
                        delay={0}
                    />
                    <StatCard
                        label="Students"
                        value={stats.totalStudents}
                        sub={`${stats.totalAdmins} admin(s)`}
                        icon="🎓"
                        gradient="from-violet-500 to-violet-600"
                        delay={50}
                    />
                    <StatCard
                        label="Total Projects"
                        value={stats.totalProjects}
                        sub={`${stats.catalogProjects} in catalog`}
                        icon="📁"
                        gradient="from-emerald-500 to-teal-600"
                        delay={100}
                    />
                    <StatCard
                        label="Approved Projects"
                        value={stats.approvedProjects}
                        sub="Passed quality gate"
                        icon="✅"
                        gradient="from-teal-500 to-emerald-600"
                        delay={150}
                    />
                    <StatCard
                        label="AI-Generated Projects"
                        value={stats.aiGeneratedProjects}
                        sub="Gemini synthesized"
                        icon="🤖"
                        gradient="from-purple-500 to-violet-600"
                        delay={200}
                    />
                    <StatCard
                        label="Assessment Attempts"
                        value={stats.totalAssessmentAttempts}
                        sub="Quiz sessions taken"
                        icon="📝"
                        gradient="from-amber-400 to-orange-500"
                        delay={250}
                    />
                    <StatCard
                        label="Skill Results"
                        value={stats.totalAssessmentResults}
                        sub="Individual skill scores"
                        icon="🏆"
                        gradient="from-sky-500 to-blue-600"
                        delay={300}
                    />
                    <StatCard
                        label="Technologies"
                        value={stats.totalTechnologies}
                        sub={`${stats.totalSkills} skills · ${stats.totalDomains} domains`}
                        icon="⚙️"
                        gradient="from-rose-500 to-pink-600"
                        delay={350}
                    />
                </div>
            )}

            {/* ─── Detailed Breakdown ─────────────────────────────────────── */}
            {!loading && stats && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                    {/* User Breakdown */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                        <SectionHeader
                            icon="👤"
                            title="User Breakdown"
                            subtitle="Registered accounts by role"
                        />
                        <div className="space-y-4">
                            <MetricRow
                                label="Students"
                                value={stats.totalStudents}
                                total={stats.totalUsers}
                                color="indigo"
                            />
                            <MetricRow
                                label="Administrators"
                                value={stats.totalAdmins}
                                total={stats.totalUsers}
                                color="rose"
                            />
                        </div>
                        <div className="mt-6 pt-4 border-t border-gray-50 flex items-center justify-between text-xs text-gray-500">
                            <span>Total registered: <strong className="text-gray-900">{fmt(stats.totalUsers)}</strong></span>
                        </div>
                    </div>

                    {/* Project Breakdown */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                        <SectionHeader
                            icon="📂"
                            title="Project Catalog Breakdown"
                            subtitle="Distribution by type and quality gate"
                        />
                        <div className="space-y-4">
                            <MetricRow
                                label="Catalog Projects"
                                value={stats.catalogProjects}
                                total={stats.totalProjects}
                                color="emerald"
                            />
                            <MetricRow
                                label="Quality Approved"
                                value={stats.approvedProjects}
                                total={stats.totalProjects}
                                color="sky"
                            />
                            <MetricRow
                                label="AI Generated (Gemini)"
                                value={stats.aiGeneratedProjects}
                                total={stats.totalProjects}
                                color="violet"
                            />
                        </div>
                        <div className="mt-6 pt-4 border-t border-gray-50 flex items-center justify-between text-xs text-gray-500">
                            <span>Total projects: <strong className="text-gray-900">{fmt(stats.totalProjects)}</strong></span>
                            <span>
                                Approval rate:{' '}
                                <strong className="text-emerald-700">
                                    {stats.totalProjects > 0
                                        ? Math.round((stats.approvedProjects / stats.totalProjects) * 100)
                                        : 0}%
                                </strong>
                            </span>
                        </div>
                    </div>

                    {/* Assessment Activity */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                        <SectionHeader
                            icon="📊"
                            title="Assessment Activity"
                            subtitle="Quiz attempts and skill evaluation results"
                        />
                        <div className="grid grid-cols-2 gap-4">
                            <div className="bg-amber-50 border border-amber-100 rounded-2xl p-4 text-center">
                                <p className="text-3xl font-black text-amber-700">{fmt(stats.totalAssessmentAttempts)}</p>
                                <p className="text-xs font-semibold text-amber-600 mt-1">Total Quiz Attempts</p>
                            </div>
                            <div className="bg-sky-50 border border-sky-100 rounded-2xl p-4 text-center">
                                <p className="text-3xl font-black text-sky-700">{fmt(stats.totalAssessmentResults)}</p>
                                <p className="text-xs font-semibold text-sky-600 mt-1">Skill Results Recorded</p>
                            </div>
                        </div>
                        <div className="mt-4 bg-gray-50 rounded-xl p-4 text-xs text-gray-600 leading-relaxed">
                            <strong className="text-gray-800">Avg results per attempt:</strong>{' '}
                            {stats.totalAssessmentAttempts > 0
                                ? (stats.totalAssessmentResults / stats.totalAssessmentAttempts).toFixed(1)
                                : '—'}{' '}
                            skills evaluated per session.
                        </div>
                    </div>

                    {/* Reference Data */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                        <SectionHeader
                            icon="🗂️"
                            title="Reference Data"
                            subtitle="Branches, domains, technologies, and skills seeded in DB"
                        />
                        <div className="grid grid-cols-2 gap-3">
                            {[
                                { label: 'Branches', value: stats.totalBranches, icon: '🌿', color: 'bg-green-50 border-green-100 text-green-700' },
                                { label: 'Domains', value: stats.totalDomains, icon: '🎯', color: 'bg-blue-50 border-blue-100 text-blue-700' },
                                { label: 'Technologies', value: stats.totalTechnologies, icon: '⚙️', color: 'bg-purple-50 border-purple-100 text-purple-700' },
                                { label: 'Skills', value: stats.totalSkills, icon: '💡', color: 'bg-orange-50 border-orange-100 text-orange-700' },
                            ].map(({ label, value, icon, color }) => (
                                <div key={label} className={`border rounded-2xl p-4 flex items-center gap-3 ${color}`}>
                                    <span className="text-2xl">{icon}</span>
                                    <div>
                                        <p className="text-2xl font-black leading-none">{fmt(value)}</p>
                                        <p className="text-xs font-semibold mt-0.5 opacity-80">{label}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* ─── Quick Links ────────────────────────────────────────────── */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <SectionHeader
                    icon="🚀"
                    title="Quick Actions"
                    subtitle="Navigate to key sections of the platform"
                />
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                    {[
                        { to: '/dashboard', icon: '📊', label: 'Student Dashboard', desc: 'View student perspective', color: 'from-indigo-50 to-indigo-100 border-indigo-200 text-indigo-700' },
                        { to: '/assessments', icon: '📝', label: 'Assessments', desc: 'Browse quiz system', color: 'from-amber-50 to-amber-100 border-amber-200 text-amber-700' },
                        { to: '/recommendations', icon: '🎯', label: 'Recommendations', desc: 'View project catalog', color: 'from-emerald-50 to-emerald-100 border-emerald-200 text-emerald-700' },
                        { to: '/profile', icon: '👤', label: 'Student Profile', desc: 'View profile editor', color: 'from-violet-50 to-violet-100 border-violet-200 text-violet-700' },
                    ].map(({ to, icon, label, desc, color }) => (
                        <Link
                            key={to}
                            to={to}
                            className={`bg-gradient-to-br ${color} border rounded-2xl p-4 flex flex-col items-center text-center gap-2 hover:shadow-md transition-all hover:scale-[1.02] active:scale-95`}
                        >
                            <span className="text-3xl">{icon}</span>
                            <div>
                                <p className="text-sm font-bold leading-snug">{label}</p>
                                <p className="text-[11px] opacity-70 mt-0.5">{desc}</p>
                            </div>
                        </Link>
                    ))}
                </div>
            </div>

            {/* ─── System Info ────────────────────────────────────────────── */}
            <div className="bg-slate-900 rounded-2xl p-6 text-white">
                <div className="flex items-center gap-2 mb-4">
                    <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                    <p className="text-sm font-bold text-emerald-400">System Status: Online</p>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs text-slate-400">
                    <div>
                        <p className="text-slate-500 font-medium uppercase tracking-wider mb-1">Backend</p>
                        <p className="text-slate-200 font-semibold">Spring Boot 3.5</p>
                        <p>Port 8080</p>
                    </div>
                    <div>
                        <p className="text-slate-500 font-medium uppercase tracking-wider mb-1">Database</p>
                        <p className="text-slate-200 font-semibold">MySQL 8.x</p>
                        <p>JPA / Hibernate</p>
                    </div>
                    <div>
                        <p className="text-slate-500 font-medium uppercase tracking-wider mb-1">AI Integration</p>
                        <p className="text-slate-200 font-semibold">Google Gemini</p>
                        <p>gemini-3.5-flash</p>
                    </div>
                    <div>
                        <p className="text-slate-500 font-medium uppercase tracking-wider mb-1">Frontend</p>
                        <p className="text-slate-200 font-semibold">React + Vite</p>
                        <p>Tailwind CSS</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;
