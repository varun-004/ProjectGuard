import api from './api';

const assessmentService = {
    getRelevantSkills: (technologyId = null) => {
        const params = technologyId ? `?technologyId=${technologyId}` : '';
        return api.get(`/assessments/skills${params}`);
    },

    getQuestionsForSkills: (skillIds) =>
        api.get(`/assessments/questions?skillIds=${skillIds.join(',')}`),

    submitAssessment: (attemptId, skillIds, answers) =>
        api.post('/assessments/submit', { attemptId, skillIds, answers }),

    getMyResults: (page = 0, size = 10) =>
        api.get(`/assessments/my-results?page=${page}&size=${size}&sort=createdAt,desc`),
};

export default assessmentService;
