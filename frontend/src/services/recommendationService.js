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
    },

    /**
     * Generate fresh, personalized project ideas via the Gemini AI backend.
     * The Gemini API key is never sent to or visible in the browser.
     * @param {number} count - Number of ideas to generate (1–5, default 3)
     */
    generateFresh: async (count = 3) => {
        return api.post('/recommendations/generate-fresh', { count });
    }
};

export default recommendationService;
