import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import studentService from '../services/studentService';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const PROFICIENCY_LEVELS = [
    'BEGINNER',
    'INTERMEDIATE',
    'ADVANCED',
    'EXPERT'
];

const proficiencyColor = {
    BEGINNER: 'bg-slate-100 text-slate-700',
    INTERMEDIATE: 'bg-blue-100 text-blue-700',
    ADVANCED: 'bg-indigo-100 text-indigo-700',
    EXPERT: 'bg-violet-100 text-violet-700',
};

// ─── Toast ─────────────────────────────────────────────────────────────────────

const Toast = ({ message, type, onClose }) => {
    useEffect(() => {
        const t = setTimeout(onClose, 3500);
        return () => clearTimeout(t);
    }, [onClose]);

    return (
        <div
            className={`fixed top-5 right-5 z-50 flex items-center gap-3 px-5 py-3 rounded-xl shadow-lg text-sm font-medium transition-all
            ${type === 'success'
                    ? 'bg-green-50 border border-green-200 text-green-800'
                    : 'bg-red-50 border border-red-200 text-red-800'
                }`}
        >
            {type === 'success' ? (
                <svg
                    className="w-5 h-5 text-green-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M5 13l4 4L19 7"
                    />
                </svg>
            ) : (
                <svg
                    className="w-5 h-5 text-red-500"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M6 18L18 6M6 6l12 12"
                    />
                </svg>
            )}

            <span>{message}</span>

            <button
                type="button"
                onClick={onClose}
                className="ml-2 opacity-60 hover:opacity-100"
            >
                ✕
            </button>
        </div>
    );
};

// ─── Section Card ──────────────────────────────────────────────────────────────

const SectionCard = ({ title, icon, children }) => (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center gap-3">
            <span className="text-xl">{icon}</span>
            <h2 className="text-base font-semibold text-gray-800">
                {title}
            </h2>
        </div>

        <div className="p-6">
            {children}
        </div>
    </div>
);

// ─── Field Row ────────────────────────────────────────────────────────────────

const FieldRow = ({ label, value, fallback = '—' }) => (
    <div className="flex flex-col sm:flex-row sm:items-start gap-1 sm:gap-4 py-2.5 border-b border-gray-50 last:border-0">
        <span className="text-sm font-medium text-gray-500 sm:w-44 shrink-0">
            {label}
        </span>

        <span className="text-sm text-gray-900 break-words">
            {value || fallback}
        </span>
    </div>
);

// ─── Form Input ───────────────────────────────────────────────────────────────

const FormInput = ({ label, id, ...props }) => (
    <div>
        <label
            htmlFor={id}
            className="block text-sm font-medium text-gray-700 mb-1"
        >
            {label}
        </label>

        <input
            id={id}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-colors"
            {...props}
        />
    </div>
);

// ─── Form Select ──────────────────────────────────────────────────────────────

const FormSelect = ({ label, id, children, ...props }) => (
    <div>
        <label
            htmlFor={id}
            className="block text-sm font-medium text-gray-700 mb-1"
        >
            {label}
        </label>

        <select
            id={id}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white transition-colors disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed"
            {...props}
        >
            {children}
        </select>
    </div>
);

// ─── Form Textarea ────────────────────────────────────────────────────────────

const FormTextarea = ({ label, id, ...props }) => (
    <div>
        <label
            htmlFor={id}
            className="block text-sm font-medium text-gray-700 mb-1"
        >
            {label}
        </label>

        <textarea
            id={id}
            rows={4}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-none transition-colors"
            {...props}
        />
    </div>
);

// ─── Skill Form ───────────────────────────────────────────────────────────────

