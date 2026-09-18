import api from './api';

const studentService = {
    // ─── Profile ─────────────────────────────────────────────────────────────

    createProfile: (profileData) =>
        api.post('/students', profileData),

    getMyProfile: () =>
        api.get('/students/me'),

    getProfile: (id) =>
        api.get(`/students/${id}`),

    updateProfile: (id, profileData) =>
        api.put(`/students/${id}`, profileData),

    // ─── Skills ─────────────────────────────────────────────────────────────

    addSkills: (id, skillsData) =>
        api.post(`/students/${id}/skills`, skillsData),

    replaceSkills: (id, skillsData) =>
        api.put(`/students/${id}/skills`, skillsData),

    // ─── Metadata ───────────────────────────────────────────────────────────

    getBranches: () =>
        api.get('/students/meta/branches'),

    getDomains: () =>
        api.get('/students/meta/domains'),

    // NEW
    getDomainsByBranch: (branchId) =>
        api.get(`/students/meta/branches/${branchId}/domains`),

    getSkillsByDomain: (branchId, domainId) =>
        api.get(`/students/meta/branches/${branchId}/domains/${domainId}/skills`),

    getSkills: () =>
        api.get('/students/meta/skills'),

    // ─── Photo ──────────────────────────────────────────────────────────────

    uploadPhoto: (id, formData) =>
        api.post(`/students/${id}/photo`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        }),

    deletePhoto: (id) =>
        api.delete(`/students/${id}/photo`),
};

export default studentService;