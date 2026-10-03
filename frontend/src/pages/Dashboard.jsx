import React, { useContext, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import studentService from '../services/studentService';
import assessmentService from '../services/assessmentService';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const levelColor = (level) => {
    if (level === 'ADVANCED')     return 'bg-green-100 text-green-700 border-green-200';
    if (level === 'INTERMEDIATE') return 'bg-blue-100 text-blue-700 border-blue-200';
    return 'bg-gray-100 text-gray-600 border-gray-200';
};

const profColor = {
    BEGINNER:     'bg-slate-100 text-slate-700',
    INTERMEDIATE: 'bg-blue-100 text-blue-700',
    ADVANCED:     'bg-indigo-100 text-indigo-700',
    EXPERT:       'bg-violet-100 text-violet-700',
};

// ─── Skeleton ─────────────────────────────────────────────────────────────────

const Skeleton = ({ className = '' }) => (
    <div className={`animate-pulse bg-gray-200 rounded-lg ${className}`} />
);

// ─── Stat Card ────────────────────────────────────────────────────────────────

const StatCard = ({ label, value, sub, color = 'indigo', icon }) => {
    const colors = {
        indigo: 'from-indigo-500 to-indigo-600',
        violet: 'from-violet-500 to-violet-600',
        emerald: 'from-emerald-500 to-emerald-600',
        amber:   'from-amber-400 to-amber-500',
    };

    return (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex items-center gap-4 hover:shadow-md transition-shadow">
            <div className={`h-12 w-12 rounded-xl bg-gradient-to-br ${colors[color]} flex items-center justify-center shrink-0 shadow-sm`}>
                <span className="text-white text-xl">{icon}</span>
            </div>
            <div className="min-w-0">
                <p className="text-2xl font-bold text-gray-900 leading-tight">{value}</p>
                <p className="text-sm font-medium text-gray-500 truncate">{label}</p>
                {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
            </div>
        </div>
    );
};

// ─── Section Card ─────────────────────────────────────────────────────────────

const SectionCard = ({ title, icon, action, children }) => (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-50 flex items-center justify-between">
            <div className="flex items-center gap-2">
                <span className="text-lg">{icon}</span>
                <h2 className="text-base font-semibold text-gray-800">{title}</h2>
            </div>
            {action}
        </div>
        <div className="p-6">{children}</div>
    </div>
);

// ─── Profile Completion ───────────────────────────────────────────────────────

const getProfileCompletion = (profile) => {
    if (!profile) return 0;
    const checks = [
        Boolean(profile.branchId || profile.branch),
        Boolean(profile.domainId || profile.domain),
        Boolean(profile.previousExperience),
        Boolean(profile.interests),
        Boolean(profile.photoUrl),
        (profile.skills?.length || 0) > 0,
    ];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
};

// ─── Main Dashboard ───────────────────────────────────────────────────────────

const Dashboard = () => {
    const { user } = useContext(AuthContext);

    const [profile,       setProfile]       = useState(null);
    const [recentResults, setRecentResults] = useState([]);
    const [profileLoading, setProfileLoading] = useState(true);
    const [assessLoading,  setAssessLoading]  = useState(true);
    const [hasProfile,    setHasProfile]    = useState(null); // null = unknown

    useEffect(() => {
        // Load profile
        studentService.getMyProfile()
            .then(res => {
                setProfile(res.data);
                setHasProfile(true);
            })
            .catch(err => {
                if (err.response?.status === 400 || err.response?.status === 404) {
                    setHasProfile(false);
                }
            })
            .finally(() => setProfileLoading(false));

        // Load last 3 assessment results
        assessmentService.getMyResults(0, 3)
            .then(res => {
                const data = res.data || {};
                setRecentResults(Array.isArray(data.content) ? data.content : []);
            })
            .catch(() => setRecentResults([]))
            .finally(() => setAssessLoading(false));
    }, []);

    const completion = getProfileCompletion(profile);

    const hour = new Date().getHours();
    const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

    return (
        <div className="max-w-6xl mx-auto space-y-8">

            {/* ─── Hero Header ──────────────────────────────────────────────── */}
            <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-600 via-indigo-700 to-violet-700 p-8 text-white shadow-xl shadow-indigo-200">
                {/* Decorative blobs */}
                <div className="absolute -top-10 -right-10 h-40 w-40 rounded-full bg-white/10 blur-2xl pointer-events-none" />
                <div className="absolute bottom-0 left-1/3 h-32 w-32 rounded-full bg-violet-400/20 blur-2xl pointer-events-none" />

                <div className="relative flex flex-col sm:flex-row sm:items-center sm:justify-between gap-6">
                    <div>
                        <p className="text-indigo-200 text-sm font-medium mb-1">{greeting} 👋</p>
                        <h1 className="text-3xl font-bold">{user?.username}</h1>
                        <p className="text-indigo-200 text-sm mt-2">
                            {hasProfile === false
                                ? 'Set up your profile to get started with assessments and project matching.'
                                : profile
                                    ? `${profile.branchName || profile.branch?.name || 'No branch'} · ${profile.domainName || profile.domain?.name || 'No domain'}`
                                    : 'Loading your profile…'}
                        </p>
                    </div>

                    <div className="flex gap-3 shrink-0">
                        <Link
                            to="/profile"
                            className="flex items-center gap-2 px-5 py-2.5 text-sm font-semibold bg-white text-indigo-700 rounded-xl hover:bg-indigo-50 shadow-md transition-colors"
                        >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                            </svg>
                            {hasProfile === false ? 'Create Profile' : 'My Profile'}
                        </Link>
                        <Link
                            to="/assessments"
                            className="flex items-center gap-2 px-5 py-2.5 text-sm font-semibold bg-white/15 text-white border border-white/30 rounded-xl hover:bg-white/25 transition-colors"
                        >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                            </svg>
                            Assessments
                        </Link>
                    </div>
                </div>
            </div>

            {/* ─── Stats Row ────────────────────────────────────────────────── */}
            {profileLoading ? (
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-24" />)}
                </div>
            ) : (
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <StatCard
                        icon="🎓"
                        label="Skills"
                        value={profile?.skills?.length ?? '—'}
                        sub="registered skills"
                        color="indigo"
                    />
                    <StatCard
                        icon="📝"
                        label="Assessments"
                        value={recentResults.length > 0 ? recentResults.length + '+' : '0'}
                        sub="taken so far"
                        color="violet"
                    />
                    <StatCard
                        icon="⏱"
                        label="Dev Time"
                        value={profile?.availableTimeWeeks ? `${profile.availableTimeWeeks}w` : '—'}
                        sub="available time"
                        color="emerald"
                    />
                    <StatCard
                        icon="👥"
                        label="Team Size"
                        value={profile?.teamSize ?? '—'}
                        sub="preferred size"
                        color="amber"
                    />
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* ─── Profile Card ─────────────────────────────────────────── */}
                <div className="lg:col-span-1">
                    <SectionCard
                        title="Profile"
                        icon="◎"
                        action={
                            <Link to="/profile" className="text-xs font-medium text-indigo-600 hover:text-indigo-800 transition-colors">
                                Edit →
                            </Link>
                        }
                    >
                        {profileLoading ? (
                            <div className="space-y-3">
                                <Skeleton className="h-4 w-3/4" />
                                <Skeleton className="h-4 w-1/2" />
                                <Skeleton className="h-2 w-full mt-4" />
                            </div>
                        ) : hasProfile === false ? (
                            <div className="text-center py-4 space-y-3">
                                <div className="h-14 w-14 mx-auto rounded-full bg-indigo-50 border-2 border-dashed border-indigo-200 flex items-center justify-center">
                                    <span className="text-2xl">👤</span>
                                </div>
                                <p className="text-sm text-gray-500">No profile yet.</p>
                                <Link
                                    to="/profile"
                                    className="inline-block px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 transition-colors"
                                >
                                    Create Profile
                                </Link>
                            </div>
                        ) : profile ? (
                            <div className="space-y-4">
                                {/* Avatar + name */}
                                <div className="flex items-center gap-3">
                                    <div className="h-12 w-12 rounded-full bg-gradient-to-br from-indigo-400 to-violet-500 flex items-center justify-center overflow-hidden shrink-0 shadow-md">
                                        {profile.photoUrl
                                            ? <img src={profile.photoUrl} alt={user?.username} className="h-full w-full object-cover" />
                                            : <span className="text-white text-lg font-bold uppercase">{user?.username?.charAt(0)}</span>
                                        }
                                    </div>
                                    <div>
                                        <p className="font-semibold text-gray-900">{user?.username}</p>
                                        <p className="text-xs text-gray-500">{profile.branchName || '—'}</p>
                                    </div>
                                </div>

                                {/* Info */}
                                <div className="space-y-1.5 text-sm">
                                    <div className="flex justify-between">
                                        <span className="text-gray-500">Domain</span>
                                        <span className="font-medium text-gray-800 text-right max-w-[160px] truncate">{profile.domainName || '—'}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-500">Level</span>
                                        <span className="font-medium text-gray-800">{profile.academicLevel || '—'}</span>
                                    </div>
                                </div>

                                {/* Completion bar */}
                                <div>
                                    <div className="flex justify-between text-xs mb-1">
                                        <span className="text-gray-500 font-medium">Profile Completion</span>
                                        <span className={`font-bold ${completion === 100 ? 'text-emerald-600' : 'text-indigo-600'}`}>{completion}%</span>
                                    </div>
                                    <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                                        <div
                                            className={`h-full rounded-full transition-all duration-700 ${completion === 100 ? 'bg-emerald-500' : 'bg-gradient-to-r from-indigo-500 to-violet-500'}`}
                                            style={{ width: `${completion}%` }}
                                        />
                                    </div>
                                </div>

                                {/* Skills preview */}
                                {profile.skills?.length > 0 && (
                                    <div>
                                        <p className="text-xs font-semibold text-gray-500 mb-2">Skills</p>
                                        <div className="flex flex-wrap gap-1.5">
                                            {profile.skills.slice(0, 6).map((s, i) => (
                                                <span key={i} className={`text-xs px-2 py-0.5 rounded-full font-medium ${profColor[s.proficiencyLevel] || 'bg-gray-100 text-gray-700'}`}>
                                                    {s.skillName}
                                                </span>
                                            ))}
                                            {profile.skills.length > 6 && (
                                                <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">
                                                    +{profile.skills.length - 6} more
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        ) : null}
                    </SectionCard>
                </div>

                {/* ─── Assessment History ───────────────────────────────────── */}
                <div className="lg:col-span-2">
                    <SectionCard
                        title="Recent Assessments"
                        icon="📋"
                        action={
                            <Link to="/assessments" className="text-xs font-medium text-indigo-600 hover:text-indigo-800 transition-colors">
                                View all →
                            </Link>
                        }
                    >
                        {assessLoading ? (
                            <div className="space-y-3">
                                {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-16" />)}
                            </div>
                        ) : recentResults.length === 0 ? (
                            <div className="text-center py-8 space-y-3">
                                <span className="text-4xl">🎯</span>
                                <p className="text-sm text-gray-500">No assessments taken yet.</p>
                                <Link
                                    to="/assessments"
                                    className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 transition-colors"
                                >
                                    Start an Assessment
                                </Link>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {recentResults.map((r) => (
                                    <div key={r.id} className="flex items-center justify-between p-4 rounded-xl bg-gray-50 border border-gray-100 hover:border-indigo-100 hover:bg-indigo-50/30 transition-colors">
                                        <div className="min-w-0">
                                            <p className="text-sm font-semibold text-gray-800 truncate">
                                                Domain Quiz — {r.skillResults?.map(s => s.skillName).join(', ') || 'Multiple skills'}
                                            </p>
                                            <p className="text-xs text-gray-400 mt-0.5">
                                                {r.createdAt ? new Date(r.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : ''}
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-3 shrink-0 ml-3">
                                            <span className="text-sm font-bold text-gray-700">
                                                {Number(r.percentage).toFixed(0)}%
                                            </span>
                                            <span className={`inline-block px-2.5 py-0.5 text-xs font-bold uppercase rounded-full border ${levelColor(r.level)}`}>
                                                {r.level}
                                            </span>
                                        </div>
                                    </div>
                                ))}

                                <Link
                                    to="/assessments"
                                    className="block w-full text-center py-2.5 text-sm font-medium text-indigo-600 border border-indigo-200 rounded-xl hover:bg-indigo-50 transition-colors mt-2"
                                >
                                    Take a new assessment
                                </Link>
                            </div>
                        )}
                    </SectionCard>
                </div>
            </div>

            {/* ─── Quick Actions ────────────────────────────────────────────── */}
            <SectionCard title="Quick Actions" icon="⚡">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {[
                        {
                            to: '/profile',
                            icon: '✏️',
                            title: hasProfile === false ? 'Create Profile' : 'Edit Profile',
                            desc: 'Update your skills, branch, and domain preferences.',
                            color: 'indigo',
                        },
                        {
                            to: '/assessments',
                            icon: '🎯',
                            title: 'Take Assessment',
                            desc: 'Evaluate your proficiency across technology stacks.',
                            color: 'violet',
                        },
                        {
                            to: '/assessments',
                            icon: '📊',
                            title: 'View History',
                            desc: 'Review past quiz results and skill-level scores.',
                            color: 'emerald',
                        },
                    ].map(({ to, icon, title, desc, color }) => {
                        const border = { indigo: 'hover:border-indigo-200', violet: 'hover:border-violet-200', emerald: 'hover:border-emerald-200' };
                        const bg = { indigo: 'hover:bg-indigo-50/50', violet: 'hover:bg-violet-50/50', emerald: 'hover:bg-emerald-50/50' };
                        const iconBg = { indigo: 'bg-indigo-100 text-indigo-600', violet: 'bg-violet-100 text-violet-600', emerald: 'bg-emerald-100 text-emerald-600' };
                        return (
                            <Link
                                key={title}
                                to={to}
                                className={`flex items-start gap-4 p-5 rounded-xl border border-gray-100 ${border[color]} ${bg[color]} transition-all group`}
                            >
                                <div className={`h-10 w-10 rounded-xl ${iconBg[color]} flex items-center justify-center shrink-0 text-xl`}>
                                    {icon}
                                </div>
                                <div>
                                    <p className="font-semibold text-gray-800 group-hover:text-gray-900">{title}</p>
                                    <p className="text-sm text-gray-500 mt-0.5 leading-snug">{desc}</p>
                                </div>
                            </Link>
                        );
                    })}
                </div>
            </SectionCard>
        </div>
    );
};

export default Dashboard;