const SkillForm = ({ skills, onAdd }) => {
    const [skillSource, setSkillSource] = useState('existing');
    const [selectedSkillId, setSelectedSkillId] = useState('');
    const [customSkillName, setCustomSkillName] = useState('');
    const [proficiency, setProficiency] = useState('BEGINNER');
    const [years, setYears] = useState('0');

    const handleAdd = () => {
        if (skillSource === 'existing' && !selectedSkillId) {
            return;
        }

        if (skillSource === 'new' && !customSkillName.trim()) {
            return;
        }

        const entry = {
            proficiencyLevel: proficiency,
            yearsOfExperience: parseFloat(years) || 0,
        };

        if (skillSource === 'existing') {
            const found = skills.find(
                (s) => String(s.id) === selectedSkillId
            );

            if (!found) {
                return;
            }

            entry.skillId = found.id;
            entry.skillName = found.name;
        } else {
            entry.skillName = customSkillName.trim();
        }

        onAdd(entry);

        setSelectedSkillId('');
        setCustomSkillName('');
        setProficiency('BEGINNER');
        setYears('0');
    };

    return (
        <div className="border border-dashed border-indigo-200 rounded-xl p-4 bg-indigo-50/40 space-y-3">
            <p className="text-xs font-semibold text-indigo-600 uppercase tracking-wider">
                Add a Skill
            </p>

            <div className="flex gap-2">
                {['existing', 'new'].map((src) => (
                    <button
                        key={src}
                        type="button"
                        onClick={() => setSkillSource(src)}
                        className={`px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors
                            ${skillSource === src
                                ? 'bg-indigo-600 text-white border-indigo-600'
                                : 'bg-white text-gray-600 border-gray-300 hover:border-indigo-400'
                            }`}
                    >
                        {src === 'existing'
                            ? 'From list'
                            : 'Type new'}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {skillSource === 'existing' ? (
                    <FormSelect
                        id="skill-select"
                        label="Skill"
                        value={selectedSkillId}
                        onChange={(e) =>
                            setSelectedSkillId(e.target.value)
                        }
                    >
                        <option value="">— Select skill —</option>

                        {skills.map((s) => (
                            <option
                                key={s.id}
                                value={s.id}
                            >
                                {s.name}
                            </option>
                        ))}
                    </FormSelect>
                ) : (
                    <FormInput
                        id="custom-skill"
                        label="Skill Name"
                        type="text"
                        placeholder="e.g. React, Python, Docker…"
                        value={customSkillName}
                        onChange={(e) =>
                            setCustomSkillName(e.target.value)
                        }
                    />
                )}

                <FormSelect
                    id="proficiency"
                    label="Proficiency"
                    value={proficiency}
                    onChange={(e) =>
                        setProficiency(e.target.value)
                    }
                >
                    {PROFICIENCY_LEVELS.map((level) => (
                        <option key={level} value={level}>
                            {level.charAt(0) +
                                level.slice(1).toLowerCase()}
                        </option>
                    ))}
                </FormSelect>

                <FormInput
                    id="years-exp"
                    label="Years of Experience"
                    type="number"
                    min="0"
                    max="50"
                    step="0.5"
                    value={years}
                    onChange={(e) =>
                        setYears(e.target.value)
                    }
                />

                <div className="flex items-end">
                    <button
                        type="button"
                        onClick={handleAdd}
                        className="w-full px-4 py-2.5 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors"
                    >
                        + Add Skill
                    </button>
                </div>
            </div>
        </div>
    );
};

// ─── Skills List ──────────────────────────────────────────────────────────────

const SkillsList = ({ skillEntries, onRemove }) => {
    if (!skillEntries || skillEntries.length === 0) {
        return (
            <p className="text-sm text-gray-400 italic py-2">
                No skills added yet.
            </p>
        );
    }

    return (
        <div className="flex flex-wrap gap-2">
            {skillEntries.map((entry, idx) => (
                <div
                    key={idx}
                    className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium ${proficiencyColor[entry.proficiencyLevel] ||
                        'bg-gray-100 text-gray-700'
                        }`}
                >
                    <span>{entry.skillName}</span>

                    <span className="opacity-60">·</span>

                    <span>
                        {entry.proficiencyLevel
                            ? entry.proficiencyLevel.charAt(0) +
                            entry.proficiencyLevel
                                .slice(1)
                                .toLowerCase()
                            : '—'}
                    </span>

                    <span className="opacity-60">·</span>

                    <span>
                        {entry.yearsOfExperience ?? 0}y
                    </span>

                    {onRemove && (
                        <button
                            type="button"
                            onClick={() => onRemove(idx)}
                            className="ml-1 opacity-50 hover:opacity-100 transition-opacity text-xs"
                            aria-label="Remove skill"
                        >
                            ✕
                        </button>
                    )}
                </div>
            ))}
        </div>
    );
};

// ─── Profile Form ─────────────────────────────────────────────────────────────

const ProfileForm = ({
    initialData,
    branches,
    domains,
    onBranchChange,
    onSubmit,
    isCreating,
    isSubmitting
}) => {
    const initialBranchId =
        initialData?.branchId ||
        initialData?.branch?.id ||
        '';

    const initialDomainId =
        initialData?.domainId ||
        initialData?.domain?.id ||
        '';

    const [form, setForm] = useState({
        branchId: initialBranchId,
        domainId: initialDomainId,
        teamSize: initialData?.teamSize || 2,
        availableTimeWeeks:
            initialData?.availableTimeWeeks || 8,
        previousExperience:
            initialData?.previousExperience || '',
        interests: initialData?.interests || '',
    });

    const [skillEntries, setSkillEntries] = useState(
        initialData?.skills?.map((s) => ({
            skillId: s.skillId,
            skillName: s.skillName,
            proficiencyLevel: s.proficiencyLevel,
            yearsOfExperience: s.yearsOfExperience,
        })) || []
    );

    // Technology cascade state
    const [technologies, setTechnologies] = useState([]);
    const [selectedTechnologyId, setSelectedTechnologyId] = useState('');
    const [allSkills, setAllSkills] = useState([]);
    const [isLoadingTechs, setIsLoadingTechs] = useState(false);
    const [isLoadingSkills, setIsLoadingSkills] = useState(false);

    // Photo state
    const [photoPreview, setPhotoPreview] = useState(initialData?.photoUrl || null);
    const [photoFile, setPhotoFile] = useState(null);
    const [removePhoto, setRemovePhoto] = useState(false);

    // On mount (edit mode): if a domain is already set, load its technologies.
    useEffect(() => {
        if (initialDomainId) {
            setIsLoadingTechs(true);
            studentService.getTechnologiesByDomain(initialDomainId)
                .then(res => setTechnologies(res.data || []))
                .catch(() => setTechnologies([]))
                .finally(() => setIsLoadingTechs(false));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Load skills whenever selectedTechnologyId changes
    useEffect(() => {
        if (selectedTechnologyId) {
            setIsLoadingSkills(true);
            studentService.getSkillsByTechnology(selectedTechnologyId)
                .then(res => setAllSkills(res.data || []))
                .catch(() => setAllSkills([]))
                .finally(() => setIsLoadingSkills(false));
        } else {
            setAllSkills([]);
        }
    }, [selectedTechnologyId]);

    const handlePhotoSelect = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        if (file.size > 5 * 1024 * 1024) {
            alert('File is too large. Maximum allowed size is 5 MB.');
            return;
        }

        setPhotoFile(file);
        setPhotoPreview(URL.createObjectURL(file));
        setRemovePhoto(false);
    };

    const handlePhotoRemove = () => {
        setPhotoFile(null);
        setPhotoPreview(null);
        setRemovePhoto(true);
    };

    const set = (field) => (e) => {
        setForm((prev) => ({
            ...prev,
            [field]: e.target.value,
        }));
    };

    const addSkill = (entry) => {
        const isDuplicate = skillEntries.some(
            (s) =>
                s.skillName?.toLowerCase() ===
                entry.skillName?.toLowerCase()
        );

        if (!isDuplicate) {
            setSkillEntries((prev) => [
                ...prev,
                entry,
            ]);
        }
    };

    const removeSkill = (idx) => {
        setSkillEntries((prev) =>
            prev.filter((_, i) => i !== idx)
        );
    };

    // Branch change → clear domain, technology, skills
    const handleBranchSelection = (e) => {
        const branchId = e.target.value;

        setForm((prev) => ({
            ...prev,
            branchId,
            domainId: '',
        }));

        setTechnologies([]);
        setSelectedTechnologyId('');
        setAllSkills([]);
        setSkillEntries([]);

        onBranchChange(branchId);
    };

    // Domain change → load technologies, clear technology + skills
    const handleDomainSelection = (e) => {
        const domainId = e.target.value;

        setForm((prev) => ({
            ...prev,
            domainId,
        }));

        setSelectedTechnologyId('');
        setAllSkills([]);
        setSkillEntries([]);

        if (!domainId) {
            setTechnologies([]);
            return;
        }

        setIsLoadingTechs(true);
        studentService.getTechnologiesByDomain(domainId)
            .then(res => setTechnologies(res.data || []))
            .catch(() => setTechnologies([]))
            .finally(() => setIsLoadingTechs(false));
    };

    // Technology change → clear skill entries (skills reload via useEffect)
    const handleTechnologySelection = (e) => {
        const technologyId = e.target.value;
        setSelectedTechnologyId(technologyId);
        setSkillEntries([]);
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!form.branchId) {
            return;
        }

        if (!form.domainId) {
            return;
        }

        onSubmit({
            profile: {
                branchId: Number(form.branchId),
                domainId: Number(form.domainId),
                teamSize: Number(form.teamSize),
                availableTimeWeeks: Number(
                    form.availableTimeWeeks
                ),
                previousExperience:
                    form.previousExperience,
                interests: form.interests,
            },
            skills: skillEntries,
            photoFile,
            removePhoto
        });
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="space-y-6"
        >
            {/* Photo Section */}
            <SectionCard title="Profile Photo" icon="📷">
                <div className="flex items-center gap-6">
                    <div className="h-24 w-24 shrink-0 rounded-full bg-gray-100 overflow-hidden border border-gray-200 flex items-center justify-center">
                        {photoPreview ? (
                            <img src={photoPreview} alt="Profile preview" className="h-full w-full object-cover" />
                        ) : (
                            <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                            </svg>
                        )}
                    </div>

                    <div className="space-y-3 flex-1">
                        <div className="flex gap-3">
                            <label className="cursor-pointer bg-white px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors focus-within:ring-2 focus-within:ring-indigo-500">
                                <span>Select Image</span>
                                <input type="file" className="sr-only" accept=".jpg,.jpeg,.png,.webp" onChange={handlePhotoSelect} />
                            </label>

                            {(photoPreview || photoFile) && (
                                <button type="button" onClick={handlePhotoRemove} className="px-4 py-2 text-sm font-medium text-red-600 bg-white border border-red-200 rounded-md hover:bg-red-50 transition-colors">
                                    Remove
                                </button>
                            )}
                        </div>
                        <p className="text-xs text-gray-500">
                            JPG, PNG, or WEBP. Max 5MB.
                        </p>
                    </div>
                </div>
            </SectionCard>

            {/* Education */}
            <SectionCard title="Education" icon="🎓">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

                    {/* Engineering Branch */}
                    <FormSelect
                        id="branch"
                        label="Engineering Branch *"
                        value={form.branchId}
                        onChange={handleBranchSelection}
                        required
                    >
                        <option value="">
                            — Select branch —
                        </option>

                        {branches.map((branch) => (
                            <option
                                key={branch.id}
                                value={branch.id}
                            >
                                {branch.name}
                            </option>
                        ))}
                    </FormSelect>

                    {/* Project Domain */}
                    <FormSelect
                        id="domain"
                        label="Project Domain *"
                        value={form.domainId}
                        onChange={handleDomainSelection}
                        disabled={!form.branchId}
                        required
                    >
                        <option value="">
                            {form.branchId
                                ? '— Select domain —'
                                : '— Select branch first —'}
                        </option>

                        {domains.map((domain) => (
                            <option
                                key={domain.id}
                                value={domain.id}
                            >
                                {domain.name}
                            </option>
                        ))}
                    </FormSelect>

                    {/* Technology / Stack */}
                    <FormSelect
                        id="technology"
                        label={isLoadingTechs ? 'Technology / Stack (loading…)' : 'Technology / Stack'}
                        value={selectedTechnologyId}
                        onChange={handleTechnologySelection}
                        disabled={!form.domainId || isLoadingTechs}
                    >
                        <option value="">
                            {!form.domainId
                                ? '— Select domain first —'
                                : isLoadingTechs
                                    ? 'Loading…'
                                    : technologies.length === 0
                                        ? '— No technologies mapped —'
                                        : '— Select technology —'}
                        </option>

                        {technologies.map((tech) => (
                            <option
                                key={tech.id}
                                value={tech.id}
                            >
                                {tech.name}
                            </option>
                        ))}
                    </FormSelect>
                </div>
            </SectionCard>

            {/* Project Preferences */}
            <SectionCard
                title="Project Preferences"
                icon="⚙️"
            >
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

                    {/* Team Size */}
                    <div>
                        <label
                            htmlFor="team-size"
                            className="block text-sm font-medium text-gray-700 mb-1"
                        >
                            Preferred Team Size *
                        </label>

                        <input
                            id="team-size"
                            type="range"
                            min={1}
                            max={20}
                            value={form.teamSize}
                            onChange={set('teamSize')}
                            className="w-full accent-indigo-600"
                        />

                        <p className="text-center text-indigo-600 font-semibold mt-1">
                            {form.teamSize}{' '}
                            {Number(form.teamSize) === 1
                                ? 'person'
                                : 'people'}
                        </p>
                    </div>

                    {/* Available Time */}
                    <div>
                        <label
                            htmlFor="avail-weeks"
                            className="block text-sm font-medium text-gray-700 mb-1"
                        >
                            Available Development Time *
                        </label>

                        <input
                            id="avail-weeks"
                            type="range"
                            min={1}
                            max={52}
                            value={form.availableTimeWeeks}
                            onChange={set(
                                'availableTimeWeeks'
                            )}
                            className="w-full accent-indigo-600"
                        />

                        <p className="text-center text-indigo-600 font-semibold mt-1">
                            {form.availableTimeWeeks}{' '}
                            {Number(
                                form.availableTimeWeeks
                            ) === 1
                                ? 'week'
                                : 'weeks'}
                        </p>
                    </div>
                </div>
            </SectionCard>

            {/* Experience & Interests */}
            <SectionCard
                title="Experience & Interests"
                icon="💡"
            >
                <div className="space-y-4">

                    <FormTextarea
                        id="prev-exp"
                        label="Previous Experience"
                        placeholder="Describe any past projects, internships, or relevant work…"
                        value={form.previousExperience}
                        onChange={set(
                            'previousExperience'
                        )}
                    />

                    <FormTextarea
                        id="interests"
                        label="Interests"
                        placeholder="What topics, technologies or domains excite you most?"
                        value={form.interests}
                        onChange={set('interests')}
                    />
                </div>
            </SectionCard>

            {/* Skills */}
            <SectionCard title="Skills" icon="🛠️">
                <div className="space-y-4">
                    {!selectedTechnologyId && (
                        <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
                            ⚠️ Select a Technology / Stack above to load available skills.
                        </p>
                    )}

                    {isLoadingSkills && (
                        <p className="text-sm text-indigo-500 italic">Loading skills…</p>
                    )}

                    <SkillsList
                        skillEntries={skillEntries}
                        onRemove={removeSkill}
                    />

                    <SkillForm
                        skills={allSkills}
                        onAdd={addSkill}
                    />
                </div>
            </SectionCard>

            {/* Actions */}
            <div className="flex flex-col sm:flex-row gap-3 justify-end pt-2">
                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="px-6 py-2.5 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-70 disabled:cursor-not-allowed transition-colors"
                >
                    {isSubmitting
                        ? isCreating
                            ? 'Creating…'
                            : 'Saving…'
                        : isCreating
                            ? 'Create Profile'
                            : 'Save Changes'}
                </button>
            </div>
        </form>
    );
};

// ─── Profile View ─────────────────────────────────────────────────────────────

const ProfileView = ({ profile, onEdit }) => (
    <div className="space-y-6">

        {/* Header */}
        <div className="bg-gradient-to-br from-indigo-600 to-violet-600 rounded-xl p-6 text-white shadow-md">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div className="flex items-center gap-4">
                    <div className="h-16 w-16 rounded-full bg-white/20 border-2 border-white/50 overflow-hidden flex items-center justify-center shrink-0">
                        {profile.photoUrl ? (
                            <img src={profile.photoUrl} alt={profile.username} className="h-full w-full object-cover" />
                        ) : (
                            <span className="text-2xl font-bold uppercase">{profile.username?.charAt(0)}</span>
                        )}
                    </div>
                    <div>
                        <p className="text-indigo-200 text-sm font-medium mb-1">
                            Student Profile
                        </p>
    
                        <h2 className="text-2xl font-bold">
                            {profile.username}
                        </h2>
    
                        <p className="text-indigo-200 text-sm mt-1">
                            {profile.branchName || profile.branch?.name || '—'}
                            {' · '}
                            {profile.domainName || profile.domain?.name || '—'}
                        </p>
                    </div>
                </div>

                <button
                    type="button"
                    onClick={onEdit}
                    className="self-start sm:self-auto flex items-center gap-2 px-4 py-2 text-sm font-medium bg-white/15 hover:bg-white/25 rounded-lg border border-white/30 transition-colors"
                >
                    <svg
                        className="w-4 h-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                        />
                    </svg>

                    Edit Profile
                </button>
            </div>
        </div>

        {/* Details */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

            <SectionCard
                title="Education"
                icon="🎓"
            >
                <FieldRow
                    label="Branch"
                    value={
                        profile.branchName ||
                        profile.branch?.name
                    }
                />

                <FieldRow
                    label="Domain"
                    value={
                        profile.domainName ||
                        profile.domain?.name
                    }
                />
            </SectionCard>

            <SectionCard
                title="Project Preferences"
                icon="⚙️"
            >
                <FieldRow
                    label="Team Size"
                    value={`${profile.teamSize || 0} ${profile.teamSize === 1
                            ? 'person'
                            : 'people'
                        }`}
                />

                <FieldRow
                    label="Available Time"
                    value={`${profile.availableTimeWeeks || 0} ${profile.availableTimeWeeks === 1
                            ? 'week'
                            : 'weeks'
                        }`}
                />
            </SectionCard>
        </div>

        <SectionCard
            title="Experience & Interests"
            icon="💡"
        >
            <FieldRow
                label="Previous Experience"
                value={profile.previousExperience}
            />

            <FieldRow
                label="Interests"
                value={profile.interests}
            />
        </SectionCard>

        <SectionCard
            title="Skills"
            icon="🛠️"
        >
            {profile.skills &&
                profile.skills.length > 0 ? (
                <SkillsList
                    skillEntries={profile.skills}
                    onRemove={null}
                />
            ) : (
                <p className="text-sm text-gray-400 italic">
                    No skills added yet. Edit your profile to add
                    skills.
                </p>
            )}
        </SectionCard>

        <p className="text-xs text-gray-400 text-right">
            Last updated:{' '}
            {profile.updatedAt
                ? new Date(
                    profile.updatedAt
                ).toLocaleString()
                : '—'}
        </p>
    </div>
);

// ─── Main Page ────────────────────────────────────────────────────────────────

const StudentProfile = () => {
    const { user } = useContext(AuthContext);

    const [mode, setMode] = useState('loading');
    const [profile, setProfile] = useState(null);

    const [branches, setBranches] = useState([]);
    const [domains, setDomains] = useState([]);

    const [toast, setToast] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoadingDomains, setIsLoadingDomains] =
        useState(false);

    const showToast = (message, type = 'success') => {
        setToast({
            message,
            type
        });
    };

    // ─── Load branches + existing profile ─────────────────────────────────────

    useEffect(() => {
        const loadData = async () => {
            // Load branches
            try {
                const branchRes =
                    await studentService.getBranches();

                setBranches(branchRes.data || []);
            } catch (err) {
                console.error(
                    'Failed to load branches:',
                    err
                );

                showToast(
                    'Failed to load engineering branches',
                    'error'
                );
            }

            // Load current profile
            try {
                const profileRes =
                    await studentService.getMyProfile();

                const existingProfile =
                    profileRes.data;

                setProfile(existingProfile);

                // Support both possible response shapes
                const branchId =
                    existingProfile?.branchId ||
                    existingProfile?.branch?.id;

                // Load only domains for saved branch
                if (branchId) {
                    try {
                        setIsLoadingDomains(true);

                        const domainRes =
                            await studentService.getDomainsByBranch(
                                branchId
                            );

                        setDomains(
                            domainRes.data || []
                        );
                    } catch (err) {
                        console.error(
                            'Failed to load domains:',
                            err
                        );

                        showToast(
                            'Failed to load project domains',
                            'error'
                        );
                    } finally {
                        setIsLoadingDomains(false);
                    }
                }

                setMode('view');
            } catch (err) {
                if (
                    err.response?.status === 400 ||
                    err.response?.status === 404
                ) {
                    setMode('create');
                } else {
                    console.error(
                        'Failed to load profile:',
                        err
                    );

                    showToast(
                        'Failed to load profile',
                        'error'
                    );

                    setMode('create');
                }
            }
        };

        loadData();
    }, []);

    // ─── Branch → Domain ──────────────────────────────────────────────────────

    const handleBranchChange = async (branchId) => {
        // Clear old domains immediately
        setDomains([]);

        if (!branchId) {
            return;
        }

        try {
            setIsLoadingDomains(true);

            const response =
                await studentService.getDomainsByBranch(
                    branchId
                );

            setDomains(response.data || []);
        } catch (err) {
            console.error(
                'Failed to load domains for branch:',
                err
            );

            showToast(
                'Failed to load project domains',
                'error'
            );
        } finally {
            setIsLoadingDomains(false);
        }
    };

    // ─── Create / Update ──────────────────────────────────────────────────────

    const handleCreateOrUpdate = async ({
        profile: profileData,
        skills,
        photoFile,
        removePhoto
    }) => {
        setIsSubmitting(true);

        try {
            let savedProfile;

            if (mode === 'create') {
                const res =
                    await studentService.createProfile(
                        profileData
                    );

                savedProfile = res.data;

                showToast(
                    'Profile created successfully! 🎉'
                );
            } else {
                const res =
                    await studentService.updateProfile(
                        profile.id,
                        profileData
                    );

                savedProfile = res.data;

                showToast(
                    'Profile updated successfully!'
                );
            }

            // Save skills
            const skillsRes =
                await studentService.replaceSkills(
                    savedProfile.id,
                    {
                        skills: skills.map((s) => ({
                            skillId:
                                s.skillId || null,
                            skillName:
                                s.skillName || null,
                            proficiencyLevel:
                                s.proficiencyLevel,
                            yearsOfExperience:
                                s.yearsOfExperience,
                        })),
                    }
                );

            let finalProfile = skillsRes.data;

            // Handle photo
            if (removePhoto) {
                const photoRes = await studentService.deletePhoto(finalProfile.id);
                finalProfile = photoRes.data;
            } else if (photoFile) {
                const formData = new FormData();
                formData.append('photo', photoFile);
                const photoRes = await studentService.uploadPhoto(finalProfile.id, formData);
                finalProfile = photoRes.data;
            }

            setProfile(finalProfile);
            setMode('view');
        } catch (err) {
            console.error(
                'Profile save error:',
                err
            );

            const msg =
                err.response?.data?.error ||
                err.response?.data?.message ||
                Object.values(
                    err.response?.data || {}
                ).join(', ') ||
                'Something went wrong';

            showToast(msg, 'error');
        } finally {
            setIsSubmitting(false);
        }
    };

    // ─── Render ────────────────────────────────────────────────────────────────

    return (
        <div className="max-w-4xl mx-auto space-y-6">

            {toast && (
                <Toast
                    message={toast.message}
                    type={toast.type}
                    onClose={() => setToast(null)}
                />
            )}

            {/* Header */}
            <header className="border-b border-gray-200 pb-4">
                <h1 className="text-3xl font-bold text-gray-900">
                    {mode === 'create'
                        ? 'Create Your Profile'
                        : 'My Profile'}
                </h1>

                {mode === 'create' && (
                    <p className="mt-1 text-sm text-gray-500">
                        Tell us about yourself so we can
                        match you to the right projects.
                    </p>
                )}

                {mode === 'edit' && (
                    <p className="mt-1 text-sm text-gray-500">
                        Update your profile details and
                        skills below.
                    </p>
                )}
            </header>

            {/* Loading */}
            {mode === 'loading' && (
                <div className="flex items-center justify-center py-24">
                    <div className="flex flex-col items-center gap-4 text-gray-400">

                        <svg
                            className="animate-spin h-8 w-8 text-indigo-600"
                            xmlns="http://www.w3.org/2000/svg"
                            fill="none"
                            viewBox="0 0 24 24"
                        >
                            <circle
                                className="opacity-25"
                                cx="12"
                                cy="12"
                                r="10"
                                stroke="currentColor"
                                strokeWidth="4"
                            />

                            <path
                                className="opacity-75"
                                fill="currentColor"
                                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                            />
                        </svg>

                        <span className="text-sm font-medium">
                            Loading profile…
                        </span>
                    </div>
                </div>
            )}

            {/* Create / Edit */}
            {(mode === 'create' ||
                mode === 'edit') && (
                    <div className="space-y-4">

                        {mode === 'edit' && (
                            <div className="flex justify-end">
                                <button
                                    type="button"
                                    onClick={() =>
                                        setMode('view')
                                    }
                                    className="text-sm text-gray-500 hover:text-gray-700 underline underline-offset-2"
                                >
                                    ← Cancel editing
                                </button>
                            </div>
                        )}

                        <ProfileForm
                            initialData={
                                mode === 'edit'
                                    ? profile
                                    : null
                            }
                            branches={branches}
                            domains={domains}
                            onBranchChange={
                                handleBranchChange
                            }
                            isLoadingDomains={
                                isLoadingDomains
                            }
                            onSubmit={
                                handleCreateOrUpdate
                            }
                            isCreating={
                                mode === 'create'
                            }
                            isSubmitting={
                                isSubmitting
                            }
                        />
                    </div>
                )}

            {/* View */}
            {mode === 'view' &&
                profile && (
                    <ProfileView
                        profile={profile}
                        onEdit={() =>
                            setMode('edit')
                        }
                    />
                )}
        </div>
    );
};

export default StudentProfile;
