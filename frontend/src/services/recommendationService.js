import api from './api';

const recommendationService = {
    /**
     * Fetch personalized project recommendations for the authenticated student.
     * @param {boolean} refresh - Force recalculation and database update
     */
    getRecommendations: async (refresh = false) => {
        return api.get(`/recommendations?refresh=${refresh}`);
    },

    /**
     * Fetch detailed recommendation dossier for a specific project.
     * @param {number|string} projectId 
     */
    getProjectRecommendation: async (projectId) => {
        return api.get(`/recommendations/${projectId}`);
    }
};

export default recommendationService;
