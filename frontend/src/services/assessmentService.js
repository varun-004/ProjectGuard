import api from './api';

const assessmentService = {
    getRelevantSkills: (technologyId = null) => {
        const params = technologyId ? `?technologyId=${technologyId}` : '';
        return api.get(`/assessments/skills${params}`);
    },

    getQuestionsForSkills: (skillIds) =>
        api.get(`/assessments/questions?skillIds=${skillIds.join(',')}`),

    submitAssessment: (skillIds, answers) =>
        api.post('/assessments/submit', { skillIds, answers }),

    getMyResults: () =>
        api.get('/assessments/my-results'),
};

export default assessmentService;
